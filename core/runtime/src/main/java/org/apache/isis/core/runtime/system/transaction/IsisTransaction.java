/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.core.runtime.system.transaction;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.isis.applib.annotation.Bulk;
import org.apache.isis.applib.annotation.PublishedObject.ChangeKind;
import org.apache.isis.applib.clock.Clock;
import org.apache.isis.applib.services.actinvoc.ActionInvocationContext;
import org.apache.isis.applib.services.audit.AuditingService3;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.clock.ClockService;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.command.Command2;
import org.apache.isis.applib.services.command.Command3;
import org.apache.isis.applib.services.command.CommandContext;
import org.apache.isis.applib.services.command.spi.CommandService;
import org.apache.isis.applib.services.iactn.Interaction;
import org.apache.isis.applib.services.iactn.InteractionContext;
import org.apache.isis.applib.services.publish.EventSerializer;
import org.apache.isis.applib.services.publish.ObjectStringifier;
import org.apache.isis.core.commons.authentication.AuthenticationSession;
import org.apache.isis.core.commons.authentication.MessageBroker;
import org.apache.isis.core.commons.components.TransactionScopedComponent;
import org.apache.isis.core.commons.config.IsisConfiguration;
import org.apache.isis.core.commons.ensure.Ensure;
import org.apache.isis.core.commons.exceptions.IsisException;
import org.apache.isis.core.commons.util.ToString;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.oid.OidMarshaller;
import org.apache.isis.core.metamodel.adapter.oid.RootOid;
import org.apache.isis.core.metamodel.consent.InteractionInitiatedBy;
import org.apache.isis.core.metamodel.facets.actions.action.invocation.CommandUtil;
import org.apache.isis.core.metamodel.facets.object.audit.AuditableFacet;
import org.apache.isis.core.metamodel.runtimecontext.ServicesInjector;
import org.apache.isis.core.metamodel.spec.feature.Contributed;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.metamodel.transactions.TransactionState;
import org.apache.isis.core.runtime.persistence.objectstore.transaction.CreateObjectCommand;
import org.apache.isis.core.runtime.persistence.objectstore.transaction.DestroyObjectCommand;
import org.apache.isis.core.runtime.persistence.objectstore.transaction.PersistenceCommand;
import org.apache.isis.core.runtime.persistence.objectstore.transaction.PublishingServiceInternal;
import org.apache.isis.core.runtime.system.context.IsisContext;

import static org.apache.isis.core.commons.ensure.Ensure.ensureThatArg;
import static org.apache.isis.core.commons.ensure.Ensure.ensureThatState;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Used by the {@link IsisTransactionManager} to captures a set of changes to be
 * applied.
 * 
 * <p>
 * Note that methods such as <tt>flush()</tt>, <tt>commit()</tt> and
 * <tt>abort()</tt> are not part of the API. The place to control transactions
 * is through the {@link IsisTransactionManager transaction manager}, because
 * some implementations may support nesting and such like. It is also the job of
 * the {@link IsisTransactionManager} to ensure that the underlying persistence
 * mechanism (for example, the <tt>ObjectStore</tt>) is also committed.
 */
public class IsisTransaction implements TransactionScopedComponent {


    public static final Predicate<ObjectAdapter> IS_COMMAND = new Predicate<ObjectAdapter>() {
        @Override
        public boolean apply(ObjectAdapter input) {
            return Command.class.isAssignableFrom(input.getSpecification().getCorrespondingClass());
        }
    };

    private static class Placeholder {
        private static Placeholder NEW = new Placeholder("[NEW]");
        private static Placeholder DELETED = new Placeholder("[DELETED]");
        private final String str;
        public Placeholder(String str) {
            this.str = str;
        }
        @Override
        public String toString() {
            return str;
        }
    }

    public enum State {
        /**
         * Started, still in progress.
         * 
         * <p>
         * May {@link IsisTransaction#flush() flush},
         * {@link IsisTransaction#commit() commit} or
         * {@link IsisTransaction#markAsAborted() abort}.
         */
        IN_PROGRESS(TransactionState.IN_PROGRESS),
        /**
         * Started, but has hit an exception.
         * 
         * <p>
         * May not {@link IsisTransaction#flush()} or
         * {@link IsisTransaction#commit() commit} (will throw an
         * {@link IllegalStateException}), but can only
         * {@link IsisTransaction#markAsAborted() abort}.
         * 
         * <p>
         * Similar to <tt>setRollbackOnly</tt> in EJBs.
         */
        MUST_ABORT(TransactionState.MUST_ABORT),
        /**
         * Completed, having successfully committed.
         * 
         * <p>
         * May not {@link IsisTransaction#flush()} or
         * {@link IsisTransaction#markAsAborted() abort}.
         * {@link IsisTransaction#commit() commit} (will throw
         * {@link IllegalStateException}).
         */
        COMMITTED(TransactionState.COMMITTED),
        /**
         * Completed, having aborted.
         * 
         * <p>
         * May not {@link IsisTransaction#flush()},
         * {@link IsisTransaction#commit() commit} or
         * {@link IsisTransaction#markAsAborted() abort} (will throw
         * {@link IllegalStateException}).
         */
        ABORTED(TransactionState.ABORTED);

        public final TransactionState transactionState;
        
        private State(TransactionState transactionState){
            this.transactionState = transactionState;
        }


        /**
         * Whether it is valid to {@link IsisTransaction#flush() flush} this
         * {@link IsisTransaction transaction}.
         */
        public boolean canFlush() {
            return this == IN_PROGRESS;
        }

        /**
         * Whether it is valid to {@link IsisTransaction#commit() commit} this
         * {@link IsisTransaction transaction}.
         */
        public boolean canCommit() {
            return this == IN_PROGRESS;
        }

        /**
         * Whether it is valid to {@link IsisTransaction#markAsAborted() abort} this
         * {@link IsisTransaction transaction}.
         */
        public boolean canAbort() {
            return this == IN_PROGRESS || this == MUST_ABORT;
        }

        /**
         * Whether the {@link IsisTransaction transaction} is complete (and so a
         * new one can be started).
         */
        public boolean isComplete() {
            return this == COMMITTED || this == ABORTED;
        }

        public boolean mustAbort() {
            return this == MUST_ABORT;
        }

        public TransactionState getRuntimeContextState() {
            return transactionState;
        }
    }


    private static final Logger LOG = LoggerFactory.getLogger(IsisTransaction.class);

    private final IsisTransactionManager.PersistenceSessionTransactionManagement persistenceSession;
    private final List<PersistenceCommand> persistenceCommands = Lists.newArrayList();
    private final IsisTransactionManager transactionManager;
    private final MessageBroker messageBroker;

    private final ServicesInjector servicesInjector;

    /**
     * the 'owning' command, (if service configured).
     */
    private final Command command;

    private final CommandContext commandContext;
    private final CommandService commandService;

    private final InteractionContext interactionContext;

    private final ClockService clockService;
    /**
     * could be null if none has been registered.
     */
    private final AuditingService3 auditingServiceIfAny;
    /**
     * could be null if none has been registered
     */
    private final PublishingServiceInternal publishingServiceInternal;

    /**
     * Will be that of the {@link #command} if not <tt>null</tt>, otherwise will be randomly created.
     */
    private final UUID transactionId;
        
    private State state;
    private IsisException abortCause;




    public IsisTransaction(
            final IsisTransactionManager transactionManager,
            final MessageBroker messageBroker,
            final IsisTransactionManager.PersistenceSessionTransactionManagement persistenceSession,
            final ServicesInjector servicesInjector,
            final UUID transactionId) {
        
        ensureThatArg(transactionManager, is(not(nullValue())), "transaction manager is required");
        ensureThatArg(messageBroker, is(not(nullValue())), "message broker is required");
        ensureThatArg(servicesInjector, is(not(nullValue())), "services injector is required");

        this.transactionManager = transactionManager;
        this.messageBroker = messageBroker;
        this.servicesInjector = servicesInjector;
        
        this.commandContext = lookupService(CommandContext.class);
        this.commandService = lookupService(CommandService.class);

        this.interactionContext = lookupService(InteractionContext.class);
        this.clockService = lookupService(ClockService.class);

        this.auditingServiceIfAny = lookupServiceIfAny(AuditingService3.class);
        this.publishingServiceInternal = lookupService(PublishingServiceInternal.class);

        // determine whether this xactn is taking place in the context of an
        // existing command in which a previous xactn has already occurred.
        // if so, reuse that transactionId.
        command = commandContext.getCommand();

        this.transactionId = transactionId;

        this.state = State.IN_PROGRESS;

        this.persistenceSession = persistenceSession;
        if (LOG.isDebugEnabled()) {
            LOG.debug("new transaction " + this);
        }
    }

    protected EventSerializer newSimpleEventSerializer() {
        return new EventSerializer.Simple();
    }

    // ////////////////////////////////////////////////////////////////
    // GUID
    // ////////////////////////////////////////////////////////////////

    public final UUID getTransactionId() {
        return transactionId;
    }
    
    
    // ////////////////////////////////////////////////////////////////
    // State
    // ////////////////////////////////////////////////////////////////

    public State getState() {
        return state;
    }

    private void setState(final State state) {
        this.state = state;
    }

    
    // //////////////////////////////////////////////////////////
    // Commands
    // //////////////////////////////////////////////////////////

    /**
     * Add the non-null command to the list of commands to execute at the end of
     * the transaction.
     */
    public void addCommand(final PersistenceCommand command) {
        if (command == null) {
            return;
        }

        final ObjectAdapter onObject = command.onAdapter();

        // Destroys are ignored when preceded by a create, or another destroy
        if (command instanceof DestroyObjectCommand) {
            if (alreadyHasCreate(onObject)) {
                removeCreate(onObject);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("ignored both create and destroy command " + command);
                }
                return;
            }

            if (alreadyHasDestroy(onObject)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("ignored command " + command + " as command already recorded");
                }
                return;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("add command " + command);
        }
        persistenceCommands.add(command);
    }



    // ////////////////////////////////////////////////////////////////
    // flush
    // ////////////////////////////////////////////////////////////////

    public synchronized final void flush() {

        // have removed THIS guard because we hit a situation where a xactn is aborted
        // from a no-arg action, the Wicket viewer attempts to render a new page that (of course)
        // contains the service menu items, and some of the 'disableXxx()' methods of those
        // service actions perform repository queries (while xactn is still in a state of ABORTED)
        //
        // ensureThatState(getState().canFlush(), is(true), "state is: " + getState());
        //
        if (LOG.isDebugEnabled()) {
            LOG.debug("flush transaction " + this);
        }

        try {
            doFlush();
        } catch (final RuntimeException ex) {
            setAbortCause(new IsisTransactionFlushException(ex));
            throw ex;
        }
    }

    /**
     * Maximum number of times we attempt to flush the transaction before giving up.
     */
    private final static int MAX_FLUSH_ATTEMPTS = 10;
    
    /**
     * Mandatory hook method for subclasses to persist all pending changes.
     * 
     * <p>
     * Called by both {@link #commit()} and by {@link #flush()}:
     * <table>
     * <tr>
     * <th>called from</th>
     * <th>next {@link #getState() state} if ok</th>
     * <th>next {@link #getState() state} if exception</th>
     * </tr>
     * <tr>
     * <td>{@link #commit()}</td>
     * <td>{@link State#COMMITTED}</td>
     * <td>{@link State#ABORTED}</td>
     * </tr>
     * <tr>
     * <td>{@link #flush()}</td>
     * <td>{@link State#IN_PROGRESS}</td>
     * <td>{@link State#MUST_ABORT}</td>
     * </tr>
     * </table>
     */
    private void doFlush() {
        
        int i = 0;
        //
        // it's possible that in executing these commands that more will be created.
        // so we keep flushing until no more are available (ISIS-533)
        //
        // this is a do...while rather than a while... just for backward compatibilty
        // with previous algorithm that always went through the execute phase at least once.
        //
        do {
            // this algorithm ensures that we never execute the same command twice,
            // and also allow new commands to be added to end
            final List<PersistenceCommand> persistenceCommandList = Lists.newArrayList(persistenceCommands);

            if(!persistenceCommandList.isEmpty()) {
                // so won't be processed again if a flush is encountered subsequently
                persistenceCommands.removeAll(persistenceCommandList);
                try {
                    this.transactionManager.getPersistenceSession().execute(persistenceCommandList);
                    for (PersistenceCommand persistenceCommand : persistenceCommandList) {
                        if (persistenceCommand instanceof DestroyObjectCommand) {
                            final ObjectAdapter adapter = persistenceCommand.onAdapter();
                            adapter.setVersion(null);
                        }
                    }
                } catch (final RuntimeException ex) {
                    // if there's an exception, we want to make sure that
                    // all commands are cleared and propagate
                    persistenceCommands.clear();
                    throw ex;
                }
            }
        } while(!persistenceCommands.isEmpty());
        
    }

    protected void doAudit(final Set<Entry<AdapterAndProperty, PreAndPostValues>> changedObjectProperties) {
        try {
            if(auditingServiceIfAny == null) {
                return;
            }

            // else
            final String currentUser = getTransactionManager().getAuthenticationSession().getUserName();
            final java.sql.Timestamp currentTime = Clock.getTimeAsJavaSqlTimestamp();
            for (Entry<AdapterAndProperty, PreAndPostValues> auditEntry : changedObjectProperties) {
                auditChangedProperty(currentTime, currentUser, auditEntry);
            }

        } finally {
            // not needed in production, but is required for integration testing
            this.changedObjectProperties.clear();
        }
    }

    public void auditChangedProperty(
            final java.sql.Timestamp timestamp,
            final String user,
            final Entry<AdapterAndProperty, PreAndPostValues> auditEntry) {

        final AdapterAndProperty aap = auditEntry.getKey();
        final ObjectAdapter adapter = aap.getAdapter();
        
        final AuditableFacet auditableFacet = adapter.getSpecification().getFacet(AuditableFacet.class);
        if(auditableFacet == null || auditableFacet.isDisabled()) {
            return;
        }

        final Bookmark target = aap.getBookmark();
        final String propertyId = aap.getPropertyId();
        final String memberId = aap.getMemberId();

        final PreAndPostValues papv = auditEntry.getValue();
        final String preValue = papv.getPreString();
        final String postValue = papv.getPostString();
        

        final String targetClass = CommandUtil.targetClassNameFor(adapter);

        auditingServiceIfAny
                .audit(getTransactionId(), targetClass, target, memberId, propertyId, preValue, postValue, user, timestamp);
    }

    private static String asString(Object object) {
        return object != null? object.toString(): null;
    }


    protected AuthenticationSession getAuthenticationSession() {
        return IsisContext.getAuthenticationSession();
    }


    
    // ////////////////////////////////////////////////////////////////
    // preCommit, commit
    // ////////////////////////////////////////////////////////////////

    synchronized void preCommit() {
        ensureThatState(getState().canCommit(), is(true), "state is: " + getState());
        ensureThatState(abortCause, is(nullValue()), "cannot commit: an abort cause has been set");

        if (LOG.isDebugEnabled()) {
            LOG.debug("preCommit transaction " + this);
        }

        if (getState() == State.COMMITTED) {
            if (LOG.isInfoEnabled()) {
                LOG.info("already committed; ignoring");
            }
            return;
        }

        try {
            final Map<AdapterAndProperty, PreAndPostValues> processedObjectProperties = Maps.newLinkedHashMap();
            while(!changedObjectProperties.isEmpty()) {

                final Set<AdapterAndProperty> keys = Sets.newLinkedHashSet(changedObjectProperties.keySet());
                for (final AdapterAndProperty aap : keys) {

                    final PreAndPostValues papv = changedObjectProperties.remove(aap);

                    final ObjectAdapter adapter = aap.getAdapter();
                    if(adapter.isDestroyed()) {
                        // don't touch the object!!!
                        // JDO, for example, will complain otherwise...
                        papv.setPost(Placeholder.DELETED);
                    } else {
                        papv.setPost(aap.getPropertyValue());
                    }

                    // if we encounter the same objectProperty again, this will simply overwrite it
                    processedObjectProperties.put(aap, papv);
                }
            }

            final Set<Entry<AdapterAndProperty, PreAndPostValues>> changedObjectProperties =
                    Collections.unmodifiableSet(
                            Sets.filter(processedObjectProperties.entrySet(), PreAndPostValues.Predicates.CHANGED));

            ensureCommandsPersistedIfDirtyXactnAndAnySafeSemanticsHonoured(changedObjectProperties);

            preCommitServices(changedObjectProperties);

        } catch (final RuntimeException ex) {
            setAbortCause(new IsisTransactionManagerException(ex));
            completeCommandAndInteractionAndClearDomainEvents();
            throw ex;
        }
    }

    private void ensureCommandsPersistedIfDirtyXactnAndAnySafeSemanticsHonoured(final Set<Entry<AdapterAndProperty, PreAndPostValues>> changedObjectProperties) {

        final Command command = commandContext.getCommand();

        // ensure that any changed objects means that the command should be persisted
        final Set<ObjectAdapter> changedAdapters = findChangedAdapters(changedObjectProperties);
        if(!changedAdapters.isEmpty() && command.getMemberIdentifier() != null) {
            command.setPersistHint(true);
        }
    }


    private static Set<ObjectAdapter> findChangedAdapters(
            final Set<Entry<AdapterAndProperty, PreAndPostValues>> changedObjectProperties) {
        return Sets.newHashSet(
                Iterables.filter(
                        Iterables.transform(
                                changedObjectProperties,
                                AdapterAndProperty.Functions.GET_ADAPTER),
                        Predicates.not(IS_COMMAND)));
    }


    private void preCommitServices(
            final Set<Entry<AdapterAndProperty, PreAndPostValues>> changedObjectProperties) {

        doAudit(changedObjectProperties);

        publishingServiceInternal.publishAction();
        doFlush();

        publishingServiceInternal.publishObjects(this.changeKindByEnlistedAdapter);
        doFlush();

        closeOtherApplibServicesIfConfigured();

        completeCommandAndInteractionAndClearDomainEvents();

        doFlush();
    }

    private void closeOtherApplibServicesIfConfigured() {
        final ActionInvocationContext actionInvocationContext = lookupService(ActionInvocationContext.class);
        if(actionInvocationContext != null) {
            Bulk.InteractionContext.current.set(null);
        }
    }

    private void completeCommandAndInteractionAndClearDomainEvents() {

        final Command command = commandContext.getCommand();
        final Interaction interaction = interactionContext.getInteraction();

        if(command.getStartedAt() != null && command.getCompletedAt() == null) {
            // the guard is in case we're here as the result of a redirect following a previous exception;just ignore.

            // copy over from the most recent interaction
            Interaction.Execution priorExecution = interaction.getPriorExecution();
            final Timestamp completedAt = priorExecution.getCompletedAt();
            command.setCompletedAt(completedAt);
        }

        commandService.complete(command);

        if(command instanceof Command3) {
            final Command3 command3 = (Command3) command;
            command3.flushActionDomainEvents();
        } else
        if(command instanceof Command2) {
            final Command2 command2 = (Command2) command;
            command2.flushActionInteractionEvents();
        }

        interaction.clear();
    }


    // ////////////////////////////////////////////////////////////////

    public synchronized void commit() {
        ensureThatState(getState().canCommit(), is(true), "state is: " + getState());
        ensureThatState(abortCause, is(nullValue()), "cannot commit: an abort cause has been set");

        if (LOG.isDebugEnabled()) {
            LOG.debug("postCommit transaction " + this);
        }

        if (getState() == State.COMMITTED) {
            if (LOG.isInfoEnabled()) {
                LOG.info("already committed; ignoring");
            }
            return;
        }

        setState(State.COMMITTED);
    }


    
    // ////////////////////////////////////////////////////////////////
    // markAsAborted
    // ////////////////////////////////////////////////////////////////

    public synchronized final void markAsAborted() {
        ensureThatState(getState().canAbort(), is(true), "state is: " + getState());
        if (LOG.isInfoEnabled()) {
            LOG.info("abort transaction " + this);
        }

        setState(State.ABORTED);
    }

    
    
    /////////////////////////////////////////////////////////////////////////
    // handle exceptions on load, flush or commit
    /////////////////////////////////////////////////////////////////////////

    // SEEMINGLY UNUSED
    @Deprecated
    public void ensureNoAbortCause() {
        Ensure.ensureThatArg(abortCause, is(nullValue()), "abort cause has been set");
    }

    
    
    /**
     * Indicate that the transaction must be aborted, and that there is
     * an unhandled exception to be rendered somehow.
     * 
     * <p>
     * If the cause is subsequently rendered by code higher up the stack, then the
     * cause can be {@link #clearAbortCause() cleared}.  However, it is not possible
     * to change the state from {@link State#MUST_ABORT}.
     */
    public void setAbortCause(IsisException abortCause) {
        setState(State.MUST_ABORT);
        this.abortCause = abortCause;
    }
    
    public IsisException getAbortCause() {
        return abortCause;
    }

    /**
     * If the cause has been rendered higher up in the stack, then clear the cause so that
     * it won't be picked up and rendered elsewhere.
     */
    public void clearAbortCause() {
        abortCause = null;
    }

    
    // //////////////////////////////////////////////////////////
    // Helpers
    // //////////////////////////////////////////////////////////

    private boolean alreadyHasCommand(final Class<?> commandClass, final ObjectAdapter onObject) {
        return getCommand(commandClass, onObject) != null;
    }

    private boolean alreadyHasCreate(final ObjectAdapter onObject) {
        return alreadyHasCommand(CreateObjectCommand.class, onObject);
    }

    private boolean alreadyHasDestroy(final ObjectAdapter onObject) {
        return alreadyHasCommand(DestroyObjectCommand.class, onObject);
    }

    private PersistenceCommand getCommand(final Class<?> commandClass, final ObjectAdapter onObject) {
        for (final PersistenceCommand command : persistenceCommands) {
            if (command.onAdapter().equals(onObject)) {
                if (commandClass.isAssignableFrom(command.getClass())) {
                    return command;
                }
            }
        }
        return null;
    }

    private void removeCommand(final Class<?> commandClass, final ObjectAdapter onObject) {
        final PersistenceCommand toDelete = getCommand(commandClass, onObject);
        persistenceCommands.remove(toDelete);
    }

    private void removeCreate(final ObjectAdapter onObject) {
        removeCommand(CreateObjectCommand.class, onObject);
    }

    // ////////////////////////////////////////////////////////////////
    // toString
    // ////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return appendTo(new ToString(this)).toString();
    }

    protected ToString appendTo(final ToString str) {
        str.append("state", state);
        str.append("commands", persistenceCommands.size());
        return str;
    }


    // ////////////////////////////////////////////////////////////////
    // Depenendencies (from constructor)
    // ////////////////////////////////////////////////////////////////

    /**
     * The owning {@link IsisTransactionManager transaction manager}.
     * 
     * <p>
     * Injected in constructor
     */
    public IsisTransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * The {@link org.apache.isis.core.commons.authentication.MessageBroker} for this transaction.
     * 
     * <p>
     * Injected in constructor
     *
     * @deprecated - obtain the {@link org.apache.isis.core.commons.authentication.MessageBroker} instead from the {@link AuthenticationSession}.
     */
    public MessageBroker getMessageBroker() {
        return messageBroker;
    }

    public static class AdapterAndProperty {
        
        private final ObjectAdapter objectAdapter;
        private final ObjectAssociation property;
        private final Bookmark bookmark;
        private final String propertyId;
        private final String bookmarkStr;

        public static AdapterAndProperty of(ObjectAdapter adapter, ObjectAssociation property) {
            return new AdapterAndProperty(adapter, property);
        }

        private AdapterAndProperty(ObjectAdapter adapter, ObjectAssociation property) {
            this.objectAdapter = adapter;
            this.property = property;

            final RootOid oid = (RootOid) adapter.getOid();

            final String objectType = oid.getObjectSpecId().asString();
            final String identifier = oid.getIdentifier();
            bookmark = new Bookmark(objectType, identifier);
            bookmarkStr = bookmark.toString();

            propertyId = property.getId();
        }
        
        public ObjectAdapter getAdapter() {
            return objectAdapter;
        }
        public ObjectAssociation getProperty() {
            return property;
        }

        public Bookmark getBookmark() {
            return bookmark;
        }
        public String getPropertyId() {
            return propertyId;
        }

        public String getMemberId() {
            return property.getIdentifier().toClassAndNameIdentityString();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final AdapterAndProperty that = (AdapterAndProperty) o;

            if (bookmarkStr != null ? !bookmarkStr.equals(that.bookmarkStr) : that.bookmarkStr != null) return false;
            if (propertyId != null ? !propertyId.equals(that.propertyId) : that.propertyId != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = propertyId != null ? propertyId.hashCode() : 0;
            result = 31 * result + (bookmarkStr != null ? bookmarkStr.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return bookmarkStr + " , " + getProperty().getId();
        }

        protected OidMarshaller getMarshaller() {
            return new OidMarshaller();
        }

        private Object getPropertyValue() {
            ObjectAdapter referencedAdapter = property.get(objectAdapter, InteractionInitiatedBy.FRAMEWORK);
            return referencedAdapter == null ? null : referencedAdapter.getObject();
        }

        static class Functions {
            private Functions(){}

            static final Function<Entry<AdapterAndProperty, PreAndPostValues>, ObjectAdapter> GET_ADAPTER = new Function<Entry<AdapterAndProperty, PreAndPostValues>, ObjectAdapter>() {
                @Override
                public ObjectAdapter apply(Entry<AdapterAndProperty, PreAndPostValues> input) {
                    final AdapterAndProperty aap = input.getKey();
                    return aap.getAdapter();
                }
            };

        }

    }

    
    ////////////////////////////////////////////////////////////////////////
    // Auditing/Publishing object tracking
    ////////////////////////////////////////////////////////////////////////

    public static class PreAndPostValues {

        static class Predicates {
            final static Predicate<Entry<?, PreAndPostValues>> CHANGED = new Predicate<Entry<?, PreAndPostValues>>(){
                @Override
                public boolean apply(Entry<?, PreAndPostValues> input) {
                    final PreAndPostValues papv = input.getValue();
                    return papv.differ();
                }};
        }

        private final Object pre;
        /**
         * Eagerly calculated because it could be that the object referenced ends up being deleted by the time that the xactn completes.
         */
        private final String preString;

        /**
         * Updated in {@link #setPost(Object)} 
         */
        private Object post;
        /**
         * Updated in {@link #setPost(Object)}, along with {@link #post}.
         */
        private String postString;

        
        public static PreAndPostValues pre(Object preValue) {
            return new PreAndPostValues(preValue, null);
        }

        private PreAndPostValues(Object pre, Object post) {
            this.pre = pre;
            this.post = post;
            this.preString = asString(pre);
        }
        /**
         * The object that was referenced before this object was changed
         * 
         * <p>
         * Note that this referenced object itself could end up being deleted in the course of the transaction; in which case use 
         * {@link #getPreString()} which is the eagerly cached <tt>toString</tt> of said object. 
         */
        public Object getPre() {
            return pre;
        }
        public String getPreString() {
            return preString;
        }
        public Object getPost() {
            return post;
        }
        public String getPostString() {
            return postString;
        }
        public void setPost(Object post) {
            this.post = post;
            this.postString = asString(post);
        }
        
        @Override
        public String toString() {
            return getPre() + " -> " + getPost();
        }

        public boolean differ() {
            if(getPre() == Placeholder.NEW || getPost() == Placeholder.DELETED) {
                return true;
            }
            return !Objects.equal(getPre(), getPost());
        }
    }
    
   
    private final Map<ObjectAdapter,ChangeKind> changeKindByEnlistedAdapter = Maps.newLinkedHashMap();
    private final Map<AdapterAndProperty, PreAndPostValues> changedObjectProperties = Maps.newLinkedHashMap();

    private ObjectStringifier objectStringifier;




    /**
     * Auditing and publishing support: for object stores to enlist an object that has just been created, 
     * capturing a dummy value <tt>'[NEW]'</tt> for the pre-modification value. 
     * 
     * <p>
     * The post-modification values are captured in {@link #preCommit()}.
     * 
     * <p>
     * Supported by the JDO object store; check documentation for support in other objectstores.
     */
    public void enlistCreated(ObjectAdapter adapter) {
        enlist(adapter, ChangeKind.CREATE);
        for (ObjectAssociation property : adapter.getSpecification().getAssociations(Contributed.EXCLUDED, ObjectAssociation.Filters.PROPERTIES)) {
            final AdapterAndProperty aap = AdapterAndProperty.of(adapter, property);
            if(property.isNotPersisted()) {
                continue;
            }
            if(changedObjectProperties.containsKey(aap)) {
                // already enlisted, so ignore
                return;
            }
            PreAndPostValues papv = PreAndPostValues.pre(Placeholder.NEW);
            changedObjectProperties.put(aap, papv);
        }
    }

    /**
     * Auditing and publishing support: for object stores to enlist an object that is about to be updated, 
     * capturing the pre-modification values of the properties of the {@link ObjectAdapter}.
     * 
     * <p>
     * The post-modification values are captured in {@link #preCommit()}.
     *
     * <p>
     * Supported by the JDO object store; check documentation for support in other objectstores.
     */
    public void enlistUpdating(ObjectAdapter adapter) {
        enlist(adapter, ChangeKind.UPDATE);
        for (ObjectAssociation property : adapter.getSpecification().getAssociations(Contributed.EXCLUDED, ObjectAssociation.Filters.PROPERTIES)) {
            final AdapterAndProperty aap = AdapterAndProperty.of(adapter, property);
            if(property.isNotPersisted()) {
                continue;
            }
            if(changedObjectProperties.containsKey(aap)) {
                // already enlisted, so ignore
                return;
            }
            PreAndPostValues papv = PreAndPostValues.pre(aap.getPropertyValue());
            changedObjectProperties.put(aap, papv);
        }
    }

    /**
     * Auditing and publishing support: for object stores to enlist an object that is about to be deleted, 
     * capturing the pre-deletion value of the properties of the {@link ObjectAdapter}. 
     * 
     * <p>
     * The post-modification values are captured in {@link #preCommit()}.  In the case of deleted objects, a
     * dummy value <tt>'[DELETED]'</tt> is used as the post-modification value.
     * 
     * <p>
     * Supported by the JDO object store; check documentation for support in other objectstores.
     */
    public void enlistDeleting(ObjectAdapter adapter) {
        final boolean enlisted = enlist(adapter, ChangeKind.DELETE);
        if(!enlisted) {
            return;
        }
        for (ObjectAssociation property : adapter.getSpecification().getAssociations(Contributed.EXCLUDED, ObjectAssociation.Filters.PROPERTIES)) {
            final AdapterAndProperty aap = AdapterAndProperty.of(adapter, property);
            if(property.isNotPersisted()) {
                continue;
            }
            if(changedObjectProperties.containsKey(aap)) {
                // already enlisted, so ignore
                return;
            }
            PreAndPostValues papv = PreAndPostValues.pre(aap.getPropertyValue());
            changedObjectProperties.put(aap, papv);
        }
    }


    /**
     *
     * @param adapter
     * @param current
     * @return <code>true</code> if successfully enlisted, <code>false</code> if was already enlisted
     */
    private boolean enlist(final ObjectAdapter adapter, final ChangeKind current) {
        final ChangeKind previous = changeKindByEnlistedAdapter.get(adapter);
        if(previous == null) {
            changeKindByEnlistedAdapter.put(adapter, current);
            return true;
        }
        switch (previous) {
            case CREATE:
                switch (current) {
                    case DELETE:
                        changeKindByEnlistedAdapter.remove(adapter);
                    case CREATE:
                    case UPDATE:
                        return false;
                }
                break;
            case UPDATE:
                switch (current) {
                    case DELETE:
                        changeKindByEnlistedAdapter.put(adapter, current);
                        return true;
                    case CREATE:
                    case UPDATE:
                        return false;
                }
                break;
            case DELETE:
                return false;
        }
        return previous == null;
    }


    ////////////////////////////////////////////////////////////////////////
    // Dependencies (lookup)
    ////////////////////////////////////////////////////////////////////////

    private <T> T lookupService(Class<T> serviceType) {
        T service = lookupServiceIfAny(serviceType);
        if(service == null) {
            throw new IllegalStateException("Could not locate service of type '" + serviceType + "'");
        }
        return service;
    }

    private <T> T lookupServiceIfAny(final Class<T> serviceType) {
        return servicesInjector.lookupService(serviceType);
    }

    ////////////////////////////////////////////////////////////////////////
    // Dependencies (from constructor)
    ////////////////////////////////////////////////////////////////////////

    protected OidMarshaller getOidMarshaller() {
        return IsisContext.getOidMarshaller();
    }

    protected IsisConfiguration getConfiguration() {
        return IsisContext.getConfiguration();
    }

    protected IsisTransactionManager.PersistenceSessionTransactionManagement getPersistenceSession() {
        return persistenceSession;
    }

}

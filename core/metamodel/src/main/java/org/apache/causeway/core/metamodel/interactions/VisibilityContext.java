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
package org.apache.causeway.core.metamodel.interactions;

import lombok.Getter;

import org.apache.causeway.applib.Identifier;
import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.applib.services.wrapper.events.VisibilityEvent;
import org.apache.causeway.core.config.CausewayConfiguration;
import org.apache.causeway.core.metamodel.consent.InteractionContextType;
import org.apache.causeway.core.metamodel.consent.InteractionInitiatedBy;

/**
 * See {@link InteractionContext} for overview; analogous to
 * {@link VisibilityEvent}.
 */
public abstract class VisibilityContext
extends InteractionContext
implements InteractionEventSupplier<VisibilityEvent> {

    @Getter private final CausewayConfiguration.Prototyping.IfHiddenPolicy ifHiddenPolicy;
    @Getter private final CausewayConfiguration.Prototyping.IfDisabledPolicy ifDisabledPolicy;

    public VisibilityContext(
            final InteractionContextType interactionType,
            final InteractionHead head,
            final Identifier identifier,
            final InteractionInitiatedBy interactionInitiatedBy,
            final Where where,
            final CausewayConfiguration.Prototyping.IfHiddenPolicy ifHiddenPolicy,
            final CausewayConfiguration.Prototyping.IfDisabledPolicy ifDisabledPolicy) {
        super(interactionType, interactionInitiatedBy, identifier, head, where);
        this.ifHiddenPolicy = ifHiddenPolicy;
        this.ifDisabledPolicy = ifDisabledPolicy;
    }

}

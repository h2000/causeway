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
package org.apache.causeway.viewer.wicket.model.models.interaction.coll;

import org.apache.causeway.applib.Identifier;
import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedCollection;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.spec.feature.ObjectAction;
import org.apache.causeway.core.metamodel.spec.feature.OneToManyAssociation;
import org.apache.causeway.core.metamodel.tabular.DataTableInteractive;
import org.apache.causeway.core.metamodel.tabular.DataTableMemento;
import org.apache.causeway.viewer.commons.model.object.UiObject;
import org.apache.causeway.viewer.wicket.model.models.interaction.BookmarkedObjectWkt;
import org.apache.causeway.viewer.wicket.model.models.interaction.HasBookmarkedOwnerAbstract;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;

/**
 * Bound to a BookmarkedObjectWkt, with the {@link DataTableInteractive}
 * representing either a <i>Collection</i> or an <i>Action</i>'s result.
 *
 * @implSpec the state of the DataTableModel is held transient,
 * that means it does not survive a serialization/de-serialization cycle;
 * it is recreated on load
 *
 * @see HasBookmarkedOwnerAbstract
 */
class DataTableModelWktO
extends HasBookmarkedOwnerAbstract<DataTableInteractive>
implements
    DataTableModelWkt {

    // -- FACTORIES

    public static DataTableModelWktO forActionModel(
            final @NonNull BookmarkedObjectWkt bookmarkedObjectModel,
            final @NonNull ObjectAction actMetaModel,
            final @NonNull Can<ManagedObject> args,
            final @NonNull ManagedObject actionResult) {

        val managedAction = ManagedAction
                .of(bookmarkedObjectModel.getObject(), actMetaModel, Where.NOT_SPECIFIED);

        val table = DataTableInteractive.forAction(
                managedAction,
                args,
                actionResult);

        val tableMemento = table.createMemento(managedAction.getMementoForArgs(args));

        val model = new DataTableModelWktO(
                bookmarkedObjectModel, actMetaModel.getFeatureIdentifier(), tableMemento);

        model.setObject(table); // memoize

        return model;
    }

    public static @NonNull DataTableModelWktO forCollection(
            final @NonNull BookmarkedObjectWkt bookmarkedObjectModel,
            final @NonNull OneToManyAssociation collMetaModel) {

        val table = DataTableInteractive.forCollection(
                ManagedCollection
                .of(bookmarkedObjectModel.getObject(), collMetaModel, Where.NOT_SPECIFIED));

        val tableMemento = table.createMemento(null);

        val model = new DataTableModelWktO(
                bookmarkedObjectModel, collMetaModel.getFeatureIdentifier(), tableMemento);

        model.setObject(table); // memoize

        return model;
    }

    // -- CONSTRUCTION

    private static final long serialVersionUID = 1L;

    @Getter private final Identifier featureIdentifier;
    private final DataTableMemento tableMemento;

    private DataTableModelWktO(
            final BookmarkedObjectWkt bookmarkedObject,
            final Identifier featureIdentifier,
            final DataTableMemento tableMemento) {
        super(bookmarkedObject);
        this.featureIdentifier = featureIdentifier;
        this.tableMemento = tableMemento;
    }

    // --

    @Override
    public UiObject getParentUiModel() {
        return ()->super.getBookmarkedOwner();
    }

    @Override
    protected DataTableInteractive load() {
        val dataTableModel = tableMemento.getDataTableModel(getBookmarkedOwner());
        return dataTableModel;
    }

    @Override
    public final void detach() {
        if(isDataTableModelDetachable()) {
            // at time of writing breaks object deletion on JDO, see [CAUSEWAY-3530]
            super.detach();
        }
        //FIXME[CAUSEWAY-3522]
        // perhaps instead call bookmarkedObjectModel().detach();
        // or add custom HOLLOW flag and mark the bookmarkedObjectModel() hollow
    }

    // -- HELPER

    private final static String PROPERTY_NAME_MODEL_REUSE = "causeway.viewer.wicket.dataTableModelReuse";
    /**
     * when set to false, forces detach
     * @deprecated remove this switch once we have a fix
     */
    @Deprecated
    private static boolean isDataTableModelDetachable() {
        return "false".equalsIgnoreCase(System.getenv(PROPERTY_NAME_MODEL_REUSE))
                || "false".equalsIgnoreCase(System.getProperty(PROPERTY_NAME_MODEL_REUSE));
    }


}

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

import java.util.Optional;
import java.util.UUID;

import org.apache.wicket.model.IModel;

import org.apache.causeway.commons.internal.exceptions._Exceptions;
import org.apache.causeway.core.metamodel.tabular.DataRow;
import org.apache.causeway.core.metamodel.tabular.DataTableInteractive;
import org.apache.causeway.core.metamodel.tabular.DataTableInteractive.TableImplementation;

public interface DataRowWkt extends IModel<DataRow>{

    static DataRowWkt chain(
            final IModel<DataTableInteractive> dataTableModelHolder,
            final DataRow dataRow) {

        switch (TableImplementation.getSelected()) {
        case OPTIMISTIC:
            return DataRowWktO.chain(dataTableModelHolder, dataRow);
        case DEFAULT:
        default:
            return DataRowWktD.chain(dataTableModelHolder, dataRow);
        }
    }

    int getRowIndex();

    @Override
    DataRow getObject();

    Optional<DataRow> dataRow();

    boolean hasMemoizedDataRow();

    @Deprecated // used by OPTIMISTIC data table
    default UUID getUuid() {
        throw _Exceptions.unsupportedOperation();
    }
    @Deprecated // used by OPTIMISTIC data table
    default DataRowToggleWkt getDataRowToggle() {
        throw _Exceptions.unsupportedOperation();
    }

}

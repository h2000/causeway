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
package org.apache.causeway.extensions.tabular.excel.exporter;

import java.io.File;
import java.util.ArrayList;
import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import org.apache.causeway.applib.value.NamedWithMimeType.CommonMimeType;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.functional.IndexedFunction;
import org.apache.causeway.commons.tabular.TabularModel;
import org.apache.causeway.core.metamodel.consent.InteractionInitiatedBy;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.tabular.simple.CollectionContentsExporter;
import org.apache.causeway.core.metamodel.tabular.simple.DataColumn;
import org.apache.causeway.core.metamodel.tabular.simple.DataTable;

@Component
public class CollectionContentsAsExcelExporter
implements CollectionContentsExporter {

    @Override
    public void createExport(final DataTable dataTable, final File tempFile,
            final @Nullable CollectionContentsExporter.AccessMode accessMode) {
        new ExcelFileWriter()
            .export(tabularModel(dataTable, accessMode), tempFile);
    }

    @Override
    public CommonMimeType getMimeType() {
        return CommonMimeType.XLSX;
    }

    @Override
    public String getTitleLabel() {
        return "Excel Download";
    }

    @Override
    public String getCssClass() {
        return "fa-solid fa-file-excel";
    }

    @Override
    public int orderOfAppearanceInUiDropdown() {
        return 2500;
    }

    // -- HELPER

    private TabularModel tabularModel(
            final DataTable dataTable,
            final CollectionContentsExporter.AccessMode accessMode) {
        var interactionInitiatedBy = switch(accessMode) {
            case PASS_THROUGH -> InteractionInitiatedBy.PASS_THROUGH;
            default -> InteractionInitiatedBy.USER;
        };

        var columns = dataTable.getDataColumns()
                .map(IndexedFunction.zeroBased(this::tabularColumn));
        var rows = dataTable.getDataRows()
                .map(dr->tabularRow(dataTable.getDataColumns(), col->dr.getCellElements(col, interactionInitiatedBy)));
        var sheet = new TabularModel.TabularSheet(dataTable.getTableFriendlyName(), columns, rows);
        return new TabularModel(Can.of(sheet));
    }

    private TabularModel.TabularColumn tabularColumn(final int index, final DataColumn dc) {
        return new TabularModel.TabularColumn(
                index,
                dc.getColumnFriendlyName(),
                dc.getColumnDescription().orElse(""));
    }

    private TabularModel.TabularRow tabularRow(
            final Can<DataColumn> dataColumns,
            final Function<DataColumn, Can<ManagedObject>> cellElementProvider) {
        var cells = new ArrayList<TabularModel.TabularCell>(dataColumns.size());
        dataColumns.forEach(null);
        return new TabularModel.TabularRow(Can.ofCollection(cells));
    }

}

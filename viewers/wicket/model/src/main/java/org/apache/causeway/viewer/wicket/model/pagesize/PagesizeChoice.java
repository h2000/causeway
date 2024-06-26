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
package org.apache.causeway.viewer.wicket.model.pagesize;

import java.io.Serializable;

@lombok.Value
public class PagesizeChoice implements Serializable {

    private static final long serialVersionUID = 1L;

    final String title;
    final long itemsPerPage;
    final String cssClass = ""; // for future use
}

//@lombok.Value
//static class LinkEntry implements Serializable {
//    private static final long serialVersionUID = 1L;
//    // -- CONSTRUCTION
//    final String title;
//    final long itemsPerPage;
//    final String cssClass = ""; // for future use
//    // -- UTILITY
//    static void addIconAndTitle(
//            final @NonNull ListItem<LinkEntry> item,
//            final @NonNull MarkupContainer link) {
//        WktLinks.listItemAsDropdownLink(item, link,
//                ID_VIEW_ITEM_TITLE, LinkEntry::nameFor,
//                ID_VIEW_ITEM_ICON, LinkEntry::cssClassFor,
//                null);
//    }
//    // -- HELPER
//    private static IModel<String> nameFor(final LinkEntry linkEntry) {
//        return Model.of(linkEntry.getTitle());
//    }
//    private static IModel<String> cssClassFor(final LinkEntry linkEntry) {
//        return Model.of(linkEntry.getCssClass());
//    }
//}

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
package org.apache.causeway.viewer.vaadin.viewer;

import org.apache.causeway.viewer.vaadin.ui.CausewayModuleViewerVaadinUi;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @since 1.x {@index}
 */
@Configuration
@Import({
        // Modules
        CausewayModuleViewerVaadinUi.class,

        // @Configuration's
/*        BootstrapInitWkt.class,
        JQueryInitWkt.class,
        Select2InitWkt.class,
        WebjarsInitWkt.class,
        WicketViewerCssBundleInit.class,
        DatatablesNetInitWkt.class,
        DebugInitWkt.class, */

        // @Service's
/*        BookmarkUiServiceWicket.class,
        ComponentFactoryRegistrarDefault.class,
        ComponentFactoryRegistryDefault.class,
        DeepLinkServiceWicket.class,
        ImageResourceCacheClassPath.class,
        HintStoreUsingWicketSession.class,
        PageClassListDefault.class,
        PageClassRegistryDefault.class,
        PageNavigationServiceDefault.class,
        WebModuleWicket.class,*/

})
public class CausewayModuleViewerVaadinViewer {

    public static final String NAMESPACE = "causeway.viewer.vaadin";
}

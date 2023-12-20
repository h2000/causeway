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
package org.apache.causeway.core.metamodel.facets.object.domainobjectlayout;

import java.util.Optional;

import org.apache.causeway.applib.layout.component.CssClassFaPosition;
import org.apache.causeway.applib.layout.component.DomainObjectLayoutData;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.core.metamodel.facetapi.Facet;
import org.apache.causeway.core.metamodel.facetapi.FacetHolder;
import org.apache.causeway.core.metamodel.facets.members.iconfa.FaFacet;
import org.apache.causeway.core.metamodel.facets.members.iconfa.FaStaticFacetAbstract;

public class FaFacetForDomainObjectLayoutXml
extends FaStaticFacetAbstract {

    public static Optional<FaFacet> create(
            final DomainObjectLayoutData domainObjectLayout,
            final FacetHolder holder,
            final Facet.Precedence precedence) {
        if(domainObjectLayout == null) {
            return Optional.empty();
        }
        final String cssClassFa = _Strings.emptyToNull(domainObjectLayout.getCssClassFa());
        CssClassFaPosition cssClassFaPosition = domainObjectLayout.getCssClassFaPosition();
        return cssClassFa != null
                ? Optional.of(new FaFacetForDomainObjectLayoutXml(
                        cssClassFa, cssClassFaPosition, holder, precedence))
                : Optional.empty();
    }

    private FaFacetForDomainObjectLayoutXml(
            final String value,
            final CssClassFaPosition position,
            final FacetHolder holder,
            final Facet.Precedence precedence) {
        super(value, position, holder, precedence);
    }

    @Override
    public boolean isObjectTypeSpecific() {
        return true;
    }

}

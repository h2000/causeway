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
package org.apache.causeway.persistence.jdo.metamodel.facets.object.datastoreidentity;

import java.util.Optional;

import javax.jdo.annotations.DatastoreIdentity;
import javax.jdo.annotations.IdGeneratorStrategy;

import org.apache.causeway.core.metamodel.facetapi.FacetHolder;
import org.apache.causeway.persistence.jdo.provider.metamodel.facets.object.datastoreidentity.JdoDatastoreIdentityFacet;

public class JdoDatastoreIdentityFacetAnnotation
extends JdoDatastoreIdentityFacetAbstract {

    public static Optional<JdoDatastoreIdentityFacet> create(
            final Optional<DatastoreIdentity> datastoreIdentityIfAny,
            final FacetHolder facetHolder) {

        return datastoreIdentityIfAny
                .map(DatastoreIdentity::strategy)
                .map(strategy->new JdoDatastoreIdentityFacetAnnotation(strategy, facetHolder));
    }

    private JdoDatastoreIdentityFacetAnnotation(
            final IdGeneratorStrategy strategy,
            final FacetHolder facetHolder) {
        super(strategy, facetHolder);
    }

}

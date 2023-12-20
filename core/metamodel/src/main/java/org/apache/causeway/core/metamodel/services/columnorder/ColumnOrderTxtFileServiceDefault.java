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
package org.apache.causeway.core.metamodel.services.columnorder;

import lombok.RequiredArgsConstructor;
import lombok.val;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.applib.annotation.Programmatic;
import org.apache.causeway.applib.services.columnorder.ColumnOrderTxtFileService;
import org.apache.causeway.commons.io.ZipUtils;
import org.apache.causeway.core.metamodel.CausewayModuleCoreMetamodel;
import org.apache.causeway.core.metamodel.spec.ObjectSpecification;
import org.apache.causeway.core.metamodel.spec.feature.MixedIn;
import org.apache.causeway.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.causeway.core.metamodel.spec.feature.ObjectFeature;
import org.apache.causeway.core.metamodel.spec.feature.OneToManyAssociation;
import org.apache.causeway.core.metamodel.specloader.SpecificationLoader;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * since 2.0 {@index}
 */
@Service
@Named(CausewayModuleCoreMetamodel.NAMESPACE + ".ColumnOrderTxtFileServiceDefault")
@Priority(PriorityPrecedence.LATE)
@Qualifier("Default")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ColumnOrderTxtFileServiceDefault implements ColumnOrderTxtFileService {

    final SpecificationLoader specificationLoader;

    @Programmatic
    public byte[] toZip(final Object domainObject) {
        val zipBuilder = ZipUtils.zipEntryBuilder();

        addStandaloneEntry(domainObject, zipBuilder);
        addCollectionEntries(domainObject, zipBuilder);

        return zipBuilder.toBytes();
    }


    // HELPERS

    private void addStandaloneEntry(Object domainObject, final ZipUtils.EntryBuilder zipBuilder) {
        val parentSpec = specificationLoader.loadSpecification(domainObject.getClass());
        val buf = new StringBuilder();

        parentSpec.streamAssociations(MixedIn.INCLUDED)
                .map(ObjectFeature::getId)
                .forEach(assocId -> buf.append(assocId).append("\n"));

        val fileContents = buf.toString();
        val fileName = String.format("%s.columnOrder.txt", parentSpec.getShortIdentifier());

        zipBuilder.addAsUtf8(fileName, fileContents);
    }

    private void addCollectionEntries(Object domainObject, final ZipUtils.EntryBuilder zipBuilder) {
        val parentSpec = specificationLoader.loadSpecification(domainObject.getClass());
        parentSpec.streamCollections(MixedIn.INCLUDED)
                .forEach(collection -> addCollection(collection, parentSpec, zipBuilder));
    }

    private void addCollection(final OneToManyAssociation collection, final ObjectSpecification parentSpec, final ZipUtils.EntryBuilder zipBuilder) {
        val buf = new StringBuilder();

        val collectionIdentifier = collection.getFeatureIdentifier();
        val elementType = collection.getElementType();

        elementType.streamAssociations(MixedIn.INCLUDED)
                .filter(ObjectAssociation.Predicates.visibleAccordingToHiddenFacet(collectionIdentifier))
                .filter(ObjectAssociation.Predicates.referencesParent(parentSpec).negate())
                .map(ObjectFeature::getId)
                .forEach(assocId -> buf.append(assocId).append("\n"));

        val fileName = String.format("%s#%s.columnOrder.txt", parentSpec.getShortIdentifier(), collection.getId());
        val fileContents = buf.toString();

        zipBuilder.addAsUtf8(fileName, fileContents);
    }

}

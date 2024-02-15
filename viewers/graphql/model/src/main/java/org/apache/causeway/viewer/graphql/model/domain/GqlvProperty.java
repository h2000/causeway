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
package org.apache.causeway.viewer.graphql.model.domain;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;

import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.applib.value.Clob;
import org.apache.causeway.core.metamodel.spec.ObjectSpecification;
import org.apache.causeway.core.metamodel.spec.feature.OneToOneAssociation;
import org.apache.causeway.viewer.graphql.model.context.Context;
import org.apache.causeway.viewer.graphql.model.types.TypeMapper;

import lombok.val;

public class GqlvProperty
        extends GqlvAssociation<OneToOneAssociation, GqlvMember.Holder>
        implements GqlvMemberHidden.Holder<OneToOneAssociation>,
        GqlvMemberDisabled.Holder<OneToOneAssociation>,
        GqlvPropertyGet.Holder,
        GqlvPropertyChoices.Holder,
        GqlvPropertyAutoComplete.Holder,
        GqlvPropertyValidate.Holder,
        GqlvPropertySet.Holder,
        GqlvAssociationDatatype.Holder<OneToOneAssociation>,
        GqlvPropertyGetBlob.Holder,
        GqlvPropertyGetClob.Holder {

    private final GqlvMemberHidden<OneToOneAssociation> hidden;
    private final GqlvMemberDisabled<OneToOneAssociation> disabled;
    private final GqlvAbstract get;
    /**
     * Populated iff there are choices
     */
    private final GqlvPropertyChoices choices;
    /**
     * Populated iff there is an autoComplete
     */
    private final GqlvPropertyAutoComplete autoComplete;
    private final GqlvPropertyValidate validate;
    /**
     * Populated iff the API variant allows for it.
     */
    private final GqlvPropertySet set;

    private final GqlvPropertyDatatype datatype;

    public GqlvProperty(
            final Holder holder,
            final OneToOneAssociation oneToOneAssociation,
            final Context context) {
        super(holder, oneToOneAssociation, TypeNames.propertyTypeNameFor(holder.getObjectSpecification(), oneToOneAssociation), context);

        addChildFieldFor(this.hidden = new GqlvMemberHidden<>(this, context));
        addChildFieldFor(this.disabled = new GqlvMemberDisabled<>(this, context));

        this.get = isBlob() ? new GqlvPropertyGetBlob(this, context) : isClob() ? new GqlvPropertyGetClob(this, context) : new GqlvPropertyGet(this, context);
        addChildFieldFor(
                isBlob()
                    ? new GqlvPropertyGetBlob(this, context)
                    : isClob()
                        ? new GqlvPropertyGetClob(this, context)
                        : new GqlvPropertyGet(this, context)
        );

        addChildFieldFor(this.validate = new GqlvPropertyValidate(this, context));
        addChildFieldFor(this.choices = new GqlvPropertyChoices(this, context));
        addChildFieldFor(this.autoComplete = new GqlvPropertyAutoComplete(this, context));
        addChildFieldFor(this.set = isSetterAllowed() ? new GqlvPropertySet(this, context) : null);
        addChildFieldFor(this.datatype = new GqlvPropertyDatatype(this, context));

        buildObjectTypeAndField(oneToOneAssociation.getId());
    }

    private boolean isSetterAllowed() {
        val apiVariant = context.causewayConfiguration.getViewer().getGraphql().getApiVariant();
        switch (apiVariant) {
            case QUERY_ONLY:
            case QUERY_AND_MUTATIONS:
                return false;
            case QUERY_WITH_MUTATIONS_NON_SPEC_COMPLIANT:
                return true;
            default:
                // shouldn't happen
                throw new IllegalArgumentException("Unknown API variant: " + apiVariant);
        }
    }



    private boolean isBlob() {
        return getOneToOneAssociation().getElementType().getCorrespondingClass() == Blob.class;
    }

    private boolean isClob() {
        return getOneToOneAssociation().getElementType().getCorrespondingClass() == Clob.class;
    }

    public void addGqlArgument(
            final OneToOneAssociation oneToOneAssociation,
            final GraphQLFieldDefinition.Builder builder,
            final TypeMapper.InputContext inputContext) {
        builder.argument(gqlArgumentFor(oneToOneAssociation, inputContext));
    }

    private GraphQLArgument gqlArgumentFor(
            final OneToOneAssociation oneToOneAssociation,
            final TypeMapper.InputContext inputContext) {
        return GraphQLArgument.newArgument()
                .name(oneToOneAssociation.getId())
                .type(context.typeMapper.inputTypeFor(oneToOneAssociation, inputContext))
                .build();
    }


    @Override
    public ObjectSpecification getObjectSpecification() {
        return holder.getObjectSpecification();
    }

    public OneToOneAssociation getOneToOneAssociation() {
        return getObjectAssociation();
    }

    @Override
    protected void addDataFetchersForChildren() {
        hidden.addDataFetcher(this);
        disabled.addDataFetcher(this);

        get.addDataFetcher(this);

        if(choices != null) {
            choices.addDataFetcher(this);
        }

        if(autoComplete != null) {
            autoComplete.addDataFetcher(this);
        }
        validate.addDataFetcher(this);

        if (set != null) {
            set.addDataFetcher(this);
        }

        datatype.addDataFetcher(this);
    }

}

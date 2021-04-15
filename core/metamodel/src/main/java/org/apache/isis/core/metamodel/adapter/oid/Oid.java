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

package org.apache.isis.core.metamodel.adapter.oid;

import java.io.Serializable;
import java.util.Optional;

import org.apache.isis.applib.annotation.Value;
import org.apache.isis.applib.id.LogicalType;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.commons.internal.codec._UrlDecoderUtil;
import org.apache.isis.core.metamodel.context.MetaModelContext;
import org.apache.isis.core.metamodel.objectmanager.load.ObjectLoader;
import org.apache.isis.core.metamodel.spec.ManagedObject;
import org.apache.isis.schema.common.v2.OidDto;

import lombok.NonNull;
import lombok.val;

/**
 * An immutable identifier for a root object.
 *
 * @apiNote value objects (strings, ints, {@link Value}s etc) do not have a 
 * semantically meaningful {@link Oid}, but as an implementation detail 
 * might have a placeholder {@link Oid}. 
 */
public interface Oid extends Serializable {

    // -- FACTORIES

    
    public static Oid empty() {
        return _EmptyOid.INSTANCE;
    }

    public static Oid root(final LogicalType logicalType, final String identifier) {
        return _RootOid.of(
                logicalType.getLogicalTypeName(), 
                identifier);
    }
    
    public static Oid forBookmark(final Bookmark bookmark) {
        return _RootOid.of(
                bookmark.getLogicalTypeName(), 
                bookmark.getIdentifier());
    }
    
    public static Oid forDto(final OidDto oid) {
        return _RootOid.of(
                oid.getType(), 
                oid.getId());
    }
    
    // --
    
    /**
     * A string representation of this {@link Oid}.
     */
    String enString();

    default boolean isEmpty() {
        return false; // default, only overridden by Oid_Value
    }
    
    /**
     * The logical-type-name of the domain object this instance is representing.
     * When representing a value returns {@code null}.
     */
    String getLogicalTypeName();

    // -- MARSHALLING

    public static interface Marshaller {
        String marshal(Oid oid);
        String joinAsOid(String logicalTypeName, String instanceId);
    }

    public static Marshaller marshaller() {
        return _OidMarshaller.INSTANCE;
    }

    // -- UN-MARSHALLING

    public static interface Unmarshaller {
        <T extends Oid> T unmarshal(String oidStr, Class<T> requestedType);
        String splitInstanceId(String oidStr);
    }

    public static Unmarshaller unmarshaller() {
        return _OidMarshaller.INSTANCE;
    }
    
    // -- REFACTORING ...
    
    String getIdentifier();

    Bookmark asBookmark();

    // -- DECODE FROM STRING

    public static Oid deStringEncoded(final String urlEncodedOidStr) {
        final String oidStr = _UrlDecoderUtil.urlDecode(urlEncodedOidStr);
        return deString(oidStr);
    }

    public static Oid deString(final String oidStr) {
        return Oid.unmarshaller().unmarshal(oidStr, Oid.class);
    }

    // -- OBJECT LOADING
    
    default public Optional<ManagedObject> loadObject(final @NonNull MetaModelContext mmc) {
        
        val objectId = this.getIdentifier();
        val specLoader = mmc.getSpecificationLoader(); 
        val objManager = mmc.getObjectManager();
        
        return specLoader
                .specForLogicalTypeName(this.getLogicalTypeName())
                .map(spec->objManager.loadObject(
                        ObjectLoader.Request.of(spec, objectId)));
        
    }

}

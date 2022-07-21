/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.isis.applib.services.bookmark.idstringifiers;

import java.util.UUID;

import javax.annotation.Priority;

import org.springframework.stereotype.Component;

import org.apache.isis.applib.annotation.PriorityPrecedence;
import org.apache.isis.applib.services.bookmark.IdStringifier;

import lombok.NonNull;

@Component
@Priority(PriorityPrecedence.LATE)
public class IdStringifierForUuid extends IdStringifier.AbstractWithPrefix<UUID> {

    public IdStringifierForUuid() {
        super(UUID.class, "u");
    }

    @Override
    public UUID doDestring(final @NonNull String stringified, @NonNull Class<?> targetEntityClass) {
        return UUID.fromString(stringified);
    }

}

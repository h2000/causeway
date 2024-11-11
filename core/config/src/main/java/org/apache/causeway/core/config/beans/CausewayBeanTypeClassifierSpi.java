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
package org.apache.causeway.core.config.beans;

import org.apache.causeway.applib.id.LogicalType;

/**
 * ServiceLoader SPI that allows for implementing instances to have a say during bean type scanning.
 * @since 2.0
 */
@FunctionalInterface
public interface CausewayBeanTypeClassifierSpi {

    /**
     * Returns the bean classification for given {@code type}.
     *
     * @apiNote Initially used to collect all concrete types that are considered by Spring
     * for type inspection, most likely without any {@code context} yet being available,
     * but later used by the {@code SpecificationLoader} to also
     * classify non-concrete types (interfaces and abstract classes).
     */
    CausewayBeanMetaData classify(LogicalType logicalType);

}

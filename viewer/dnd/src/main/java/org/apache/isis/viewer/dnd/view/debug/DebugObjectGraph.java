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


package org.apache.isis.viewer.dnd.view.debug;

import org.apache.isis.core.commons.debug.DebuggableWithTitle;
import org.apache.isis.core.commons.debug.DebugString;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.runtimes.dflt.runtime.context.IsisContext;
import org.apache.isis.runtimes.dflt.runtime.util.Dump;


public class DebugObjectGraph implements DebuggableWithTitle {
    private final ObjectAdapter object;

    public DebugObjectGraph(final ObjectAdapter object) {
        this.object = object;
    }

    public void debugData(final DebugString debug) {
        dumpGraph(object, debug);
    }

    public String debugTitle() {
        return "Object Graph";
    }

    private void dumpGraph(final ObjectAdapter object, final DebugString info) {
        if (object != null) {
            Dump.graph(object, info, IsisContext.getAuthenticationSession());
        }
    }
}

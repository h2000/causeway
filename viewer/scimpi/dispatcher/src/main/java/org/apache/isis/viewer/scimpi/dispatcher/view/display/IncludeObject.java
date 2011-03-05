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


package org.apache.isis.viewer.scimpi.dispatcher.view.display;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.runtimes.dflt.runtime.context.IsisContext;
import org.apache.isis.viewer.scimpi.dispatcher.AbstractElementProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.ForbiddenException;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestContext.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.Request;

/**
 * Element to include another file that will display an object.
 */
public class IncludeObject extends AbstractElementProcessor {

    public void process(Request request) {
        String path = request.getOptionalProperty("file");
        String id = request.getOptionalProperty(OBJECT);
        String fieldName = request.getOptionalProperty(FIELD);
        ObjectAdapter object = request.getContext().getMappedObjectOrResult(id);
        if (fieldName != null) {
            ObjectAssociation field = object.getSpecification().getAssociation(fieldName);
            if (field.isVisible(IsisContext.getAuthenticationSession(), object).isVetoed()) {
                throw new ForbiddenException(field, ForbiddenException.VISIBLE);
            }
            object = field.get(object);
            id = request.getContext().mapObject(object, Scope.REQUEST);
        }
        
        if (object != null) {
            IsisContext.getPersistenceSession().resolveImmediately(object);
            request.getContext().addVariable("_object", id, Scope.REQUEST);
            importFile(request, path); 
        }
        request.closeEmpty();
    }
    
    private static void importFile(Request request, String path) { 
    // TODO load in file via HtmlFileParser 
        File file = new File(path); 
        try { 
            if (file.exists()) { 
                BufferedReader reader; 
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file))); 
                String line; 
                while ((line = reader.readLine()) != null) { 
                    request.appendHtml(line); 
                } 
            } else { 
                request.appendHtml("<P classs=\"error\">File " + path + " not found to import</P>"); 
            } 
        } catch (FileNotFoundException e) { 
            throw new RuntimeException(e); 
        } catch (IOException e) { 
            throw new RuntimeException(e); 
        } 
    } 
    
    public String getName() {
        return "include-object";
    }

}


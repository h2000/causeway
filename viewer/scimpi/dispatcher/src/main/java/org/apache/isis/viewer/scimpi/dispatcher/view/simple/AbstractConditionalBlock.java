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


package org.apache.isis.viewer.scimpi.dispatcher.view.simple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.isis.core.commons.authentication.AuthenticationSession;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.consent.Consent;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacet;
import org.apache.isis.core.metamodel.facets.typeof.TypeOfFacet;
import org.apache.isis.core.metamodel.spec.ActionType;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.runtimes.dflt.runtime.context.IsisContext;
import org.apache.isis.viewer.scimpi.dispatcher.AbstractElementProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.ScimpiException;
import org.apache.isis.viewer.scimpi.dispatcher.action.Attributes;
import org.apache.isis.viewer.scimpi.dispatcher.processor.Request;
import org.apache.isis.viewer.scimpi.dispatcher.util.MethodsUtils;


public abstract class AbstractConditionalBlock extends AbstractElementProcessor {

    private static Map<String, Test> tests = new HashMap<String, Test>();
    
    static {
        addNormal(new TestHasRole(), "has-role");
        addNegated(new TestHasRole(), "has-not-role");

        addNormal(new TestVariableExists(), "variable-exists");
        addNegated(new TestVariableExists(), "variable-missing");
        addNormal(new TestVariableTrue(), "variable-true");
        addNegated(new TestVariableTrue(), "variable-false");
        
        addNormal(new TestObjectPersistent(), "object-persistent");
        addNegated(new TestObjectPersistent(), "object-transient");
        addNormal(new TestObjectType(), "object-type");
        /*
        addNormal(new TestObjectIs(), "object-is");
        addNegated(new TestObjectIs(), "object-is-not");
        addNormal(new TestObjectType(), "object-type");
        addNormal(new TestObjectType(), "object-title-equals");
        addNegated(new TestObjectType(), "object-title-not-equals");
        addNormal(new TestObjectType(), "object-title-contains");
        addNegated(new TestObjectType(), "object-title-not-contains");
*/
        addNormal(new TestCollectionFull(), "collection-full");
        addNegated(new TestCollectionFull(), "collection-empty");
        addNormal(new TestCollectionType(), "collection-type");
//      addNormal(new TestCollectionSize(), "collection-size-equal");
//      addNormal(new TestCollectionSize(), "collection-size-less-than");
//      addNormal(new TestCollectionSize(), "collection-size-greater-than");

        
        addNormal(new TestFieldExists(), "field-exists");
        addNegated(new TestFieldExists(), "field-missing");
        addNormal(new TestFieldVisible(), "field-visible");
        addNegated(new TestFieldVisible(), "field-hidden");
        addNormal(new TestFieldEditable(), "field-editable");
        addNegated(new TestFieldEditable(), "field-not-editable");
        addNormal(new TestFieldType(), "field-type");
        addNormal(new TestFieldSet(), "field-set");
        addNegated(new TestFieldSet(), "field-empty");
        // empty/set etc
        
        addNormal(new TestMethodExists(), "method-exists");
        addNegated(new TestMethodExists(), "method-missing");
        addNormal(new TestMethodVisible(), "method-visible");
        addNegated(new TestMethodVisible(), "method-hidden");
        addNormal(new TestMethodUseable(), "method-useable");
        addNegated(new TestMethodUseable(), "method-not-useable");
        
           
    }
    
    private static void addNegated(Test test, String name) {
        test.name = name;
        test.negateResult = true;
        tests.put(name, test);
    }

    private static void addNormal(Test test, String name) {
        test.name = name;
        tests.put(name, test);
    }

    @Override
    public void process(Request request) {
        String id = request.getOptionalProperty(OBJECT);
        
        boolean checkMade = false;
        boolean allConditionsMet = true;
        
        String[] propertyNames = request.getAttributes().getPropertyNames(new String[] {"object", "collection"});
        for (String propertyName : propertyNames) {
            boolean result;
            if (propertyName.equals("set")) {
                result = request.isPropertySet("set");
            } else {
                Test test = tests.get(propertyName);
                if (test == null) {
                    throw new ScimpiException("No such test: " + propertyName);
                }
                String attributeValue = request.getOptionalProperty(propertyName, false);
                result = test.test(request, attributeValue, id);
                if (test.negateResult) {
                    result =  !result;
                }
            }
            checkMade = true;
            allConditionsMet &= result;
        }
        
        
       /* 
        
        // Check variables       
        if (request.isPropertySpecified("variable-exists")) {
            boolean valuePresent = request.isPropertySet("variable-exists");
            checkMade = true;
            allConditionsMet &= valuePresent;
        }
        
        String variable = request.getOptionalProperty("variable-true");
        if (variable != null) {
            String value = (String) request.getContext().getVariable(variable);
            checkMade = true;
            allConditionsMet &= Attributes.isTrue(value);
        }

        variable = request.getOptionalProperty("variable-equals");
        if (variable != null) {
            String value = (String) request.getContext().getVariable(variable);
            checkMade = true;
            allConditionsMet &= variable.equals(value);
        }

        */
        // Check Object
        
        /*
        // Check Collection
        String collection = request.getOptionalProperty("collection-" + TYPE);
        if (collection != null) {
            ObjectAdapter object = MethodsUtils.findObject(request.getContext(), collection);
            Class<?> cls = forClass(request);
            TypeOfFacet facet = object.getSpecification().getFacet(TypeOfFacet.class);
            boolean hasType = object != null && (cls == null || cls.isAssignableFrom(facet.value())); 
            checkMade = true;
            allConditionsMet &= hasType;;
        }
        
        collection = request.getOptionalProperty("collection-" + "empty");
        if (collection != null) {
            ObjectAdapter object = MethodsUtils.findObject(request.getContext(), id);
            CollectionFacet facet = object.getSpecification().getFacet(CollectionFacet.class);
            boolean isEmpty = facet != null &&   facet.size(object) == 0;
            processTags(isEmpty, request);
            allConditionsMet &= isEmpty;
        }*/
        
        // Check Methods
        /*
        String method = request.getOptionalProperty(METHOD + "-exists");
        if (method != null) {
            ObjectAdapter object = MethodsUtils.findObject(request.getContext(), id);
            List<? extends ObjectAction> objectActions = object.getSpecification().getObjectActions(ActionType.USER);
            boolean methodExists = false;
            for (ObjectAction objectAssociation : objectActions) {
                if (objectAssociation.getId().equals(method)) {
                    methodExists = true;
                    break;  
                }
            }
            checkMade = true;
            allConditionsMet &= methodExists;
        }
        
        method = request.getOptionalProperty(METHOD + "-visible");
        if (method != null) {
            ObjectAdapter object = MethodsUtils.findObject(request.getContext(), id);
            // TODO needs to work irrespective of parameters 
            ObjectAction objectAction = object.getSpecification().getObjectAction(ActionType.USER, method, ObjectSpecification.EMPTY_LIST);
            Consent visible = objectAction.isVisible(IsisContext.getAuthenticationSession(), object);
            checkMade = true;
            allConditionsMet &= visible.isAllowed();
        }

        method = request.getOptionalProperty(METHOD + "-usable");
        if (method != null) {
            ObjectAdapter object = MethodsUtils.findObject(request.getContext(), id);
            // TODO needs to work irrespective of parameters 
            ObjectAction objectAction = object.getSpecification().getObjectAction(ActionType.USER, method, ObjectSpecification.EMPTY_LIST);
            Consent usable = objectAction.isUsable(IsisContext.getAuthenticationSession(), object);
            checkMade = true;
            allConditionsMet &= usable.isAllowed();
        }
        
        
        // Check Fields
        String field = request.getOptionalProperty(FIELD + "-exists");
        if (field != null) {
            ObjectAdapter object = MethodsUtils.findObject(request.getContext(), id);
            List<? extends ObjectAssociation> objectFields = object.getSpecification().getAssociations();
            boolean fieldExists = false;
            for (ObjectAssociation objectAssociation : objectFields) {
                if (objectAssociation.getId().equals(field)) {
                    fieldExists = true;
                    break;  
                }
            }
            checkMade = true;
            allConditionsMet &= fieldExists;
        }
        
        field = request.getOptionalProperty(FIELD + "-visible");
        if (field != null) {
            ObjectAdapter object = MethodsUtils.findObject(request.getContext(), id);
            ObjectAssociation objectField = object.getSpecification().getAssociation(field);
            Consent visible = objectField.isVisible(IsisContext.getAuthenticationSession(), object);
            checkMade = true;
            allConditionsMet &= visible.isAllowed();
        }

        field = request.getOptionalProperty(FIELD + "-editable");
        if (field != null) {
            ObjectAdapter object = MethodsUtils.findObject(request.getContext(), id);
            ObjectAssociation objectField = object.getSpecification().getAssociation(field);
            Consent usable = objectField.isUsable(IsisContext.getAuthenticationSession(), object);
            checkMade = true;
            allConditionsMet &= usable.isAllowed();
        }
       
        field = request.getOptionalProperty(FIELD + "-empty");
        if (field != null) {
            ObjectAdapter object = MethodsUtils.findObject(request.getContext(), id);
            ObjectAssociation objectField = object.getSpecification().getAssociation(field);
            IsisContext.getPersistenceSession().resolveField(object, objectField);
            ObjectAdapter fld = objectField.get(object);
            if (fld == null) {
                checkMade = true;
                allConditionsMet &= true;
            } else {
                CollectionFacet facet = fld.getSpecification().getFacet(CollectionFacet.class);
                boolean isEmpty = facet != null &&   facet.size(fld) == 0;
                processTags(isEmpty, request);
                allConditionsMet &= isEmpty;
            }
        }

        field = request.getOptionalProperty(FIELD + "-set");
        if (field != null) {
            ObjectAdapter object = MethodsUtils.findObject(request.getContext(), id);
            ObjectAssociation objectField = object.getSpecification().getAssociation(field);
            IsisContext.getPersistenceSession().resolveField(object, objectField);
            ObjectAdapter fld = objectField.get(object);
            if (fld == null) {
                throw new ScimpiException("No object for field-set " + field);
            }
            Object fieldValue = fld.getObject();
            if (fieldValue instanceof Boolean) {
                checkMade = true;
                allConditionsMet &= ((Boolean) fieldValue).booleanValue();
            } else {
                checkMade = true;
                allConditionsMet &= true;
            }
        }
*/
        
        // Check User
  /*      String hasRole = request.getOptionalProperty("has-role");
        if (hasRole != null) {
            AuthenticationSession session = IsisContext.getSession().getAuthenticationSession();
            List<String> roles = session.getRoles();
            boolean hasMatchingRole = false;
            for (String role : roles) {
                if (role.equals(hasRole.trim())) {
                    hasMatchingRole = true;
                    break;
                }
            }
            checkMade = true;
            allConditionsMet &= hasMatchingRole;
        }
        
     */   
        
        
        
        

        String persistent = request.getOptionalProperty("persistent");
        if (persistent != null) {
            ObjectAdapter object = request.getContext().getMappedObjectOrResult(persistent);
            checkMade = true;
            allConditionsMet &= object.isPersistent();
        }
/*
        String type = request.getOptionalProperty(TYPE);
        if (type != null) {
            ObjectAdapter object = MethodsUtils.findObject(request.getContext(), id);
            Class<?> cls = forClass(request);
            boolean hasType = object != null && (cls == null || cls.isAssignableFrom(object.getObject().getClass())); 
            checkMade = true;
            allConditionsMet &= hasType;;
        }
*/
        if (request.isPropertySpecified("empty")) {
            if (request.isPropertySet("empty")) {
                String collection = request.getOptionalProperty("empty");
                if (collection != null) {
                    ObjectAdapter object = request.getContext().getMappedObjectOrResult(collection);
                    CollectionFacet facet = object.getSpecification().getFacet(CollectionFacet.class);
                        checkMade = true;
                        allConditionsMet &= facet.size(object) == 0;
                }
            } else {
                checkMade = true;
                allConditionsMet &= true;
            }
        }
        
        if (request.isPropertySpecified("set")) {
            boolean valuePresent = request.isPropertySet("set");
            checkMade = true;
            allConditionsMet &= valuePresent;
        }
        
        
        
        
        
        
        if (checkMade) {
            processTags(allConditionsMet, request);
        } else {
            throw new ScimpiException("No condition in " + getName());
        }
    }

    protected abstract void processTags(boolean isSet, Request request);

}


abstract class Test {
    String name;
    boolean negateResult;

    abstract boolean test(Request request, String attributeName, String targetId);
    
    protected Class<?> forClass(String className) { 
        Class<?> cls = null; 
        if (className != null) {
            try {
                cls = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new ScimpiException("No class for " + className, e);
            }
        }
        return cls;
    }
}

class TestVariableExists extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        return request.getContext().getVariable(attributeName) != null;
    }
}

class TestVariableTrue extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        String value = (String) request.getContext().getVariable(attributeName);
        return Attributes.isTrue(value);
    }
}

class TestObjectPersistent extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        ObjectAdapter object = request.getContext().getMappedObjectOrResult(attributeName);
        return object.isPersistent();
    }
}

class TestObjectType extends TestObjectPersistent {
    boolean test(Request request, String attributeName, String targetId) {
        ObjectAdapter object = MethodsUtils.findObject(request.getContext(), targetId);
        Class<?> cls = forClass(attributeName);
        boolean hasType = object != null && (cls == null || cls.isAssignableFrom(object.getObject().getClass())); 
        return hasType;
    }
}

class TestCollectionType extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        ObjectAdapter object = MethodsUtils.findObject(request.getContext(), targetId);
        Class<?> cls = forClass(attributeName);
        TypeOfFacet facet = object.getSpecification().getFacet(TypeOfFacet.class);
        boolean hasType = object != null && (cls == null || cls.isAssignableFrom(facet.value())); 
        return hasType;
    }
}

class TestCollectionFull extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        ObjectAdapter object = MethodsUtils.findObject(request.getContext(), attributeName);
        CollectionFacet facet = object.getSpecification().getFacet(CollectionFacet.class);
        boolean isEmpty = facet != null &&   facet.size(object) == 0;
        return !isEmpty;
    }
}

class TestMethodExists extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        ObjectAdapter object = MethodsUtils.findObject(request.getContext(), targetId);
        List<? extends ObjectAction> objectActions = object.getSpecification().getObjectActions(ActionType.USER);
        boolean methodExists = false;
        for (ObjectAction objectAssociation : objectActions) {
            if (objectAssociation.getId().equals(attributeName)) {
                methodExists = true;
                break;  
            }
        }
        return methodExists;
    }
}

class TestMethodVisible extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        ObjectAdapter object = MethodsUtils.findObject(request.getContext(), targetId);
        // TODO needs to work irrespective of parameters 
        ObjectAction objectAction = object.getSpecification().getObjectAction(ActionType.USER, attributeName, ObjectSpecification.EMPTY_LIST);
        Consent visible = objectAction.isVisible(IsisContext.getAuthenticationSession(), object);
        return visible.isAllowed();
    }
}

class TestMethodUseable extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        ObjectAdapter object = MethodsUtils.findObject(request.getContext(), targetId);
        // TODO needs to work irrespective of parameters 
        ObjectAction objectAction = object.getSpecification().getObjectAction(ActionType.USER, attributeName, ObjectSpecification.EMPTY_LIST);
        Consent usable = objectAction.isUsable(IsisContext.getAuthenticationSession(), object);
        return usable.isAllowed();
    }
}

class TestFieldExists extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        ObjectAdapter object = MethodsUtils.findObject(request.getContext(), targetId);
        List<? extends ObjectAssociation> objectFields = object.getSpecification().getAssociations();
        boolean fieldExists = false;
        for (ObjectAssociation objectAssociation : objectFields) {
            if (objectAssociation.getId().equals(attributeName)) {
                fieldExists = true;
                break;  
            }
        }
        return fieldExists;
    }
}

class TestFieldVisible extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        ObjectAdapter object = MethodsUtils.findObject(request.getContext(), targetId);
        ObjectAssociation objectField = object.getSpecification().getAssociation(attributeName);
        Consent visible = objectField.isVisible(IsisContext.getAuthenticationSession(), object);
        return visible.isAllowed();
    }
}

class TestFieldEditable extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        ObjectAdapter object = MethodsUtils.findObject(request.getContext(), targetId);
        ObjectAssociation objectField = object.getSpecification().getAssociation(attributeName);
        Consent usable = objectField.isUsable(IsisContext.getAuthenticationSession(), object);
        return usable.isAllowed();
    }
}

class TestFieldType extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        ObjectAdapter object = MethodsUtils.findObject(request.getContext(), targetId);
        Class<?> cls = forClass(attributeName);
        boolean hasType = object != null && (cls == null || cls.isAssignableFrom(object.getObject().getClass())); 
        return hasType;
    }
}

class TestFieldSet extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        ObjectAdapter object = MethodsUtils.findObject(request.getContext(), targetId);
        ObjectAssociation objectField = object.getSpecification().getAssociation(attributeName);
        IsisContext.getPersistenceSession().resolveField(object, objectField);
        ObjectAdapter fld = objectField.get(object);
        if (fld != null) {
            Object fieldValue = fld.getObject();
            if (fieldValue instanceof Boolean) {
                return ((Boolean) fieldValue).booleanValue();
            } else if (fld.getSpecification().containsFacet(CollectionFacet.class)) {
                CollectionFacet facet = fld.getSpecification().getFacet(CollectionFacet.class);
                boolean isEmpty = facet != null &&   facet.size(fld) == 0;
                return !isEmpty;
            } else {
                return true;
            }
        }
        return false;
    }
}

class TestHasRole extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        String[] requiredRoles = attributeName.split("\\|");
        AuthenticationSession session = IsisContext.getSession().getAuthenticationSession();
        List<String> sessionRoles = session.getRoles();
        for (String sessionRole : sessionRoles) {
            for (String requiredRole : requiredRoles) {
                if (requiredRole.trim().equals(sessionRole)) {
                    return true;
                }
            }
        }
        return false;
    }
}

class TestSet extends Test {
    boolean test(Request request, String attributeName, String targetId) {
        boolean valuePresent = request.isPropertySet("set");
        return valuePresent;
    }
}



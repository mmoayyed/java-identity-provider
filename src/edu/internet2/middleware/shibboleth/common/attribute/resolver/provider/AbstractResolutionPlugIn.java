/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.AttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.DataConnector;


/**
 * Base class for all {@link ResolutionPlugIn}s.
 * 
 * @param <ResolvedType> object type this plug-in resolves to
 */
public abstract class AbstractResolutionPlugIn<ResolvedType> implements ResolutionPlugIn<ResolvedType> {

    /** The identifier for this plug-in. */
    private String id;

    /** IDs of the {@link ResolutionPlugIn}s this plug-in depends on. */
    private List<String> dependencyIds;

    /** Constructor. */
    public AbstractResolutionPlugIn() {
        dependencyIds = new ArrayList<String>();
    }

    /** {@inheritDoc} */
    public List<String> getDependencyIds() {
        return dependencyIds;
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /**
     * Set plug-in id.
     * 
     * @param newId new plug-in id
     */
    public void setId(String newId) {
        id = newId;
    }

    /**
     * Get values from dependencies.
     * 
     * @param context resolution context
     * @param sourceAttribute ID of attribute to retrieve from dependencies
     * @return collection of values
     */
    protected Collection<Object> getValuesFromAllDependencies(ShibbolethResolutionContext context, String sourceAttribute) {
        List<Object> values = new ArrayList<Object>();
    
        for (String id : getDependencyIds()) {
            if (context.getResolvedAttributeDefinitions().containsKey(id)) {
                values.addAll(getValuesFromAttributeDependency(context, id));
            } else if (context.getResolvedDataConnectors().containsKey(id)) {
                values.addAll(getValuesFromConnectorDependency(context, id, sourceAttribute));
            }
        }
    
        return values;
    }

    /**
     * Get values from attribute dependencies.
     * 
     * @param context resolution context
     * @param id ID of attribute to retrieve dependencies for
     * 
     * @return collection of values
     */
    protected Collection<Object> getValuesFromAttributeDependency(ShibbolethResolutionContext context, String id) {
        List<Object> values = new ArrayList<Object>();
    
        AttributeDefinition definition = context.getResolvedAttributeDefinitions().get(id);
        if (definition != null) {
            try {
                BaseAttribute attribute = definition.resolve(context);
                for (Object o : attribute.getValues()) {
                    values.add(o);
                }
            } catch (AttributeResolutionException e) {
                // TODO Auto-generated catch block
            }
        }
    
        return values;
    }

    /**
     * Get values from data connectors.
     * 
     * @param context resolution context
     * @param id ID of attribute to retrieve dependencies for
     * @param sourceAttribute ID of attribute to retrieve from connector dependencies
     * 
     * @return collection of values
     */
    protected Collection<Object> getValuesFromConnectorDependency(ShibbolethResolutionContext context, String id, String sourceAttribute) {
        List<Object> values = new ArrayList<Object>();
    
        DataConnector connector = context.getResolvedDataConnectors().get(id);
        if (connector != null) {
            try {
                Map<String, BaseAttribute> attributes = connector.resolve(context);
                for (String attributeId : attributes.keySet()) {
                    if (attributeId != null && attributeId.equals(sourceAttribute)) {
                        for (Object o : attributes.get(attributeId).getValues()) {
                            values.add(o);
                        }
                    }
                }
            } catch (AttributeResolutionException e) {
                // TODO Auto-generated catch block
            }
    
        }
    
        return values;
    }
}
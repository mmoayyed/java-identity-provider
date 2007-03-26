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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.AbstractResolutionPlugIn;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.DataConnector;

/**
 * Base class for {@link AttributeDefinition} plug-ins.
 */
public abstract class BaseAttributeDefinition extends AbstractResolutionPlugIn<Attribute> implements
        AttributeDefinition {

    /** Whether this attribute definition is only a dependency and thus its values should never be released. */
    private boolean dependencyOnly;

    /** Attribute encoders associated with this definition. */
    private Map<String, AttributeEncoder> encoders;

    /** Name of the attribute from data connectors to use to populate this definition. */
    private String sourceAttributeID;

    /**
     * Constructor.
     */
    public BaseAttributeDefinition() {
        dependencyOnly = false;
        encoders = new HashMap<String, AttributeEncoder>();
    }

    /** {@inheritDoc} */
    public boolean isDependencyOnly() {
        return dependencyOnly;
    }

    /**
     * Sets whether this attribute definition is only a dependency and thus its values should never be released outside
     * the resolver.
     * 
     * @param isDependencyOnly whether this attribute definition is only a dependency
     */
    public void setDependencyOnly(boolean isDependencyOnly) {
        dependencyOnly = isDependencyOnly;
    }

    /** {@inheritDoc} */
    public Map<String, AttributeEncoder> getAttributeEncoders() {
        return encoders;
    }

    /** {@inheritDoc} */
    public Attribute resolve(ShibbolethResolutionContext resolutionContext) throws AttributeResolutionException {
        Attribute resolvedAttribute = doResolve(resolutionContext);

        if (getAttributeEncoders() != null) {
            resolvedAttribute.getEncoders().putAll(getAttributeEncoders());
        }

        return resolvedAttribute;
    }

    /**
     * Creates and populates the values for the resolved attribute. Implimentations should *not* set, or otherwise
     * manage, the attribute encoders for the resolved attribute.
     * 
     * @param resolutionContext current attribute resolution context
     * 
     * @return resolved attribute
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving and creating the attribute
     */
    protected abstract Attribute doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException;

    /**
     * Get values from dependencies.
     * 
     * @param context resolution context
     * @return collection of values
     */
    protected Collection<Object> getValuesFromAllDependencies(ShibbolethResolutionContext context) {
        Set<Object> values = new HashSet<Object>();

        if (!getAttributeDefinitionDependencyIds().isEmpty()) {
            values.addAll(getValuesFromAttributeDependencies(context));
        }

        if (!getDataConnectorDependencyIds().isEmpty()) {
            values.addAll(getValuesFromConnectorDependencies(context));
        }

        return values;
    }

    /**
     * Get values from attribute dependencies.
     * 
     * @param context resolution context
     * @return collection of values
     */
    protected Collection<Object> getValuesFromAttributeDependencies(ShibbolethResolutionContext context) {
        Set<Object> values = new HashSet<Object>();

        for (String id : getAttributeDefinitionDependencyIds()) {
            AttributeDefinition definition = context.getResolvedAttributeDefinitions().get(id);
            if (definition != null) {
                try {
                    Attribute attribute = definition.resolve(context);
                    for (Object o : attribute.getValues()) {
                        values.add(o);
                    }
                } catch (AttributeResolutionException e) {
                    // TODO Auto-generated catch block
                }
            }
        }

        return values;
    }

    /**
     * Get values from data connectors.
     * 
     * @param context resolution context
     * @return collection of values
     */
    protected Collection<Object> getValuesFromConnectorDependencies(ShibbolethResolutionContext context) {
        Set<Object> values = new HashSet<Object>();

        for (String connectorId : getDataConnectorDependencyIds()) {
            DataConnector connector = context.getResolvedDataConnectors().get(connectorId);
            if (connector != null) {
                try {
                    Map<String, Attribute> attributes = connector.resolve(context);
                    for (String attributeId : attributes.keySet()) {
                        if (attributeId != null
                                && (getSourceAttributeID() != null && attributeId.equals(getSourceAttributeID()))
                                || attributeId.equals(getId())) {
                            for (Object o : attributes.get(attributeId).getValues()) {
                                values.add(o);
                            }
                        }
                    }
                } catch (AttributeResolutionException e) {
                    // TODO Auto-generated catch block
                }
            }
        }

        return values;
    }

    /**
     * Return the source attribute. If the source attribute is null, return the definition ID.
     * 
     * @return Returns the sourceAttribute.
     */
    public String getSourceAttributeID() {
        if (sourceAttributeID != null) {
            return sourceAttributeID;
        } else {
            return getId();
        }
    }

    /**
     * Set the source attribute.
     * 
     * @param newSourceAttributeID The sourceAttribute to set.
     */
    public void setSourceAttributeID(String newSourceAttributeID) {
        sourceAttributeID = newSourceAttributeID;
    }

}
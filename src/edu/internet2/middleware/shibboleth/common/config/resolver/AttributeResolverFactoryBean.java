/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.config.resolver;

import java.util.Collection;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.DataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.PrincipalConnector;

/**
 * FactoryBean for creating the {@link AttributeResolver}.
 */
public class AttributeResolverFactoryBean extends AbstractFactoryBean {

    /** Attribute resolver. */
    private AttributeResolver resolver;

    /** Attribute definitions. */
    private Collection<AttributeDefinition> attributeDefinitions;

    /** Data connectors. */
    private Collection<DataConnector> dataConnectors;

    /** Principal connectors. */
    private Collection<PrincipalConnector> principalConnectors;

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        if (attributeDefinitions != null) {
            for (AttributeDefinition definition : attributeDefinitions) {
                resolver.getAttributeDefinitions().put(definition.getId(), definition);
            }
        }

        if (dataConnectors != null) {
            for (DataConnector connector : dataConnectors) {
                resolver.getDataConnectors().put(connector.getId(), connector);
            }
        }

        if (principalConnectors != null) {
            for (PrincipalConnector connector : principalConnectors) {
                resolver.getPrincipalConnectors().put(connector.getId(), connector);
            }
        }

        return resolver;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return AttributeResolver.class;
    }

    /**
     * Set the resolver.
     * 
     * @param newResolver the resolver
     */
    public void setResolver(AttributeResolver newResolver) {
        resolver = newResolver;
    }

    /**
     * Set the {@link AttributeDefinition}s.
     * 
     * @param definitions collection of definitions.
     */
    public void setAttributeDefinitions(Collection<AttributeDefinition> definitions) {
        attributeDefinitions = definitions;
    }

    /**
     * Set the {@link DataConnector}s.
     * 
     * @param connectors collection of connectors.
     */
    public void setDataConnectors(Collection<DataConnector> connectors) {
        dataConnectors = connectors;
    }

    /**
     * Set the {@link PrincipalConnector}s.
     * 
     * @param connectors collection of connectors.
     */
    public void setPrincipalConnectors(Collection<PrincipalConnector> connectors) {
        principalConnectors = connectors;
    }

}
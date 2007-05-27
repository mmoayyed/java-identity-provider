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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver;

import java.util.List;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.AttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.DataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.principalConnector.PrincipalConnector;

/**
 * A bean that represents the set of loaded plugins for a resolver resource.
 */
public class AttributeResolverBean {

    /** Loaded principal connectors. */
    private List<PrincipalConnector> principalConnectors;

    /** Loaded data connectors. */
    private List<DataConnector> dataConnectors;

    /** Loaded attribute definitions. */
    private List<AttributeDefinition> attributeDefinitions;

    /**
     * Gets the attribute definitions for this service.
     * 
     * @return attribute definitions for this service
     */
    public List<AttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    /**
     * Sets the attribute definitions for this service.
     * 
     * @param definitions attribute definitions for this service
     */
    public void setAttributeDefinitions(List<AttributeDefinition> definitions) {
        attributeDefinitions = definitions;
    }

    /**
     * Gets the data connectors for this service.
     * 
     * @return data connectors for this service
     */
    public List<DataConnector> getDataConnectors() {
        return dataConnectors;
    }

    /**
     * Sets the data connectors for this service.
     * 
     * @param connectors data connectors for this service
     */
    public void setDataConnectors(List<DataConnector> connectors) {
        dataConnectors = connectors;
    }

    /**
     * Gets the principal connectors for this service.
     * 
     * @return principal connectors for this service
     */
    public List<PrincipalConnector> getPrincipalConnectors() {
        return principalConnectors;
    }

    /**
     * Sets the principal connectors for this service.
     * 
     * @param connectors principal connectors for this service
     */
    public void setPrincipalConnectors(List<PrincipalConnector> connectors) {
        principalConnectors = connectors;
    }
}
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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector;

import java.util.List;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.StaticDataConnector;

/**
 * Spring bean factory that produces {@link StaticDataConnector}s.
 */
public class StaticDataConnectorFactoryBean extends BaseDataConnectorBeanFactory {

    /** Static attributes returned by the created data connector. */
    private List<Attribute<String>> staticAttributes;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return StaticDataConnector.class;
    }

    /**
     * Gets the static attributes returned by the created data connector.
     * 
     * @return static attributes returned by the created data connector
     */
    public List<Attribute<String>> getStaticAttributes() {
        return staticAttributes;
    }

    /**
     * Sets the static attributes returned by the created data connector.
     * 
     * @param attributes static attributes returned by the created data connector
     */
    public void setStaticAttributes(List<Attribute<String>> attributes) {
        staticAttributes = attributes;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        StaticDataConnector connector = new StaticDataConnector(staticAttributes);
        connector.setId(getPluginId());

        if(getAttributeDefinitionDependencyIds() != null){
            connector.getAttributeDefinitionDependencyIds().addAll(getAttributeDefinitionDependencyIds());
        }
        
        if(getDataConnectorDependencyIds() != null){
            connector.getDataConnectorDependencyIds().addAll(getDataConnectorDependencyIds());
        }
        
        if(getFailoverDataConnectorIds()!= null){
            connector.getFailoverDependencyIds().addAll(getFailoverDataConnectorIds());
        }
        
        return connector;
    }
}
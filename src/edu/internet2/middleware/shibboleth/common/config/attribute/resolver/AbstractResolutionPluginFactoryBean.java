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

import java.util.Set;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Base class for resolver resolution plugin factories.
 */
public abstract class AbstractResolutionPluginFactoryBean extends AbstractFactoryBean {

    /** ID of attribute definitions this plugin depends on. */
    private Set<String> attributeDefinitionDependencyIds;
    
    /** ID of data connectors this plugin depends on. */
    private Set<String> dataConnectorDependencyIds;
    
    /** Unqieu ID of the plugin. */
    private String pluginId;

    /**
     * Gets the ID of attribute definitions this plugin depends on.
     * 
     * @return ID of attribute definitions this plugin depends on
     */
    public Set<String> getAttributeDefinitionDependencyIds() {
        return attributeDefinitionDependencyIds;
    }

    /**
     * Sets the ID of attribute definitions this plugin depends on.
     * 
     * @param ids ID of attribute definitions this plugin depends on
     */
    public void setAttributeDefinitionDependencyIds(Set<String> ids) {
        attributeDefinitionDependencyIds = ids;
    }

    /**
     * Gets the ID of data connectors this plugin depends on.
     * 
     * @return ID of data connectors this plugin depends on
     */
    public Set<String> getDataConnectorDependencyIds() {
        return dataConnectorDependencyIds;
    }

    /**
     * Sets the ID of data connectors this plugin depends on.
     * 
     * @param ids ID of data connectors this plugin depends on
     */
    public void setDataConnectorDependencyIds(Set<String> ids) {
        dataConnectorDependencyIds = ids;
    }

    /**
     * Gets the unique ID of this plugin.
     * 
     * @return unique ID of this plugin
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Sets the unique ID of this plugin.
     * 
     * @param id unique ID of this plugin
     */
    public void setPluginId(String id) {
        pluginId = id;
    }    
}
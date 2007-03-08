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

import org.springframework.beans.factory.FactoryBean;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionPlugIn;

/**
 * TODO
 */
public abstract class AbstractResolutionPlugInFactoryBean<PlugInType extends ResolutionPlugIn<?>> implements
        FactoryBean {

    /** Internal resolution plugin. */
    private PlugInType plugin;

    /** Ids of attribute defintion dependencies. */
    private Collection<String> attributeDefinitionDependencyIds;

    /** Ids of data connector dependencies. */
    private Collection<String> dataConnectorDependencyIds;

    /** {@inheritDoc} */
    public Object getObject() throws Exception {
        buildPlugin(plugin);

        return plugin;
    }

    /**
     * Implementations should perform any custom building of the plugin such as setting properties build from child
     * elements. Most implementations should be sure to call <code>super.buildPlugin(internalPlugin)</code>.
     * 
     * @param internalPlugin internal plugin to build
     */
    protected void buildPlugin(PlugInType internalPlugin) {
        if (attributeDefinitionDependencyIds != null) {
            for (String id : attributeDefinitionDependencyIds) {
                if (id != null) {
                    internalPlugin.getAttributeDefinitionDependencyIds().add(id);
                }
            }
        }

        if (dataConnectorDependencyIds != null) {
            for (String id : dataConnectorDependencyIds) {
                if (id != null) {
                    internalPlugin.getDataConnectorDependencyIds().add(id);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public boolean isSingleton() {
        return false;
    }

    /**
     * Set plugin.
     * 
     * @param newPlugin new plugin
     */
    public void setPlugin(PlugInType newPlugin) {
        plugin = newPlugin;
    }

    /**
     * Set attributeDefinitionDependencyIds.
     * 
     * @param newAttributeDefinitionDependencyIds new ids
     */
    public void setAttributeDefinitionDependencyIds(Collection<String> newAttributeDefinitionDependencyIds) {
        attributeDefinitionDependencyIds = newAttributeDefinitionDependencyIds;
    }

    /**
     * Set dataConnectorDependencyIds.
     * 
     * @param newDataConnectorDependencyIds new ids
     */
    public void setDataConnectorDependencyIds(Collection<String> newDataConnectorDependencyIds) {
        dataConnectorDependencyIds = newDataConnectorDependencyIds;
    }

}
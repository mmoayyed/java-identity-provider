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

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.BaseDataConnector;
import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.AbstractResolutionPluginFactoryBean;

/**
 * Base class for data connector factories.
 */
public abstract class BaseDataConnectorFactoryBean extends AbstractResolutionPluginFactoryBean {

    /** ID of failover data connectors for this plugin. */
    private String failoverDataConnectorId;

    /**
     * Gets the ID of failover data connectors for this plugin.
     * 
     * @return ID of failover data connectors for this plugin
     */
    public String getFailoverDataConnectorId() {
        return failoverDataConnectorId;
    }

    /**
     * Sets the ID of failover data connectors for this plugin.
     * 
     * @param id ID of failover data connectors for this plugin
     */
    public void setFailoverDataConnectorIds(String id) {
        failoverDataConnectorId = id;
    }

    /**
     * Populates data connector with information from this factory.
     * 
     * @param connector data connector with information from this factory
     */
    protected void populateDataConnector(BaseDataConnector connector) {
        connector.setId(getPluginId());

        if (getDependencyIds() != null) {
            connector.getDependencyIds().addAll(getDependencyIds());
        }

        if (getFailoverDataConnectorId() != null) {
            connector.setFailoverDependencyIds(getFailoverDataConnectorId());
        }
    }
}
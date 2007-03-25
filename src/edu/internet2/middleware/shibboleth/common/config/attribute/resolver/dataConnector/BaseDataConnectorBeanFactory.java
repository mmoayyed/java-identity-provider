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

import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.AbstractResolutionPluginFactoryBean;

/**
 * Base class for data connector factories.
 */
public abstract class BaseDataConnectorBeanFactory extends AbstractResolutionPluginFactoryBean {

    /** IDs of failover data connectors for this plugin. */
    private List<String> failoverDataConnectorIds;

    /**
     * Gets the IDs of failover data connectors for this plugin.
     * 
     * @return IDs of failover data connectors for this plugin
     */
    public List<String> getFailoverDataConnectorIds() {
        return failoverDataConnectorIds;
    }

    /**
     * Sets the IDs of failover data connectors for this plugin.
     * 
     * @param ids IDs of failover data connectors for this plugin
     */
    public void setFailoverDataConnectorIds(List<String> ids) {
        failoverDataConnectorIds = ids;
    }
}
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

import edu.internet2.middleware.shibboleth.common.attribute.resolver.DataConnector;

/**
 * Base factory bean for data connectors.
 * 
 * @param <PlugInType> type of data connector
 */
public abstract class BaseDataConnectorFactoryBean<PlugInType extends DataConnector> extends
        AbstractResolutionPlugInFactoryBean<PlugInType> {

    /** Ids of failover data connectors. */
    private Collection<String> failoverDataConnectorIds;

    /** {@inheritDoc} */
    protected void buildPlugin(PlugInType internalPlugin) {
        super.buildPlugin(internalPlugin);

        if (failoverDataConnectorIds != null && !failoverDataConnectorIds.isEmpty()) {
            for (String id : failoverDataConnectorIds) {
                if (id != null) {
                    internalPlugin.getAttributeDefinitionDependencyIds().add(id);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return DataConnector.class;
    }

    /**
     * Set failoverDataConnectorIds.
     * 
     * @param newFailoverDataConnectorIds new ids
     */
    public void setFailoverDataConnectorIds(Collection<String> newFailoverDataConnectorIds) {
        failoverDataConnectorIds = newFailoverDataConnectorIds;
    }

}
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

import edu.internet2.middleware.shibboleth.common.attribute.impl.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.impl.StaticDataConnector;

/**
 * FactoryBean for creating {@link StaticDataConnector}s.
 */
public class StaticDataConnectorFactoryBean extends BaseDataConnectorFactoryBean<StaticDataConnector> {

    /** Attributes. */
    private Collection<BaseAttribute<String>> attributes;

    /**
     * Set the attributes.
     * 
     * @param newAttributes collection of attributes.
     */
    public void setAttributes(Collection<BaseAttribute<String>> newAttributes) {
        attributes = newAttributes;
    }

    /** {@inheritDoc} */
    protected void buildPlugin(StaticDataConnector connector) {
        super.buildPlugin(connector);

        if (attributes != null && !attributes.isEmpty()) {
            for (BaseAttribute<String> a : attributes) {
                connector.getSourceData().add(a);
            }
        }
    }

}
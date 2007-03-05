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

import javolution.util.FastList;

import org.springframework.beans.factory.FactoryBean;

import edu.internet2.middleware.shibboleth.common.attribute.impl.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.impl.StaticDataConnector;

/**
 * FactoryBean for creating {@link StaticDataConnector}s.
 */
public class StaticDataConnectorFactoryBean implements FactoryBean {

    /** Data connector. */
    private StaticDataConnector connector;

    /** Attributes. */
    private Collection<BaseAttribute<String>> attributes;

    /** Constructor. */
    public StaticDataConnectorFactoryBean() {
        attributes = new FastList<BaseAttribute<String>>();
    }

    /**
     * Set the data connector.
     * 
     * @param newConnector the data connector
     */
    public void setConnector(StaticDataConnector newConnector) {
        connector = newConnector;
    }

    /**
     * Set the attributes.
     * 
     * @param newAttributes collection of attributes.
     */
    public void setAttributes(Collection<BaseAttribute<String>> newAttributes) {
        attributes = newAttributes;
    }

    /** {@inheritDoc} */
    public Object getObject() throws Exception {
        for (BaseAttribute<String> a : attributes) {
            connector.getSourceData().add(a);
        }

        return connector;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return StaticDataConnector.class;
    }

    /** {@inheritDoc} */
    public boolean isSingleton() {
        return false;
    }

}
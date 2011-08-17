/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Map;

import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.UnmodifiableComponentException;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;


/** A data connector that just returns a static collection of attributes. */
@ThreadSafe
public class StaticDataConnector extends BaseDataConnector {

    /** Static collection of values returned by this connector. */
    private Map<String, Attribute<?>> values;

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == values) {
            throw new ComponentInitializationException("Static Data connector " + getId()
                    + " does not have values set up.");
        }
    }

    /**
     * Set the values used.
     * @param newValues what to set.
     */
    public synchronized void setValues(Map<String, Attribute<?>> newValues) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Static Data Connector " + getId()
                    + " has already been initialized, values can not be changed.");
        }
        values = newValues;
    }
    
    /**
     * Get our values.
     * @return the values we return.
     */
    public Map<String, Attribute<?>> getValues() {
        return values;
    }
    
    /** {@inheritDoc} */
    protected Map<String, Attribute<?>> doDataConnectorResolve(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        return values;
    }
}
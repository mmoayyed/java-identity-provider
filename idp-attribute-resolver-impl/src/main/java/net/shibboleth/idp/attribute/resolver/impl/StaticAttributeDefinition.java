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

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;

import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.UnmodifiableComponentException;

/** An attribute definition that simply returns a static value. */
@ThreadSafe
public class StaticAttributeDefinition extends BaseAttributeDefinition {

    /** Static value returned by this definition. */
    private Attribute<?> value;

    /**
     * Set the attribute value we are returning.
     * @param newAttrribute what to set.
     */
    public synchronized void setAttribute(Attribute<?> newAttrribute) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Static Attribute definition " + getId()
                    + " has already been initialized, attribute can not be changed.");
        }
        value = newAttrribute;
    }

    /**
     * Return the static attribute we are returning.
     * @return the attribute.
     */
    public Attribute<?> getValue() {
        return value;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == value) {
            throw new ComponentInitializationException("Static Attribute definition " + getId()
                    + " does not have an attribute set up.");
        }
    }

    /** {@inheritDoc} */
    protected Attribute<?> doAttributeResolution(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        return value;
    }
}
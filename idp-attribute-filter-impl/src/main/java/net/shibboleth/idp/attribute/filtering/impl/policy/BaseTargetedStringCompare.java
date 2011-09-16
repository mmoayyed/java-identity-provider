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

package net.shibboleth.idp.attribute.filtering.impl.policy;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.util.StringSupport;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.UnmodifiableComponentException;

/**
 * The basis of all String-based Filter criteria.
 * 
 * Principal, AttributeValue, AttributeScope criteria all extend this. This class's job is to just provide the match
 * functor that they call.
 */
@ThreadSafe
public abstract class BaseTargetedStringCompare extends BaseStringCompare {

    /** The name of the target attribute. */
    private String attributeName;

    /**
     * Gets the name of the attribute under consideration.
     * 
     * @return the name of the attribute under consideration, never null or empty after initialization.
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Sets the attribute name. Cannot be called after initialization.
     * 
     * @param theName the name of the attribute to user.
     */
    public synchronized void setAttributeName(final String theName) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attempting to set the attribute name after class initialization");
        }
        attributeName = StringSupport.trimOrNull(theName);
    }

    /** Check parameters. {@inheritDoc}. */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (null == attributeName) {
            throw new ComponentInitializationException(
                    "String comparison criterion being initialized without a valid attribute name being set");
        }
    }
}
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

package net.shibboleth.idp.attribute.filter.impl.policyrule.saml;

import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import com.google.common.base.Objects;


/**
 * Base class for matchers that perform an exact match of a given attribute string value against entity attribute
 * value.  Most of the work is done by {@link AbstractEntityAttributePolicyRule}, this class purely handle the
 * comparison.
 */
public abstract class AbstractEntityAttributeExactPolicyRule extends AbstractEntityAttributePolicyRule {

    /** The value of the entity attribute the entity must have. */
    private String value;

    /**
     * Gets the value of the entity attribute the entity must have.
     * 
     * @return value of the entity attribute the entity must have
     */
    @NonnullAfterInit public String getValue() {
        return value;
    }

    /**
     * Sets the value of the entity attribute the entity must have.
     * 
     * @param attributeValue value of the entity attribute the entity must have
     */
    public void setValue(@Nullable final String attributeValue) {
        value = attributeValue;
    }

    /** {@inheritDoc} */
    protected boolean entityAttributeValueMatches(@Nullable final String stringValue) {
        return Objects.equal(value, stringValue);
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (null == value) {
            throw new ComponentInitializationException(getLogPrefix() + " No value supplied to compare against");
        }
    }
}

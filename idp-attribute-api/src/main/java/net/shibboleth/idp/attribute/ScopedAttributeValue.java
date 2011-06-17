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

package net.shibboleth.idp.attribute;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.util.Assert;
import org.opensaml.util.ObjectSupport;
import org.opensaml.util.StringSupport;

/** An attribute value with an associated scope. */
@ThreadSafe
public class ScopedAttributeValue {

    /** Value of the attribute. */
    private final String value;

    /** Scope of the attribute value. */
    private final String scope;

    /**
     * Constructor.
     * 
     * @param attributeValue value of the attribute, never null
     * @param valueScope scope of the value, never null
     */
    public ScopedAttributeValue(final String attributeValue, final String valueScope) {
        value = StringSupport.trimOrNull(attributeValue);
        Assert.isNotNull(value, "Attribute value may not be null or empty");

        scope = StringSupport.trimOrNull(valueScope);
        Assert.isNotNull(scope, "Attribute value scope may not be null or empty");
    }

    /**
     * Gets the value of the attribute.
     * 
     * @return value of the attribute
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the scope of the value.
     * 
     * @return scope of the value
     */
    public String getScope() {
        return scope;
    }

    /** {@inheritDoc} */
    public String toString() {
        return value;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + value.hashCode();
        hash = hash * 31 + scope.hashCode();

        return hash;
    }

    /** {@inheritDoc} */
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ScopedAttributeValue)) {
            return false;
        }

        ScopedAttributeValue otherValue = (ScopedAttributeValue) obj;
        return ObjectSupport.equals(getValue(), otherValue.getValue())
                && ObjectSupport.equals(getScope(), otherValue.getScope());
    }
}
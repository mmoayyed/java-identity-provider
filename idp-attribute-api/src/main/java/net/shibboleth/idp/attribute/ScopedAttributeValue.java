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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Assert;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Objects;

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
    public ScopedAttributeValue(@Nonnull @NotEmpty final String attributeValue,
            @Nonnull @NotEmpty final String valueScope) {
        value = Assert.isNotNull(StringSupport.trimOrNull(attributeValue), "Value may not be null or empty");
        scope = Assert.isNotNull(StringSupport.trimOrNull(valueScope), "Scope may not be null or empty");
    }

    /**
     * Gets the value of the attribute.
     * 
     * @return value of the attribute
     */
    @Nonnull @NotEmpty public String getValue() {
        return value;
    }

    /**
     * Gets the scope of the value.
     * 
     * @return scope of the value
     */
    @Nonnull @NotEmpty public String getScope() {
        return scope;
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String toString() {
        return Objects.toStringHelper(this).add("value", value).add("scope", scope).toString();
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return Objects.hashCode(value, scope);
    }

    /** {@inheritDoc} */
    public boolean equals(@Nullable final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ScopedAttributeValue)) {
            return false;
        }

        ScopedAttributeValue otherValue = (ScopedAttributeValue) obj;
        return Objects.equal(getValue(), otherValue.getValue()) && Objects.equal(getScope(), otherValue.getScope());
    }
}
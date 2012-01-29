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

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Assert;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Objects;

/** Base class for {@link Attribute} values that are strings. */
public class StringAttributeValue implements AttributeValue<String> {

    /** The attribute value. */
    private final String value;

    /**
     * Constructor.
     * 
     * @param attributeValue the attribute value
     */
    public StringAttributeValue(@Nonnull @NotEmpty String attributeValue) {
        value = Assert.isNotNull(StringSupport.trim(attributeValue), "Attribute value can not be null or empty");
    }

    /**
     * Gets the attribute value.
     * 
     * @return the attribute value
     */
    @Nonnull @NotEmpty public final String getValue() {
        return value;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof StringAttributeValue)) {
            return false;
        }

        StringAttributeValue other = (StringAttributeValue) obj;
        return Objects.equal(value, other.value);
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return value.hashCode();
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("value", value).toString();
    }
}
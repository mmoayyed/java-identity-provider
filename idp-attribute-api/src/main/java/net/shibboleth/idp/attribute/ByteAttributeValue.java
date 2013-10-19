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

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.apache.commons.codec.binary.Hex;

import com.google.common.base.Objects;

/** A <code>byte[]</code> value for an {@link IdPAttribute}. */
public class ByteAttributeValue implements AttributeValue<byte[]> {

    /** Value of the attribute. */
    private final byte[] value;

    /**
     * Constructor.
     * 
     * @param attributeValue value of the attribute
     */
    public ByteAttributeValue(@Nonnull final byte[] attributeValue) {
        value = Constraint.isNotNull(attributeValue, "Attribute value can not be null");
    }

    /** {@inheritDoc} */
    public final byte[] getValue() {
        return value;
    }

    /**
     * Gets this value as a hex-encoded string.
     * 
     * @return a hex-encoded string
     */
    @Nonnull public String toHex() {
        return Hex.encodeHexString(value);
    }

    /**
     * Gets this value as a Base64-encoded string.
     * 
     * @return a Base64-encoded string
     */
    @Nonnull public String toBase64() {
        return Base64Support.encode(value, Base64Support.UNCHUNKED);
    }

    /** {@inheritDoc} */
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof ByteAttributeValue)) {
            return false;
        }

        ByteAttributeValue other = (ByteAttributeValue) obj;
        return Arrays.equals(other.value, value);
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("value", value).toString();
    }
}
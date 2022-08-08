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

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

import com.google.common.base.MoreObjects;

/**
 * Base class for {@link IdPAttribute} values that are date/time values.
 * 
 * @since 4.3.0
 */
public class DateTimeAttributeValue implements IdPAttributeValue {

    /** The attribute value. */
    @Nonnull @NotEmpty private final Instant value;

    /**
     * Constructor.
     * 
     * @param attributeValue the attribute value
     */
    public DateTimeAttributeValue(
            @Nonnull @NotEmpty @ParameterName(name="attributeValue") final Instant attributeValue) {
        value = Constraint.isNotNull(attributeValue, "Attribute value cannot be null or empty");
    }

    /**
     * Constructor.
     * 
     * @param attributeValue the attribute value
     */
    public DateTimeAttributeValue(
            @Nonnull @NotEmpty @ParameterName(name="attributeValue") final ZonedDateTime attributeValue) {
        value = Constraint.isNotNull(attributeValue, "Attribute value cannot be null or empty").toInstant();
    }

    /** {@inheritDoc} */
    @Override
    public Object getNativeValue() {
        return value;
    }

    /** Return the value.
     * @return the value
     */
    @Nonnull @NotEmpty public final Instant getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override @Nonnull @NotEmpty public String getDisplayValue() {
        return value.toString();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof DateTimeAttributeValue)) {
            return false;
        }

        final DateTimeAttributeValue other = (DateTimeAttributeValue) obj;
        return Objects.equals(value, other.value);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return value.hashCode();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return MoreObjects.toStringHelper(this).add("value", value).toString();
    }

}
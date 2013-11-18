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

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import com.google.common.base.Objects;

/** An optionally localized String value of an {@link IdPAttribute}. */
public class LocalizedStringAttributeValue extends StringAttributeValue {

    /** The locale of the attribute value. */
    private final Locale valueLocale;

    /**
     * Constructor.
     * 
     * @param attributeValue the value of the attribute
     * @param attributeValueLocale the locale of the attribute value
     */
    public LocalizedStringAttributeValue(@Nonnull @NotEmpty final String attributeValue,
            @Nullable Locale attributeValueLocale) {
        super(attributeValue);
        valueLocale = attributeValueLocale;
    }

    /**
     * Gets the locale of the attribute value.
     * 
     * @return the local of the attribute or null if there no explicit locale
     */
    @Nullable public final Locale getValueLocale() {
        return valueLocale;
    }

    /** {@inheritDoc} */
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof LocalizedStringAttributeValue)) {
            return false;
        }

        LocalizedStringAttributeValue other = (LocalizedStringAttributeValue) obj;
        return Objects.equal(getValue(), other.getValue()) && Objects.equal(valueLocale, other.valueLocale);
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return Objects.hashCode(getValue(), valueLocale);
    }

    /** {@inheritDoc} */
    @Nonnull public String toString() {
        return Objects.toStringHelper(this).add("value", getValue()).add("locale", valueLocale).toString();
    }
}
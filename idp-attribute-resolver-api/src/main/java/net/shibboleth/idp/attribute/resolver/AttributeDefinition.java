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

package net.shibboleth.idp.attribute.resolver;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.AttributeEncoder;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

/** Defintion of attribute definition resolver plugins. */
@ThreadSafe
public interface AttributeDefinition extends ResolverPlugin<IdPAttribute> {

    /**
     * Gets whether this attribute definition is only a dependency and thus its values should never be released outside
     * the resolver.
     * 
     * @return true if this attribute is only used as a dependency, false otherwise
     */
    public boolean isDependencyOnly();

    /**
     * Sets whether this attribute definition is only a dependency and thus its values should never be released outside
     * the resolver.
     * 
     * @param isDependencyOnly whether this attribute definition is only a dependency
     */
    public void setDependencyOnly(final boolean isDependencyOnly);

    /**
     * Gets the localized human readable descriptions of attribute.
     * 
     * @return human readable descriptions of attribute
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<Locale, String> getDisplayDescriptions();

    /**
     * Sets the localized human readable descriptions of attribute.
     * 
     * @param descriptions localized human readable descriptions of attribute
     */
    public void setDisplayDescriptions(@Nullable @NullableElements Map<Locale, String> descriptions);

    /**
     * Gets the localized human readable names of the attribute.
     * 
     * @return human readable names of the attribute
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<Locale, String> getDisplayNames();

    /**
     * Sets the localized human readable names of the attribute.
     * 
     * @param names localized human readable names of the attribute
     */
    public void setDisplayNames(@Nullable @NullableElements Map<Locale, String> names);

    /**
     * Gets the unmodifiable encoders used to encode the values of this attribute in to protocol specific formats. The
     * returned collection is never null nor contains any null.
     * 
     * @return encoders used to encode the values of this attribute in to protocol specific formats, never null
     */
    @Nonnull @NonnullElements @Unmodifiable public Set<AttributeEncoder<?>> getAttributeEncoders();

    /**
     * Sets the encoders used to encode the values of this attribute in to protocol specific formats.
     * 
     * @param attributeEncoders encoders used to encode the values of this attribute in to protocol specific formats
     */
    public void setAttributeEncoders(@Nullable @NullableElements Set<AttributeEncoder<?>> attributeEncoders);

    /**
     * Gets the source attribute id.
     * 
     * @return the source attribute id
     */
    public String getSourceAttributeId();

    /**
     * Sets the source attribute id.
     * 
     * @param attributeId the source attribute id
     */
    public void setSourceAttributeId(String attributeId);

}
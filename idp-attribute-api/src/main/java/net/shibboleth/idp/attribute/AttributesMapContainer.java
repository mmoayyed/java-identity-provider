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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Multimap;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

/**
 * Container for decoded attributes. This gives us a distinguished class to look for in the
 * {@link org.opensaml.core.xml.XMLObject#getObjectMetadata()}.
 */
public final class AttributesMapContainer implements Supplier<Multimap<String,IdPAttribute>> {

    /** The map we are encapsulating.*/
    @Nullable @NonnullElements private final Multimap<String,IdPAttribute> providedValue;

    /**
     * Constructor.
     * 
     * @param value the value to return.
     */
    public AttributesMapContainer(@Nullable @NonnullElements final Multimap<String,IdPAttribute> value) {
        providedValue = value;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable @NonnullElements public Multimap<String,IdPAttribute> get() {
        return providedValue;
    }

    /**
     * Shorthand method that returns a collapsed copy of the String values of a given
     * IdPAttribute in the container, or an empty collection. 
     * 
     * @param id    attribute ID
     * 
     * @return unmodifiable collection of string values
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Collection<String> getStringValues(
            @Nonnull @NotEmpty final String id) {
        
        if (providedValue != null) {
            return providedValue.get(id)
                    .stream()
                    .map(IdPAttribute::getValues)
                    .flatMap(List::stream)
                    .filter(StringAttributeValue.class::isInstance)
                    .map(StringAttributeValue.class::cast)
                    .map(StringAttributeValue::getValue)
                    .collect(Collectors.toUnmodifiableList());
        }
        return Collections.emptyList();
    }
    
}
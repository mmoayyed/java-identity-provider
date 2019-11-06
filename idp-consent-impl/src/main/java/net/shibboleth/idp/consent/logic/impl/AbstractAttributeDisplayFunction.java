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

package net.shibboleth.idp.consent.logic.impl;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.net.HttpServletSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Abstract Function which returns {@link Locale}-aware information about an attribute. The abstract method
 * {@link #getDisplayInfo(IdPAttribute)} returns the information selected from the attribute. This function defaults to
 * returning the attribute ID if no information is selected from the attribute for the desired locales.
 */
public abstract class AbstractAttributeDisplayFunction implements Function<IdPAttribute, String> {

    /** The range of locales from the request. */
    @Nonnull @Unmodifiable private final List<Locale.LanguageRange> languageRange;

    /** The tags for the fallback languages. */
    @Nonnull @Unmodifiable private final List<Locale.LanguageRange> defaultLanguageRange;

    
    /**
     * Constructor.
     * 
     * @param request {@link HttpServletRequest} used to get preferred languages
     * @param defaultLanguages list of fallback languages in order of decreasing preference
     */
    public AbstractAttributeDisplayFunction(@Nonnull final HttpServletRequest request,
            @Nullable final List<String> defaultLanguages) {

        languageRange = HttpServletSupport.getLanguageRange(request);
        if (defaultLanguages == null || defaultLanguages.isEmpty()) {
            defaultLanguageRange = Collections.emptyList();
        } else {
            defaultLanguageRange = defaultLanguages.
                    stream().
                    map(StringSupport::trimOrNull).
                    filter(e -> e != null).
                    map(s -> new LanguageRange(s)).
                    collect(Collectors.toUnmodifiableList());
        }
    }

    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final IdPAttribute input) {
        if (input == null) {
            return "N/A";
        }
        
        final Map<Locale, String> displayInfo = getDisplayInfo(input);
        
        Locale locale = Locale.lookup(languageRange, displayInfo.keySet());
        if (locale == null) {
            locale = Locale.lookup(defaultLanguageRange, displayInfo.keySet());
        }
        if (locale == null) {
            return input.getId();
        }
        return displayInfo.get(locale);
    }

    /**
     * Get the information to be displayed from the attribute.
     * 
     * @param input the attribute to consider
     * @return the map of locale dependent information to be displayed
     */
    @Nonnull protected abstract Map<Locale, String> getDisplayInfo(@Nonnull final IdPAttribute input);
}

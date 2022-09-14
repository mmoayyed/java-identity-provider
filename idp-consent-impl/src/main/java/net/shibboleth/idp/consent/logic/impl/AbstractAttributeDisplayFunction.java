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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.shared.spring.util.SpringSupport;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/**
 * Abstract Function which returns {@link Locale}-aware information about an attribute. The abstract method
 * {@link #getDisplayInfo(AttributeTranscoderRegistry, IdPAttribute)} returns the information selected for
 * the attribute from the transcoder.
 * 
 * This function defaults to returning the attribute ID if no information is selected from the attribute
 * for the desired locales.
 */
public abstract class AbstractAttributeDisplayFunction implements Function<IdPAttribute, String> {

    /** The range of locales from the request. */
    @Nonnull @Unmodifiable private final List<Locale.LanguageRange> languageRange;

    /** The tags for the fallback languages. */
    @Nonnull @Unmodifiable private final List<Locale.LanguageRange> defaultLanguageRange;

    /** Cache of already looked up values. */
    @Nonnull private Map<IdPAttribute, Map<Locale, String>> cachedInfo = new HashMap<>();

    /** How to do the lookup. */
    @Nonnull private ReloadableService<AttributeTranscoderRegistry> transcoder;
    
    /**
     * Constructor.
     * 
     * @param request {@link HttpServletRequest} used to get preferred languages
     * @param defaultLanguages list of fallback languages in order of decreasing preference
     * @param transcoderService the attribute transcoder service
     */
    public AbstractAttributeDisplayFunction(@Nonnull final HttpServletRequest request,
            @Nullable final List<String> defaultLanguages,
            final ReloadableService<AttributeTranscoderRegistry> transcoderService) {

        languageRange = SpringSupport.getLanguageRange(request);
        transcoder = Constraint.isNotNull(transcoderService, "Injected transocde service should be non-null");
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

        Map<Locale, String> displayInfo = cachedInfo.get(input);
        if (displayInfo == null) {
            try (final ServiceableComponent<AttributeTranscoderRegistry> component =
                    transcoder.getServiceableComponent()) {
                if (component != null) {
                    displayInfo = getDisplayInfo(component.getComponent(), input);
                }
            }
            cachedInfo.put(input, displayInfo);
        }
        
        Locale locale = Locale.lookup(languageRange, displayInfo.keySet());
        if (locale == null) {
            locale = Locale.lookup(defaultLanguageRange, displayInfo.keySet());
        }
        if (locale == null) {
            return input.getId();
        }
        final String result = displayInfo.get(locale);
        if (result == null) {
            return input.getId();
        }
        return result;
    }

    /**
     * Get the information to be displayed from the attribute.
     * 
     * @param registry the {@link AttributeTranscoderRegistry} to ask.
     * @param attribute the attribute to consider
     * @return the map of locale dependent information to be displayed
     */
    @Nonnull protected abstract Map<Locale, String> getDisplayInfo(
            @Nonnull final AttributeTranscoderRegistry registry,
            @Nonnull final IdPAttribute attribute);
}

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

package net.shibboleth.idp.consent.logic;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import com.google.common.base.Function;

/**
 * Abstract Function which returns information about attribute for the defined {@link Locale}. The abstract method
 * {@link #getDisplayInfo(IdPAttribute)} returns the information select from and the default is the attribute id if no
 * information is returned.
 */
public abstract class AbstractAttributeDisplayFunction implements Function<IdPAttribute, String> {

    /** Desired locales in order of preference. */
    @Nonnull private final List<Locale> locales;

    /**
     * Constructor.
     * 
     * @param request The {@link HttpServletRequest} this is used to get the languages.
     * @param defaultLanguages the comma delimited list of fallback languages
     */
    public AbstractAttributeDisplayFunction(@Nonnull HttpServletRequest request,
            @Nullable List<String> defaultLanguages) {

        final Enumeration<Locale> requestLocales = request.getLocales();

        final List<Locale> newLocales = new ArrayList<>();

        while (requestLocales.hasMoreElements()) {
            newLocales.add(requestLocales.nextElement());
        }
        if (null != defaultLanguages) {
            for (final String s : defaultLanguages) {
                newLocales.add(new Locale(s));
            }
        }
        locales = newLocales;
    }

    /** {@inheritDoc} */
    @Override @Nonnull @NotEmpty public String apply(@Nonnull final IdPAttribute input) {
        if (input == null) {
            return "N/A";
        }
        final Map<Locale, String> displayNames = getDisplayInfo(input);
        if (null != displayNames && !displayNames.isEmpty()) {
            for (final Locale locale : locales) {
                String displayName = displayNames.get(locale);
                if (displayName != null) {
                    return displayName;
                }
                displayName = displayNames.get(Locale.forLanguageTag(locale.getLanguage()));
                if (displayName != null) {
                    return displayName;
                }
            }
        }
        return input.getId();
    }

    /**
     * Get the information to be displayed from the attribute.
     * 
     * @param input The attribute to consider
     * @return the map of locale dependant information.
     */
    @Nonnull protected abstract Map<Locale, String> getDisplayInfo(@Nonnull final IdPAttribute input);
}

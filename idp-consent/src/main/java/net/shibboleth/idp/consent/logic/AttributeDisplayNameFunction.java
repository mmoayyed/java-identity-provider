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

import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Function which returns the display name of an attribute for the defined {@link Locale}, defaulting to the attribute
 * id if no display names are configured.
 */
public class AttributeDisplayNameFunction implements Function<IdPAttribute, String> {

    /** Locale. */
    @Nonnull private final List<Locale> locales;
    
    /**
     * Constructor.
     * 
     * @param request The {@link HttpServletRequest} this is used to get the languages.
     * @param defaultLangauages the comma delimited list of fallback languages
     */
    public AttributeDisplayNameFunction(@Nonnull HttpServletRequest request, @Nullable List<String> defaultLangauages) {

        final Enumeration<Locale> requestLocales = request.getLocales();
        
        final List<Locale> newLocales = new ArrayList<>();

        while (requestLocales.hasMoreElements()) {
            Locale l = requestLocales.nextElement();
            newLocales.add(l);
            LoggerFactory.getLogger(this.getClass()).trace("Adding locale {}", l);
        }
        if (null != defaultLangauages) {
            for (String s : defaultLangauages) {
                newLocales.add(new Locale(s));
                LoggerFactory.getLogger(this.getClass()).trace("Adding language {}", s);
            }
        }
        locales = newLocales;
    }

    /** {@inheritDoc} */
    @Override @Nonnull @NotEmpty public String apply(@Nonnull final IdPAttribute input) {
        if (input == null) {
            return "N/A";
        }
        final Map<Locale, String> displayNames = input.getDisplayNames();
        if (!displayNames.isEmpty()) {
            for (Locale locale : locales) {
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
}

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

package net.shibboleth.idp.saml.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.opensaml.saml.ext.saml2mdui.Keywords;
import org.opensaml.saml.ext.saml2mdui.Logo;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.opensaml.saml.saml2.metadata.LocalizedName;
import org.opensaml.saml.saml2.metadata.LocalizedURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.CollectionSupport;

/**
 * Class to contain a processed form of the {@link UIInfo} suitable for display purposes. */
public class IdPUIInfo {
    
    /** logger. */
    private static final Logger LOG = LoggerFactory.getLogger(IdPUIInfo.class);
    
    /** The Display Names as a map from locale to actual value.*/ 
    @Nonnull @Unmodifiable private final Map<Locale, String> displayNames;
    
    /** The Keywords as a map from locale to lists of actual values.*/ 
    @Nonnull @Unmodifiable private final Map<Locale, List<String>> keywordList;
    
    /** The Descriptions as a map from locale to actual value.*/ 
    @Nonnull @Unmodifiable private final Map<Locale, String> descriptions;
    
    /** The Logos as a map from locale to actual value.*/ 
    @Nonnull @Unmodifiable private final Map<Locale, List<Logo>> localeLogos;

    /** The non Locale bearing Logos .*/ 
    @Nonnull @Unmodifiable private final List<Logo> nonLocaleLogos;

    /** The Information URLs as a map from locale to actual value.*/ 
    @Nonnull @Unmodifiable private final Map<Locale, String> informationURLs;
    
    /** The Privacy Statement URLs as a map from locale to actual value.*/ 
    @Nonnull @Unmodifiable private final Map<Locale, String> privacyStatementURLs; 
    
    /** Warning check against a non localized URL. */
    private final Predicate<LocalizedURI> nullLanguageURL = new Predicate<>() {
        public boolean test(final LocalizedURI u) {
            if (u.getXMLLang() == null) {
                LOG.warn("URI with value {} in <IdpUIInfo/> has no language associated, ignoring", u.getURI());
                return false;
            }
            if (u.getURI() == null) {
                LOG.warn("Ignoring empty URUI in <IdpUIInfo/>", u.getURI());
                return false;
            }

            return true;
        }
    };
    
    /** Warning check against a non localized String. */
    private final Predicate<LocalizedName> nullLanguageString = new Predicate<>() {
        public boolean test(final LocalizedName u) {
            if (u.getXMLLang() == null) {
                LOG.warn("String with value {} in <IdpUIInfo/> has no language associated, ignoring", u.getValue());
                return false;
            }
            if (u.getValue()== null) {
                LOG.warn("Ignoring empty string in <IdpUIInfo/>");
                return false;
            }
            return true;
        }
    };

    /** Warning check against a non localized keyword. */
    private final Predicate<Keywords> nullLanguageKeyword = new Predicate<>() {
        public boolean test(final Keywords u) {
            if (u.getXMLLang() == null) {
                LOG.warn("Keyword with value {} in <IdpUIInfo/> has no language associated, ignoring",
                        u.getKeywords().toString());
                return false;
            }
            return true;
        }
    };

    /**
     * Constructor.
     *
     * @param uiInfo The OpenSaml UIInfo to convert.
     */
    public IdPUIInfo(@Nonnull final UIInfo uiInfo) {
        
        displayNames = uiInfo.
                getDisplayNames().
                stream().
                filter(nullLanguageString).
                collect(Collectors.toUnmodifiableMap(
                        displayName -> Locale.forLanguageTag(displayName.getXMLLang()), 
                        displayName -> displayName.getValue(),
                        CollectionSupport.warningMergeFunction("IdpUIInfo DisplayName", false)));
        keywordList = uiInfo.
                getKeywords().
                stream().
                filter(nullLanguageKeyword).
                collect(Collectors.toUnmodifiableMap(
                        keywords -> Locale.forLanguageTag(keywords.getXMLLang()), 
                        keywords -> keywords.getKeywords(),
                        CollectionSupport.warningMergeFunction("IdpUIInfo Keyword", false)));

        descriptions = uiInfo.
                getDescriptions().
                stream().
                filter(nullLanguageString).
                collect(Collectors.toUnmodifiableMap(
                        description -> Locale.forLanguageTag(description.getXMLLang()), 
                        description -> description.getValue(),
                        CollectionSupport.warningMergeFunction("IdpUIInfo Descriptions", false)));

        informationURLs = uiInfo.
                getInformationURLs().
                stream().
                filter(nullLanguageURL).
                collect(Collectors.toUnmodifiableMap(
                        url -> Locale.forLanguageTag(url.getXMLLang()), 
                        dn -> dn.getURI(),
                        CollectionSupport.warningMergeFunction("IdpUIInfo InformationURL", false)));

        privacyStatementURLs = uiInfo.
                getPrivacyStatementURLs().
                stream().
                filter(nullLanguageURL).
                collect(Collectors.toUnmodifiableMap(
                        url -> Locale.forLanguageTag(url.getXMLLang()), 
                        url -> url.getURI(),
                        CollectionSupport.warningMergeFunction("IdpUIInfo PrivacyStatementURL", false)));
        
        final List<Logo> noLocaleLogo = new ArrayList<>();
        final Map<Locale, List<Logo>> withLocaleLogo = new HashMap<>();
        for (final Logo logo : uiInfo.getLogos()) {
            if (logo.getURI() == null) {
                LOG.warn("IdpUIInfo has Logo with null URL, ignoring");
                continue;
            }
            if (logo.getXMLLang() != null) {
                final Locale l = Locale.forLanguageTag(logo.getXMLLang());
                if (withLocaleLogo.get(l) == null) {
                    withLocaleLogo.put(l, new ArrayList<>());
                }
                withLocaleLogo.get(l).add(logo);
            } else { 
                noLocaleLogo.add(logo);
            }
        }
        localeLogos = Collections.unmodifiableMap(withLocaleLogo);
        nonLocaleLogos = Collections.unmodifiableList(noLocaleLogo);
    }

    /** 
     * Get the Display Names as a map from locale to actual value.
     * 
     * @return the display names
     */
    @Nonnull @Unmodifiable public Map<Locale, String> getDisplayNames() {
        return displayNames; 
    }
    
    /** 
     * Get the keywords as a map from locale to actual value.
     *
     * @return the display names
     */
    @Nonnull @Unmodifiable public Map<Locale, List<String>> getKeywords() {
        return keywordList;
    }
    
    /**
     * Return the descriptions as a map from locale to actual value.
     *
     * @return the descriptions names (if any)
     */
    @Nonnull @Unmodifiable public Map<Locale, String> getDescriptions() {
        return descriptions;
    }
    
    /**
     * Get the logos as a map from locale to the OpenSAML {@link Logo}.
     *
     * @return the logos (if any)
     */
    @Nonnull @Unmodifiable public Map<Locale, List<Logo>> getLocaleLogos() {
        return localeLogos;
    }
    
    /**
     * Get the logos with no associated locale to the OpenSAML {@link Logo}.
     *
     * @return the logos (if any)
     */
    @Nonnull @Unmodifiable public List<Logo> getNonLocaleLogos() {
        return nonLocaleLogos;
    }

    
    /** 
     * Get the URLs as a map from locale to actual value.
     * 
     * @return the URLs (if any)
     */
    @Nonnull @Unmodifiable public Map<Locale, String> getInformationURLs() {
        return informationURLs;
    }
    
    /**
     * Get the Privacy Statement URLsas a map from locale to actual value.
     * 
     * @return the URLs (if any)
     */
    @Nonnull @Unmodifiable public Map<Locale, String> getPrivacyStatementURLs() {
        return privacyStatementURLs;
    }
    
}

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

import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.opensaml.saml.saml2.metadata.LocalizedName;
import org.opensaml.saml.saml2.metadata.LocalizedURI;
import org.opensaml.saml.saml2.metadata.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

/**
 * Class to contain a processed form of the {@link Organization} suitable for display purposes. */
public class OrganizationUIInfo {
    
    /** logger. */
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationUIInfo.class);
    
    /** The Organization Names as a map from locale to actual value.*/ 
    @Nonnull @Unmodifiable private final Map<Locale, String> organizationNames;
    
    /** The Organization Display Names as a map from locale to actual value.*/ 
    @Nonnull @Unmodifiable private final Map<Locale, String> displayNames;
       
    /** The Organization URLs as a map from locale to actual value.*/ 
    @Nonnull @Unmodifiable private final Map<Locale, String> urls;
    
    /** Warning check against a non localized String. */
    private final Predicate<LocalizedName> nullLanguageString = new Predicate<>() {
        public boolean test(final LocalizedName u) {
            if (u.getXMLLang() == null) {
                LOG.warn("String with value {} in <{}> has no language associated, ignoring", u.getValue(),
                        u.getElementQName().getLocalPart());
                return false;
            }
            if (u.getValue()== null) {
                LOG.warn("Ignoring empty <{}> element", u.getElementQName().getLocalPart());
                return false;
            }
            return true;
        }
    };
    
    /** Warning check against a non localized URL. */
    private final Predicate<LocalizedURI> nullLanguageURL = new Predicate<>() {
        public boolean test(final LocalizedURI u) {
            if (u.getXMLLang() == null) {
                LOG.warn("URL with value {} in <{}> has no language associated, ignoring", u.getURI(),
                        u.getElementQName().getLocalPart());
                return false;
            }
            if (u.getURI() == null) {
                LOG.warn("Ignoring empty <{}> element", u.getElementQName().getLocalPart());
                return false;
            }
            return true;
        }
    };
    
    /**
     * Constructor.
     *
     * @param organization The OpenSaml Organization to convert.
     */
    public OrganizationUIInfo(@Nonnull final Organization organization) {
        
        organizationNames = organization.
                getOrganizationNames().
                stream().
                filter(nullLanguageString).
                collect(Collectors.toUnmodifiableMap(
                        serviceName -> Locale.forLanguageTag(serviceName.getXMLLang()), 
                        serviceName -> serviceName.getValue()));
        displayNames = organization.
                getDisplayNames().
                stream().
                filter(nullLanguageString).
                collect(Collectors.toUnmodifiableMap(
                        description -> Locale.forLanguageTag(description.getXMLLang()), 
                        description -> description.getValue()));
        
        urls = organization.
                getURLs().
                stream().
                filter(nullLanguageURL).
                collect(Collectors.toUnmodifiableMap(
                        url -> Locale.forLanguageTag(url.getXMLLang()), 
                        dn -> dn.getURI()));
    }

    /** 
     * Get the Organization Names as a map from locale to actual value.
     * 
     * @return the display names
     */
    @Nonnull @Unmodifiable public Map<Locale, String> getOrganizationNames() {
        return organizationNames; 
    }
    
    /**
     * Return the Organization Display Names as a map from locale to actual value.
     *
     * @return the descriptions names (if any)
     */
    @Nonnull @Unmodifiable public Map<Locale, String> getOrganizationDisplayNames() {
        return displayNames;
    }

    /**
     * Return the Organization URLs as a map from locale to actual value.
     *
     * @return the descriptions names (if any)
     */
    @Nonnull @Unmodifiable public Map<Locale, String> getOrganizationUrls() {
        return urls;
    }

}

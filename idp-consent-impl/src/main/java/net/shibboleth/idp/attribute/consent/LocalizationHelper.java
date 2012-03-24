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

package net.shibboleth.idp.attribute.consent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.annotation.concurrent.NotThreadSafe;

import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.ServiceDescription;
import org.opensaml.saml2.metadata.ServiceName;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Localization helper class.
 * 
 * Provides utility methods for getting localized names and descriptions.
 */
@NotThreadSafe
public class LocalizationHelper {

    /** Class logger. */
    private final Logger logger = LoggerFactory.getLogger(LocalizationHelper.class);

    /** Preferred locale. */
    private Locale preferredLocale;

    /** Whether the preferred is enforced or not. */
    private boolean localeEnforcement;

    /** The {@see MetadataProvider} used for resolving localized information. */
    private MetadataProvider metadataProvider;

    /**
     * Sets the preferred locale.
     * 
     * @param newPreferredLocale The preferred locale to set.
     */
    public void setPreferredLocale(final Locale newPreferredLocale) {
        preferredLocale = newPreferredLocale;
    }

    /**
     * Sets or unsets locale enforcement.
     * 
     * @param localeEnforced The locale enforced to set.
     */
    public void setLocaleEnforcement(final boolean localeEnforced) {
        localeEnforcement = localeEnforced;
    }

    /**
     * Sets the metadata provider.
     * 
     * @param newMetadataProvider The metadata provider to set.
     */
    public void setMetadataProvider(final MetadataProvider newMetadataProvider) {
        metadataProvider = newMetadataProvider;
    }

    /**
     * Gets the localized name of a relying party.
     * 
     * @param relyingPartyId The entityId of the relying party.
     * @param userLocale The user's preferred locale.
     * @return Returns the localized name of a relying party.
     */
    public String getRelyingPartyName(final String relyingPartyId, final Locale userLocale) {
        final List<ServiceName> serviceNames = getServiceNames(relyingPartyId);
        final Locale locale = selectLocale(getAvailableNameLocales(serviceNames), userLocale);
        if (locale == null) {
            return relyingPartyId;
        }
        return getRelyingPartyName(serviceNames, locale);
    }

    /**
     * Gets the localized description of a relying party.
     * 
     * @param relyingPartyId The entityId of the relying party.
     * @param userLocale The user's preferred locale.
     * @return Returns the localized description of a relying party.
     */
    public String getRelyingPartyDescription(final String relyingPartyId, final Locale userLocale) {
        final List<ServiceDescription> serviceDescription = getServiceDescriptions(relyingPartyId);
        final Locale locale = selectLocale(getAvailableDescriptionLocales(serviceDescription), userLocale);
        if (locale == null) {
            return "";
        }
        return getRelyingPartyDescription(serviceDescription, locale);
    }

    /**
     * Gets the localized name of an attribute.
     * 
     * @param attribute The attribute
     * @param userLocale The user's preferred locale.
     * @return Returns the localized name of an attribute.
     */
    public String getAttributeName(final Attribute<?> attribute, final Locale userLocale) {
        final Locale locale = selectLocale(attribute.getDisplayNames().keySet(), userLocale);
        logger.debug("Locale {} choosen for attribute {} name", locale, attribute.getId());
        if (locale == null) {
            return attribute.getId();
        }
        return attribute.getDisplayNames().get(locale);
    }

    /**
     * Gets the localized description of an attribute.
     * 
     * @param attribute The attribute
     * @param userLocale The user's preferred locale.
     * @return Returns the localized description of an attribute.
     */
    public String getAttributeDescription(final Attribute<?> attribute, final Locale userLocale) {
        final Locale locale = selectLocale(attribute.getDisplayDescriptions().keySet(), userLocale);
        logger.debug("Locale {} choosen for attribute {} description", locale, attribute.getId());
        if (locale == null) {
            return "";
        }
        return attribute.getDisplayDescriptions().get(locale);
    }

    /**
     * Selects the right locale from a collection of available locales.
     * 
     * First it is checked whether a locale is enforced. If yes and it is available, too, it is selected, else none
     * locale is selected. Second it checks if the user's preferred local is available, if yes, it is selected. Third it
     * checks if the configured preferred local is available, if yes, it is selected, else none locale is selected.
     * 
     * @param availableLocales A collection of all available locales.
     * @param userLocale The user's preferred locale.
     * @return Returns the right locale.
     */
    public Locale selectLocale(final Collection<Locale> availableLocales, final Locale userLocale) {

        if (localeEnforcement) {
            if (availableLocales.contains(preferredLocale)) {
                logger.debug("Locale {} is enforced and available", preferredLocale);
                return preferredLocale;
            } else {
                logger.debug("Locale {} is enforced but not available", preferredLocale);
                return null;
            }
        }

        if (availableLocales.contains(userLocale)) {
            logger.debug("User's locale {} is available", userLocale);
            return userLocale;
        }

        if (availableLocales.contains(preferredLocale)) {
            logger.debug("Preferred locale {} is available", preferredLocale);
            return preferredLocale;
        }

        logger.debug("Neither user's locale {} nor preferred locale {} is available", userLocale, preferredLocale);
        return null;
    }

    /**
     * Gets all available locales for relying party names.
     * 
     * @param serviceNames A list of all service names of the relying party .
     * @return Returns a collection of available locales.
     */
    private Collection<Locale> getAvailableNameLocales(final List<ServiceName> serviceNames) {
        final Collection<Locale> availableLocales = new HashSet<Locale>();
        for (ServiceName serviceName : serviceNames) {
            availableLocales.add(new Locale(serviceName.getXMLLang()));
        }
        return availableLocales;
    }

    /**
     * Gets all available locales for relying party descriptions.
     * 
     * @param serviceDescriptions A list of all service descriptions of the relying party.
     * @return Returns a collection of available locales.
     */
    private Collection<Locale> getAvailableDescriptionLocales(final List<ServiceDescription> serviceDescriptions) {
        final Collection<Locale> availableLocales = new HashSet<Locale>();
        for (ServiceDescription serviceDescription : serviceDescriptions) {
            availableLocales.add(new Locale(serviceDescription.getXMLLang()));
        }
        return availableLocales;
    }

    /**
     * Gets the localized name of a list of service names.
     * 
     * @param serviceNames A list of all service names.
     * @param locale The locale which should be used.
     * @return Returns the localized name or null if the service name is not available for the given locale.
     */
    private String getRelyingPartyName(final List<ServiceName> serviceNames, final Locale locale) {
        for (ServiceName serviceName : serviceNames) {
            if (serviceName.getXMLLang().equals(locale.getLanguage())) {
                return serviceName.getXMLLang();
            }
        }
        return null;
    }

    /**
     * Gets the localized name of a list of service descriptions.
     * 
     * @param serviceDescriptions A list of all service descriptions.
     * @param locale The locale which should be used.
     * @return Returns the localized description or null if the service description is not available for the given
     *         locale.
     */
    private String getRelyingPartyDescription(final List<ServiceDescription> serviceDescriptions, final Locale locale) {
        for (ServiceDescription serviceDescription : serviceDescriptions) {
            if (serviceDescription.getXMLLang().equals(locale.getLanguage())) {
                return serviceDescription.getXMLLang();
            }
        }
        return null;
    }

    /**
     * Gets a list of all service names for a relying party.
     * 
     * @param entityId The entityId of the relying party.
     * @return Returns a list of all service names.
     */
    private List<ServiceName> getServiceNames(final String entityId) {
        final AttributeConsumingService attributeConsumingService = getAttributeConsumingService(entityId);
        if (attributeConsumingService != null) {
            return attributeConsumingService.getNames();
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Gets a list of all service descriptions for a relying party.
     * 
     * @param entityId The entityId of the relying party.
     * @return Returns a list of all service descriptions.
     */
    private List<ServiceDescription> getServiceDescriptions(final String entityId) {
        final AttributeConsumingService attributeConsumingService = getAttributeConsumingService(entityId);
        if (attributeConsumingService != null) {
            return attributeConsumingService.getDescriptions();
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Gets the attribute consuming service for a specific relying party.
     * 
     * @param entityId The entityId of the relying party.
     * @return Returns the attribute consuming service element or null if not available.
     */
    private AttributeConsumingService getAttributeConsumingService(final String entityId) {
        Assert.notNull(metadataProvider);

        EntityDescriptor entityDescriptor = null;
        try {
            entityDescriptor = metadataProvider.getEntityDescriptor(entityId);
        } catch (MetadataProviderException e) {
            logger.warn("Unable to retrieve relying party description for {}", entityId, e);
            return null;
        }
        String[] protocols = {SAMLConstants.SAML20P_NS, SAMLConstants.SAML11P_NS, SAMLConstants.SAML10P_NS};
        for (String protocol : protocols) {
            final SPSSODescriptor spSSODescriptor = entityDescriptor.getSPSSODescriptor(protocol);
            if (spSSODescriptor == null) {
                continue;
            }
            final AttributeConsumingService defaultAttributeConsumingService =
                    spSSODescriptor.getDefaultAttributeConsumingService();
            if (defaultAttributeConsumingService != null) {
                return defaultAttributeConsumingService;
            }
            final List<AttributeConsumingService> list = spSSODescriptor.getAttributeConsumingServices();
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        }
        return null;
    }

}

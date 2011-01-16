/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 *
 */
public class LocalizationHelper {
    
    private final Logger logger = LoggerFactory.getLogger(LocalizationHelper.class);
    private Locale preferredLocale;
    private boolean localeEnforcement;
    
    private MetadataProvider metadataProvider;
    
    /**
     * @param preferredLocale The preferredLocale to set.
     */
    public void setPreferredLocale(Locale preferredLocale) {
        this.preferredLocale = preferredLocale;
    }

    /**
     * @param localeEnforcement The localeEnforcement to set.
     */
    public void setLocaleEnforcement(boolean localeEnforcement) {
        this.localeEnforcement = localeEnforcement;
    }

    /**
     * @param metadataProvider The metadataProvider to set.
     */
    public void setMetadataProvider(MetadataProvider metadataProvider) {
        this.metadataProvider = metadataProvider;
    }

    public String getRelyingPartyName(String relyingPartyId, Locale userLocale) {
        List<ServiceName> serviceNames = getServiceNames(metadataProvider, relyingPartyId);
        Locale locale = selectLocale(getAvailableNameLocales(serviceNames), userLocale);
        if (locale == null) {
            return relyingPartyId;
        }
        return getRelyingPartyName(serviceNames, locale);
    }
    
    public String getRelyingPartyDescription(String relyingPartyId, Locale userLocale) {
        List<ServiceDescription> serviceDescription = getServiceDescriptions(metadataProvider, relyingPartyId);
        Locale locale = selectLocale(getAvailableDescriptionLocales(serviceDescription), userLocale);
        if (locale == null) {
            return "";
        }
        return getRelyingPartyDescription(serviceDescription, locale);        
    }
    
    public String getAttributeName(Attribute<?> attribute, Locale userLocale) {
        Locale locale = selectLocale(attribute.getDisplayNames().keySet(), userLocale);
        logger.debug("Locale {} choosen for attribute {} name", locale, attribute.getId());
        if (locale == null) {
            return attribute.getId();
        }
        return attribute.getDisplayNames().get(locale);
    }
    
    public String getAttributeDescription(Attribute<?> attribute, Locale userLocale) {
        Locale locale = selectLocale(attribute.getDisplayDescriptions().keySet(), userLocale);
        logger.debug("Locale {} choosen for attribute {} description", locale, attribute.getId());
        if (locale == null) {
            return "";
        }
        return attribute.getDisplayDescriptions().get(locale);
    }
    
    // TODO: check modifier
    Locale selectLocale(Collection<Locale> availableLocales, Locale userLocale) {              
        
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
    
    private Collection<Locale> getAvailableNameLocales(List<ServiceName> serviceNames) {
        Collection<Locale> availableLocales = new HashSet<Locale>();
        for (ServiceName serviceName: serviceNames) {
            availableLocales.add(new Locale(serviceName.getName().getLanguage()));
        }
        return availableLocales;
    }
    
    private Collection<Locale> getAvailableDescriptionLocales(List<ServiceDescription> serviceDescriptions) {
        Collection<Locale> availableLocales = new HashSet<Locale>();
        for (ServiceDescription serviceDescription: serviceDescriptions) {
            availableLocales.add(new Locale(serviceDescription.getDescription().getLanguage()));
        }
        return availableLocales;
    }
    
    private String getRelyingPartyName(List<ServiceName> serviceNames, Locale locale) {
        for (ServiceName serviceName: serviceNames) {
            if (serviceName.getName().getLanguage().equals(locale.getLanguage())) {
                return serviceName.getName().getLocalString();
            }
        }      
        return null;
    }
    
    private String getRelyingPartyDescription(List<ServiceDescription> serviceDescriptions, Locale locale) {
        for (ServiceDescription serviceDescription: serviceDescriptions) {
            if (serviceDescription.getDescription().getLanguage().equals(locale.getLanguage())) {
                return serviceDescription.getDescription().getLocalString();
            }
        }      
        return null;
    }

    private List<ServiceName> getServiceNames(MetadataProvider metadataProvider, String entityId) {
        AttributeConsumingService attributeConsumingService = getAttributeConsumingService(metadataProvider, entityId);  
        if (attributeConsumingService != null) {
            return attributeConsumingService.getNames();
        } else {
            return Collections.EMPTY_LIST;
        }
    }
    
    private List<ServiceDescription> getServiceDescriptions(MetadataProvider metadataProvider, String entityId) {
        AttributeConsumingService attributeConsumingService = getAttributeConsumingService(metadataProvider, entityId);  
        if (attributeConsumingService != null) {
            return attributeConsumingService.getDescriptions();
        } else {
            return Collections.EMPTY_LIST;
        }
    }
    
    /**
     * @return
     */
    private AttributeConsumingService getAttributeConsumingService(MetadataProvider metadataProvider, String entityId) {
        Assert.notNull(metadataProvider);
        
        EntityDescriptor entityDescriptor = null;
        try {
            entityDescriptor = metadataProvider.getEntityDescriptor(entityId);
        } catch (MetadataProviderException e) {
            logger.warn("Unable to retrieve relying party description for {}", entityId, e);
            return null;
        }
        String[] protocols = {SAMLConstants.SAML20P_NS, SAMLConstants.SAML11P_NS, SAMLConstants.SAML10P_NS};
        for (String protocol: protocols) {
            SPSSODescriptor spSSODescriptor = entityDescriptor.getSPSSODescriptor(protocol);                
            if (spSSODescriptor == null) {
                continue;
            }
            AttributeConsumingService defaultAttributeConsumingService = spSSODescriptor.getDefaultAttributeConsumingService();
            if (defaultAttributeConsumingService != null) {
                return defaultAttributeConsumingService;
            } 
            List<AttributeConsumingService> list = spSSODescriptor.getAttributeConsumingServices();
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        }
        return null;
    }

}

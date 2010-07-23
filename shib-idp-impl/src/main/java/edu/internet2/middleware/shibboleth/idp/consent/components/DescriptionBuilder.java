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

package edu.internet2.middleware.shibboleth.idp.consent.components;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

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

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.relyingparty.provider.SAMLMDRelyingPartyConfigurationManager;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentException;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;

/**
 *
 */
public class DescriptionBuilder {
	
    private final Logger logger = LoggerFactory.getLogger(DescriptionBuilder.class);
   
	private SAMLMDRelyingPartyConfigurationManager relyingPartyConfigurationManager;
	
	public void  setSAMLMDRelyingPartyConfigurationManager(SAMLMDRelyingPartyConfigurationManager relyingPartyConfigurationManager) {
	    this.relyingPartyConfigurationManager = relyingPartyConfigurationManager;
	}
	
	private Locale preferredLocale = Locale.ENGLISH;
	private boolean localeEnforcement = false;
		
	public void setPreferredLocale(Locale preferredLocale) {
	    this.preferredLocale = preferredLocale;
	}
	
    public void setLocaleEnforcement(boolean localeEnforcement) {
        this.localeEnforcement = localeEnforcement;
	}
	
    /**
	 * @param attributes
     * @throws UserConsentException 
	 */
	public void attachDescription(Map<String, BaseAttribute> baseAttributes, Collection<Attribute> attributes, Locale userLocale) throws UserConsentException {		
		for (Attribute attribute : attributes) {
			
			String displayName = attribute.getId();
			BaseAttribute baseAttribute = baseAttributes.get(attribute.getId());
			
			if (baseAttribute == null) {
			    throw new UserConsentException("Description attachment requested for the non released attribute {}", attribute);
			}
			
			Collection<Locale> availableNameLocales = (Collection<Locale>) baseAttribute.getDisplayNames().keySet();
			Locale usingNameLocale = selectLocale(availableNameLocales, userLocale);
			if (usingNameLocale != null) {
			    displayName = (String) baseAttribute.getDisplayNames().get(usingNameLocale);
			}
			attribute.setDisplayName(displayName);
			
	        String displayDescription = "";
            Collection<Locale> availableDescriptionLocales = (Collection<Locale>) baseAttribute.getDisplayDescriptions().keySet();
            Locale usingDescriptionLocale = selectLocale(availableDescriptionLocales, userLocale);
            if (usingDescriptionLocale != null) {
                displayDescription = (String) baseAttribute.getDisplayDescriptions().get(usingDescriptionLocale);
            }
            attribute.setDisplayDescription(displayDescription);
		}
	}

	/**
	 * @param relyingParty
	 * @throws UserConsentException 
	 */
	public void attachDescription(RelyingParty relyingParty, Locale userLocale) throws UserConsentException {
	    MetadataProvider metadataProvider = relyingPartyConfigurationManager.getMetadataProvider();
	    EntityDescriptor entityDescriptor = null;
        try {
                entityDescriptor = metadataProvider.getEntityDescriptor(relyingParty.getEntityId());
        } catch (MetadataProviderException e) {
                throw new UserConsentException("Error retrieving entity descriptor from metadata", e);
        }
        
        String name = relyingParty.getEntityId();
        String description = "";
        
        AttributeConsumingService attrService = getAttributeConsumingService(entityDescriptor);
        if (attrService != null) {
        	
            Collection<Locale> availableNameLocales = new HashSet<Locale>();
        	for (ServiceName serviceName: attrService.getNames()) {
        	    availableNameLocales.add(new Locale(serviceName.getName().getLanguage()));
        	}        	
        	
        	Locale usingNameLocale = selectLocale(availableNameLocales, userLocale);
        	if (usingNameLocale != null) {
                for (ServiceName serviceName: attrService.getNames()) {
                    if (serviceName.getName().getLanguage().equals(usingNameLocale.getLanguage())) {
                        name = serviceName.getName().getLocalString();
                    }
                }      
        	}
        	
            Collection<Locale> availableDescriptionLocales = new HashSet<Locale>();
            for (ServiceDescription serviceDescription: attrService.getDescriptions()) {
                availableDescriptionLocales.add(new Locale(serviceDescription.getDescription().getLanguage()));
            }           
            
            Locale usingDescriptionLocale = selectLocale(availableDescriptionLocales, userLocale);
            if (usingDescriptionLocale != null) {
                for (ServiceDescription serviceDescription: attrService.getDescriptions()) {
                    if (serviceDescription.getDescription().getLanguage().equals(usingDescriptionLocale.getLanguage())) {
                        description = serviceDescription.getDescription().getLocalString();
                    }
                }      
            }
	
        }
        relyingParty.setDisplayName(name);
        relyingParty.setDisplayDescription(description);
	}
	

	private final Locale selectLocale(Collection<Locale> availableLocales, Locale userLocale) {
	    	    
        if (localeEnforcement && availableLocales.contains(preferredLocale)) {
            return preferredLocale;
        }
        
        if (availableLocales.contains(userLocale)) {
            return userLocale;
        }
                
        if (availableLocales.contains(preferredLocale)) {
            return preferredLocale;
        }
        
        return null;
	}
		
	/**
	 * @param entityDescriptor
	 * @return
	 */
	private final AttributeConsumingService getAttributeConsumingService(EntityDescriptor entityDescriptor) {
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

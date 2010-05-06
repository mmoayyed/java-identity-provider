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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.ServiceDescription;
import org.opensaml.saml2.metadata.ServiceName;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;

import edu.internet2.middleware.shibboleth.idp.consent.UserConsentException;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.mock.BaseAttribute;

/**
 *
 */
public class DescriptionBuilder {
	
	private UILocale uiLocale;
	
	private MetadataProvider metadataProvider;


	public DescriptionBuilder(Locale preferedLocale, boolean enforced) {
		this.uiLocale = new UILocale(preferedLocale, enforced);
	}

    /**
	 * @param attributes
	 */
	public void attachDescription(Collection<Attribute> attributes, Locale userLocale) {
		Map<String, BaseAttribute> baseAttributes = null;
		
		for (Attribute attribute : attributes) {
			BaseAttribute baseAttribute = baseAttributes.get(attribute.getId());
			
			String displayName = null;
			for (Object object : baseAttribute.getDisplayNames().keySet()) {
				Locale locale = (Locale) object;
				
				if (uiLocale.isEnforced() && uiLocale.getLocale().equals(locale)) {
					displayName = (String) baseAttribute.getDisplayNames().get(locale);
	    			break;
	    		}
	    		
	    		if (userLocale.equals(locale)) {
	    			displayName = (String) baseAttribute.getDisplayNames().get(locale);
	    			break;
	    		}
	    		   		
	    		if (uiLocale.getLocale().equals(locale)) {
	    			displayName = (String) baseAttribute.getDisplayNames().get(locale);
	    			break;
	    		}
			}
			attribute.setDisplayName(displayName);
			
			
			String displayDescription = null;
			for (Object object : baseAttribute.getDisplayDescriptions().keySet()) {
				Locale locale = (Locale) object;
				
				if (uiLocale.isEnforced() && uiLocale.getLocale().equals(locale)) {
					displayDescription = (String) baseAttribute.getDisplayDescriptions().get(locale);
	    			break;
	    		}
	    		
	    		if (userLocale.equals(locale)) {
	    			displayDescription = (String) baseAttribute.getDisplayDescriptions().get(locale);
	    			break;
	    		}
	    		   		
	    		if (uiLocale.getLocale().equals(locale)) {
	    			displayDescription = (String) baseAttribute.getDisplayDescriptions().get(locale);
	    			break;
	    		}
			}
			attribute.setDisplayDescription(displayDescription);
		}
	}

	/**
	 * @param relyingParty
	 * @throws UserConsentException 
	 */
	public void attachDescription(RelyingParty relyingParty, Locale userLocale) throws UserConsentException {
		EntityDescriptor entityDescriptor = null;
        try {
                entityDescriptor = metadataProvider.getEntityDescriptor(relyingParty.getEntityId());
        } catch (MetadataProviderException e) {
                throw new UserConsentException("Error retrieving entity descriptor", e);
        }
        
        AttributeConsumingService attrService = getAttributeConsumingService(entityDescriptor);

        if (attrService != null) {
        	String name = null;
        	for (ServiceName serviceName: attrService.getNames()) {
        		Locale locale = new Locale(serviceName.getName().getLanguage());
        		if (uiLocale.isEnforced() && uiLocale.getLocale().equals(locale)) {
        			name = serviceName.getName().getLocalString();
        			break;
        		}
        		
        		if (userLocale.equals(locale)) {
        			name = serviceName.getName().getLocalString();
        			break;
        		}
        		   		
        		if (uiLocale.getLocale().equals(locale)) {
        			name = serviceName.getName().getLocalString();
        			break;
        		}
        	}
        	relyingParty.setDisplayName(name);
        	
        	String description = null;
        	for (ServiceDescription serviceDescription: attrService.getDescriptions()) {
        		Locale locale = new Locale(serviceDescription.getDescription().getLanguage());
        		if (uiLocale.isEnforced() && uiLocale.getLocale().equals(locale)) {
        			description = serviceDescription.getDescription().getLocalString();
        			break;
        		}
        		
        		if (userLocale.equals(locale)) {
        			description = serviceDescription.getDescription().getLocalString();
        			break;
        		}
        		   		
        		if (uiLocale.getLocale().equals(locale)) {
        			description = serviceDescription.getDescription().getLocalString();
        			break;
        		}
        	}
        	relyingParty.setDisplayDescription(description);	
        }
	}
	

		
	/**
	 * @param entityDescriptor
	 * @return
	 */
	private AttributeConsumingService getAttributeConsumingService(EntityDescriptor entityDescriptor) {
        String[] protocols = {SAMLConstants.SAML20P_NS, SAMLConstants.SAML11P_NS, SAMLConstants.SAML10P_NS};
        AttributeConsumingService result = null;
        List<AttributeConsumingService> list;
        for (String protocol: protocols) {
                SPSSODescriptor spSSODescriptor = entityDescriptor.getSPSSODescriptor(protocol);                
                if (spSSODescriptor == null) {
                        continue;
                }                
                result = spSSODescriptor.getDefaultAttributeConsumingService();
                if (result != null) {
                        return result;
                } 
                list = spSSODescriptor.getAttributeConsumingServices();
                if (list != null && !list.isEmpty()) {
                        return list.get(0);
                }     
        }
        return result;
	}



	private class UILocale {
		private final Locale locale;
		private final boolean enforced;
		
		private UILocale(Locale locale, boolean enforced) {
			this.locale = locale;
			this.enforced = enforced;
		}
		
		private Locale getLocale() {
			return locale;
		}
		
		private boolean isEnforced() {
			return enforced;
		}
	}
}

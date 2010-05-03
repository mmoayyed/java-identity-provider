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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.internet2.middleware.shibboleth.idp.consent.UserConsentContext;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentException;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.mock.IdPMock;
import edu.internet2.middleware.shibboleth.idp.consent.persistence.Storage;

/**
 *
 */

public class UserConsentContextBuilder {

    private final Logger logger = LoggerFactory.getLogger(UserConsentContextBuilder.class);
    
    @Autowired
    private Storage storage;
    
    @Autowired
    private AttributeList attributeList;
    
    private String uniqueIdAttribute;
    
    // TODO remove (only testing)
    private IdPMock dummyIdP;
    
    // TODO remove (only testing)
    public void setIdPMock(IdPMock dummyIdP) {
    	this.dummyIdP = dummyIdP;
    }
    
    /**
     * @return Returns the uniqueIdAttribute.
     */
    public String getUniqueIdAttribute() {
        return uniqueIdAttribute;
    }

    /**
     * @param uniqueIdAttribute The uniqueIdAttribute to set.
     */
    public void setUniqueIdAttribute(String uniqueIdAttribute) {
        this.uniqueIdAttribute = uniqueIdAttribute;
    }

    public UserConsentContext buildUserConsentContext() throws UserConsentException {
   
        final RelyingParty relyingParty = setupRelyingParty(dummyIdP.getEntityID());
        attachDescription(relyingParty);
        
        final Collection<Attribute> attributesToBeReleasedFromIdP = dummyIdP.getReleasedAttributes();
        final Collection<Attribute> attributesToBeReleased = setupAttributes(attributesToBeReleasedFromIdP);
        final Collection<Attribute> attributesToBeReleasedWithoutBlacklisted = attributeList.removeBlacklisted(attributesToBeReleased);
        final Collection<Attribute> attributes = attributeList.sortAttributes(attributesToBeReleasedWithoutBlacklisted);
        
        attachDescription(attributes);
        
        final String uniqueId = findUniqueId(attributes);
        final Principal principal = setupPrincipal(uniqueId);

        final Collection<AttributeReleaseConsent> attributeReleaseConsent = storage.readAttributeReleaseConsents(principal, relyingParty);
        principal.setAttributeReleaseConsents(relyingParty, attributeReleaseConsent);
        
        return new UserConsentContext(principal, relyingParty, attributes);
    }
    
    /**
	 * @param attributes
	 */
    private Collection<Attribute> setupAttributes(Collection<Attribute> attributes) {
        // TODO: Convert the attributes from IdP to user consent attributes
    	// including attributeId, attributeValues
    	return attributes;
    }
        
    /**
	 * @param attributes
	 */
	private void attachDescription(Collection<Attribute> attributes) {
        // TODO attach displayNames (localized), displayDescriptions (localized)	
	}

	/**
	 * @param relyingParty
	 */
	private void attachDescription(RelyingParty relyingParty) {
        // TODO: Retrieve displayNames, displayDescriptions from
        // MetadataProvider/EntityDescriptor/SPSSODescriptor/AttributeConsumingService
        // Convert localization.
	}

	private Principal setupPrincipal(String uniqueId) {
        Principal principal;
        
    	long id = storage.findPrincipal(uniqueId);
        if (id == 0) {
        	principal = storage.createPrincipal(uniqueId);
            logger.debug("First access of principal {}. Create entry.", principal);
        } else {
            principal = storage.readPrincipal(id);
            Collection<AgreedTermsOfUse> agreedTermsOfUses = storage.readAgreedTermsOfUses(principal);
            principal.setAgreedTermsOfUses(agreedTermsOfUses);
            logger.debug("Further access of principal {}. Entry loaded.", principal);
        }
        return principal;
    }
    
    private RelyingParty setupRelyingParty(String entityId) {
        long id = storage.findRelyingParty(entityId);
        
        RelyingParty relyingParty;
        if (id == 0) {
        	relyingParty = storage.createRelyingParty(entityId);
        	logger.debug("First access to relyingParty {}. Create entry.", relyingParty);
        } else {
        	relyingParty = storage.readRelyingParty(id);
        	logger.debug("Further access to relyingParty {}. Entry loaded.", relyingParty);
        }
        return relyingParty;
    }
    
    private String findUniqueId(Collection<Attribute> attributes) throws UserConsentException {
       for (Attribute attribute : attributes) {
           if  (attribute.getId().equals(uniqueIdAttribute)) {
               if (attribute.getValues().size() == 1) {
                   return attribute.getValues().iterator().next();
               }
               throw new UserConsentException("uniqueId attribute {} has none or more than one values {}.", uniqueIdAttribute, attribute.getValues());
           }
       }
       throw new UserConsentException("uniqueId attribute {} will not be released.", uniqueIdAttribute);     
    }
}
    

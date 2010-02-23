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

package edu.internet2.middleware.shibboleth.idp.consent.logic;

import java.util.Collection;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.internet2.middleware.shibboleth.idp.consent.UserConsentException;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.mock.ProfileContext;
import edu.internet2.middleware.shibboleth.idp.consent.persistence.Storage;

/**
 *
 */
public class UserConsentContextBuilder {

    private final Logger logger = LoggerFactory.getLogger(UserConsentContextBuilder.class);
    
    @Autowired
    private Storage storage;
    
    private String uniqueIdAttribute;
    
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

    public UserConsentContext buildUserConsentContext(ProfileContext profileContext) throws UserConsentException {

        final UserConsentContext userConsentContext = new UserConsentContext();
        // TODO may be from ProfileContext?
        userConsentContext.setAccessTime(new Date());
        userConsentContext.setConsentRevocationRequested(profileContext.isConsentRevocationRequested());
   
        final RelyingParty relyingParty = getRelyingPartyFromProfileContext(profileContext);
        setupRelyingParty(relyingParty);
        userConsentContext.setRelyingParty(relyingParty);
        
        final Collection<Attribute> attributesToBeReleased = getReleasedAttributesFromProfileContext(profileContext);
        userConsentContext.setAttributesToBeReleased(attributesToBeReleased);
        
        final String uniqueId = findUniqueId(attributesToBeReleased);
        final Principal principal = new Principal();
        principal.setUniqueId(uniqueId);
        principal.setLastAccess(userConsentContext.getAccessTime());
        setupPrincipal(principal);
        final Collection<AttributeReleaseConsent> attributeReleaseConsent = storage.readAttributeReleaseConsents(principal, relyingParty);
        principal.setAttributeReleaseConsents(relyingParty, attributeReleaseConsent);
        userConsentContext.setPrincipal(principal);
        
        logger.debug("User consent context builded: {}.", userConsentContext);
        
        return userConsentContext;
    }
        
    private Principal setupPrincipal(Principal principal) {
        storage.findPrincipal(principal);
        
        if (principal.getId() == 0) {
            logger.debug("First access of principal {}. Create entry.", principal);
            principal.setFirstAccess(principal.getLastAccess());
            principal.setGlobalConsent(false);
            storage.createPrincipal(principal);
        } else {
            principal = storage.readPrincipal(principal);
            Collection<AgreedTermsOfUse> agreedTermsOfUses = storage.readAgreedTermsOfUses(principal);
            principal.setAgreedTermsOfUses(agreedTermsOfUses);
        }
        logger.debug("Principal after setup {}", principal);
        return principal;
    }
    
    private RelyingParty setupRelyingParty(RelyingParty relyingParty) {
        storage.findRelyingParty(relyingParty);
        
        if (relyingParty.getId() == 0) {
            logger.debug("First access to relying Party {}. Create entry.", relyingParty);
            storage.createRelyingParty(relyingParty);
        }
        return relyingParty;
    }
    
    
    private Collection<Attribute> getReleasedAttributesFromProfileContext(ProfileContext profileContext) {
        // TODO Convert the attributes from IdP profile context to user consent attributes
        // including attributeId, attributeValues, displayNames (localized), displayDescriptions (localized)
        // For now, the profile context mock returns a user consent formed List ;-)
        
        // TODO Sort the Attributes according the attribute order list
        return profileContext.getReleasedAttributes();    
    }
    
    private RelyingParty getRelyingPartyFromProfileContext(ProfileContext profileContext) {
        // TODO Retrieve the RelyingParty information from the profile context including entityId.
        // Retrieve displayNames, displayDescriptions from
        // MetadataProvider/EntityDescriptor/SPSSODescriptor/AttributeConsumingService
        // Logic for default one and convert localization.
        // For now, the profile context mock returns a user consent formed RelyingParty ;-)
        return profileContext.getRelyingParty();    
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
    

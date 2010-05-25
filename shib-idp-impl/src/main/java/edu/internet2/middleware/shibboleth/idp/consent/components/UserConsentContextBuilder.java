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
import java.util.Map;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.internet2.middleware.shibboleth.idp.consent.UserConsentContext;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.mock.BaseAttribute;
import edu.internet2.middleware.shibboleth.idp.consent.mock.IdPContext;
import edu.internet2.middleware.shibboleth.idp.consent.persistence.Storage;



public class UserConsentContextBuilder {

    private final Logger logger = LoggerFactory.getLogger(UserConsentContextBuilder.class);
    
    @Autowired
    private Storage storage;
    
    @Autowired
    private AttributeList attributeList;
    
    private String uniqueIdAttribute;
    
    /**
     * @return Returns the uniqueIdAttribute.
     */
    public final String getUniqueIdAttribute() {
        return uniqueIdAttribute;
    }

    /**
     * @param uniqueIdAttribute The uniqueIdAttribute to set.
     */
    public void setUniqueIdAttribute(String uniqueIdAttribute) {
        this.uniqueIdAttribute = uniqueIdAttribute;
    }

    // TODO synchronized?
    public final UserConsentContext buildUserConsentContext(IdPContext idpContext) {
    	
    	final DateTime accessDate = new DateTime();
    	
        final RelyingParty relyingParty = setupRelyingParty(idpContext.getEntityID());
        
        Collection<Attribute> attributes = setupAttributes(idpContext.getReleasedAttributes());

             
        String uniqueId = findUniqueId(attributes);
        if (uniqueId == null) {
            uniqueId = idpContext.getPrincipalName();
            logger.warn("Using principalName {} as uniqueId", uniqueId);
        }
        
        final Principal principal = setupPrincipal(uniqueId, accessDate);

        final Collection<AttributeReleaseConsent> attributeReleaseConsent = storage.readAttributeReleaseConsents(principal, relyingParty);
        principal.setAttributeReleaseConsents(relyingParty, attributeReleaseConsent);
        
        // remove blacklisted attributes
        attributes = attributeList.removeBlacklisted(attributes);
        
        // sort attributes
        attributes = attributeList.sortAttributes(attributes);
        
        return new UserConsentContext(principal, relyingParty, attributes, accessDate, idpContext.getRequest().getLocale());
    }
    
    /**
	 * @param attributes
	 */
    private final Collection<Attribute> setupAttributes(Map<String, BaseAttribute<String>> baseAttributes) {
        Collection<Attribute> attributes = new HashSet<Attribute>();
        for (BaseAttribute<String> baseAttribute : baseAttributes.values()) {
        	Collection<String> attributeValues = new HashSet<String>();
        	attributeValues.addAll(baseAttribute.getValues());
        	attributes.add(new Attribute(baseAttribute.getId(), attributeValues));
        }
    	return attributes;
    }

	private final Principal setupPrincipal(String uniqueId, DateTime accessDate) {
        Principal principal;
        
        if (!storage.containsPrincipal(uniqueId)) {
        	principal = storage.createPrincipal(uniqueId, accessDate);
            logger.debug("First access of principal {}. Create entry.", principal);
        } else {
            principal = storage.readPrincipal(uniqueId);
            Collection<AgreedTermsOfUse> agreedTermsOfUses = storage.readAgreedTermsOfUses(principal);
            principal.setAgreedTermsOfUses(agreedTermsOfUses);
            principal.setLastAccess(accessDate);
            logger.debug("Further access of principal {}. Entry loaded.", principal);
        }
        return principal;
    }
    
    private final RelyingParty setupRelyingParty(String entityId) {
        RelyingParty relyingParty;
        
        if (!storage.containsRelyingParty(entityId)) {
        	relyingParty = storage.createRelyingParty(entityId);
        	logger.debug("First access to relyingParty {}. Create entry.", relyingParty);
        } else {
        	relyingParty = storage.readRelyingParty(entityId);
        	logger.debug("Further access to relyingParty {}. Entry loaded.", relyingParty);
        }
        return relyingParty;
    }
    
    private final String findUniqueId(Collection<Attribute> attributes) {
       for (Attribute attribute : attributes) {
           if  (attribute.getId().equals(uniqueIdAttribute)) {
               if (attribute.getValues().size() == 0) { 
                   logger.warn("uniqueId attribute {} contains no values.", uniqueIdAttribute);
                   return null;
               }
               if (attribute.getValues().size() == 1) {
                   return attribute.getValues().iterator().next();
               }
               logger.warn("uniqueId attribute {} has more than one values {}.", uniqueIdAttribute, attribute.getValues());
               return attribute.getValues().iterator().next();
           }
       }
       logger.warn("uniqueId attribute {} will not be released", uniqueIdAttribute);
       return null;
    }
}
    

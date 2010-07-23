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

package edu.internet2.middleware.shibboleth.idp.consent.controller;

import java.util.Collection;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;



import edu.internet2.middleware.shibboleth.idp.consent.ProfileContext;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentContext;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentException;
import edu.internet2.middleware.shibboleth.idp.consent.components.RelyingPartyBlacklist;
import edu.internet2.middleware.shibboleth.idp.consent.components.TermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.components.UserConsentContextBuilder;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.persistence.Storage;

/**
 *
 */

@Controller
@RequestMapping("/userconsent")
@SessionAttributes("userConsentContext")
public class UserConsentEngineController {

    private final Logger logger = LoggerFactory.getLogger(UserConsentEngineControllerTest.class);

    @Resource(name="storage")
    private Storage storage;
    
    @Resource(name="termsOfUse")
    private TermsOfUse termsOfUse;
    
    @Resource(name="relyingPartyBlacklist")
    private RelyingPartyBlacklist relyingPartyBlacklist;
    
    @Resource(name="userConsentContextBuilder")
    private UserConsentContextBuilder userConsentContextBuilder;
    
    @RequestMapping(method = RequestMethod.GET)
    public String service(@ModelAttribute("profileContext") ProfileContext profileContext, @RequestParam("consent-revocation") boolean consentRevocationRequested) throws UserConsentException {        
        
        if (profileContext == null) {
            throw new UserConsentException("No profile context found");
        }
        
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(profileContext);
        
        Principal principal = userConsentContext.getPrincipal();
        RelyingParty relyingParty = userConsentContext.getRelyingParty();
        
        if (termsOfUse == null) {
        	logger.debug("Terms of use are not configured");
        } else if (!principal.hasAcceptedTermsOfUse(termsOfUse)) {
        	logger.info("{} has not accepted {}", principal, termsOfUse);
        	return "redirect:/idp/userconsent/terms-of-use";
        }
        
        if (consentRevocationRequested) {
            principal.setGlobalConsent(false);
            storage.updatePrincipal(principal);
         
            principal.setAttributeReleaseConsents(relyingParty, null);
            storage.deleteAttributeReleaseConsents(principal, relyingParty);
            
            // TODO: log into audit log
        }
        
        if (principal.hasGlobalConsent()) {
        	logger.info("Principal {} has given global consent", principal);
            return "redirect:/idp";
        }
        
        if (relyingPartyBlacklist.contains(relyingParty)) {
            logger.info("Relying party {} is blacklisted", relyingParty);
        	return "redirect:/idp";
        }
        
        Collection<Attribute> attributes = userConsentContext.getAttributes();
        if (principal.hasApproved(attributes, relyingParty)) {
        	logger.info("Principal {} has appoved set of attributes for relying party {}", principal, relyingParty);
        	return "redirect:/idp";
        }
        
    	logger.debug("Redirect to attribute release view");
        return "redirect:/idp/userconsent/attribute-release";
        
    }
}

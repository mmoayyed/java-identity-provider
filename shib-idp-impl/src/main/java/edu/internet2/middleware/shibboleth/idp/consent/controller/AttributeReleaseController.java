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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import edu.internet2.middleware.shibboleth.idp.consent.UserConsentContext;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentException;
import edu.internet2.middleware.shibboleth.idp.consent.components.DescriptionBuilder;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.persistence.Storage;

/**
 *
 */

@Controller
@RequestMapping("/userconsent/attribute-release")
@SessionAttributes("userConsentContext")
public class AttributeReleaseController {

    private final Logger logger = LoggerFactory.getLogger(AttributeReleaseController.class);
    
    @Autowired
    private Storage storage;
    
    @Autowired
    private DescriptionBuilder descriptionBuilder;
    
    // TODO set by configuration
    private boolean globalConsentEnabled;
    
        
    @RequestMapping(method = RequestMethod.GET)
    public String getView(UserConsentContext userConsentContext, Model model) throws UserConsentException {
        Collection<Attribute> attributes = userConsentContext.getAttributes();
        RelyingParty relyingParty = userConsentContext.getRelyingParty();
        
        descriptionBuilder.attachDescription(attributes, userConsentContext.getLocale());
        descriptionBuilder.attachDescription(relyingParty, userConsentContext.getLocale());
        
        model.addAttribute("relyingParty", userConsentContext.getRelyingParty());
        model.addAttribute("attributes", attributes);
        return "redirect:/userconsent/attribute-release";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String submit(@RequestParam("global-consent") boolean globalConsent, UserConsentContext userConsentContext, BindingResult result, SessionStatus status) {        
    	Principal principal = userConsentContext.getPrincipal();
		RelyingParty relyingParty = userConsentContext.getRelyingParty();
		Collection<Attribute> attributes = userConsentContext.getAttributes();
		logger.debug("Principal {} accepted attribute release for relying party {}", principal, relyingParty);
		for (Attribute attribute : attributes) {
			logger.trace("Attribute release for attribute {}", attribute);
			if (storage.readAttributeReleaseConsent(principal, relyingParty, attribute) == null) {
				storage.createAttributeReleaseConsent(principal, relyingParty, attribute, userConsentContext.getAccessDate());
			} else {
				storage.updateAttributeReleaseConsent(principal, relyingParty, attribute, userConsentContext.getAccessDate());
			}
		}
		
		if (globalConsentEnabled && globalConsent) {
			logger.debug("Principal {} has given global consent", principal);
			principal.setGlobalConsent(true);
			storage.updatePrincipal(principal);
		}
		
		status.setComplete();
		return "redirect:/userconsent/";
    }
    
}
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

import javax.annotation.Resource;

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
import edu.internet2.middleware.shibboleth.idp.consent.components.TermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.persistence.Storage;

/**
 *
 */

@Controller
@RequestMapping("/userconsent/terms-of-use")
@SessionAttributes("userConsentContext")
public class TermsOfUseController {

    private final Logger logger = LoggerFactory.getLogger(TermsOfUseControllerTest.class);

    @Resource(name="termsOfUse")
    private TermsOfUse termsOfUse;

    @Resource(name="storage")
    private Storage storage;

    @RequestMapping(method = RequestMethod.GET)
    public String getView(Model model) {
        model.addAttribute("terms-of-use", termsOfUse);
        return "redirect:/idp/userconsent/terms-of-use/view";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String submit(@RequestParam("accept") boolean accepted, UserConsentContext userConsentContext, BindingResult result, SessionStatus status) throws UserConsentException {
    	
        if (userConsentContext == null) {
            throw new UserConsentException("No user consent context found");
        }
        
        Principal principal = userConsentContext.getPrincipal();
    	if (accepted) {
    		logger.debug("Principal {} accepted terms of use {}", principal, termsOfUse);
            
    		if (storage.readAgreedTermsOfUse(principal, termsOfUse) == null) {
        		storage.createAgreedTermsOfUse(principal, termsOfUse, userConsentContext.getAccessDate());
    		} else {
        		storage.updateAgreedTermsOfUse(principal, termsOfUse, userConsentContext.getAccessDate());
    		}
    		    
            status.setComplete();
            return "redirect:/idp/userconsent/";
        } else {
        	logger.debug("Principal {} did not accepted terms of use {}", principal, termsOfUse);
        	result.reject("terms-of-use.not-accepted");
            return "redirect:/idp/userconsent/terms-of-use/view";
        }
    }
}
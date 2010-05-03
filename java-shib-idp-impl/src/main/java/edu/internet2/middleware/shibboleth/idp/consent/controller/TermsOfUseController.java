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

    private final Logger logger = LoggerFactory.getLogger(TermsOfUseController.class);

    @Autowired
    private TermsOfUse termsOfUse;

    @Autowired
    private Storage storage;

    @RequestMapping(method = RequestMethod.GET)
    public String getView(Model model) {
        model.addAttribute("terms-of-use", termsOfUse);
        return "redirect:/userconsent/terms-of-use";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String submit(@RequestParam("accept") boolean accepted, UserConsentContext userConsentContext, BindingResult result, SessionStatus status) {
    	Principal principal = userConsentContext.getPrincipal();
    	if (accepted) {
    		logger.debug("Principal {} accepted terms of use {}", principal, termsOfUse);
            storage.createAgreedTermsOfUse(principal, termsOfUse);
            // TODO update?           
            status.setComplete();
            return "redirect:/userconsent/";
        } else {
        	logger.debug("Principal {} did not accepted terms of use {}", principal, termsOfUse);
        	result.reject("terms-of-use.not-accepted");
            return "terms-of-use";
        }
    }
}
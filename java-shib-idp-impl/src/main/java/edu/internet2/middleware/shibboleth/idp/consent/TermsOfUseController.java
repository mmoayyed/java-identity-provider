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

package edu.internet2.middleware.shibboleth.idp.consent;

import java.util.Collection;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;

import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.entities.TermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.logic.AttributeList;
import edu.internet2.middleware.shibboleth.idp.consent.logic.RelyingPartyBlacklist;
import edu.internet2.middleware.shibboleth.idp.consent.logic.UserConsentContext;
import edu.internet2.middleware.shibboleth.idp.consent.logic.UserConsentContextBuilder;
import edu.internet2.middleware.shibboleth.idp.consent.mock.ProfileContext;
import edu.internet2.middleware.shibboleth.idp.consent.persistence.Storage;

/**
 *
 */

@Controller
@RequestMapping("/userconsent/attributerelease")
@SessionAttributes("userConsentContext")
public class TermsOfUseController {

    private final Logger logger = LoggerFactory.getLogger(TermsOfUseController.class);

    @Autowired
    private TermsOfUse termsOfUse;

    @Autowired
    private Storage storage;

    @RequestMapping(method = RequestMethod.GET)
    public String getView(Model model) {
        model.addAttribute("termsOfUse", termsOfUse);
        return "redirect:/userconsent/tou";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String submit(UserConsentContext userConsentContext, BindingResult result) {
        if (true) {
            Principal principal = userConsentContext.getPrincipal();
            Date agreeDate = userConsentContext.getAccessTime();
            storage.createAgreedTermsOfUse(principal, termsOfUse, agreeDate);
            // TODO update?
            return "redirect:/userconsent/";
        } else {
            return "redirect:/userconsent/tou";
        }
    }
}
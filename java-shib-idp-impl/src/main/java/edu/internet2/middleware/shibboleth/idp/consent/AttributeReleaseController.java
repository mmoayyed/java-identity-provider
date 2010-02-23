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
@RequestMapping("/userconsent/tou")
@SessionAttributes("userConsentContext")
public class AttributeReleaseController {

    private final Logger logger = LoggerFactory.getLogger(AttributeReleaseController.class);

    @Autowired
    private AttributeList attributeList;
    
    @Autowired
    private Storage storage;
        
    @RequestMapping(method = RequestMethod.GET)
    public String getView(UserConsentContext userConsentContext, Model model) {
        Collection<Attribute> attributes = userConsentContext.getAttributesToBeReleased();
        attributes = attributeList.removeBlacklisted(attributes);
        attributes = attributeList.sortAttributes(attributes);
        model.addAttribute("relyingParty", userConsentContext.getRelyingParty());
        model.addAttribute("attributes", attributes);
        return "redirect:/userconsent/attributerelease";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String submit(UserConsentContext userConsentContext, BindingResult result) {        
        if (true) {
            Principal principal = userConsentContext.getPrincipal();
            RelyingParty relyingParty = userConsentContext.getRelyingParty();
            Date releaseDate = userConsentContext.getAccessTime();
            Collection<Attribute> attributes = userConsentContext.getAttributesToBeReleased();
            attributes = attributeList.removeBlacklisted(attributes);
            for (Attribute attribute : attributes) {
                storage.createAttributeReleaseConsent(principal, relyingParty, attribute, releaseDate);
                // TODO update?
            }
            return "redirect:/userconsent/";
        } else {
            return "redirect:/userconsent/attributerelease";
        }
    }
    
}
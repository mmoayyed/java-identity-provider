/*
 * Copyright 2009 University Corporation for Advanced Internet Development, Inc.
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

import java.util.Set;

import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;

/**
 *
 */
public class AttributeReleaseConsentCheck {

    public boolean PrincipalHasAttribuesApprovedForRelyingParty(Principal principal, RelyingParty relyingParty,
            Set<Attribute> attributes) {
        int approved = 0;
        for (Attribute attribute : attributes) {
            for (AttributeReleaseConsent attributeReleaseConsent : principal.getAttributeReleaseConsents()) {
                if (attributeReleaseConsent.getRelyingParty().equals(relyingParty)
                        && attributeReleaseConsent.getAttribute().equals(attribute)) {
                    approved++;
                }
            }
        }
        return attributes.size() == approved;
    }
}

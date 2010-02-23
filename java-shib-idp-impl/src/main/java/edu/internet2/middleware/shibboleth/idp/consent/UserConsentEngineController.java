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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.entities.TermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.logic.RelyingPartyBlacklist;
import edu.internet2.middleware.shibboleth.idp.consent.logic.UserConsentContext;

/**
 *
 */

public class UserConsentEngineController {

    public static final int STATE_CONTINUE = 0;
    public static final int STATE_VIEW_TOU = 1;
    public static final int STATE_VIEW_ARC = 2;
    
    private final Logger logger = LoggerFactory.getLogger(UserConsentEngineController.class);
    
    private TermsOfUse termsOfUse;
    
    private RelyingPartyBlacklist relyingPartyBlacklist;
    
    /**
     * @return Returns the termsOfUse.
     */
    public TermsOfUse getTermsOfUse() {
        return termsOfUse;
    }

    /**
     * @param termsOfUse The termsOfUse to set.
     */
    public void setTermsOfUse(TermsOfUse termsOfUse) {
        this.termsOfUse = termsOfUse;
    }

    /**
     * @return Returns the relyingPartyBlacklist.
     */
    public RelyingPartyBlacklist getRelyingPartyBlacklist() {
        return relyingPartyBlacklist;
    }

    /**
     * @param relyingPartyBlacklist The relyingPartyBlacklist to set.
     */
    public void setRelyingPartyBlacklist(RelyingPartyBlacklist relyingPartyBlacklist) {
        this.relyingPartyBlacklist = relyingPartyBlacklist;
    }

    public int service(UserConsentContext userConsentContext) throws UserConsentException {        
        
        Principal principal = userConsentContext.getPrincipal();
        RelyingParty relyingParty = userConsentContext.getRelyingParty();
        
        if (termsOfUse != null && !principal.hasAcceptedTermsOfUse(termsOfUse)) {
            logger.info("{} has not accepted {}", principal, termsOfUse);
            return STATE_VIEW_TOU;
        }
        
        if (userConsentContext.isConsentRevocationRequested()) {
            principal.setGlobalConsent(false);
            principal.setAttributeReleaseConsents(relyingParty, null);            
            // TODO: log into audit log
        }
        
        if (principal.hasGlobalConsent()) {
           return STATE_CONTINUE;
        }
        
        if (relyingPartyBlacklist.contains(relyingParty)) {
            return STATE_CONTINUE;
        }
        
        if (!principal.hasApproved(userConsentContext.getAttributesToBeReleased(), relyingParty)) {
            return STATE_VIEW_ARC;
        }
        
        return STATE_CONTINUE;
    }
}

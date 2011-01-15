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
import java.util.Collections;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.attribute.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.ConsentContext.Consent;
import edu.internet2.middleware.shibboleth.idp.consent.storage.Storage;


/**
 *
 */

public class ConsentEngineImpl implements ConsentEngine {

    private final Logger logger = LoggerFactory.getLogger(ConsentEngineImpl.class);

    @Resource(name="consent.storage")
    private Storage storage;
    
    @Resource(name="consent.relyingPartyBlacklist")
    private Collection<String> relyingPartyBlacklist;
    
    @Resource(name="consent.attributeBlacklist")
    private Collection<String> attributeBlacklist;
    
    @Resource(name="consent.userIdAttribute")
    private String userIdAttribute;
    
    /** {@inheritDoc} */
    public void determineConsent(final ConsentContext consentContext) throws ConsentException {
        
        // TODO Replace null checks with asserts
        // TODO Check if ProfileContext is owner
        if (consentContext == null) {
            throw new ConsentException("No consent context found");
        }
        
        String relyingPartyId = ConsentHelper.getRelyingParty(consentContext);
        Collection<Attribute<?>> attributes = consentContext.getUserAttributes().values();
        String userId = ConsentHelper.findUserId(userIdAttribute, attributes);
        User user = createUser(userId, relyingPartyId);

        boolean consentRevocationRequested = ConsentHelper.isConsentRevocationRequested(consentContext);
        
        if (consentRevocationRequested) {
            user.setGlobalConsent(false);
            user.setAttributeReleases(relyingPartyId, Collections.EMPTY_SET);
            storage.deleteAttributeReleases(user.getId(), relyingPartyId); // TODO check if needed, might it solved in update?
        }
        
        if (user.hasGlobalConsent()) {
            logger.info("user {} has given global consent", user);
            consentContext.setConsentDecision(Consent.PRIOR);
            return;
        }
        
        if (relyingPartyBlacklist.contains(relyingPartyId)) {
            logger.info("Relying party {} is blacklisted", relyingPartyId);
            consentContext.setConsentDecision(Consent.UNSPECIFIED); // TODO IMPLICIT?
            return;
        }
        
        attributes = ConsentHelper.removeBlacklistedAttributes(attributeBlacklist, attributes);
        logger.debug("Blacklisted attributes are removed from the release set, considered attributes are {}", attributes);
        
        if (attributes.isEmpty()) {
            logger.info("No attributes of user {} for relying party {} are released", user, relyingPartyId);
            consentContext.setConsentDecision(Consent.INAPPLICABLE);
            return;
        }
        
        if (user.hasApprovedAttributes(relyingPartyId, attributes)) {
            logger.info("user {} has appoved set of attributes for relying party {}", user, relyingPartyId);
            consentContext.setConsentDecision(Consent.PRIOR);
            return;
        }
        
        
        // TODO
        // request.setAttribute(USER_KEY, user);
        // request.setAttribute(RELYINGPARTYID_KEY, relyingPartyId);
        // request.setAttribute(ATTRIBUTES_KEY, releasedAttributes);        
        // request.forward("");
        logger.debug("Dispatch to attribute release view");
        return;
    }
    
    
    /**
     * @param userId
     * @return
     */
    private User createUser(final String userId, final String relyingPartyId) {
        User user;
        if (storage.containsUser(userId)) {
            user = storage.readUser(userId);
            Collection<AttributeRelease> attributeRelease = storage.readAttributeReleases(userId, relyingPartyId);
            user.setAttributeReleases(relyingPartyId, attributeRelease);
        } else {
            user = new User(userId, false);
            user.setAttributeReleases(relyingPartyId, Collections.EMPTY_SET);
        }
        return user;
    }
}

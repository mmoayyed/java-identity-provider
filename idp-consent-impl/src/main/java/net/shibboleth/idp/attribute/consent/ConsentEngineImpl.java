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

package net.shibboleth.idp.attribute.consent;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Resource;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.consent.ConsentContext;
import net.shibboleth.idp.attribute.consent.ConsentEngine;
import net.shibboleth.idp.attribute.consent.ConsentException;
import net.shibboleth.idp.attribute.consent.ConsentContext.Consent;
import net.shibboleth.idp.attribute.consent.storage.Storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;



/**
 *
 */

public class ConsentEngineImpl implements ConsentEngine {

    private final Logger logger = LoggerFactory.getLogger(ConsentEngineImpl.class);

    @Resource(name="consent.storage")
    private Storage storage;
    
    @Resource(name="consent.config.relyingPartyBlacklist")
    private Collection<String> relyingPartyBlacklist;
    
    @Resource(name="consent.config.attributeBlacklist")
    private Collection<String> attributeBlacklist;
    
    @Resource(name="consent.config.userIdAttribute")
    private String userIdAttribute;
    
    @Resource(name="consent.config.alwaysRequireConsent")
    private boolean alwaysRequireConsent;  
    
    /** {@inheritDoc} */
    public void determineConsent(final ConsentContext consentContext) throws ConsentException {
        
        Assert.notNull(consentContext, "No consent context found");
        Assert.state(consentContext.getOwner().getClass().equals(null/* TODO ProfileContext.class*/), "Owner of a consent context must be a profile context");
        
        final String relyingPartyId = ConsentHelper.getRelyingParty(consentContext);
        Collection<Attribute<?>> attributes = consentContext.getUserAttributes().values();
        
        final String userId = ConsentHelper.findUserId(userIdAttribute, attributes);
        Assert.notNull(userId, "No userId found");
        logger.debug("Using {}({}) as userId attribute", userIdAttribute, userId);
        
        final User user = createUser(userId, relyingPartyId);

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
            consentContext.setConsentDecision(Consent.INAPPLICABLE); // TODO INAPPLICABLE?
            return;
        }
        
        if (alwaysRequireConsent) {
            logger.debug("Always require consent is enabled");
            showAttributeReleaseView(user, relyingPartyId, attributes);
            return;
        }
        
        if (user.hasGlobalConsent()) {
            logger.info("User {} has given global consent", user);
            consentContext.setConsentDecision(Consent.PRIOR);
            return;
        }
        
        if (user.hasApprovedAttributes(relyingPartyId, attributes)) {
            logger.info("User {} has appoved set of attributes for relying party {}", user, relyingPartyId);
            consentContext.setConsentDecision(Consent.PRIOR);
            return;
        }
        
        showAttributeReleaseView(user, relyingPartyId, attributes);
    }
    
    private void showAttributeReleaseView(final User user, final String relyingPartyId, final Collection<Attribute<?>> attributes) {
        // TODO
        // request.setAttribute(ConsentServlet.USER_KEY, user);
        // request.setAttribute(ConsentServlet.RELYINGPARTYID_KEY, relyingPartyId);
        // request.setAttribute(ConsentServlet.ATTRIBUTES_KEY, attributes);        
        logger.debug("Dispatch to attribute release view");
        // request.forward("");
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

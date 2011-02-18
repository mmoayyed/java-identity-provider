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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.consent.ConsentContext.Consent;
import net.shibboleth.idp.attribute.consent.storage.Storage;
import net.shibboleth.idp.profile.ProfileContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/** Implements {@see ConsentEngine}. */
public class ConsentEngineImpl implements ConsentEngine {

    /** Class logger. */
    private final Logger logger = LoggerFactory.getLogger(ConsentEngineImpl.class);

    /** The {@see Storage} instance which is used. */
    @Resource(name = "consent.storage")
    private Storage storage;

    /** A collection of relying party ids which are white- or blacklisted. */
    @Resource(name = "consent.config.relyingPartyWhiteBlackList")
    private Collection<String> relyingPartyWhiteBlackList;

    /** Whether the relyingPartyWhiteBlackList is treated as blacklist or not (whitelist). */
    @Resource(name = "consent.config.relyingPartyWhiteBlackList.isBlacklist")
    private boolean relyingPartyWhiteBlackListIsBlacklist;

    /** A collection of attribute ids which are blacklisted. */
    @Resource(name = "consent.config.attributeBlacklist")
    private Collection<String> attributeBlacklist;

    /** A attribute id which is used for unique user identification. */
    @Resource(name = "consent.config.userIdAttribute")
    private String userIdAttribute;

    /** Determines if user consent is required always. */
    @Resource(name = "consent.config.alwaysRequireConsent")
    private boolean alwaysRequireConsent;

    /** {@inheritDoc} */
    @Override
    public void determineConsent(final ConsentContext consentContext) throws ConsentException {

        Assert.notNull(consentContext, "No consent context found");
        Assert.state(consentContext.getOwner().getClass().equals(ProfileContext.class),
                "Owner of a consent context must be a profile context");

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
            // TODO check if needed, might it solved in update?
            storage.deleteAttributeReleases(user.getId(), relyingPartyId);
        }

        if (user.hasGlobalConsent()) {
            logger.info("user {} has given global consent", user);
            consentContext.setConsentDecision(Consent.PRIOR);
            return;
        }

        if (ConsentHelper.skipRelyingParty(relyingPartyWhiteBlackList, relyingPartyWhiteBlackListIsBlacklist,
                relyingPartyId)) {
            logger.info("Skip relying party {}", relyingPartyId);
            // TODO IMPLICIT?
            consentContext.setConsentDecision(Consent.UNSPECIFIED);
            return;
        }

        attributes = ConsentHelper.removeBlacklistedAttributes(attributeBlacklist, attributes);
        logger.debug("Blacklisted attributes are removed from the release set, considered attributes are {}",
                attributes);

        if (attributes.isEmpty()) {
            logger.info("No attributes of user {} for relying party {} are released", user, relyingPartyId);
            // TODO INAPPLICABLE?
            consentContext.setConsentDecision(Consent.INAPPLICABLE);
            return;
        }

        if (alwaysRequireConsent) {
            logger.debug("Always require consent is enabled");
            showAttributeReleaseView(consentContext, user, relyingPartyId, attributes);
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

        showAttributeReleaseView(consentContext, user, relyingPartyId, attributes);
    }

    /**
     * Forwards to the attribute release view.
     * 
     * @param consentContext The {@see ConsentContext}.
     * @param user The current {@see User}.
     * @param relyingPartyId The relying party id.
     * @param attributes A collection of attributes.
     */
    private void showAttributeReleaseView(final ConsentContext consentContext, final User user,
            final String relyingPartyId, final Collection<Attribute<?>> attributes) {

        final HttpServletRequest request = ConsentHelper.getRequest(consentContext);
        final HttpServletResponse response = ConsentHelper.getResponse(consentContext);

        request.setAttribute(ConsentServlet.USER_KEY, user);
        request.setAttribute(ConsentServlet.RELYINGPARTYID_KEY, relyingPartyId);
        request.setAttribute(ConsentServlet.ATTRIBUTES_KEY, attributes);
        logger.debug("Dispatch to attribute release view");
        try {
            request.getRequestDispatcher("attribute-release").forward(request, response);
        } catch (ServletException e) {
            logger.error("Error while dispatching to attribute release view", e);
        } catch (IOException e) {
            logger.error("Error while dispatching to attribute release view", e);
        }
    }

    /**
     * Creates a user and attaches her the already given attribute release consents for a specific relying party.
     * 
     * The method checks if a user is already present in the persistence storage. If not a new user is created.
     * 
     * @param userId The user id.
     * @param relyingPartyId The relying party id.
     * @return Returns an {@see User} with attached attribute releases for this relying party.
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

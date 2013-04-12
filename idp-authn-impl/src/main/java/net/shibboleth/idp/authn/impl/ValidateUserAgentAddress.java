/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.authn.impl;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationRequestContext;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.UserAgentAddressContext;
import net.shibboleth.idp.authn.UsernamePrincipal;
import net.shibboleth.idp.profile.ActionSupport;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.net.IPRange;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * A stage that ensures that the user-agent address is within a given range and, if so acts as if the configured user
 * has been authenticated.
 */
@Events({
        @Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = AuthnEventIds.INVALID_AUTHN_CTX,
                description = "authentication context does not contain UserAgentAddressContext"),
        @Event(id = AuthnEventIds.INVALID_CREDENTIALS, description = "user agent IP address not in allowed IP ranges")})
public class ValidateUserAgentAddress extends AbstractAuthenticationAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ValidateUserAgentAddress.class);

    /** The ID of the user to treat as authenticated by this stage. */
    private final String authenticatedUser;

    /** List of designated IP ranges. */
    private final List<IPRange> designatedRanges;

    /** Whether the {@link #designatedRanges} should be treated as whitelist or a blacklist. */
    private final boolean whitelistingIPRanges;

    /**
     * Constructor.
     * 
     * @param authenticatedUserId the ID of the user to treat as authenticated by this stage
     * @param ranges list of designated IP ranges
     * @param isWhitelistingRanges whether the {@link #designatedRanges} should be treated as whitelist or a blacklist
     */
    public ValidateUserAgentAddress(@Nonnull @NotEmpty final String authenticatedUserId,
            @Nullable @NullableElements final Collection<IPRange> ranges, final boolean isWhitelistingRanges) {
        authenticatedUser =
                Constraint.isNotNull(StringSupport.trimOrNull(authenticatedUserId),
                        "Authenticated user ID can not be null");

        if (ranges == null) {
            designatedRanges = Collections.emptyList();
        } else {
            designatedRanges = ImmutableList.copyOf(Iterables.filter(ranges, Predicates.notNull()));
        }

        whitelistingIPRanges = isWhitelistingRanges;
    }

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationRequestContext authenticationContext) throws AuthenticationException {

        final UserAgentAddressContext uaaCtx =
                authenticationContext.getSubcontext(UserAgentAddressContext.class, false);
        if (uaaCtx == null) {
            log.debug("Action {}: no UserAgentAddressContext available within authentication context", getId());
            return ActionSupport.buildEvent(this, AuthnEventIds.INVALID_AUTHN_CTX);
        }

        if (isAuthenticated(uaaCtx.getUserAgentAddress())) {
            log.debug("Action {}: authenticated user agent", getId());
            authenticationContext.setAuthenticatedPrincipal(new UsernamePrincipal(authenticatedUser));
            return ActionSupport.buildProceedEvent(this);
        } else {
            log.debug("Action {}: user agent was not authenticated", getId());
            return ActionSupport.buildEvent(this, AuthnEventIds.INVALID_CREDENTIALS);
        }
    }

    /**
     * Checks whether the given IP address meets this stage's IP range requirements.
     * 
     * @param address the IP address to check
     * 
     * @return true if the given IP address meets this stage's IP range requirements, false otherwise
     */
    private boolean isAuthenticated(InetAddress address) {
        byte[] resolvedAddress = address.getAddress();

        boolean containedInRange = false;
        for (IPRange range : designatedRanges) {
            if (range.contains(resolvedAddress)) {
                containedInRange = true;
                break;
            }
        }

        if (whitelistingIPRanges && containedInRange) {
            return true;
        }

        if (!whitelistingIPRanges && !containedInRange) {
            return true;
        }

        return false;
    }
}
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

package net.shibboleth.idp.cas.flow.impl;

import java.time.Instant;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.context.navigate.SubjectContextPrincipalLookupFunction;
import net.shibboleth.idp.cas.config.ConfigLookupFunction;
import net.shibboleth.idp.cas.config.LoginConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.ServiceTicketRequest;
import net.shibboleth.idp.cas.protocol.ServiceTicketResponse;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.config.SecurityConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates and stores a CAS protocol service ticket. Possible outcomes:
 * <ul>
 *     <li><code>null</code> on success</li>
 *     <li>{@link ProtocolError#TicketCreationError TicketCreationError}</li>
 * </ul>
 *
 * @author Marvin S. Addison
 */
public class GrantServiceTicketAction extends AbstractCASProtocolAction<ServiceTicketRequest, ServiceTicketResponse> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(GrantServiceTicketAction.class);

    /** Profile configuration lookup function. */
    @Nonnull private final ConfigLookupFunction<LoginConfiguration> configLookupFunction;

    /** Looks up an IdP session context from IdP profile request context. */
    @Nonnull private final Function<ProfileRequestContext, SessionContext> sessionContextFunction;

    /** AuthenticationContext lookup function. */
    @Nonnull private final Function<ProfileRequestContext, AuthenticationContext> authnCtxLookupFunction;

    /** Function to retrieve subject principal name. */
    @Nonnull private final Function<ProfileRequestContext, String> principalLookupFunction;

    /** Strategy used to locate the {@link AttributeContext} associated with a given {@link ProfileRequestContext}. */
    @Nonnull private Function<ProfileRequestContext,AttributeContext> attributeContextLookupStrategy;

    /** Manages CAS tickets. */
    @Nonnull private final TicketService casTicketService;

    /** Profile config. */
    @Nullable private LoginConfiguration loginConfig;
    
    /** Security config. */
    @Nullable private SecurityConfiguration securityConfig;
    
    /** IdP's session. */
    @Nullable private IdPSession session;
    
    /** Authentication result. */
    @Nullable private AuthenticationResult authnResult;

    /** Whether consent needs to be stored in ticket. */
    private boolean storeConsent;
    
    /** AttributeContext to use. */
    @Nullable private AttributeContext attributeCtx;

    /** CAS request. */
    @Nullable private ServiceTicketRequest request;

    /**
     * Constructor.
     *
     * @param ticketService Ticket service component.
     */
    public GrantServiceTicketAction(@Nonnull final TicketService ticketService) {
        casTicketService = Constraint.isNotNull(ticketService, "TicketService cannot be null");
        
        configLookupFunction = new ConfigLookupFunction<>(LoginConfiguration.class);
        sessionContextFunction = new ChildContextLookup<>(SessionContext.class);
        authnCtxLookupFunction = new ChildContextLookup<>(AuthenticationContext.class);
        principalLookupFunction = new SubjectContextPrincipalLookupFunction().compose(
                new ChildContextLookup<>(SubjectContext.class));
        attributeContextLookupStrategy = new ChildContextLookup<>(AttributeContext.class).compose(
                new ChildContextLookup<>(RelyingPartyContext.class));
    }
    
    /**
     * Set the strategy used to locate the {@link AttributeContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link AttributeContext} associated with a given
     *            {@link ProfileRequestContext}
     *            
     * @since 4.2.0
     */
    public void setAttributeContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AttributeContext> strategy) {
        checkSetterPreconditions();
        attributeContextLookupStrategy =
                Constraint.isNotNull(strategy, "AttributeContext lookup strategy cannot be null");
    }

    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        loginConfig = configLookupFunction.apply(profileRequestContext);
        if (loginConfig == null) {
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        
        securityConfig = loginConfig.getSecurityConfiguration(profileRequestContext);
        if (securityConfig == null) {
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_SEC_CFG);
            return false;
        }
        
        try {
            request = getCASRequest(profileRequestContext);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return false;
        }

        session = getIdPSession(profileRequestContext);
        if (session == null) {
            // TODO: I think this should be revisited, unclear why the TicketState later needs this.
            // It may be needed specifically if the check below for an active AuthnResult fails, but that's
            // a secondary requirement that would only happen when absolutely needed.
            log.warn("{} No IdP session found", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        final AuthenticationContext authnCtx = authnCtxLookupFunction.apply(profileRequestContext);
        if (authnCtx != null) {
            authnResult = authnCtx.getAuthenticationResult();
        } else {
            authnResult = getLatestAuthenticationResult();
        }
        
        if (authnResult == null) {
            log.warn("{} No AuthenticationResult found", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return false;
        }

        if (loginConfig.getPostAuthenticationFlows(profileRequestContext).contains("attribute-release")) {
            attributeCtx = attributeContextLookupStrategy.apply(profileRequestContext);
            if (attributeCtx != null) {
                storeConsent = attributeCtx.isConsented() || loginConfig.isStoreConsentInTickets(profileRequestContext);
                if (storeConsent) {
                    log.debug("{} Storing consented attribute IDs into ticket: {}", getLogPrefix(),
                            attributeCtx.getIdPAttributes().keySet());
                }
            }
        }

        return true;
    }    
    
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
                
        final ServiceTicket ticket;
        try {
            log.debug("{} Granting service ticket for {}", getLogPrefix(), request.getService());
            final TicketState state = new TicketState(
                    session.getId(),
                    getPrincipalName(profileRequestContext),
                    authnResult.getAuthenticationInstant(),
                    authnResult.getAuthenticationFlowId());
            
            if (storeConsent) {
                state.setConsentedAttributeIds(attributeCtx.getIdPAttributes().keySet());
            }
            
            ticket = casTicketService.createServiceTicket(
                    securityConfig.getIdGenerator().generateIdentifier(),
                    Instant.now().plus(loginConfig.getTicketValidityPeriod(profileRequestContext)),
                    request.getService(),
                    state,
                    request.isRenew());
        } catch (final RuntimeException e) {
            log.error("{} Failed granting service ticket due to error.", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, ProtocolError.TicketCreationError.event(this));
            return;
        }
        
        final ServiceTicketResponse response = new ServiceTicketResponse(request.getService(), ticket.getId());
        if (request.isSAML()) {
            response.setSaml(true);
        }
        
        try {
            setCASResponse(profileRequestContext, response);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return;
        }

        log.info("{} Granted service ticket for {}", getLogPrefix(), request.getService());
    }

    /**
     * Get the IdP session.
     *
     * @param prc profile request context
     * 
     * @return IdP session
     */
    @Nullable private IdPSession getIdPSession(final ProfileRequestContext prc) {
        final SessionContext sessionContext = sessionContextFunction.apply(prc);
        return sessionContext != null ? sessionContext.getIdPSession() : null;
    }

    /**
     * Get the IdP subject principal name.
     *
     * @param prc profile request context.
     * @return Principal name.
     */
    @Nonnull private String getPrincipalName(final ProfileRequestContext prc) {
        final String principal = principalLookupFunction.apply(prc);
        if (principal == null ) {
            throw new IllegalStateException("Cannot determine IdP subject principal name.");
        }
        return principal;
    }

    /**
     * Gets the most recent authentication result from the IdP session.
     *
     * @return Latest authentication result.
     *
     * @throws IllegalStateException If no authentication results are found.
     */
    @Nullable private AuthenticationResult getLatestAuthenticationResult() {
        AuthenticationResult latest = null;
        
        for (final AuthenticationResult result : session.getAuthenticationResults()) {
            if (latest == null || result.getAuthenticationInstant().isAfter(latest.getAuthenticationInstant())) {
                latest = result;
            }
        }
        
        return latest;
    }

}
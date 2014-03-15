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

package net.shibboleth.idp.saml.impl.profile.saml2;

import java.security.Principal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.principal.DefaultPrincipalDeterminationStrategy;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;

import net.shibboleth.idp.profile.config.navigate.IdentifierGenerationStrategyLookupFunction;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthnContextDeclRefPrincipal;
import net.shibboleth.idp.saml.impl.profile.config.navigate.SessionLifetimeLookupFunction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml2.core.SubjectLocality;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.profile.SAML2ActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * Action that builds an {@link AuthnStatement} and adds it to the {@link Response} returned by a lookup
 * strategy, by default the message returned by {@link ProfileRequestContext#getOutboundMessageContext()}.
 * 
 * <p>If the {@link Response} does not contain an {@link Assertion} one will be created and added to,
 * otherwise the {@link AuthnStatement} will be added to first {@link Assertion} in the {@link Response},
 * unless the option is set to preclude this.</p>
 * 
 * <p>A constructed {@link Assertion} will have its ID, IssueInstant, Issuer, and Version properties set.
 * The issuer is based on {@link net.shibboleth.idp.relyingparty.RelyingPartyConfiguration#getResponderId()}.</p>
 * 
 * <p>The {@link AuthnStatement} will have its authentication instant set, based on
 * {@link AuthenticationResult#getAuthenticationInstant()} via {@link AuthenticationContext#getAuthenticationResult()}.
 * The {@link AuthnContext} will be set via {@link RequestedPrincipalContext#getMatchingPrincipal()}, or via an injected
 * or defaulted function that obtains a custom principal from the profile context.</p>
 * 
 * <p>The SessionIndex and optionally SessionNotOnOrAfter attributes will also be set.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link AuthnEventIds#INVALID_AUTHN_CTX}
 */
public class AddAuthnStatementToAssertion extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AddAuthnStatementToAssertion.class);

    /**
     * Whether the generated authentication statement should be placed in its own assertion or added to one if it
     * exists.
     */
    private boolean statementInOwnAssertion;

    /** Strategy used to locate the {@link Response} to operate on. */
    @Nonnull private Function<ProfileRequestContext,Response> responseLookupStrategy;

    /** Strategy used to locate the {@link IdentifierGenerationStrategy} to use. */
    @NonnullAfterInit private Function<ProfileRequestContext,IdentifierGenerationStrategy> idGeneratorLookupStrategy;

    /** Strategy used to obtain the assertion issuer value. */
    @Nullable private Function<ProfileRequestContext,String> issuerLookupStrategy;
    
    /** Strategy used to determine the AuthnContextClassRef. */
    @Nonnull private Function<ProfileRequestContext,AuthnContextClassRefPrincipal> classRefLookupStrategy;

    /** Strategy used to determine SessionNotOnOrAfter value to set. */
    @Nullable private Function<ProfileRequestContext,Long> sessionLifetimeLookupStrategy;
    
    /** The generator to use. */
    @Nullable private IdentifierGenerationStrategy idGenerator;
    
    /** AuthenticationResult basis of statement. */
    @Nullable private AuthenticationResult authenticationResult;
    
    /** Response to modify. */
    @Nullable private Response response;
    
    /** EntityID to populate as assertion issuer. */
    @Nullable private String issuerId;
    
    /** Constructor. */
    public AddAuthnStatementToAssertion() {
        statementInOwnAssertion = false;

        responseLookupStrategy =
                Functions.compose(new MessageLookup<>(Response.class), new OutboundMessageContextLookup());
        idGeneratorLookupStrategy = new IdentifierGenerationStrategyLookupFunction();
        issuerLookupStrategy = new ResponderIdLookupFunction();
        classRefLookupStrategy = new DefaultPrincipalDeterminationStrategy<>(AuthnContextClassRefPrincipal.class,
                new AuthnContextClassRefPrincipal(AuthnContext.UNSPECIFIED_AUTHN_CTX));
        sessionLifetimeLookupStrategy = new SessionLifetimeLookupFunction();
    }

    /**
     * Set whether the generated authentication statement should be placed in its own assertion or added to one if it
     * exists.
     * 
     * @param inOwnAssertion whether the generated authentication statement should be placed in its own assertion or
     *            added to one if it exists
     */
    public synchronized void setStatementInOwnAssertion(boolean inOwnAssertion) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        statementInOwnAssertion = inOwnAssertion;
    }
        
    /**
     * Set the strategy used to locate the {@link Response} to operate on.
     * 
     * @param strategy strategy used to locate the {@link Response} to operate on
     */
    public synchronized void setResponseLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, Response> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        responseLookupStrategy = Constraint.isNotNull(strategy, "Response lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link IdentifierGenerationStrategy} to use.
     * 
     * @param strategy lookup strategy
     */
    public synchronized void setIdentifierGeneratorLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, IdentifierGenerationStrategy> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        idGeneratorLookupStrategy =
                Constraint.isNotNull(strategy, "IdentifierGenerationStrategy lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the issuer value to use.
     * 
     * @param strategy lookup strategy
     */
    public synchronized void setIssuerLookupStrategy(@Nonnull final Function<ProfileRequestContext, String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        issuerLookupStrategy = Constraint.isNotNull(strategy, "Issuer lookup strategy cannot be null");
    }
    
    /**
     * Set the strategy function to use to obtain the authentication context class reference to use.
     * 
     * @param strategy  authentication context class reference lookup strategy
     */
    public synchronized void setClassRefLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AuthnContextClassRefPrincipal> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        classRefLookupStrategy = Constraint.isNotNull(strategy,
                "Authentication context class reference strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the SessionNotOnOrAfter value to use.
     * 
     * @param strategy lookup strategy
     */
    public synchronized void setSessionLifetimeLookupStrategy(
            @Nullable final Function<ProfileRequestContext, Long> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        sessionLifetimeLookupStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {
        log.debug("{} Attempting to add an AuthnStatement to Response", getLogPrefix());

        idGenerator = idGeneratorLookupStrategy.apply(profileRequestContext);
        if (idGenerator == null) {
            log.debug("{} No identifier generation strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        issuerId = issuerLookupStrategy.apply(profileRequestContext);
        if (issuerId == null) {
            log.debug("{} No assertion issuer value", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        authenticationResult = authenticationContext.getAuthenticationResult();
        if (authenticationResult == null) {
            log.debug("{} No AuthenticationResult in current authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_AUTHN_CTX);
            return false;
        }
        
        response = responseLookupStrategy.apply(profileRequestContext);
        if (response == null) {
            log.debug("{} No SAML response located in current profile request context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }
        
        return super.doPreExecute(profileRequestContext, authenticationContext);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        final Assertion assertion = getStatementAssertion();
        final AuthnStatement statement = buildAuthnStatement(profileRequestContext,
                authenticationContext.getSubcontext(RequestedPrincipalContext.class, false)); 
        assertion.getAuthnStatements().add(statement);

        log.debug("{} Added AuthenticationStatement to assertion {}", getLogPrefix(), assertion.getID());
    }

    /**
     * Get the assertion to which the authentication statement will be added.
     * 
     * @return the assertion to which the attribute statement will be added
     */
    @Nonnull private Assertion getStatementAssertion() {
        if (statementInOwnAssertion || response.getAssertions().isEmpty()) {
            return SAML2ActionSupport.addAssertionToResponse(this, response, idGenerator, issuerId);
        } else {
            return response.getAssertions().get(0);
        }
    }

    /**
     * Build the {@link AuthnStatement} to be added to the {@link Response}.
     * 
     * @param profileRequestContext current request context
     * @param requestedPrincipalContext context specifying request requirements for authn context
     * 
     * @return the authentication statement
     */
    @Nonnull private AuthnStatement buildAuthnStatement(
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nullable final RequestedPrincipalContext requestedPrincipalContext) {

        final XMLObjectBuilderFactory bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final SAMLObjectBuilder<AuthnStatement> statementBuilder = (SAMLObjectBuilder<AuthnStatement>)
                bf.<AuthnStatement>getBuilderOrThrow(AuthnStatement.TYPE_NAME);
        final SAMLObjectBuilder<AuthnContext> authnContextBuilder = (SAMLObjectBuilder<AuthnContext>)
                bf.<AuthnContext>getBuilderOrThrow(AuthnContext.TYPE_NAME);
        final SAMLObjectBuilder<SubjectLocality> localityBuilder = (SAMLObjectBuilder<SubjectLocality>)
                bf.<SubjectLocality>getBuilderOrThrow(SubjectLocality.TYPE_NAME);

        final AuthnStatement statement = statementBuilder.buildObject();
        statement.setAuthnInstant(new DateTime(authenticationResult.getAuthenticationInstant()));
        
        final AuthnContext authnContext = authnContextBuilder.buildObject();
        statement.setAuthnContext(authnContext);
        
        if (requestedPrincipalContext != null && requestedPrincipalContext.getMatchingPrincipal() != null) {
            final Principal matchingPrincipal = requestedPrincipalContext.getMatchingPrincipal();
            if (matchingPrincipal instanceof AuthnContextClassRefPrincipal) {
                authnContext.setAuthnContextClassRef(
                        ((AuthnContextClassRefPrincipal) matchingPrincipal).getAuthnContextClassRef());
            } else if (matchingPrincipal instanceof AuthnContextDeclRefPrincipal) {
                authnContext.setAuthnContextDeclRef(
                        ((AuthnContextDeclRefPrincipal) matchingPrincipal).getAuthnContextDeclRef());
            } else {
                authnContext.setAuthnContextClassRef(new AuthnContextClassRefPrincipal(
                        AuthnContext.UNSPECIFIED_AUTHN_CTX).getAuthnContextClassRef());
            }
        } else {
            authnContext.setAuthnContextClassRef(
                    classRefLookupStrategy.apply(profileRequestContext).getAuthnContextClassRef());
        }
        
        if (sessionLifetimeLookupStrategy != null) {
            final Long lifetime = sessionLifetimeLookupStrategy.apply(profileRequestContext);
            if (lifetime != null && lifetime > 0) {
                statement.setSessionNotOnOrAfter(new DateTime().plus(lifetime));
            }
        }
        
        statement.setSessionIndex(idGenerator.generateIdentifier());

        if (getHttpServletRequest() != null) {
            final SubjectLocality locality = localityBuilder.buildObject();
            locality.setAddress(getHttpServletRequest().getRemoteAddr());
            statement.setSubjectLocality(locality);
        } else {
            log.debug("{} HttpServletRequest not available, omitting SubjectLocality element", getLogPrefix());
        }
        
        return statement;
    }    
}
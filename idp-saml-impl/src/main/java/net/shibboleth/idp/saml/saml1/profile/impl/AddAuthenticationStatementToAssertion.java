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

package net.shibboleth.idp.saml.saml1.profile.impl;

import java.security.Principal;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.saml.saml1.core.SubjectLocality;
import org.opensaml.saml.saml1.profile.SAML1ActionSupport;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.impl.DefaultPrincipalDeterminationStrategy;
import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;
import net.shibboleth.idp.saml.profile.impl.BaseAddAuthenticationStatementToAssertion;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;

/**
 * Action that builds an {@link AuthenticationStatement} and adds it to an {@link Assertion} returned by a lookup
 * strategy, by default in the {@link ProfileRequestContext#getOutboundMessageContext()}.
 * 
 * <p>If no {@link Response} exists, then an {@link Assertion} directly in the outbound message context will
 * be used or created</p>
 * 
 * <p>A constructed {@link Assertion} will have its ID, IssueInstant, Issuer, and Version properties set.
 * The issuer is based on
 * {@link net.shibboleth.profile.relyingparty.RelyingPartyConfiguration#getResponderId(ProfileRequestContext)}.</p>
 * 
 * <p>The {@link AuthenticationStatement} will have its authentication instant set, based on
 * {@link net.shibboleth.idp.authn.AuthenticationResult#getAuthenticationInstant()}
 * via {@link AuthenticationContext#getAuthenticationResult()}.
 * The method property will be set via {@link RequestedPrincipalContext#getMatchingPrincipal()}, or via an injected
 * or defaulted function that obtains an {@link AuthenticationMethodPrincipal} from the profile context.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link net.shibboleth.idp.authn.AuthnEventIds#INVALID_AUTHN_CTX}
 */
public class AddAuthenticationStatementToAssertion extends BaseAddAuthenticationStatementToAssertion {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AddAuthenticationStatementToAssertion.class);

    /** Strategy used to locate the {@link Assertion} to operate on. */
    @NonnullAfterInit private Function<ProfileRequestContext,Assertion> assertionLookupStrategy;
    
    /** Strategy used to determine the AuthenticationMethod attribute. */
    @NonnullAfterInit private Function<ProfileRequestContext,AuthenticationMethodPrincipal> methodLookupStrategy;
    
    /** The generator to use. */
    @Nullable private IdentifierGenerationStrategy idGenerator;
        
    /**
     * Set the strategy used to locate the {@link Assertion} to operate on.
     * 
     * @param strategy strategy used to locate the {@link Assertion} to operate on
     */
    public void setAssertionLookupStrategy(@Nonnull final Function<ProfileRequestContext,Assertion> strategy) {
        checkSetterPreconditions();
        assertionLookupStrategy = Constraint.isNotNull(strategy, "Assertion lookup strategy cannot be null");
    }
    
    /**
     * Set the strategy function to use to obtain the authentication method to use.
     * 
     * @param strategy  authentication method lookup strategy
     */
    public void setAuthenticationMethodLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,AuthenticationMethodPrincipal> strategy) {
        checkSetterPreconditions();
        methodLookupStrategy = Constraint.isNotNull(strategy, "Authentication method strategy cannot be null");
    }
        
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (methodLookupStrategy == null) {
            methodLookupStrategy = new DefaultPrincipalDeterminationStrategy<>(AuthenticationMethodPrincipal.class,
                    new AuthenticationMethodPrincipal(AuthenticationStatement.UNSPECIFIED_AUTHN_METHOD));
        }

        if (assertionLookupStrategy == null) {
            assertionLookupStrategy = new AssertionStrategy();
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final Assertion assertion = assertionLookupStrategy.apply(profileRequestContext);
        if (assertion == null) {
            log.error("Unable to obtain Assertion to modify");
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return;
        }

        final AuthenticationStatement statement = buildAuthenticationStatement(profileRequestContext,
                authenticationContext.getSubcontext(RequestedPrincipalContext.class)); 
        assertion.getAuthenticationStatements().add(statement);

        log.debug("{} Added AuthenticationStatement to Assertion {}", getLogPrefix(), assertion.getID());
    }

    /**
     * Build the {@link AuthenticationStatement} to be added to the {@link Response}.
     * 
     * @param profileRequestContext current request context
     * @param requestedPrincipalContext context specifying request requirements for authn method
     * 
     * @return the authentication statement
     */
    @Nonnull private AuthenticationStatement buildAuthenticationStatement(
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nullable final RequestedPrincipalContext requestedPrincipalContext) {

        final XMLObjectBuilderFactory bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final SAMLObjectBuilder<AuthenticationStatement> statementBuilder = (SAMLObjectBuilder<AuthenticationStatement>)
                bf.<AuthenticationStatement>getBuilderOrThrow(AuthenticationStatement.TYPE_NAME);
        final SAMLObjectBuilder<SubjectLocality> localityBuilder = (SAMLObjectBuilder<SubjectLocality>)
                bf.<SubjectLocality>getBuilderOrThrow(SubjectLocality.TYPE_NAME);

        final AuthenticationStatement statement = statementBuilder.buildObject();
        statement.setAuthenticationInstant(getAuthenticationResult().getAuthenticationInstant());

        final Principal matchingPrincipal = requestedPrincipalContext != null ? requestedPrincipalContext.getMatchingPrincipal() : null;
        if (matchingPrincipal != null && matchingPrincipal instanceof AuthenticationMethodPrincipal) {
            statement.setAuthenticationMethod(matchingPrincipal.getName());
        } else {
            statement.setAuthenticationMethod(methodLookupStrategy.apply(profileRequestContext).getName());
        }
        
        final String address = getAddressLookupStrategy().apply(profileRequestContext);
        if (address != null) {
            final SubjectLocality locality = localityBuilder.buildObject();
            locality.setIPAddress(address);
            statement.setSubjectLocality(locality);
        } else {
            log.debug("{} Address not available, omitting SubjectLocality element", getLogPrefix());
        }
        
        return statement;
    }
    
    /**
     * Default strategy for obtaining assertion to modify.
     * 
     * <p>If the outbound context is empty, a new assertion is created and stored there. If the outbound
     * message is already an assertion, it's returned. If the outbound message is a response, then either
     * an existing or new assertion in the response is returned, depending on the action setting. If the
     * outbound message is anything else, null is returned.</p>
     */
    private class AssertionStrategy implements Function<ProfileRequestContext,Assertion> {

        /** {@inheritDoc} */
        @Override
        @Nullable public Assertion apply(@Nullable final ProfileRequestContext input) {
            final MessageContext omc = input == null ? null : input.getOutboundMessageContext(); 
            if (input != null && omc != null) {
                final Object outboundMessage = omc.getMessage();
                if (outboundMessage == null) {
                    final Assertion ret = SAML1ActionSupport.buildAssertion(AddAuthenticationStatementToAssertion.this,
                            getIdGenerator(), getIssuerId());
                    omc.setMessage(ret);
                    return ret;
                } else if (outboundMessage instanceof Assertion) {
                    return (Assertion) outboundMessage;
                } else if (outboundMessage instanceof Response) {
                    if (isStatementInOwnAssertion() || ((Response) outboundMessage).getAssertions().isEmpty()) {
                        return SAML1ActionSupport.addAssertionToResponse(AddAuthenticationStatementToAssertion.this,
                                (Response) outboundMessage, getIdGenerator(), getIssuerId());
                    }
                    return ((Response) outboundMessage).getAssertions().get(0); 
                }
            }
            
            return null;
        }
        
    }
        
}
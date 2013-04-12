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

package net.shibboleth.idp.saml.impl.profile.saml1;

import javax.annotation.Nonnull;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.authn.AuthenticationRequestContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.idp.saml.profile.SamlEventIds;
import net.shibboleth.idp.saml.profile.saml1.Saml1ActionSupport;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.joda.time.DateTime;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml1.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/**
 * Builds an {@link AuthenticationStatement} and adds it to the {@link Response} set as the message of the
 * {@link ProfileRequestContext#getOutboundMessageContext()}. If the {@link Response} does not contain any
 * {@link Assertion} one will be created and added to it, otherwise the {@link AuthenticationStatement} will be added to
 * first {@link Assertion} in the {@link Response}.
 * 
 * A constructed {@link Assertion} will have its ID, issue instant, issuer, and version properties set. The issuer is
 * retrieved from the {@link org.opensaml.messaging.context.BasicMessageMetadataContext} on the
 * {@link ProfileRequestContext#getOutboundMessageContext()}.
 * 
 * The constructed {@link AuthenticationStatement} will have its authentication instant and method properties set. This
 * information is retrieved from the {@link AuthenticationRequestContext} on the {@link ProfileRequestContext}.
 */
@Events({
        @Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = EventIds.INVALID_RELYING_PARTY_CTX,
                description = "Returned if no relying party information is associated with the current request"),
        @Event(id = SamlEventIds.NO_RESPONSE,
                description = "No SAML response object is associated with the current request")})
public class AddAuthenticationStatementToAssertion extends AbstractProfileAction<Object, Response> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddAuthenticationStatementToAssertion.class);

    /**
     * Whether the generated authentication statement should be placed in its own assertion or added to one if it
     * exists.
     */
    private boolean statementInOwnAssertion;

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Constructor. */
    public AddAuthenticationStatementToAssertion() {
        super();

        statementInOwnAssertion = false;

        relyingPartyContextLookupStrategy =
                new ChildContextLookup<ProfileRequestContext, RelyingPartyContext>(RelyingPartyContext.class, false);
    }

    /**
     * Gets whether the generated authentication statement should be placed in its own assertion or added to one if it
     * exists.
     * 
     * @return whether the generated authentication statement should be placed in its own assertion or added to one if
     *         it exists
     */
    public boolean isStatementInOwnAssertion() {
        return statementInOwnAssertion;
    }

    /**
     * Sets whether the generated authentication statement should be placed in its own assertion or added to one if it
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
     * Gets the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @return strategy used to locate the {@link RelyingPartyContext} associated with a given
     *         {@link ProfileRequestContext}
     */
    @Nonnull public Function<ProfileRequestContext, RelyingPartyContext> getRelyingPartyContextLookupStrategy() {
        return relyingPartyContextLookupStrategy;
    }

    /**
     * Sets the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public synchronized void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy can not be null");
    }

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(
            @Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext<Object, Response> profileRequestContext) throws ProfileException {
        log.debug("Action {}: Attempting to add an AuthenticationStatement to outgoing Response", getId());

        final RelyingPartyContext relyingPartyCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (relyingPartyCtx == null) {
            log.error("Action {}: No relying party context located in current profile request context", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_RELYING_PARTY_CTX);
        }
        
        final Response response = profileRequestContext.getOutboundMessageContext().getMessage();
        if (response == null) {
            log.error("Action {}: No SAML response located in current profile request context", getId());
            return ActionSupport.buildEvent(this, SamlEventIds.NO_RESPONSE);
        }

        final AuthenticationStatement statement = buildAuthenticationStatement(profileRequestContext);
        if (statement == null) {
            log.debug("Action {}: No AuthenticationStatement was built, nothing left to do");
            return ActionSupport.buildProceedEvent(this);
        }

        final Assertion assertion =
                getStatementAssertion(relyingPartyCtx, profileRequestContext.getOutboundMessageContext().getMessage());
        assertion.getAuthenticationStatements().add(statement);

        log.debug("Action {}: Added AuthenticationStatement to assertion {}", getId(), assertion.getID());
        return ActionSupport.buildProceedEvent(this);
    }

    /**
     * Gets the assertion to which the authentication statement will be added.
     * 
     * @param relyingPartyContext current relying party information
     * @param response current response
     * 
     * @return the assertion to which the attribute statement will be added
     */
    private Assertion getStatementAssertion(RelyingPartyContext relyingPartyContext, Response response) {
        final Assertion assertion;
        if (statementInOwnAssertion || response.getAssertions().isEmpty()) {
            assertion = Saml1ActionSupport.addAssertionToResponse(this, relyingPartyContext, response);
        } else {
            assertion = response.getAssertions().get(0);
        }

        return assertion;
    }

    /**
     * Builds the {@link AuthenticationStatement} to be added to the {@link Response}.
     * 
     * @param profileRequestContext current request context
     * 
     * @return the authentication statement or null if no {@link AuthenticationRequestContext} is available
     */
    private AuthenticationStatement buildAuthenticationStatement(
            final ProfileRequestContext<Object, Response> profileRequestContext) {
        final AuthenticationRequestContext authnCtx =
                profileRequestContext.getSubcontext(AuthenticationRequestContext.class, false);
        if (authnCtx == null) {
            log.debug("Action {}: Not AuthenticationRequestContext available, nothing left to do", getId());
            return null;
        }

        final SAMLObjectBuilder<AuthenticationStatement> statementBuilder =
                (SAMLObjectBuilder<AuthenticationStatement>) XMLObjectProviderRegistrySupport.getBuilderFactory()
                        .getBuilder(AuthenticationStatement.TYPE_NAME);

        final AuthenticationStatement statement = statementBuilder.buildObject();
        statement.setAuthenticationInstant(new DateTime(authnCtx.getCompletionInstant()));
        statement.setAuthenticationMethod(authnCtx.getAttemptedWorkflow().get().getWorkflowId());
        return statement;
    }
}
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.authn.AuthenticationRequestContext;
import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartySubcontext;
import net.shibboleth.idp.saml.profile.saml1.Saml1ActionSupport;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml1.core.Assertion;
import org.opensaml.saml1.core.AuthenticationStatement;
import org.opensaml.saml1.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

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
public class AddAuthenticationStatementToAssertion extends AbstractIdentityProviderAction<Object, Response> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddAuthenticationStatementToAssertion.class);

    /** {@inheritDoc} */
    protected Event doExecute(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            RequestContext springRequestContext, ProfileRequestContext<Object, Response> profileRequestContext)
            throws ProfileException {
        log.debug("Action {}: Attempting to add an AuthenticationStatement to outgoing Response", getId());

        final RelyingPartySubcontext relyingPartyCtx =
                ActionSupport.getRequiredRelyingPartyContext(this, profileRequestContext);

        final AuthenticationStatement statement = buildAuthenticationStatement(profileRequestContext);
        if (statement == null) {
            log.debug("Action {}: No AuthenticationStatement was built, nothing left to do");
            return ActionSupport.buildProceedEvent(this);
        }

        final Assertion assertion =
                getStatementAssertion(relyingPartyCtx,
                        ActionSupport.getRequiredOutboundMessage(this, profileRequestContext));
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
    private Assertion getStatementAssertion(RelyingPartySubcontext relyingPartyContext, Response response) {
        // TODO allow for a configuration option that forces the statement in to its own assertion

        final Assertion assertion;
        if (response.getAssertions().isEmpty()) {
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
                (SAMLObjectBuilder<AuthenticationStatement>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        AuthenticationStatement.TYPE_NAME);

        final AuthenticationStatement statement = statementBuilder.buildObject();
        statement.setAuthenticationInstant(new DateTime(authnCtx.getCompletionInstant()));
        statement.setAuthenticationMethod(authnCtx.getAttemptedWorkflow().getWorkflowId());
        return statement;
    }
}
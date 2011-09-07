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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.authn.AuthenticationRequestContext;
import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.joda.time.DateTime;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.SAMLVersion;
import org.opensaml.messaging.context.BasicMessageMetadataSubcontext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml1.core.Assertion;
import org.opensaml.saml1.core.AuthenticationStatement;
import org.opensaml.saml1.core.Response;
import org.opensaml.xml.Configuration;
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
 * retrieved from the {@link BasicMessageMetadataSubcontext} on the
 * {@link ProfileRequestContext#getOutboundMessageContext()}.
 * 
 * The constructed {@link AuthenticationStatement} will have its authentication instant and method properties set. This
 * information is retrieved from the {@link AuthenticationRequestContext} on the {@link ProfileRequestContext}.
 */
public class AddAuthenticationStatementToAssertion extends AbstractIdentityProviderAction<Object, Response> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddAuthenticationStatementToAssertion.class);

    /** {@inheritDoc} */
    public Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext,
            final ProfileRequestContext<Object, Response> profileRequestContext) {
        log.debug("Action {}: attempting to add DoNotCache condition to every Assertion in outgoing Response", getId());

        final MessageContext<Response> outMsgCtx = profileRequestContext.getOutboundMessageContext();
        if (outMsgCtx == null) {
            log.debug("Action {}: no outbound message context available, no DoNotCache condition added", getId());
            return ActionSupport.buildProceedEvent(this);
        }

        final Response response = outMsgCtx.getMessage();
        if (response == null) {
            log.debug("Action {}: no outbound message available, no DoNotCache condition added", getId());
            return ActionSupport.buildProceedEvent(this);
        }

        final List<Assertion> assertions = response.getAssertions();

        final Assertion assertion;
        if (assertions.isEmpty()) {
            assertion = buildAssertion(profileRequestContext, outMsgCtx, response);
            if (assertion == null) {
                return ActionSupport.buildProceedEvent(this);
            }
        } else {
            assertion = assertions.get(0);
        }

        final AuthenticationStatement statement = buildAuthenticationStatement(profileRequestContext);
        if (statement == null) {
            return ActionSupport.buildProceedEvent(this);
        }

        assertion.getAuthenticationStatements().add(statement);
        log.debug("Action {}: add authentication statement to assertion {} in response {}", new Object[] {getId(),
                assertion.getID(), response.getID(),});
        return ActionSupport.buildProceedEvent(this);
    }

    /**
     * Builds and adds an {@link Assertion} to the {@link Response}.
     * 
     * @param profileRequestContext current request context
     * @param outboundMessageContext the outbound message context
     * @param response response to which the assertion will be added
     * 
     * @return the constructed assertion
     */
    private Assertion buildAssertion(final ProfileRequestContext<Object, Response> profileRequestContext,
            final MessageContext outboundMessageContext, final Response response) {
        final BasicMessageMetadataSubcontext outMsgMetadataCtx =
                outboundMessageContext.getSubcontext(BasicMessageMetadataSubcontext.class, false);
        if (outMsgMetadataCtx == null) {

            return null;
        }

        final SAMLObjectBuilder<Assertion> assertionBuilder =
                (SAMLObjectBuilder<Assertion>) Configuration.getBuilderFactory().getBuilder(Assertion.TYPE_NAME);

        final Assertion assertion = assertionBuilder.buildObject();
        // TODO set ID once security configuration containing identifier generator is available
        // assertion.setID(id);
        assertion.setIssueInstant(response.getIssueInstant());
        assertion.setIssuer(outMsgMetadataCtx.getMessageIssuer());
        assertion.setVersion(SAMLVersion.VERSION_11);

        response.getAssertions().add(assertion);
        log.debug("Action {}: added assertion {} to response", getId(), assertion.getID());

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
            log.debug(
                    "Action {}: no authentication request context available, unable to construct authentication statement",
                    getId());
            return null;
        }

        final SAMLObjectBuilder<AuthenticationStatement> statementBuilder =
                (SAMLObjectBuilder<AuthenticationStatement>) Configuration.getBuilderFactory().getBuilder(
                        AuthenticationStatement.TYPE_NAME);

        final AuthenticationStatement statement = statementBuilder.buildObject();
        statement.setAuthenticationInstant(new DateTime(authnCtx.getCompletionInstant()));
        statement.setAuthenticationMethod(authnCtx.getAttemptedWorkflow().getWorkflowId());
        return statement;
    }
}
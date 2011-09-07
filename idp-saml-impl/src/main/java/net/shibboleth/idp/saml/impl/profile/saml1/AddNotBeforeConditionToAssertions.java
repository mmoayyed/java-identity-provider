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

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml1.core.Assertion;
import org.opensaml.saml1.core.Conditions;
import org.opensaml.saml1.core.Response;
import org.opensaml.xml.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Adds the <code>NotBefore</code> condition to every {@link Assertion} in the outgoing {@link Response} retrieved from
 * the {@link ProfileRequestContext#getOutboundMessageContext()}. If no {@link Conditions} is present on and
 * {@link Assertion} one will be created.
 */
public class AddNotBeforeConditionToAssertions extends AbstractIdentityProviderAction<Object, Response> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddNotBeforeConditionToAssertions.class);

    /** {@inheritDoc} */
    public Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext,
            final ProfileRequestContext<Object, Response> profileRequestContext) {
        log.debug("Action {}: attempting to add NotBefore condition to every Assertion in outgoing Response", getId());

        final MessageContext<Response> outMsgCtx = profileRequestContext.getOutboundMessageContext();
        if (outMsgCtx == null) {
            log.debug("Action {}: no outbound message context available, no NotBefore condition added", getId());
            return ActionSupport.buildProceedEvent(this);
        }

        final Response response = outMsgCtx.getMessage();
        if (response == null) {
            log.debug("Action {}: no outbound message available,no NotBefore condition added", getId());
            return ActionSupport.buildProceedEvent(this);
        }

        final SAMLObjectBuilder<Conditions> conditionsBuilder =
                (SAMLObjectBuilder<Conditions>) Configuration.getBuilderFactory().getBuilder(Conditions.TYPE_NAME);

        final List<Assertion> assertions = response.getAssertions();
        if (assertions == null || assertions.isEmpty()) {
            log.debug("Action {}: no assertions present in response, nothing to add NotBefore conditions to", getId());
            return ActionSupport.buildProceedEvent(this);
        }

        Conditions conditions;
        for (Assertion assertion : assertions) {
            conditions = assertion.getConditions();
            if (conditions == null) {
                conditions = conditionsBuilder.buildObject();
                assertion.setConditions(conditions);
            }

            log.debug("Action {}: added NotBefore condition to assertion {}", getId(), assertion.getID());
            conditions.setNotBefore(response.getIssueInstant());
        }

        log.debug("Action {}: added NotBefore condition to all assertions in response {}", getId(), response.getID());
        return ActionSupport.buildProceedEvent(this);
    }
}
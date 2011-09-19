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

import net.shibboleth.idp.profile.AbstractProfileRequestSubcontextAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartySubcontext;
import net.shibboleth.idp.saml.profile.saml1.Saml1Support;
import net.shibboleth.idp.saml.relyingparty.saml1.AbstractSAML1ProfileConfiguration;

import org.joda.time.DateTime;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml1.core.Assertion;
import org.opensaml.saml1.core.Conditions;
import org.opensaml.saml1.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

//TODO profile config needs to carry assertion l

/**
 *
 */
public class AddNotOnOrAfterConditionToAssertions extends
        AbstractProfileRequestSubcontextAction<Object, Response, RelyingPartySubcontext> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddNotOnOrAfterConditionToAssertions.class);

    /** {@inheritDoc} */
    protected Class<RelyingPartySubcontext> getSubcontextType() {
        return RelyingPartySubcontext.class;
    }

    /** {@inheritDoc} */
    protected Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext,
            final ProfileRequestContext<Object, Response> profileRequestContext,
            final RelyingPartySubcontext relyingPartyContext) throws ProfileException {
        log.debug("Action {}: Attempting to add NotOnOrAfter condition to every Assertion in outgoing Response",
                getId());

        final MessageContext<Response> messageContext =
                ActionSupport.getOutboundMessageContext(this, profileRequestContext);

        final Response response = ActionSupport.getOutboundMessage(this, messageContext);

        final List<Assertion> assertions =
                Saml1Support.getAssertionsFromResponse(this, profileRequestContext, relyingPartyContext);

        if (!(relyingPartyContext.getProfileConfig() instanceof AbstractSAML1ProfileConfiguration)) {
            // TODO error
        }

        final AbstractSAML1ProfileConfiguration profileConfig =
                (AbstractSAML1ProfileConfiguration) relyingPartyContext.getProfileConfig();

        Conditions conditions;
        DateTime expiration = new DateTime(response.getIssueInstant()); // TODO add assertion lifetime
        for (Assertion assertion : assertions) {
            conditions = Saml1Support.getConditionsFromAssertion(this, assertion);
            log.debug(
                    "Action {}: Added NotOnOrAfter condition, to Assertion {}, that indicates Assertione expiration of {}",
                    new Object[] {getId(), assertion.getID(), expiration});
            conditions.setNotOnOrAfter(expiration);
        }

        return ActionSupport.buildProceedEvent(this);
    }
}
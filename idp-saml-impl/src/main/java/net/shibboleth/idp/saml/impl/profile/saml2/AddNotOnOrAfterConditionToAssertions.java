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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.InvalidOutboundMessageException;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartySubcontext;
import net.shibboleth.idp.saml.profile.config.AbstractSamlProfileConfiguration;
import net.shibboleth.idp.saml.profile.saml2.Saml2ActionSupport;

import org.joda.time.DateTime;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Sets the NotOnOrAfter attribute on the {@link Conditions} in every {@link Assertion} in the outgoing {@link Response}
 * retrieved from the {@link ProfileRequestContext#getOutboundMessageContext()}. If no {@link Conditions} is present on
 * the {@link Assertion} one will be created.
 * 
 * This action requires that the outbound message context to contain a {@link Response} with one, or more,
 * {@link Assertion}.
 */
public class AddNotOnOrAfterConditionToAssertions extends AbstractIdentityProviderAction<Object, Response> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddNotOnOrAfterConditionToAssertions.class);

    /** {@inheritDoc} */
    protected Event doExecute(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            RequestContext springRequestContext, ProfileRequestContext<Object, Response> profileRequestContext)
            throws ProfileException {
        log.debug("Action {}: Attempting to add NotOnOrAfter condition to every Assertion in outgoing Response",
                getId());

        final RelyingPartySubcontext relyingPartyCtx =
                ActionSupport.getRequiredRelyingPartyContext(this, profileRequestContext);

        final Response response = ActionSupport.getRequiredOutboundMessage(this, profileRequestContext);

        final List<Assertion> assertions = response.getAssertions();
        if (assertions.isEmpty()) {
            log.error("Action {}: Unable to add Conditions, outbound Response does not contain any Asertions");
            throw new InvalidOutboundMessageException("No Assertion available within the Response");
        }

        final AbstractSamlProfileConfiguration profileConfig =
                (AbstractSamlProfileConfiguration) relyingPartyCtx.getProfileConfig();

        Conditions conditions;
        DateTime expiration = new DateTime(response.getIssueInstant()).plus(profileConfig.getAssertionLifetime());
        for (Assertion assertion : assertions) {
            conditions = Saml2ActionSupport.addConditionsToAssertion(this, assertion);
            log.debug(
                    "Action {}: Added NotOnOrAfter condition, indicating an expiration instant of {}, to Assertion {}",
                    new Object[] {getId(), expiration, assertion.getID()});
            conditions.setNotOnOrAfter(expiration);
        }

        return ActionSupport.buildProceedEvent(this);
    }
}
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

import javax.annotation.Nonnull;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.saml.profile.SamlEventIds;
import net.shibboleth.idp.saml.profile.saml2.Saml2ActionSupport;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

/**
 * Adds the <code>NotBefore</code> condition to every {@link Assertion} in the outgoing {@link Response} retrieved from
 * the {@link ProfileRequestContext#getOutboundMessageContext()}. If no {@link Conditions} is present on and
 * {@link Assertion} one will be created.
 */
@Events({
        @Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = SamlEventIds.NO_ASSERTION, description = "Outbound response does not contain an assertion"),
        @Event(id = SamlEventIds.NO_RESPONSE,
                description = "No SAML response object is associated with the current request")})
public class AddNotBeforeConditionToAssertions extends AbstractProfileAction<Object, Response> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddNotBeforeConditionToAssertions.class);

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(@Nonnull final RequestContext springRequestContext,
            ProfileRequestContext<Object, Response> profileRequestContext) throws ProfileException {
        log.debug("Action {}: Attempting to add NotBefore condition to every Assertion in outgoing Response", getId());

        final Response response = profileRequestContext.getOutboundMessageContext().getMessage();
        if (response == null) {
            log.error("Action {}: No SAML response located in current profile request context", getId());
            return ActionSupport.buildEvent(this, SamlEventIds.NO_RESPONSE);
        }

        final List<Assertion> assertions = response.getAssertions();
        if (assertions.isEmpty()) {
            log.debug("Action {}: Unable to add NotBefore condition, Response does not contain an Asertion", getId());
            return ActionSupport.buildEvent(this, SamlEventIds.NO_ASSERTION);
        }

        Conditions conditions;
        for (Assertion assertion : assertions) {
            conditions = Saml2ActionSupport.addConditionsToAssertion(this, assertion);
            log.debug("Action {}: Added NotBefore condition to Assertion {}", getId(), assertion.getID());
            conditions.setNotBefore(response.getIssueInstant());
        }

        return ActionSupport.buildProceedEvent(this);
    }
}
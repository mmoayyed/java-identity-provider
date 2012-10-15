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

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.EventIds;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.saml.profile.SamlEventIds;
import net.shibboleth.idp.saml.profile.saml1.Saml1ActionSupport;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Conditions;
import org.opensaml.saml.saml1.core.DoNotCacheCondition;
import org.opensaml.saml.saml1.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a {@link DoNotCacheCondition} to every {@link Assertion} in the outgoing {@link Response} retrieved from the
 * {@link ProfileRequestContext#getOutboundMessageContext()}. If no {@link Conditions} is present on the
 * {@link Assertion} one will be created.
 * 
 * This action requires that the outbound message context to contain a {@link Response} with one, or more,
 * {@link Assertion}.
 */
@Events({
        @Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = SamlEventIds.NO_ASSERTION, description = "Outbound response does not contain an assertion"),
        @Event(id = SamlEventIds.NO_RESPONSE,
                description = "No SAML response object is associated with the current request")})
public class AddDoNotCacheConditionToAssertions extends AbstractProfileAction<Object, Response> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddDoNotCacheConditionToAssertions.class);

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, ProfileRequestContext<Object, Response> profileRequestContext)
            throws ProfileException {
        log.debug("Action {}: Attempting to add DoNotCache condition to every Assertion in outgoing Response", getId());

        final Response response = profileRequestContext.getOutboundMessageContext().getMessage();
        if (response == null) {
            log.error("Action {}: No SAML response located in current profile request context", getId());
            return ActionSupport.buildEvent(this, SamlEventIds.NO_RESPONSE);
        }

        final List<Assertion> assertions = response.getAssertions();
        if (assertions.isEmpty()) {
            log.debug("Action {}: Unable to add DoNotCacheCondition, Response does not contain an Asertion", getId());
            return ActionSupport.buildEvent(this, SamlEventIds.NO_ASSERTION);
        }

        final SAMLObjectBuilder<DoNotCacheCondition> dncConditionBuilder =
                (SAMLObjectBuilder<DoNotCacheCondition>) XMLObjectProviderRegistrySupport.getBuilderFactory()
                        .getBuilder(DoNotCacheCondition.TYPE_NAME);

        Conditions conditions;
        List<DoNotCacheCondition> dncConditions;
        for (Assertion assertion : assertions) {
            conditions = Saml1ActionSupport.addConditionsToAssertion(this, assertion);
            dncConditions = conditions.getDoNotCacheConditions();
            if (dncConditions.isEmpty()) {
                dncConditions.add(dncConditionBuilder.buildObject());
                log.debug("Action {}: Added DoNotCache condition to Assertion {}", getId(), assertion.getID());
            } else {
                log.debug("Action {}: Assertion {} already contained DoNotCache condition, another was not added",
                        getId(), assertion.getID());
            }

        }

        return ActionSupport.buildProceedEvent(this);
    }
}
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

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.EventIds;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.idp.saml.profile.SamlEventIds;
import net.shibboleth.idp.saml.profile.config.AbstractSamlProfileConfiguration;
import net.shibboleth.idp.saml.profile.saml1.Saml1ActionSupport;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.joda.time.DateTime;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Conditions;
import org.opensaml.saml.saml1.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/**
 * Sets the NotOnOrAfter attribute on the {@link Conditions} in every {@link Assertion} in the outgoing {@link Response}
 * retrieved from the {@link ProfileRequestContext#getOutboundMessageContext()}. If no {@link Conditions} is present on
 * the {@link Assertion} one will be created.
 * 
 * This action requires that the outbound message context to contain a {@link Response} with one, or more,
 * {@link Assertion}.
 */
@Events({
        @Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = EventIds.NO_RELYING_PARTY_CTX,
                description = "No relying party information is associated with the current request"),
        @Event(id = SamlEventIds.NO_ASSERTION, description = "Outbound response does not contain an assertion"),
        @Event(id = SamlEventIds.NO_RESPONSE,
                description = "No SAML response object is associated with the current request")})
public class AddNotOnOrAfterConditionToAssertions extends AbstractProfileAction<Object, Response> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddNotOnOrAfterConditionToAssertions.class);

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Constructor. */
    public AddNotOnOrAfterConditionToAssertions() {
        super();

        relyingPartyContextLookupStrategy =
                new ChildContextLookup<ProfileRequestContext, RelyingPartyContext>(RelyingPartyContext.class, false);
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
    protected org.springframework.webflow.execution.Event doExecute(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, RequestContext springRequestContext,
            ProfileRequestContext<Object, Response> profileRequestContext) throws ProfileException {
        log.debug("Action {}: Attempting to add NotOnOrAfter condition to every Assertion in outgoing Response",
                getId());

        final RelyingPartyContext relyingPartyCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (relyingPartyCtx == null) {
            log.error("Action {}: No relying party context located in current profile request context", getId());
            return ActionSupport.buildEvent(this, EventIds.NO_RELYING_PARTY_CTX);
        }

        final Response response = profileRequestContext.getOutboundMessageContext().getMessage();
        if (response == null) {
            log.error("Action {}: No SAML response located in current profile request context", getId());
            return ActionSupport.buildEvent(this, SamlEventIds.NO_RESPONSE);
        }

        final List<Assertion> assertions = response.getAssertions();
        if (assertions.isEmpty()) {
            log.debug("Action {}: Unable to add NotOnOrAfter condition, Response does not contain an Asertion",
                    getId());
            return ActionSupport.buildEvent(this, SamlEventIds.NO_ASSERTION);
        }

        final AbstractSamlProfileConfiguration profileConfig =
                (AbstractSamlProfileConfiguration) relyingPartyCtx.getProfileConfig();

        Conditions conditions;
        DateTime expiration = new DateTime(response.getIssueInstant()).plus(profileConfig.getAssertionLifetime());
        for (Assertion assertion : assertions) {
            conditions = Saml1ActionSupport.addConditionsToAssertion(this, assertion);
            log.debug(
                    "Action {}: Added NotOnOrAfter condition, indicating an expiration instant of {}, to Assertion {}",
                    new Object[] {getId(), expiration, assertion.getID()});
            conditions.setNotOnOrAfter(expiration);
        }

        return ActionSupport.buildProceedEvent(this);
    }
}
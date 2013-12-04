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

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.EventIds;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.idp.saml.profile.SamlEventIds;
import net.shibboleth.idp.saml.profile.config.AbstractSamlProfileConfiguration;
import net.shibboleth.idp.saml.profile.saml1.Saml1ActionSupport;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Audience;
import org.opensaml.saml.saml1.core.AudienceRestrictionCondition;
import org.opensaml.saml.saml1.core.Conditions;
import org.opensaml.saml.saml1.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/** Adds an {@link AudienceRestrictionCondition} to every {@link Assertion} contained on the {@link Response}. */
@Events({
        @Event(id = org.opensaml.profile.action.EventIds.PROCEED_EVENT_ID),
        @Event(id = EventIds.INVALID_RELYING_PARTY_CTX,
                description = "No relying party information is associated with the current request"),
        @Event(id = SamlEventIds.NO_ASSERTION, description = "Outbound response does not contain an assertion"),
        @Event(id = SamlEventIds.NO_RESPONSE,
                description = "No SAML response object is associated with the current request")})
public class AddAudienceRestrictionToAssertions extends AbstractProfileAction<Object, Response> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddAudienceRestrictionToAssertions.class);

    /**
     * Whether, if an assertion already contains an audience restriction, this action will add its audiences to that
     * restriction or create another one.
     */
    private boolean addingAudiencesToExistingRestriction;

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /**
     * Constructor. Initializes {@link #addingAudiencesToExistingRestriction} to <code>true</code>. Initializes
     * {@link #relyingPartyContextLookupStrategy} to {@link ChildContextLookup}.
     */
    public AddAudienceRestrictionToAssertions() {
        super();

        addingAudiencesToExistingRestriction = true;

        relyingPartyContextLookupStrategy =
                new ChildContextLookup<ProfileRequestContext, RelyingPartyContext>(RelyingPartyContext.class, false);
    }

    /**
     * Gets whether, if an assertion already contains an audience restriction, this action will add its audiences to
     * that restriction or create another one.
     * 
     * @return whether this action will add its audiences to that restriction or create another one
     */
    public boolean isAddingAudiencesToExistingRestriction() {
        return addingAudiencesToExistingRestriction;
    }

    /**
     * Sets whether, if an assertion already contains an audience restriction, this action will add its audiences to
     * that restriction or create another one.
     * 
     * @param addingToExistingRestriction whether this action will add its audiences to that restriction or create
     *            another one
     */
    public synchronized void setAddingAudiencesToExistingRestriction(boolean addingToExistingRestriction) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        addingAudiencesToExistingRestriction = addingToExistingRestriction;
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
    protected org.springframework.webflow.execution.Event
            doExecute(@Nonnull final RequestContext springRequestContext,
                    @Nonnull final ProfileRequestContext<Object, Response> profileRequestContext)
                            throws ProfileException {
        log.debug("Action {}: Attempting to add an AudienceRestrictionCondition to outgoing assertions", getId());

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

        final List<Assertion> assertions = response.getAssertions();
        if (assertions.isEmpty()) {
            log.debug("Action {}: Unable to add AudienceRestrictionCondition, Response does not contain an Asertion",
                    getId());
            return ActionSupport.buildEvent(this, SamlEventIds.NO_ASSERTION);
        }

        Conditions conditions;
        for (Assertion assertion : assertions) {
            conditions = Saml1ActionSupport.addConditionsToAssertion(this, assertion);
            addAudienceRestriction(conditions, relyingPartyCtx);
            log.debug("Action {}: Added AudienceRestrictionCondition to Assertion {}", getId(), assertion.getID());
        }

        return ActionSupport.buildProceedEvent(this);
    }

    /**
     * Adds the {@link RelyingPartyContext#getRelyingPartyId()} and any additional audiences configured in the active
     * {@link AbstractSamlProfileConfiguration} as {@link Audience} to the {@link AudienceRestrictionCondition}. If no
     * {@link AudienceRestrictionCondition} exists on the given {@link Conditions} one is created and added.
     * 
     * @param conditions condition that has, or will received the created, {@link AudienceRestrictionCondition}
     * @param relyingPartyCtx information about the current relying party
     */
    private void addAudienceRestriction(final Conditions conditions, final RelyingPartyContext relyingPartyCtx) {
        final AudienceRestrictionCondition condition = getAudienceRestrictionCondition(conditions);

        final SAMLObjectBuilder<Audience> audienceBuilder =
                (SAMLObjectBuilder<Audience>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        Audience.DEFAULT_ELEMENT_NAME);

        log.debug("Action {}: Adding {} as an Audience of the AudienceRestrictionCondition", getId(),
                relyingPartyCtx.getRelyingPartyId());
        Audience audience = audienceBuilder.buildObject();
        audience.setUri(relyingPartyCtx.getRelyingPartyId());
        condition.getAudiences().add(audience);

        if (relyingPartyCtx.getProfileConfig() instanceof AbstractSamlProfileConfiguration) {
            final AbstractSamlProfileConfiguration profileConfig =
                    (AbstractSamlProfileConfiguration) relyingPartyCtx.getProfileConfig();
            for (String audienceId : profileConfig.getAdditionalAudiencesForAssertion()) {
                log.debug("Action {}: Adding {} as an Audience of the AudienceRestrictionCondition", getId(),
                        audienceId);
                audience = audienceBuilder.buildObject();
                audience.setUri(audienceId);
                condition.getAudiences().add(audience);
            }
        }
    }

    /**
     * Gets the {@link AudienceRestrictionCondition} to which audiences will be added.
     * 
     * @param conditions existing set of conditions
     * 
     * @return the condition to which audiences will be added
     */
    private AudienceRestrictionCondition getAudienceRestrictionCondition(Conditions conditions) {
        final AudienceRestrictionCondition condition;

        if (!addingAudiencesToExistingRestriction || conditions.getAudienceRestrictionConditions().isEmpty()) {
            final SAMLObjectBuilder<AudienceRestrictionCondition> conditionBuilder =
                    (SAMLObjectBuilder<AudienceRestrictionCondition>) XMLObjectProviderRegistrySupport
                            .getBuilderFactory().getBuilder(AudienceRestrictionCondition.TYPE_NAME);
            log.debug("Action {}: Conditions did not contain an AudienceRestrictionCondition, adding one", getId());
            condition = conditionBuilder.buildObject();
            conditions.getAudienceRestrictionConditions().add(condition);
        } else {
            log.debug("Action {}: Conditions already contained an AudienceRestrictionCondition, using it", getId());
            condition = conditions.getAudienceRestrictionConditions().get(0);
        }

        return condition;
    }
}
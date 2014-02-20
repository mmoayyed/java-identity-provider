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
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.saml.profile.SAMLEventIds;
import net.shibboleth.idp.saml.profile.config.SAMLProfileConfiguration;
import net.shibboleth.idp.saml.profile.saml2.SAML2ActionSupport;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/** Adds an {@link AudienceRestriction} to every {@link Assertion} contained on the {@link Response}. */
@Events({
        @Event(id = org.opensaml.profile.action.EventIds.PROCEED_EVENT_ID),
        @Event(id = IdPEventIds.INVALID_RELYING_PARTY_CTX,
                description = "No relying party information is associated with the current request"),
        @Event(id = SAMLEventIds.NO_ASSERTION, description = "Outbound response does not contain an assertion"),
        @Event(id = SAMLEventIds.NO_RESPONSE,
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
    protected org.springframework.webflow.execution.Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext<Object, Response> profileRequestContext) throws ProfileException {
        log.debug("Action {}: Attempting to add an AudienceRestrictionCondition to outgoing assertions", getId());

        final RelyingPartyContext relyingPartyCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (relyingPartyCtx == null) {
            log.error("Action {}: No relying party context located in current profile request context", getId());
            return ActionSupport.buildEvent(this, IdPEventIds.INVALID_RELYING_PARTY_CTX);
        }

        final Response response = profileRequestContext.getOutboundMessageContext().getMessage();
        if (response == null) {
            log.error("Action {}: No SAML response located in current profile request context", getId());
            return ActionSupport.buildEvent(this, SAMLEventIds.NO_RESPONSE);
        }

        final List<Assertion> assertions = response.getAssertions();
        if (assertions.isEmpty()) {
            log.debug("Action {}: Unable to add AudienceRestrictionCondition, Response does not contain an Asertion",
                    getId());
            return ActionSupport.buildEvent(this, SAMLEventIds.NO_ASSERTION);
        }

        Conditions conditions;
        for (Assertion assertion : assertions) {
            conditions = SAML2ActionSupport.addConditionsToAssertion(this, assertion);
            addAudienceRestriction(conditions, relyingPartyCtx);
            log.debug("Action {}: Added AudienceRestrictionCondition to Assertion {}", getId(), assertion.getID());
        }

        return ActionSupport.buildProceedEvent(this);
    }

    /**
     * Adds the {@link RelyingPartyContext#getRelyingPartyId()} and any additional audiences configured in the active
     * {@link AbstractSAMLProfileConfiguration} as {@link Audience} to the {@link AudienceRestriction}. If no
     * {@link AudienceRestriction} exists on the given {@link Conditions} one is created and added.
     * 
     * @param conditions condition that has, or will received the created, {@link AudienceRestriction}
     * @param relyingPartyCtx information about the current relying party
     */
    private void addAudienceRestriction(final Conditions conditions, final RelyingPartyContext relyingPartyCtx) {
        final AudienceRestriction condition = getAudienceRestrictionCondition(conditions);

        final SAMLObjectBuilder<Audience> audienceBuilder =
                (SAMLObjectBuilder<Audience>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        Audience.DEFAULT_ELEMENT_NAME);

        log.debug("Action {}: Adding {} as an Audience of the AudienceRestrictionCondition", getId(),
                relyingPartyCtx.getRelyingPartyId());
        Audience audience = audienceBuilder.buildObject();
        audience.setAudienceURI(relyingPartyCtx.getRelyingPartyId());
        condition.getAudiences().add(audience);

        if (relyingPartyCtx.getProfileConfig() instanceof SAMLProfileConfiguration) {
            final SAMLProfileConfiguration profileConfig =
                    (SAMLProfileConfiguration) relyingPartyCtx.getProfileConfig();
            for (String audienceId : profileConfig.getAdditionalAudiencesForAssertion()) {
                log.debug("Action {}: Adding {} as an Audience of the AudienceRestrictionCondition", getId(),
                        audienceId);
                audience = audienceBuilder.buildObject();
                audience.setAudienceURI(audienceId);
                condition.getAudiences().add(audience);
            }
        }
    }

    /**
     * Gets the {@link AudienceRestriction} to which audiences will be added.
     * 
     * @param conditions existing set of conditions
     * 
     * @return the condition to which audiences will be added
     */
    private AudienceRestriction getAudienceRestrictionCondition(Conditions conditions) {
        final AudienceRestriction condition;

        if (!addingAudiencesToExistingRestriction || conditions.getAudienceRestrictions().isEmpty()) {
            final SAMLObjectBuilder<AudienceRestriction> conditionBuilder =
                    (SAMLObjectBuilder<AudienceRestriction>) XMLObjectProviderRegistrySupport.getBuilderFactory()
                            .getBuilder(AudienceRestriction.TYPE_NAME);
            log.debug("Action {}: Conditions did not contain an AudienceRestrictionCondition, adding one", getId());
            condition = conditionBuilder.buildObject();
            conditions.getAudienceRestrictions().add(condition);
        } else {
            log.debug("Action {}: Conditions already contained an AudienceRestrictionCondition, using it", getId());
            condition = conditions.getAudienceRestrictions().get(0);
        }

        return condition;
    }
}
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
import net.shibboleth.idp.profile.InvalidOutboundMessageException;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartySubcontext;
import net.shibboleth.idp.saml.profile.config.AbstractSamlProfileConfiguration;
import net.shibboleth.idp.saml.profile.saml1.Saml1ActionSupport;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Audience;
import org.opensaml.saml.saml1.core.AudienceRestrictionCondition;
import org.opensaml.saml.saml1.core.Conditions;
import org.opensaml.saml.saml1.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

//TODO have an option that controls, if a restriction condition already exists, if a new one is added or if the audiences are just added the existing condition

/** Adds an {@link AudienceRestrictionCondition} to every {@link Assertion} contained on the {@link Response}. */
public class AddAudienceRestrictionToAssertions extends AbstractIdentityProviderAction<Object, Response> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddAudienceRestrictionToAssertions.class);

    /** {@inheritDoc} */
    protected Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext,
            final ProfileRequestContext<Object, Response> profileRequestContext) throws ProfileException {
        log.debug("Action {}: Attempting to add an AudienceRestrictionCondition to outgoing assertions", getId());

        final RelyingPartySubcontext relyingPartyCtx =
                ActionSupport.getRequiredRelyingPartyContext(this, profileRequestContext);

        final Response response = ActionSupport.getRequiredOutboundMessage(this, profileRequestContext);

        final List<Assertion> assertions = response.getAssertions();
        if (assertions.isEmpty()) {
            log.error("Action {}: Unable to add AudienceRestrictionCondition, Response does not contain an Asertion",
                    getId());
            throw new InvalidOutboundMessageException("No Assertion available within the Response");
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
     * Adds the {@link RelyingPartySubcontext#getRelyingPartyId()} and any additional audiences configured in the active
     * {@link AbstractSamlProfileConfiguration} as {@link Audience} to the {@link AudienceRestrictionCondition}. If no
     * {@link AudienceRestrictionCondition} exists on the given {@link Conditions} one is created and added.
     * 
     * @param conditions condition that has, or will received the created, {@link AudienceRestrictionCondition}
     * @param relyingPartyCtx information about the current relying party
     */
    private void addAudienceRestriction(final Conditions conditions, final RelyingPartySubcontext relyingPartyCtx) {
        final AudienceRestrictionCondition condition;
        if (conditions.getAudienceRestrictionConditions().isEmpty()) {
            final SAMLObjectBuilder<AudienceRestrictionCondition> conditionBuilder =
                    (SAMLObjectBuilder<AudienceRestrictionCondition>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                            AudienceRestrictionCondition.TYPE_NAME);
            log.debug("Action {}: Conditions did not contain an AudienceRestrictionCondition, adding one", getId());
            condition = conditionBuilder.buildObject();
            conditions.getAudienceRestrictionConditions().add(condition);
        } else {
            log.debug("Action {}: Conditions already contained an AudienceRestrictionCondition, using it", getId());
            condition = conditions.getAudienceRestrictionConditions().get(0);
        }

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
}
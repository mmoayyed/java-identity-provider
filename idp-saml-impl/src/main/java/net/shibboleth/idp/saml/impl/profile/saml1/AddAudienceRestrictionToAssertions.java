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
import net.shibboleth.idp.saml.profile.saml1.Saml1ActionSupport;
import net.shibboleth.idp.saml.relyingparty.AbstractSAMLProfileConfiguration;
import net.shibboleth.idp.saml.relyingparty.saml1.AbstractSAML1ProfileConfiguration;

import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml1.core.Assertion;
import org.opensaml.saml1.core.Audience;
import org.opensaml.saml1.core.AudienceRestrictionCondition;
import org.opensaml.saml1.core.Conditions;
import org.opensaml.saml1.core.Response;
import org.opensaml.xml.Configuration;
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
    protected Class<RelyingPartySubcontext> getSubcontextType() {
        return RelyingPartySubcontext.class;
    }

    /** {@inheritDoc} */
    protected Event doExecute(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            RequestContext springRequestContext, ProfileRequestContext<Object, Response> profileRequestContext)
            throws ProfileException {
        log.debug("Action {}: Attempting to add an AudienceRestrictionCondition to outgoing assertions", getId());

        final RelyingPartySubcontext relyingPartyCtx =
                ActionSupport.getRequiredRelyingPartyContext(this, profileRequestContext);
        
        final Response response = ActionSupport.getRequiredOutboundMessage(this, profileRequestContext);

        final List<Assertion> assertions = response.getAssertions();
        if (assertions.isEmpty()) {
            log.error("Action {}: Unable to add DoNotCacheCondition, outbound Response does not contain any Asertions");
            throw new InvalidOutboundMessageException("No Assertion available within the Response");
        }

        Conditions conditions;
        for (Assertion assertion : assertions) {
            conditions = Saml1ActionSupport.addConditionsToAssertion(this, assertion);
            conditions.getAudienceRestrictionConditions().add(buildAudienceRestriction(relyingPartyCtx));
            log.debug("Action {}: Added AudienceRestrictionCondition to Assertion {}", getId(), assertion.getID());
        }

        return ActionSupport.buildProceedEvent(this);
    }

    /**
     * Creates an {@link AudienceRestrictionCondition}.
     * 
     * @param relyingPartyCtx current relying party configuration
     * 
     * @return the constructed {@link AudienceRestrictionCondition}
     */
    private AudienceRestrictionCondition buildAudienceRestriction(RelyingPartySubcontext relyingPartyCtx) {
        final SAMLObjectBuilder<AudienceRestrictionCondition> conditionBuilder =
                (SAMLObjectBuilder<AudienceRestrictionCondition>) Configuration.getBuilderFactory().getBuilder(
                        AudienceRestrictionCondition.TYPE_NAME);
        final AudienceRestrictionCondition condition = conditionBuilder.buildObject();

        final SAMLObjectBuilder<Audience> audienceBuilder =
                (SAMLObjectBuilder<Audience>) Configuration.getBuilderFactory().getBuilder(
                        Audience.DEFAULT_ELEMENT_NAME);

        Audience audience = audienceBuilder.buildObject();
        audience.setUri(relyingPartyCtx.getRelyingPartyId());
        condition.getAudiences().add(audience);

        if (relyingPartyCtx.getProfileConfig() instanceof AbstractSAML1ProfileConfiguration) {
            AbstractSAMLProfileConfiguration profileConfig =
                    (AbstractSAMLProfileConfiguration) relyingPartyCtx.getProfileConfig();
            for (String audienceId : profileConfig.getAdditionalAudiencesForAssertion()) {
                audience = audienceBuilder.buildObject();
                audience.setUri(audienceId);
                condition.getAudiences().add(audience);
            }
        }

        return condition;
    }
}
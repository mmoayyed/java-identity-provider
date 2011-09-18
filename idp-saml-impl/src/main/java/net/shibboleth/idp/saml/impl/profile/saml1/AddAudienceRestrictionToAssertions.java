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

//TODO need access to the profile configuration

/** Adds an {@link AudienceRestrictionCondition} to every {@link Assertion} contained on the {@link Response}. */
public class AddAudienceRestrictionToAssertions extends
        AbstractProfileRequestSubcontextAction<Object, Response, RelyingPartySubcontext> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddAudienceRestrictionToAssertions.class);

    /** {@inheritDoc} */
    protected Class<RelyingPartySubcontext> getSubcontextType() {
        return RelyingPartySubcontext.class;
    }

    /** {@inheritDoc} */
    protected Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext,
            final ProfileRequestContext<Object, Response> profileRequestContext,
            final RelyingPartySubcontext relyingPartyContext) throws ProfileException {

        log.debug("Action {}: Attempting to add an AudienceRestrictionCondition to outgoing assertions", getId());

        final List<Assertion> assertions =
                Saml1Support.getAssertionsFromResponse(this, profileRequestContext, relyingPartyContext);

        Conditions conditions;
        for (Assertion assertion : assertions) {
            conditions = Saml1Support.getConditionsFromAssertion(this, assertion);
            conditions.getAudienceRestrictionConditions().add(buildAudienceRestriction(relyingPartyContext));
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

        // TODO add additional audiences if AbstractSamlProfileConfiguration carries them

        return condition;
    }
}
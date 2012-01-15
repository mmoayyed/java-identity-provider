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

package net.shibboleth.idp.saml.profile.saml2;

import javax.annotation.Nonnull;

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.config.SecurityConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartySubcontext;

import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper methods for SAML 2 IdP actions. */
public final class Saml2ActionSupport {

    /** Constructor. */
    private Saml2ActionSupport() {

    }

    /**
     * Constructs and adds a {@link Assertion} to the given {@link Response}. The {@link Assertion} is constructed as
     * follows:
     * <ul>
     * <li>its ID is generated via the {@link org.opensaml.util.IdentifierGenerator} located on the
     * {@link SecurityConfiguration} located on the {@link net.shibboleth.idp.profile.config.ProfileConfiguration} of
     * the {@link RelyingPartySubcontext}</li>
     * <li>its issue instant is set to the issue instant of the given {@link Response}</li>
     * <li>its issuer is set to the responder entity ID given by the
     * {@link net.shibboleth.idp.relyingparty.RelyingPartyConfiguration} of the {@link RelyingPartySubcontext}</li>
     * <li>its version is set to {@link SAMLVersion#VERSION_11}</li>
     * </ul>
     * 
     * @param action the current action
     * @param relyingPartyContext the current relying party configuration subcontext
     * @param response the response to which the assertion will be added
     * 
     * @return the assertion that was added to the response
     */
    @Nonnull public static Assertion addAssertionToResponse(@Nonnull final AbstractIdentityProviderAction action,
            @Nonnull final RelyingPartySubcontext relyingPartyContext, @Nonnull final Response response) {

        final SAMLObjectBuilder<Assertion> assertionBuilder =
                (SAMLObjectBuilder<Assertion>) Configuration.getBuilderFactory().getBuilder(Assertion.TYPE_NAME);

        final SAMLObjectBuilder<Issuer> issuerBuilder =
                (SAMLObjectBuilder<Issuer>) Configuration.getBuilderFactory().getBuilder(Issuer.DEFAULT_ELEMENT_NAME);

        final SecurityConfiguration securityConfig = relyingPartyContext.getProfileConfig().getSecurityConfiguration();

        final Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(relyingPartyContext.getConfiguration().getResponderEntityId());

        final Assertion assertion = assertionBuilder.buildObject();
        assertion.setID(securityConfig.getIdGenerator().generateIdentifier());
        assertion.setIssueInstant(response.getIssueInstant());
        assertion.setIssuer(issuer);
        assertion.setVersion(SAMLVersion.VERSION_11);

        getLogger().debug("Action {}: Added Assertion {} to Response {}",
                new Object[] {action, assertion.getID(), response.getID(),});
        response.getAssertions().add(assertion);

        return assertion;
    }

    /**
     * Creates and adds a {@link Conditions} to a given {@link Assertion}. If the {@link Assertion} already contains an
     * {@link Conditions} this method just returns.
     * 
     * @param action current action
     * @param assertion assertion to which the condition will be added
     * 
     * @return the {@link Conditions} that already existed on, or the one that was added to, the {@link Assertion}
     */
    @Nonnull public static Conditions addConditionsToAssertion(@Nonnull final AbstractIdentityProviderAction action,
            @Nonnull final Assertion assertion) {
        Conditions conditions = assertion.getConditions();
        if (conditions == null) {
            final SAMLObjectBuilder<Conditions> conditionsBuilder =
                    (SAMLObjectBuilder<Conditions>) Configuration.getBuilderFactory().getBuilder(Conditions.TYPE_NAME);
            conditions = conditionsBuilder.buildObject();
            assertion.setConditions(conditions);
            getLogger().debug("Action {}: Assertion {} did not already contain a Conditions, one was added",
                    action.getId(), assertion.getID());
        } else {
            getLogger().debug("Action {}: Assertion {} already contain a Conditions, nothing was done", action.getId(),
                    assertion.getID());
        }

        return conditions;
    }

    /**
     * Gets the logger for this class.
     * 
     * @return logger for this class, never null
     */
    private static Logger getLogger() {
        return LoggerFactory.getLogger(Saml2ActionSupport.class);
    }
}
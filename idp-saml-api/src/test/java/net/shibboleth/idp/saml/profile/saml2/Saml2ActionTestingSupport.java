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

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.config.SecurityConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartySubcontext;
import net.shibboleth.idp.saml.profile.config.saml1.ArtifactResolutionProfileConfiguration;
import net.shibboleth.idp.saml.profile.config.saml1.AttributeQueryProfileConfiguration;
import net.shibboleth.idp.saml.profile.config.saml1.SsoProfileConfiguration;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.joda.time.DateTime;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.SAMLVersion;
import org.opensaml.messaging.context.BaseContext;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AttributeQuery;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Subject;
import org.opensaml.util.criteria.StaticResponseEvaluableCriterion;
import org.opensaml.xml.XMLObjectProviderRegistrySupport;

/**
 * Helper methods for creating/testing SAML 1 objects within profile action tests. When methods herein refer to mock
 * objects they are always objects that have been created via Mockito unless otherwise noted.
 */
public final class Saml2ActionTestingSupport {

    /** ID used for all generated {@link Response} objects. */
    public final static String REQUEST_ID = "request";

    /** ID used for all generated {@link Response} objects. */
    public final static String RESPONSE_ID = "response";

    /** ID used for all generated {@link Assertion} objects. */
    public final static String ASSERTION_ID = "assertion";

    /**
     * Builds a {@link RelyingPartySubcontext} that is a child of the given parent context. The build subcontext
     * contains:
     * <ul>
     * <li>a {@link RelyingPartyConfiguration} whose ID is the given relying party ID or
     * {@link SamlActionTestingSupport#INBOUND_MSG_ISSUER} if none is given</li>
     * <li>the set of {@link ProfileConfiguration} created by {@link #buildProfileConfigurations()}</li>
     * <li>the {@link SsoProfileConfiguration} set as the active profile configuration</li>
     * </ul>
     * 
     * @param parent the parent of the created subcontext
     * @param relyingPartyId the ID of the relying party
     * @param activeProfileConfig the active profile configuration
     * 
     * @return the constructed subcontext
     */
    public static RelyingPartySubcontext buildRelyingPartySubcontext(@Nonnull final BaseContext parent,
            @Nullable final String relyingPartyId) {

        String id = StringSupport.trimOrNull(relyingPartyId);
        if (id == null) {
            id = ActionTestingSupport.INBOUND_MSG_ISSUER;
        }

        final RelyingPartyConfiguration rpConfig =
                new RelyingPartyConfiguration(id, ActionTestingSupport.OUTBOUND_MSG_ISSUER,
                        StaticResponseEvaluableCriterion.TRUE_RESPONSE, buildProfileConfigurations());

        RelyingPartySubcontext subcontext = new RelyingPartySubcontext(id);
        subcontext.setProfileConfiguration(rpConfig.getProfileConfiguration(SsoProfileConfiguration.PROFILE_ID));
        subcontext.setRelyingPartyConfiguration(rpConfig);

        parent.addSubcontext(subcontext);

        return subcontext;
    }

    /**
     * Builds a {@link ProfileConfiguration} collection containing a {@link ArtifactResolutionProfileConfiguration},
     * {@link AttributeQueryProfileConfiguration}, and {@link ArtifactResolutionProfileConfiguration}.
     * 
     * @return the constructed {@link ProfileConfiguration}
     */
    public static Collection<ProfileConfiguration> buildProfileConfigurations() {
        ArrayList<ProfileConfiguration> profileConfigs = new ArrayList<ProfileConfiguration>();

        SecurityConfiguration securityConfig = new SecurityConfiguration();

        ArtifactResolutionProfileConfiguration artifactConfig = new ArtifactResolutionProfileConfiguration();
        artifactConfig.setSecurityConfiguration(securityConfig);
        profileConfigs.add(artifactConfig);

        AttributeQueryProfileConfiguration attributeConfig = new AttributeQueryProfileConfiguration();
        attributeConfig.setSecurityConfiguration(securityConfig);
        profileConfigs.add(attributeConfig);

        SsoProfileConfiguration ssoConfig = new SsoProfileConfiguration();
        ssoConfig.setSecurityConfiguration(securityConfig);
        profileConfigs.add(ssoConfig);

        return profileConfigs;
    }

    /**
     * Builds an empty response. The ID of the message is {@link SamlActionTestingSupport#OUTBOUND_MSG_ID}, the issues
     * instant is 1970-01-01T00:00:00Z and the SAML version is {@link SAMLVersion#VERSION_11}.
     * 
     * @return the constructed response
     */
    public static Response buildResponse() {
        final SAMLObjectBuilder<Response> responseBuilder =
                (SAMLObjectBuilder<Response>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        Response.DEFAULT_ELEMENT_NAME);

        final Response response = responseBuilder.buildObject();
        response.setID(ActionTestingSupport.OUTBOUND_MSG_ID);
        response.setIssueInstant(new DateTime(0));
        response.setVersion(SAMLVersion.VERSION_20);

        return response;
    }

    /**
     * Builds an empty assertion. The ID of the message is {@link #ASSERTION_ID}, the issues instant is
     * 1970-01-01T00:00:00Z and the SAML version is {@link SAMLVersion#VERSION_11}.
     * 
     * @return the constructed assertion
     */
    public static Assertion buildAssertion() {
        final SAMLObjectBuilder<Assertion> assertionBuilder =
                (SAMLObjectBuilder<Assertion>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(Assertion.TYPE_NAME);

        final Assertion assertion = assertionBuilder.buildObject();
        assertion.setID(ASSERTION_ID);
        assertion.setIssueInstant(new DateTime(0));
        assertion.setVersion(SAMLVersion.VERSION_20);

        return assertion;
    }

    /**
     * Builds a {@link Subject}. If a principal name is given a {@link NameID}, whose value is the given principal name,
     * will be created and added to the {@link Subject}.
     * 
     * @param principalName the principal name to add to the subject
     * 
     * @return the built subject
     */
    public static Subject buildSubject(final @Nullable String principalName) {
        final SAMLObjectBuilder<Subject> subjectBuilder =
                (SAMLObjectBuilder<Subject>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(Subject.TYPE_NAME);
        final Subject subject = subjectBuilder.buildObject();

        if (principalName != null) {
            final SAMLObjectBuilder<NameID> nameIdBuilder =
                    (SAMLObjectBuilder<NameID>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(NameID.TYPE_NAME);
            final NameID nameId = nameIdBuilder.buildObject();
            nameId.setValue(principalName);
            subject.setNameID(nameId);
        }

        return subject;
    }

    /**
     * Builds an {@link AttributeQuery}. If a {@link Subject} is given, it will be added to the constructed
     * {@link AttributeQuery}.
     * 
     * @param subject the subject to add to the query
     * 
     * @return the built query
     */
    public static AttributeQuery buildAttributeQueryRequest(final @Nullable Subject subject) {
        final SAMLObjectBuilder<Issuer> issuerBuilder =
                (SAMLObjectBuilder<Issuer>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(Issuer.DEFAULT_ELEMENT_NAME);

        final SAMLObjectBuilder<AttributeQuery> queryBuilder =
                (SAMLObjectBuilder<AttributeQuery>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        AttributeQuery.TYPE_NAME);

        final Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(ActionTestingSupport.INBOUND_MSG_ISSUER);

        final AttributeQuery query = queryBuilder.buildObject();
        query.setID(REQUEST_ID);
        query.setIssueInstant(new DateTime(0));
        query.setIssuer(issuer);
        query.setVersion(SAMLVersion.VERSION_20);

        if (subject != null) {
            query.setSubject(subject);
        }

        return query;
    }
}
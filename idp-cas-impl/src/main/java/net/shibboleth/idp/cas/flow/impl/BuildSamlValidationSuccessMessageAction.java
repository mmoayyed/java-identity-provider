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

package net.shibboleth.idp.cas.flow.impl;

import java.time.Instant;

import javax.annotation.Nonnull;

import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Attribute;
import org.opensaml.saml.saml1.core.AttributeStatement;
import org.opensaml.saml.saml1.core.AttributeValue;
import org.opensaml.saml.saml1.core.Audience;
import org.opensaml.saml.saml1.core.AudienceRestrictionCondition;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml1.core.Conditions;
import org.opensaml.saml.saml1.core.ConfirmationMethod;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.saml.saml1.core.Status;
import org.opensaml.saml.saml1.core.StatusCode;
import org.opensaml.saml.saml1.core.Subject;
import org.opensaml.saml.saml1.core.SubjectConfirmation;
import org.slf4j.Logger;

import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;
/**
 * Creates the SAML response message for successful ticket validation at the <code>/samlValidate</code> URI.
 *
 * @author Marvin S. Addison
 */
public class BuildSamlValidationSuccessMessageAction extends AbstractOutgoingSamlMessageAction {

    /** Attribute namespace. */
    private static final String NAMESPACE = "http://www.ja-sig.org/products/cas/";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BuildSamlValidationSuccessMessageAction.class);

    /** Attribute value node builder. */
    private final XMLObjectBuilder<XSString> attrValueBuilder;

    /** SAML identifier generation strategy. */
    @Nonnull
    private final IdentifierGenerationStrategy identifierGenerationStrategy;

    /** IdP entity ID used to set issuer field of generated assertions. */
    @Nonnull
    private final String entityID;


    /**
     * Constructor.
     *
     * @param strategy SAML identifier generation strategy.
     * @param id IdP entity ID.
     */
    public BuildSamlValidationSuccessMessageAction(@Nonnull final IdentifierGenerationStrategy strategy,
            @Nonnull @NotEmpty final String id) {
        Constraint.isNotNull(strategy, "IdentifierGenerationStrategy cannot be null");
        identifierGenerationStrategy = strategy;
        entityID = Constraint.isNotNull(StringSupport.trimOrNull(id), "EntityID cannot be null");
        
        attrValueBuilder = XMLObjectProviderRegistrySupport.getBuilderFactory().<XSString>getBuilderOrThrow(
                XSString.TYPE_NAME);
    }

    @Override
    @Nonnull protected Response buildSamlResponse(@Nonnull final ProfileRequestContext profileRequestContext)
            throws EventException {

        final Instant now = Instant.now();

        final TicketValidationRequest request = getCASRequest(profileRequestContext);
        final TicketValidationResponse ticketResponse = getCASResponse(profileRequestContext);
        final Ticket ticket = getCASTicket(profileRequestContext);
        final TicketState state = ticket.getTicketState();
        if (state == null) {
            throw new EventException(ProtocolError.IllegalState.name());
        }
        log.debug("Building SAML response for {} in IdP session {}", request.getService(), state.getSessionId());

        final Response response = newSAMLObject(Response.class, Response.DEFAULT_ELEMENT_NAME);
        response.setID(request.getTicket());
        response.setIssueInstant(now);
        final Status status = newSAMLObject(Status.class, Status.DEFAULT_ELEMENT_NAME);
        final StatusCode code = newSAMLObject(StatusCode.class, StatusCode.DEFAULT_ELEMENT_NAME);
        code.setValue(StatusCode.SUCCESS);
        status.setStatusCode(code);
        response.setStatus(status);

        final Assertion assertion = newSAMLObject(Assertion.class, Assertion.DEFAULT_ELEMENT_NAME);
        assertion.setID(identifierGenerationStrategy.generateIdentifier());
        assertion.setIssueInstant(now);
        assertion.setVersion(SAMLVersion.VERSION_11);
        assertion.setIssuer(entityID);

        final Conditions conditions = newSAMLObject(Conditions.class, Conditions.DEFAULT_ELEMENT_NAME);
        conditions.setNotBefore(now);
        conditions.setNotOnOrAfter(now.plusSeconds(60));
        final AudienceRestrictionCondition audienceRestriction = newSAMLObject(
                AudienceRestrictionCondition.class, AudienceRestrictionCondition.DEFAULT_ELEMENT_NAME);
        final Audience audience = newSAMLObject(Audience.class, Audience.DEFAULT_ELEMENT_NAME);
        audience.setURI(request.getService());
        audienceRestriction.getAudiences().add(audience);
        conditions.getAudienceRestrictionConditions().add(audienceRestriction);
        assertion.setConditions(conditions);
        assertion.getAuthenticationStatements().add(
                newAuthenticationStatement(now, state.getAuthenticationMethod(), state.getPrincipalName()));

        final AttributeStatement attrStatement = newSAMLObject(
                AttributeStatement.class, AttributeStatement.DEFAULT_ELEMENT_NAME);
        attrStatement.setSubject(newSubject(state.getPrincipalName()));
        for (final net.shibboleth.idp.cas.attribute.Attribute casAttr : ticketResponse.getAttributes()) {
            final Attribute attribute = newSAMLObject(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
            attribute.setAttributeName(casAttr.getName());
            attribute.setAttributeNamespace(NAMESPACE);
            for (final String value : casAttr.getValues()) {
                attribute.getAttributeValues().add(newAttributeValue(value));
            }
            attrStatement.getAttributes().add(attribute);
        }
        assertion.getAttributeStatements().add(attrStatement);

        response.getAssertions().add(assertion);
        return response;
    }

    /**
     * Build a new subject.
     * 
     * @param identifier subject identifier
     * @return new subject
     */
    @Nonnull private Subject newSubject(final String identifier) {
        final SubjectConfirmation confirmation = newSAMLObject(
                SubjectConfirmation.class, SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        final ConfirmationMethod method = newSAMLObject(
                ConfirmationMethod.class, ConfirmationMethod.DEFAULT_ELEMENT_NAME);
        method.setURI(ConfirmationMethod.METHOD_ARTIFACT);
        confirmation.getConfirmationMethods().add(method);
        final NameIdentifier nameIdentifier = newSAMLObject(NameIdentifier.class, NameIdentifier.DEFAULT_ELEMENT_NAME);
        nameIdentifier.setValue(identifier);
        final Subject subject = newSAMLObject(Subject.class, Subject.DEFAULT_ELEMENT_NAME);
        subject.setNameIdentifier(nameIdentifier);
        subject.setSubjectConfirmation(confirmation);
        return subject;
    }

    /**
     * Build new authentication statement.
     * 
     * @param authnInstant authentication instant
     * @param authnMethod authentication method
     * @param principal authenticated principal
     * @return new authentication statement
     */
    private AuthenticationStatement newAuthenticationStatement(
            final Instant authnInstant, final String authnMethod, final String principal) {
        final AuthenticationStatement authnStatement = newSAMLObject(
                AuthenticationStatement.class, AuthenticationStatement.DEFAULT_ELEMENT_NAME);
        authnStatement.setAuthenticationInstant(authnInstant);
        authnStatement.setAuthenticationMethod(authnMethod);
        authnStatement.setSubject(newSubject(principal));
        return authnStatement;
    }

    /**
     * Build new attribute value.
     * 
     * @param value attribute value
     * @return new attribute value
     */
    private XSString newAttributeValue(final String value) {
        final XSString stringValue = attrValueBuilder.buildObject(
                AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        stringValue.setValue(value);
        return stringValue;
    }

}
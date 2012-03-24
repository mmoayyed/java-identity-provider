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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.idp.attribute.AttributeEncoder;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartySubcontext;
import net.shibboleth.idp.saml.attribute.encoding.AbstractSaml1AttributeEncoder;
import net.shibboleth.idp.saml.profile.saml1.Saml1ActionSupport;

import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml1.core.Assertion;
import org.opensaml.saml1.core.AttributeStatement;
import org.opensaml.saml1.core.Response;
import org.opensaml.xml.XMLObjectProviderRegistrySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Builds an {@link AttributeStatement} and adds it to the {@link Response} set as the message of the
 * {@link ProfileRequestContext#getOutboundMessageContext()}. The {@link Attribute} set to be encoded is drawn from the
 * {@link AttributeContext} located on the {@link RelyingPartySubcontext} located on the
 * {@link ProfileRequestContext}.
 */
public class AddAttributeStatementToAssertion extends AbstractIdentityProviderAction<Object, Response> {
// TODO unit tests
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddAttributeStatementToAssertion.class);

    /** {@inheritDoc} */
    protected Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext,
            final ProfileRequestContext<Object, Response> profileRequestContext) throws ProfileException {
        log.debug("Action {}: Attempting to add an AttributeStatement to outgoing Response", getId());

        final RelyingPartySubcontext relyingPartyCtx =
                ActionSupport.getRequiredRelyingPartyContext(this, profileRequestContext);

        final AttributeContext attributeCtx = relyingPartyCtx.getSubcontext(AttributeContext.class, false);
        if (attributeCtx == null) {
            log.debug("Action {}: No AttributeSubcontext available for relying party  {}, nothing left to do", getId(),
                    relyingPartyCtx.getRelyingPartyId());
            return ActionSupport.buildProceedEvent(this);
        }

        final AttributeStatement statement = buildAttributeStatement(attributeCtx.getAttributes().values());
        if (statement == null) {
            log.debug("Action {}: No AttributeStatement was built, nothing left to do", getId());
            return ActionSupport.buildProceedEvent(this);
        }

        final Assertion assertion =
                getStatementAssertion(relyingPartyCtx,
                        ActionSupport.getRequiredOutboundMessage(this, profileRequestContext));
        assertion.getAttributeStatements().add(statement);

        log.debug("Action {}: Adding constructed AttributeStatement to Assertion {} ", getId(), assertion.getID());
        return ActionSupport.buildProceedEvent(this);
    }

    /**
     * Gets the assertion to which the attribute statement will be added.
     * 
     * @param relyingPartyContext current relying party information
     * @param response current response
     * 
     * @return the assertion to which the attribute statement will be added
     */
    private Assertion getStatementAssertion(RelyingPartySubcontext relyingPartyContext, Response response) {
        // TODO allow for a configuration option that forces the statement in to its own assertion

        final Assertion assertion;
        if (response.getAssertions().isEmpty()) {
            assertion = Saml1ActionSupport.addAssertionToResponse(this, relyingPartyContext, response);
        } else {
            assertion = response.getAssertions().get(0);
        }

        return assertion;
    }

    /**
     * Builds an attribute statement from a collection of attributes.
     * 
     * @param attributes the collection of attributes, may be null or contain null elements
     * 
     * @return the attribute statement or null if no attributes can be encoded
     * 
     * @throws UnableToEncodeAttributeException thrown if there is a problem encoding an attribute
     */
    private AttributeStatement buildAttributeStatement(Collection<Attribute> attributes)
            throws UnableToEncodeAttributeException {
        if (attributes == null || attributes.isEmpty()) {
            log.debug("Action {}: No attributes available to be encoded, nothing left to do");
            return null;
        }

        ArrayList<org.opensaml.saml1.core.Attribute> encodedAttributes =
                new ArrayList<org.opensaml.saml1.core.Attribute>(attributes.size());
        org.opensaml.saml1.core.Attribute encodedAttribute = null;
        for (Attribute attribute : attributes) {
            encodedAttribute = encodeAttribute(attribute);
            if (encodedAttribute != null) {
                encodedAttributes.add(encodedAttribute);
            }
        }

        if (encodedAttributes.isEmpty()) {
            log.debug("Action {}: No attributes were encoded as SAML 1 Attributes, nothing futher to do");
            return null;
        }

        SAMLObjectBuilder<AttributeStatement> statementBuilder =
                (SAMLObjectBuilder<AttributeStatement>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        AttributeStatement.TYPE_NAME);

        AttributeStatement statement = statementBuilder.buildObject();
        statement.getAttributes().addAll(encodedAttributes);
        return statement;
    }

    /**
     * Encodes a {@link Attribute} into a {@link org.opensaml.saml1.core.Attribute} if a proper encoder is available.
     * 
     * @param attribute the attribute to be encoded, may be null
     * 
     * @return the encoded attribute of null if the attribute could not be encoded
     * 
     * @throws UnableToEncodeAttributeException thrown if there is a problem encoding an attribute
     */
    private org.opensaml.saml1.core.Attribute encodeAttribute(Attribute attribute)
            throws UnableToEncodeAttributeException {
        if (attribute == null) {
            return null;
        }

        log.debug("Action {}: Attempting to encode attribute {} as a SAML 1 Attribute", getId(), attribute.getId());
        final Set<AttributeEncoder<?>> encoders = attribute.getEncoders();

        if (encoders.isEmpty()) {
            log.debug("Action {}: Attribute {} does not have any encoders, nothing to do", getId(), attribute.getId());
            return null;
        }

        for (AttributeEncoder<?> encoder : encoders) {
            if (SAMLConstants.SAML11P_NS.equals(encoder.getProtocol())
                    && encoder instanceof AbstractSaml1AttributeEncoder) {
                log.debug("Action {}: Encoding attribute {} as a SAML 1 Attribute", getId(), attribute.getId());
                try {
                    return (org.opensaml.saml1.core.Attribute) encoder.encode(attribute);
                } catch (AttributeEncodingException e) {
                    throw new UnableToEncodeAttributeException(attribute, e);
                }
            }
        }

        log.debug("Action {}: Attribute {} did not have a SAML 1 Attribute encoder associated with it, nothing to do",
                getId(), attribute.getId());
        return null;
    }

    /** Exception thrown if there is a problem encoding an attribute. */
    public class UnableToEncodeAttributeException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = 3915220535913429394L;

        /**
         * Constructor.
         * 
         * @param attribute the attribute that could not be encoded
         * @param e the exception that occurred when attempting to encode the attribute
         */
        public UnableToEncodeAttributeException(Attribute attribute, AttributeEncodingException e) {
            super("Action " + getId() + ": Unable to encode attribute " + attribute.getId(), e);
        }
    }
}
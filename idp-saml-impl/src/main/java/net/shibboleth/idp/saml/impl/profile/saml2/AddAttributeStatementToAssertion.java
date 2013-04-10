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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.annotation.Nonnull;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.idp.attribute.AttributeEncoder;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.EventIds;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.idp.saml.attribute.encoding.AbstractSaml2AttributeEncoder;
import net.shibboleth.idp.saml.profile.SamlEventIds;
import net.shibboleth.idp.saml.profile.saml2.Saml2ActionSupport;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/**
 * Builds an {@link AttributeStatement} and adds it to the {@link Response} set as the message of the
 * {@link ProfileRequestContext#getOutboundMessageContext()}. The {@link Attribute} set to be encoded is drawn from the
 * {@link AttributeContext} located on the {@link RelyingPartyContext} looked up via the
 * {@link #relyingPartyContextLookupStrategy}.
 */
@Events({
        @Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = EventIds.INVALID_RELYING_PARTY_CTX,
                description = "Returned if no relying party information is associated with the current request"),
        @Event(id = EventIds.INVALID_ATTRIBUTE_CTX,
                description = "Returned if no attribute context is associated with the relying party context"),
        @Event(id = SamlEventIds.UNABLE_ENCODE_ATTRIBUTE,
                description = "Returned if there was a problem encoding an attribute")})
public class AddAttributeStatementToAssertion extends AbstractProfileAction<Object, Response> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddAttributeStatementToAssertion.class);

    /** Whether the generated attribute statement should be placed in its own assertion or added to one if it exists. */
    private boolean statementInOwnAssertion;

    /**
     * Whether attributes that result in an {@link AttributeEncodingException} when being encoded should be ignored or
     * result in an {@link #UNABLE_ENCODE_ATTRIBUTE} transition.
     */
    private boolean ignoringUnencodableAttributes;

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Constructor. */
    public AddAttributeStatementToAssertion() {
        super();

        statementInOwnAssertion = false;

        relyingPartyContextLookupStrategy =
                new ChildContextLookup<ProfileRequestContext, RelyingPartyContext>(RelyingPartyContext.class, false);
    }

    /**
     * Gets whether the generated attribute statement should be placed in its own assertion or added to one if it
     * exists.
     * 
     * @return whether the generated attribute statement should be placed in its own assertion or added to one if it
     *         exists
     */
    public boolean isStatementInOwnAssertion() {
        return statementInOwnAssertion;
    }

    /**
     * Sets whether the generated attribute statement should be placed in its own assertion or added to one if it
     * exists.
     * 
     * @param inOwnAssertion whether the generated attribute statement should be placed in its own assertion or added to
     *            one if it exists
     */
    public synchronized void setStatementInOwnAssertion(boolean inOwnAssertion) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        statementInOwnAssertion = inOwnAssertion;
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
        log.debug("Action {}: Attempting to add an AttributeStatement to outgoing Response", getId());

        final RelyingPartyContext relyingPartyCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (relyingPartyCtx == null) {
            log.error("Action {}: No relying party context located in current profile request context", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_RELYING_PARTY_CTX);
        }

        final AttributeContext attributeCtx = relyingPartyCtx.getSubcontext(AttributeContext.class, false);
        if (attributeCtx == null) {
            log.debug("Action {}: No AttributeSubcontext available for relying party  {}, nothing left to do", getId(),
                    relyingPartyCtx.getRelyingPartyId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_ATTRIBUTE_CTX);
        }

        try {
            final AttributeStatement statement = buildAttributeStatement(attributeCtx.getAttributes().values());
            if (statement == null) {
                log.debug("Action {}: No AttributeStatement was built, nothing left to do", getId());
                return ActionSupport.buildProceedEvent(this);
            }

            final Assertion assertion =
                    getStatementAssertion(relyingPartyCtx, profileRequestContext.getOutboundMessageContext()
                            .getMessage());
            assertion.getAttributeStatements().add(statement);

            log.debug("Action {}: Adding constructed AttributeStatement to Assertion {} ", getId(), assertion.getID());
            return ActionSupport.buildProceedEvent(this);
        } catch (AttributeEncodingException e) {
            return ActionSupport.buildEvent(this, SamlEventIds.UNABLE_ENCODE_ATTRIBUTE);
        }
    }

    /**
     * Gets the assertion to which the attribute statement will be added.
     * 
     * @param relyingPartyContext current relying party information
     * @param response current response
     * 
     * @return the assertion to which the attribute statement will be added
     */
    private Assertion getStatementAssertion(RelyingPartyContext relyingPartyContext, Response response) {
        final Assertion assertion;
        if (statementInOwnAssertion || response.getAssertions().isEmpty()) {
            assertion = Saml2ActionSupport.addAssertionToResponse(this, relyingPartyContext, response);
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
     * @throws AttributeEncodingException thrown if there is a problem encoding an attribute
     */
    private AttributeStatement buildAttributeStatement(Collection<Attribute> attributes)
            throws AttributeEncodingException {
        if (attributes == null || attributes.isEmpty()) {
            log.debug("Action {}: No attributes available to be encoded, nothing left to do", getId());
            return null;
        }

        ArrayList<org.opensaml.saml.saml2.core.Attribute> encodedAttributes =
                new ArrayList<org.opensaml.saml.saml2.core.Attribute>(attributes.size());
        org.opensaml.saml.saml2.core.Attribute encodedAttribute = null;
        for (Attribute attribute : attributes) {
            encodedAttribute = encodeAttribute(attribute);
            if (encodedAttribute != null) {
                encodedAttributes.add(encodedAttribute);
            }
        }

        if (encodedAttributes.isEmpty()) {
            log.debug("Action {}: No attributes were encoded as SAML 2 Attributes, nothing futher to do");
            return null;
        }

        SAMLObjectBuilder<AttributeStatement> statementBuilder =
                (SAMLObjectBuilder<AttributeStatement>) XMLObjectProviderRegistrySupport.getBuilderFactory()
                        .getBuilder(AttributeStatement.TYPE_NAME);

        AttributeStatement statement = statementBuilder.buildObject();
        statement.getAttributes().addAll(encodedAttributes);
        return statement;
    }

    /**
     * Encodes a {@link Attribute} into a {@link org.opensaml.saml.saml2.core.Attribute} if a proper encoder is
     * available.
     * 
     * @param attribute the attribute to be encoded, may be null
     * 
     * @return the encoded attribute of null if the attribute could not be encoded
     * 
     * @throws AttributeEncodingException thrown if there is a problem encoding an attribute
     */
    private org.opensaml.saml.saml2.core.Attribute encodeAttribute(Attribute attribute)
            throws AttributeEncodingException {
        if (attribute == null) {
            return null;
        }

        log.debug("Action {}: Attempting to encode attribute {} as a SAML 2 Attribute", getId(), attribute.getId());
        final Set<AttributeEncoder<?>> encoders = attribute.getEncoders();

        if (encoders.isEmpty()) {
            log.debug("Action {}: Attribute {} does not have any encoders, nothing to do", getId(), attribute.getId());
            return null;
        }

        for (AttributeEncoder<?> encoder : encoders) {
            if (SAMLConstants.SAML20P_NS.equals(encoder.getProtocol())
                    && encoder instanceof AbstractSaml2AttributeEncoder) {
                log.debug("Action {}: Encoding attribute {} as a SAML 2 Attribute", getId(), attribute.getId());
                try {
                    return (org.opensaml.saml.saml2.core.Attribute) encoder.encode(attribute);
                } catch (AttributeEncodingException e) {
                    if (ignoringUnencodableAttributes) {
                        log.debug("Action {}: Unable to encode attribute '{}' as SAML 2 attribute because: {}",
                                new Object[] {getId(), attribute.getId(), e});
                    } else {
                        throw e;
                    }
                }
            }
        }

        log.debug("Action {}: Attribute {} did not have a SAML 2 Attribute encoder associated with it, nothing to do",
                getId(), attribute.getId());
        return null;
    }
}
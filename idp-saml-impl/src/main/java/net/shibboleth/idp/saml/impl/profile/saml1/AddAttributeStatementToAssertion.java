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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeEncoder;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;

import net.shibboleth.idp.saml.attribute.encoding.AbstractSAML1AttributeEncoder;
import net.shibboleth.idp.saml.impl.profile.config.navigate.IdentifierGenerationStrategyLookupFunction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Attribute;
import org.opensaml.saml.saml1.core.AttributeStatement;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.saml.saml1.profile.SAML1ActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * Action that builds an {@link AttributeStatement} and adds it to the {@link Response} returned by a lookup
 * strategy, by default the message returned by {@link ProfileRequestContext#getOutboundMessageContext()}.

 * <p>The {@link IdPAttribute} set to be encoded is drawn from an {@link AttributeContext} returned from a
 * lookup strategy, by default located on the {@link RelyingPartyContext} beneath the profile request context.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link IdPEventIds#UNABLE_ENCODE_ATTRIBUTE}
 */
public class AddAttributeStatementToAssertion extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AddAttributeStatementToAssertion.class);

    /** Whether the generated attribute statement should be placed in its own assertion or added to one if it exists. */
    private boolean statementInOwnAssertion;

    /**
     * Whether attributes that result in an {@link AttributeEncodingException} when being encoded should be ignored or
     * result in an {@link IdPEventIds#UNABLE_ENCODE_ATTRIBUTE} transition.
     */
    private boolean ignoringUnencodableAttributes;

    /**
     * Strategy used to locate the {@link AttributeContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext, AttributeContext> attributeContextLookupStrategy;

    /** Strategy used to locate the {@link IdentifierGenerationStrategy} to use. */
    @NonnullAfterInit private Function<ProfileRequestContext,IdentifierGenerationStrategy> idGeneratorLookupStrategy;

    /** Strategy used to obtain the assertion issuer value. */
    @Nullable private Function<ProfileRequestContext,String> issuerLookupStrategy;
    
    /** Strategy used to locate the {@link Response} to operate on. */
    @Nonnull private Function<ProfileRequestContext, Response> responseLookupStrategy;

    /** AttributeContext to use. */
    @Nullable private AttributeContext attributeCtx;

    /** The generator to use. */
    @Nullable private IdentifierGenerationStrategy idGenerator;

    /** EntityID to populate as assertion issuer. */
    @Nullable private String issuerId;
    
    /** Response to modify. */
    @Nullable private Response response;
    
    /** Constructor. */
    public AddAttributeStatementToAssertion() {
        statementInOwnAssertion = false;
        ignoringUnencodableAttributes = true;

        attributeContextLookupStrategy = Functions.compose(new ChildContextLookup<>(AttributeContext.class),
                new ChildContextLookup<ProfileRequestContext,RelyingPartyContext>(RelyingPartyContext.class));
        responseLookupStrategy =
                Functions.compose(new MessageLookup<>(Response.class), new OutboundMessageContextLookup());
        idGeneratorLookupStrategy = new IdentifierGenerationStrategyLookupFunction();
        issuerLookupStrategy = new ResponderIdLookupFunction();
    }

    /**
     * Set whether the generated attribute statement should be placed in its own assertion or added to one if it
     * exists.
     * 
     * @param flag whether the generated attribute statement should be placed in its own assertion or added to
     *            one if it exists
     */
    public synchronized void setStatementInOwnAssertion(boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        statementInOwnAssertion = flag;
    }

    /**
     * Set whether the attributes that result in an {@link AttributeEncodingException} when being encoded should be
     * ignored or result in an {@link IdPEventIds#UNABLE_ENCODE_ATTRIBUTE} transition.
     * 
     * @param flag flag to set
     */
    public synchronized void setIgnoringUnencodableAttributes(boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        ignoringUnencodableAttributes = flag;
    }

    /**
     * Set the strategy used to locate the {@link AttributeContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link AttributeContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public synchronized void setAttributeContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AttributeContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        attributeContextLookupStrategy =
                Constraint.isNotNull(strategy, "AttributeContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link Response} to operate on.
     * 
     * @param strategy strategy used to locate the {@link Response} to operate on
     */
    public synchronized void setResponseLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, Response> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        responseLookupStrategy = Constraint.isNotNull(strategy, "Response lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link IdentifierGenerationStrategy} to use.
     * 
     * @param strategy lookup strategy
     */
    public synchronized void setIdentifierGeneratorLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, IdentifierGenerationStrategy> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        idGeneratorLookupStrategy =
                Constraint.isNotNull(strategy, "IdentifierGenerationStrategy lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the issuer value to use.
     * 
     * @param strategy lookup strategy
     */
    public synchronized void setIssuerLookupStrategy(@Nonnull final Function<ProfileRequestContext, String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        issuerLookupStrategy = Constraint.isNotNull(strategy, "Issuer lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        log.debug("{} Attempting to add an AttributeStatement to outgoing Response", getLogPrefix());

        idGenerator = idGeneratorLookupStrategy.apply(profileRequestContext);
        if (idGenerator == null) {
            log.debug("{} No identifier generation strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        issuerId = issuerLookupStrategy.apply(profileRequestContext);
        if (issuerId == null) {
            log.debug("{} No assertion issuer value", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        attributeCtx = attributeContextLookupStrategy.apply(profileRequestContext);
        if (attributeCtx == null) {
            log.debug("{} No AttributeSubcontext available, nothing to do", getLogPrefix());
            return false;
        }
        
        response = responseLookupStrategy.apply(profileRequestContext);
        if (response == null) {
            log.debug("{} No SAML response located in current profile request context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }
        
        return super.doPreExecute(profileRequestContext);
    }

    /** {@inheritDoc} */
    @Override
   protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        try {
            final AttributeStatement statement = buildAttributeStatement(attributeCtx.getIdPAttributes().values());
            if (statement == null) {
                log.debug("{} No AttributeStatement was built, nothing to do", getLogPrefix());
                return;
            }

            final Assertion assertion = getStatementAssertion();
            assertion.getAttributeStatements().add(statement);

            log.debug("{} Adding constructed AttributeStatement to Assertion {} ", getLogPrefix(), assertion.getID());
        } catch (final AttributeEncodingException e) {
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_ENCODE_ATTRIBUTE);
        }
    }

    /**
     * Get the assertion to which the attribute statement will be added.
     * 
     * @return the assertion to which the attribute statement will be added
     */
    @Nonnull private Assertion getStatementAssertion() {
        if (statementInOwnAssertion || response.getAssertions().isEmpty()) {
            return SAML1ActionSupport.addAssertionToResponse(this, response, idGenerator, issuerId);
        } else {
            return response.getAssertions().get(0);
        }
    }

    /**
     * Builds an attribute statement from a collection of attributes.
     * 
     * @param attributes the collection of attributes
     * 
     * @return the attribute statement or null if no attributes can be encoded
     * @throws AttributeEncodingException thrown if there is a problem encoding an attribute
     */
    @Nullable private AttributeStatement buildAttributeStatement(
            @Nullable @NullableElements final Collection<IdPAttribute> attributes)
            throws AttributeEncodingException {
        if (attributes == null || attributes.isEmpty()) {
            log.debug("{} No attributes available to be encoded, nothing to do", getLogPrefix());
            return null;
        }

        final ArrayList<Attribute> encodedAttributes = Lists.newArrayListWithExpectedSize(attributes.size());
        for (final IdPAttribute attribute : Collections2.filter(attributes, Predicates.notNull())) {
            final Attribute encodedAttribute = encodeAttribute(attribute);
            if (encodedAttribute != null) {
                encodedAttributes.add(encodedAttribute);
            }
        }

        if (encodedAttributes.isEmpty()) {
            log.debug("{} No attributes were encoded as SAML 1 Attributes, nothing to do", getLogPrefix());
            return null;
        }

        final SAMLObjectBuilder<AttributeStatement> statementBuilder = (SAMLObjectBuilder<AttributeStatement>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<AttributeStatement>getBuilderOrThrow(
                        AttributeStatement.DEFAULT_ELEMENT_NAME);

        final AttributeStatement statement = statementBuilder.buildObject();
        statement.getAttributes().addAll(encodedAttributes);
        return statement;
    }

    /**
     * Encodes a {@link IdPAttribute} into a {@link Attribute} if a proper encoder is available.
     * 
     * @param attribute the attribute to be encoded
     * 
     * @return the encoded attribute, or null if the attribute could not be encoded
     * @throws AttributeEncodingException thrown if there is a problem encoding an attribute
     */
    @Nullable private Attribute encodeAttribute(@Nonnull final IdPAttribute attribute)
            throws AttributeEncodingException {

        log.debug("{} Attempting to encode attribute {} as a SAML 1 Attribute", getLogPrefix(), attribute.getId());
        
        final Set<AttributeEncoder<?>> encoders = attribute.getEncoders();
        if (encoders.isEmpty()) {
            log.debug("{} Attribute {} does not have any encoders, nothing to do", getLogPrefix(), attribute.getId());
            return null;
        }

        for (final AttributeEncoder<?> encoder : encoders) {
            if (SAMLConstants.SAML11P_NS.equals(encoder.getProtocol())
                    && encoder instanceof AbstractSAML1AttributeEncoder) {
                log.debug("{} Encoding attribute {} as a SAML 1 Attribute", getLogPrefix(), attribute.getId());
                try {
                    return (Attribute) encoder.encode(attribute);
                } catch (final AttributeEncodingException e) {
                    if (ignoringUnencodableAttributes) {
                        log.debug(getLogPrefix() + " Unable to encode attribute " + attribute.getId()
                                + "as SAML 1 attribute", e);
                    } else {
                        throw e;
                    }
                }
            }
        }

        log.debug("{} Attribute {} did not have a SAML 1 Attribute encoder associated with it, nothing to do",
                getLogPrefix(), attribute.getId());
        return null;
    }
    
}
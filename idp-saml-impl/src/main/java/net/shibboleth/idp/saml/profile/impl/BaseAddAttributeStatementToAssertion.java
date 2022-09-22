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

package net.shibboleth.idp.saml.profile.impl;

import java.util.Collection;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoder;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscoderSupport;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.config.navigate.IdentifierGenerationStrategyLookupFunction;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Base class for actions that encode an {@link AttributeContext} into a SAML attribute statement.

 * <p>The {@link net.shibboleth.idp.attribute.IdPAttribute} set to be encoded is drawn from
 * an {@link AttributeContext} returned from a
 * lookup strategy, by default located on the {@link RelyingPartyContext} beneath the profile request context.</p>
 * 
 * @param <T> type of objects being encoded
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 */
public abstract class BaseAddAttributeStatementToAssertion<T extends SAMLObject> extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseAddAttributeStatementToAssertion.class);

    /** Whether the generated attribute statement should be placed in its own assertion or added to one if it exists. */
    private boolean statementInOwnAssertion;

    /**
     * Whether attributes that result in an {@link net.shibboleth.idp.attribute.AttributeEncodingException}
     * when being encoded should be ignored or
     * result in an {@link net.shibboleth.idp.profile.IdPEventIds#UNABLE_ENCODE_ATTRIBUTE} transition.
     */
    private boolean ignoringUnencodableAttributes;

    /** Strategy used to locate the {@link IdentifierGenerationStrategy} to use. */
    @NonnullAfterInit private Function<ProfileRequestContext,IdentifierGenerationStrategy> idGeneratorLookupStrategy;

    /** Strategy used to obtain the assertion issuer value. */
    @Nonnull private Function<ProfileRequestContext,String> issuerLookupStrategy;

    /**
     * Strategy used to locate the {@link AttributeContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext,AttributeContext> attributeContextLookupStrategy;

    /** Transcoder registry service object. */
    @NonnullAfterInit private ReloadableService<AttributeTranscoderRegistry> transcoderRegistry;
    
    /** AttributeContext to use. */
    @Nullable private AttributeContext attributeCtx;

    /** The generator to use. */
    @Nullable private IdentifierGenerationStrategy idGenerator;

    /** EntityID to populate as assertion issuer. */
    @Nullable private String issuerId;
    
    /** Constructor. */
    public BaseAddAttributeStatementToAssertion() {
        statementInOwnAssertion = false;
        ignoringUnencodableAttributes = true;

        attributeContextLookupStrategy = new ChildContextLookup<>(AttributeContext.class).compose(
                new ChildContextLookup<>(RelyingPartyContext.class));
        idGeneratorLookupStrategy = new IdentifierGenerationStrategyLookupFunction();
        issuerLookupStrategy = new ResponderIdLookupFunction();
    }
    
    /**
     * Set whether the generated attribute statement should be placed in its own assertion or added to one if it
     * exists.
     * 
     * @return whether the generated attribute statement should be placed in its own assertion or added to
     *            one if it exists
     */
    public boolean isStatementInOwnAssertion() {
        return statementInOwnAssertion;
    }

    /**
     * Set whether the generated attribute statement should be placed in its own assertion or added to one if it
     * exists.
     * 
     * @param flag whether the generated attribute statement should be placed in its own assertion or added to
     *            one if it exists
     */
    public void setStatementInOwnAssertion(final boolean flag) {
        checkSetterPreconditions();
        statementInOwnAssertion = flag;
    }

    /**
     * Get whether the attributes that result in an {@link net.shibboleth.idp.attribute.AttributeEncodingException}
     * when being encoded should be ignored or result in an
     * {@link net.shibboleth.idp.profile.IdPEventIds#UNABLE_ENCODE_ATTRIBUTE} transition.
     * 
     * @return whether the attributes that result in an {@link net.shibboleth.idp.attribute.AttributeEncodingException}
     *  when being encoded should be ignored or result in an
     *  {@link net.shibboleth.idp.profile.IdPEventIds#UNABLE_ENCODE_ATTRIBUTE} transition
     */
    public boolean isIgnoringUnencodableAttributes() {
        return ignoringUnencodableAttributes;
    }

    /**
     * Set whether the attributes that result in an {@link net.shibboleth.idp.attribute.AttributeEncodingException}
     *  when being encoded should be ignored or result in an
     *  {@link net.shibboleth.idp.profile.IdPEventIds#UNABLE_ENCODE_ATTRIBUTE} transition.
     * 
     * @param flag flag to set
     */
    public void setIgnoringUnencodableAttributes(final boolean flag) {
        checkSetterPreconditions();
        ignoringUnencodableAttributes = flag;
    }

    /**
     * Set the strategy used to locate the {@link AttributeContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link AttributeContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public void setAttributeContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AttributeContext> strategy) {
        checkSetterPreconditions();
        attributeContextLookupStrategy =
                Constraint.isNotNull(strategy, "AttributeContext lookup strategy cannot be null");
    }
    
    /**
     * Set the strategy used to locate the {@link IdentifierGenerationStrategy} to use.
     * 
     * @param strategy lookup strategy
     */
    public void setIdentifierGeneratorLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,IdentifierGenerationStrategy> strategy) {
        checkSetterPreconditions();
        idGeneratorLookupStrategy =
                Constraint.isNotNull(strategy, "IdentifierGenerationStrategy lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the issuer value to use.
     * 
     * @param strategy lookup strategy
     */
    public void setIssuerLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        issuerLookupStrategy = Constraint.isNotNull(strategy, "Issuer lookup strategy cannot be null");
    }

    /**
     * Gets the registry of transcoding rules to apply to encode attributes.
     * 
     * @return registry
     */
    @NonnullAfterInit public ReloadableService<AttributeTranscoderRegistry> getTranscoderRegistry() {
        return transcoderRegistry;
    }
    
    /**
     * Sets the registry of transcoding rules to apply to encode attributes.
     * 
     * @param registry registry service interface
     */
    public void setTranscoderRegistry(@Nonnull final ReloadableService<AttributeTranscoderRegistry> registry) {
        checkSetterPreconditions();
        transcoderRegistry = Constraint.isNotNull(registry, "AttributeTranscoderRegistry cannot be null");
    }
    
    /**
     * Get the {@link AttributeContext} to encode.
     * 
     * @return the context to encode
     */
    @Nonnull public AttributeContext getAttributeContext() {
        Constraint.isNotNull(attributeCtx, "AttributeContext has not been initialized yet");
        return attributeCtx;
    }

    /**
     * Get the {@link IdentifierGenerationStrategy} to use if an assertion must be created.
     * 
     * @return the ID generation strategy
     */
    @Nonnull public IdentifierGenerationStrategy getIdGenerator() {
        Constraint.isNotNull(idGenerator, "IdentifierGenerationStrategy has not been initialized yet");
        return idGenerator;
    }

    /**
     * Get the issuer name to use if an assertion must be created.   
     *
     * @return the issuer name
     */
    @Nonnull @NotEmpty public String getIssuerId() {
        Constraint.isNotNull(issuerId, "Issuer name has not been initialized yet");
        return issuerId;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (transcoderRegistry == null) {
            throw new ComponentInitializationException("AttributeTranscoderRegistry cannot be null");
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        log.debug("{} Attempting to add an AttributeStatement to outgoing Assertion", getLogPrefix());

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
        
        return true;
    }


    /**
     * Access the registry of transcoding rules to transform the input attribute into a target type.
     * 
     * @param registry  registry of transcoding rules
     * @param profileRequestContext current profile request context
     * @param attribute input attribute
     * @param to target type
     * @param results collection to add results to
     * 
     * @return number of results added
     * 
     * @throws AttributeEncodingException if a non-ignorable error occurs
     */
    protected int encodeAttribute(@Nonnull final AttributeTranscoderRegistry registry,
            @Nonnull final ProfileRequestContext profileRequestContext, @Nonnull final IdPAttribute attribute,
            @Nonnull final Class<T> to, @Nonnull @NonnullElements @Live final Collection<T> results)
                    throws AttributeEncodingException {
        
        final Collection<TranscodingRule> transcodingRules = registry.getTranscodingRules(attribute, to);
        if (transcodingRules.isEmpty()) {
            log.debug("{} Attribute {} does not have any transcoding rules, nothing to do", getLogPrefix(),
                    attribute.getId());
            return 0;
        }
        
        int count = 0;
        
        for (final TranscodingRule rules : transcodingRules) {
            try {
                final AttributeTranscoder<T> transcoder = TranscoderSupport.<T>getTranscoder(rules);
                final T encodedAttribute = transcoder.encode(profileRequestContext, attribute, to, rules);
                if (encodedAttribute != null) {
                    results.add(encodedAttribute);
                    count++;
                }
            } catch (final AttributeEncodingException e) {
                if (isIgnoringUnencodableAttributes()) {
                    log.debug("{} Unable to encode attribute {}", getLogPrefix(), attribute.getId(), e);
                } else {
                    throw e;
                }
            }
        }
        
        return count;
    }
    
}
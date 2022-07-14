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

package net.shibboleth.idp.saml.saml1.profile.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.saml1.core.AttributeDesignator;
import org.opensaml.saml.saml1.core.AttributeQuery;
import org.opensaml.saml.saml1.core.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.AttributeDecodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoder;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscoderSupport;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/**
 * Action that filters a set of attributes against the {@link org.opensaml.saml.saml1.core.AttributeDesignator}
 * objects in an {@link AttributeQuery}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 */
public class FilterByQueriedAttributeDesignators extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(FilterByQueriedAttributeDesignators.class);

    /** Transcoder registry service object. */
    @NonnullAfterInit private ReloadableService<AttributeTranscoderRegistry> transcoderRegistry;
    
    /** Strategy used to locate the {@link Request} containing the query to filter against. */
    @Nonnull private Function<ProfileRequestContext,Request> requestLookupStrategy;

    /** Strategy used to locate the {@link AttributeContext} to filter. */
    @Nonnull private Function<ProfileRequestContext,AttributeContext> attributeContextLookupStrategy;

    /** Query to filter against. */
    @Nullable private AttributeQuery query;
    
    /** AttributeContext to filter. */
    @Nullable private AttributeContext attributeContext;

    /** Constructor. */
    public FilterByQueriedAttributeDesignators() {
        attributeContextLookupStrategy = new ChildContextLookup<>(AttributeContext.class).compose(
                new ChildContextLookup<>(RelyingPartyContext.class));
        
        requestLookupStrategy = new MessageLookup<>(Request.class).compose(new InboundMessageContextLookup());
    }

    /**
     * Sets the registry of transcoding rules to apply to encode attributes.
     * 
     * @param registry registry service interface
     */
    public void setTranscoderRegistry(@Nonnull final ReloadableService<AttributeTranscoderRegistry> registry) {
        throwSetterPreconditionExceptions();
        transcoderRegistry = Constraint.isNotNull(registry, "AttributeTranscoderRegistry cannot be null");
    }
    
    /**
     * Set the strategy used to locate the {@link Request} associated with a given {@link ProfileRequestContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setRequestLookupStrategy(@Nonnull final Function<ProfileRequestContext,Request> strategy) {
        throwSetterPreconditionExceptions();
        requestLookupStrategy = Constraint.isNotNull(strategy, "Request lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link AttributeContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setAttributeContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,AttributeContext> strategy) {
        throwSetterPreconditionExceptions();
        attributeContextLookupStrategy =
                Constraint.isNotNull(strategy, "AttributeContext lookup strategy cannot be null");
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
        
        final Request request = requestLookupStrategy.apply(profileRequestContext);
        if (request != null) {
            query = request.getAttributeQuery();
        }
        
        if (query == null || query.getAttributeDesignators().isEmpty()) {
            log.debug("No AttributeDesignators found, nothing to do ");
            return false;
        }
        
        attributeContext = attributeContextLookupStrategy.apply(profileRequestContext);
        if (attributeContext == null) {
            log.debug("{} No attribute context, no attributes to filter", getLogPrefix());
            return false;
        }

        if (attributeContext.getIdPAttributes().isEmpty()) {
            log.debug("{} No attributes to filter", getLogPrefix());
            return false;
        }

        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        final Set<String> decodedAttributeIds = new HashSet<>();

        ServiceableComponent<AttributeTranscoderRegistry> component = null;
        try {
            component = transcoderRegistry.getServiceableComponent();
            if (component == null) {
                log.error("Attribute transcoder service unavailable");
                ActionSupport.buildEvent(profileRequestContext, EventIds.MESSAGE_PROC_ERROR);
                return;
            }

            for (final AttributeDesignator designator : query.getAttributeDesignators()) {
                try {
                    decodeAttributeDesignator(component.getComponent(), profileRequestContext, designator,
                            decodedAttributeIds);
                } catch (final AttributeDecodingException e) {
                    log.warn("{} Error decoding AttributeDesignators", getLogPrefix(), e);
                }
            }
        } finally {
            if (component != null) {
                component.unpinComponent();
            }
        }
                
        
        final Collection<IdPAttribute> keepers = new ArrayList<>(query.getAttributeDesignators().size());
        log.debug("Query content mapped to attribute IDs: {}", decodedAttributeIds);
        
        for (final IdPAttribute attribute : attributeContext.getIdPAttributes().values()) {
            if (decodedAttributeIds.contains(attribute.getId())) {
                log.debug("Retaining attribute '{}' requested by query", attribute.getId());
                keepers.add(attribute);
            } else {
                log.debug("Removing attribute '{}' not requested by query", attribute.getId());
            }
        }
        
        attributeContext.setIdPAttributes(keepers);
    }

    /**
     * Access the registry of transcoding rules to decode the input {@link AttributeDesignator}.
     * 
     * @param registry  registry of transcoding rules
     * @param profileRequestContext current profile request context
     * @param input input object
     * @param results collection to add attributeIDs to
     * 
     * @throws AttributeDecodingException if an error occurs or no results were obtained
     */
    private void decodeAttributeDesignator(@Nonnull final AttributeTranscoderRegistry registry,
            @Nonnull final ProfileRequestContext profileRequestContext, @Nonnull final AttributeDesignator input,
            @Nonnull @NonnullElements @Live final Collection<String> results)
                    throws AttributeDecodingException {
        
        final Collection<TranscodingRule> transcodingRules = registry.getTranscodingRules(input);
        if (transcodingRules.isEmpty()) {
            throw new AttributeDecodingException("No transcoding rule for AttributeDesignator '" +
                    input.getAttributeName() + "'");
        }
        
        for (final TranscodingRule rules : transcodingRules) {
            final AttributeTranscoder<AttributeDesignator> transcoder = TranscoderSupport.getTranscoder(rules);
            final IdPAttribute decodedAttribute = transcoder.decode(profileRequestContext, input, rules);
            if (decodedAttribute != null) {
                results.add(decodedAttribute.getId());
            }
        }
    }

}
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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.shibboleth.idp.attribute.AttributeDecodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoder;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscoderSupport;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceException;
import net.shibboleth.shared.service.ServiceableComponent;

/**
 * Action that filters a set of attributes against the {@link org.opensaml.saml.saml2.core.Attribute} objects in
 * an {@link AttributeQuery}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 */
public class FilterByQueriedAttributes extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(FilterByQueriedAttributes.class);

    /** Transcoder registry service object. */
    @NonnullAfterInit private ReloadableService<AttributeTranscoderRegistry> transcoderRegistry;

    /** Strategy used to locate the {@link AttributeQuery} to filter against. */
    @Nonnull private Function<ProfileRequestContext,AttributeQuery> queryLookupStrategy;

    /** Strategy used to locate the {@link AttributeContext} to filter. */
    @Nonnull private Function<ProfileRequestContext,AttributeContext> attributeContextLookupStrategy;

    /** Query to filter against. */
    @Nullable private AttributeQuery query;
    
    /** AttributeContext to filter. */
    @Nullable private AttributeContext attributeContext;

    /** Constructor. */
    public FilterByQueriedAttributes() {
        attributeContextLookupStrategy = new ChildContextLookup<>(AttributeContext.class).compose(
                new ChildContextLookup<>(RelyingPartyContext.class));
        
        queryLookupStrategy = new MessageLookup<>(AttributeQuery.class).compose(new InboundMessageContextLookup());
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
     * Set the strategy used to locate the {@link AttributeQuery} associated with a given {@link ProfileRequestContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setQueryLookupStrategy(@Nonnull final Function<ProfileRequestContext,AttributeQuery> strategy) {
        checkSetterPreconditions();
        queryLookupStrategy = Constraint.isNotNull(strategy, "Request lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link AttributeContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setAttributeContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,AttributeContext> strategy) {
        checkSetterPreconditions();
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
        
        query = queryLookupStrategy.apply(profileRequestContext);
        
        if (query == null || query.getAttributes().isEmpty()) {
            log.debug("No queried Attributes found, nothing to do ");
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
                
        final Multimap<String,IdPAttribute> mapped = HashMultimap.create();

        try (final ServiceableComponent<AttributeTranscoderRegistry> component =
                transcoderRegistry.getServiceableComponent()) {

            for (final Attribute designator : query.getAttributes()) {
                try {
                    decodeAttribute(component.getComponent(), profileRequestContext, designator, mapped);
                } catch (final AttributeDecodingException e) {
                    log.error("{} Error decoding queried Attribute", getLogPrefix(), e);
                }
            }
        } catch (final ServiceException e) {
            log.error("Attribute transcoder service unavailable", e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.MESSAGE_PROC_ERROR);
            return;
        }
                
        log.debug("{} Query content mapped to attribute IDs: {}", getLogPrefix(), mapped.keySet());

        final Collection<IdPAttribute> keepers = new ArrayList<>(query.getAttributes().size());
        
        for (final IdPAttribute attribute : attributeContext.getIdPAttributes().values()) {
            
            final Collection<IdPAttribute> requested = mapped.get(attribute.getId());
            
            if (!requested.isEmpty()) {
                log.debug("{} Attribute '{}' requested by query, checking for requested values", getLogPrefix(),
                        attribute.getId());
                
                final int count = filterRequestedValues(attribute, requested);
                if (count > 0) {
                    log.debug("{} Retaining requested attribute '{}' with {} value(s)", getLogPrefix(),
                            attribute.getId(), count);
                    keepers.add(attribute);
                } else {
                    log.debug("{} Removing requested attribute '{}', no values left after filtering", getLogPrefix(),
                            attribute.getId());
                }
            } else {
                log.debug("{} Removing attribute '{}' not requested by query", getLogPrefix(), attribute.getId());
            }
        }
        
        attributeContext.setIdPAttributes(keepers);
    }
    
    /**
     * Access the registry of transcoding rules to decode the input {@link Attribute}.
     * 
     * @param registry  registry of transcoding rules
     * @param profileRequestContext current profile request context
     * @param input input object
     * @param results collection to add results to
     * 
     * @throws AttributeDecodingException if an error occurs or no results were obtained
     */
    protected void decodeAttribute(@Nonnull final AttributeTranscoderRegistry registry,
            @Nonnull final ProfileRequestContext profileRequestContext, @Nonnull final Attribute input,
            @Nonnull @NonnullElements @Live final Multimap<String,IdPAttribute> results)
                    throws AttributeDecodingException {
        
        final Collection<TranscodingRule> transcodingRules = registry.getTranscodingRules(input);
        if (transcodingRules.isEmpty()) {
            throw new AttributeDecodingException("No transcoding rule for Attribute '" + input.getName() + "'");
        }
        
        for (final TranscodingRule rules : transcodingRules) {
            final AttributeTranscoder<Attribute> transcoder = TranscoderSupport.getTranscoder(rules);
            final IdPAttribute decodedAttribute = transcoder.decode(profileRequestContext, input, rules);
            if (decodedAttribute != null) {
                results.put(decodedAttribute.getId(), decodedAttribute);
            }
        }
    }
    
    /**
     * Adjust an input attribute's values based on any values requested.
     * 
     * @param attribute attribute to filter
     * @param requestedAttributes the attributes (and possibly values) requested
     * 
     * @return  the number of values left in the input attribute
     */
    private int filterRequestedValues(@Nonnull final IdPAttribute attribute,
            @Nonnull @NonnullElements final Collection<IdPAttribute> requestedAttributes) {
        
        boolean requestedValues = false;

        final List<IdPAttributeValue> keepers = new ArrayList<>(attribute.getValues().size());
        
        for (final IdPAttributeValue value : attribute.getValues()) {
            
            for (final IdPAttribute requested : requestedAttributes) {
                if (!requested.getValues().isEmpty()) {
                    requestedValues = true;
                    if (requested.getValues().contains(value)) {
                        keepers.add(value);
                        break;
                    }
                }
            }
            
            if (!requestedValues) {
                keepers.add(value);
            }
        }
        
        attribute.setValues(keepers);
        return keepers.size();
    }

}

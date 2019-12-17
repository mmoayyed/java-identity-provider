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

package net.shibboleth.idp.attribute.transcoding.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import net.shibboleth.ext.spring.service.AbstractServiceableComponent;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoder;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.profile.logic.RelyingPartyIdPredicate;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** Service implementation of the {@link AttributeTranscoderRegistry} interface. */
@ThreadSafe
public class AttributeTranscoderRegistryImpl extends AbstractServiceableComponent<AttributeTranscoderRegistry>
        implements AttributeTranscoderRegistry {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeTranscoderRegistryImpl.class);
    
    /** Registry of transcoding instructions for a given "name" and type of object. */
    @Nonnull @NonnullElements private final Map<String,Multimap<Class<?>,TranscodingRule>> transcodingRegistry;
    
    /** Registry of display name mappings associated with internal attribute IDs. */
    @Nonnull @NonnullElements private final Map<String,Map<Locale,String>> displayNameRegistry;
    
    /** Registry of description mappings associated with internal attribute IDs. */
    @Nonnull @NonnullElements private final Map<String,Map<Locale,String>> descriptionRegistry;
    
    /** Registry of naming functions for supported object types. */
    @Nonnull @NonnullElements private final Map<Class<?>,Function<?,String>> namingFunctionRegistry;
    
    /** Constructor. */
    public AttributeTranscoderRegistryImpl() {
        transcodingRegistry = new HashMap<>();
        namingFunctionRegistry = new HashMap<>();
        displayNameRegistry = new HashMap<>();
        descriptionRegistry = new HashMap<>();
    }
    
    /** {@inheritDoc} */
    @Override @Nonnull public AttributeTranscoderRegistry getComponent() {
        return this;
    }
    
    /**
     * Installs registry of naming functions mapped against the types of objects they support.
     * 
     * @param registry map of types to naming functions
     */
    public void setNamingRegistry(@Nonnull @NonnullElements final Map<Class<?>,Function<?,String>> registry) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        namingFunctionRegistry.clear();
        
        if (registry == null) {
            return;
        }
        
        registry.forEach((k,v) -> {
            if (k != null && v != null) {
                namingFunctionRegistry.put(k, v);
            }
        });
    }

    /**
     * Installs the transcoder mappings en masse.
     * 
     * <p>Each map connects an {@link IdPAttribute} name to the rules for transcoding to/from it.</p>
     * 
     * <p>The rules MUST contain at least:</p>
     * <ul>
     *  <li>{@link #PROP_ID} - internal attribute ID to map to/from</li>
     *  <li>{@link #PROP_TRANSCODER} - an {@link AttributeTranscoder} instance supporting the type</li>
     * </ul>
     * 
     * Transcoders will generally require particular properties in their own right to function.
     * 
     * @param mappings transcoding rulesets
     */
    public void setTranscoderRegistry(@Nonnull @NonnullElements final Collection<TranscodingRule> mappings) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(mappings, "Mappings cannot be null");
        
       transcodingRegistry.clear();
        
        for (final TranscodingRule mapping : mappings) {
            
            final String internalId = StringSupport.trimOrNull(mapping.get(PROP_ID, String.class));
            if (internalId != null && !IdPAttribute.isInvalidId(internalId)) {
                final Predicate<?> activationCondition = buildActivationCondition(mapping.getMap());
                if (activationCondition != null) {
                    mapping.getMap().put(PROP_CONDITION, activationCondition);
                } else {
                    mapping.getMap().remove(PROP_CONDITION);
                }
                
                final Collection<AttributeTranscoder<?>> transcoders = getAttributeTranscoders(mapping);
                for (final AttributeTranscoder<?> transcoder : transcoders) {
                    addMapping(internalId, transcoder, mapping.getMap());
                }
            } else {
                log.warn("Ignoring TranscodingRule with invalid id property: {}", internalId);
            }
        }
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Map<Locale,String> getDisplayNames(
            @Nonnull final IdPAttribute attribute) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(attribute, "IdPAttribute cannot be null");
     
        if (displayNameRegistry.containsKey(attribute.getId())) {
            return displayNameRegistry.get(attribute.getId());
        }
        return Collections.emptyMap();
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Map<Locale,String> getDescriptions(
            @Nonnull final IdPAttribute attribute) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(attribute, "IdPAttribute cannot be null");
        
        if (descriptionRegistry.containsKey(attribute.getId())) {
            return descriptionRegistry.get(attribute.getId());
        }
        return Collections.emptyMap();
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements @Unmodifiable public Collection<TranscodingRule> getTranscodingRules(
            @Nonnull final IdPAttribute from, @Nonnull final Class<?> to) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(from, "IdPAttribute cannot be null");
        Constraint.isNotNull(to, "Target type cannot be null");
        
        final Multimap<Class<?>,TranscodingRule> propertyCollections = transcodingRegistry.get(from.getId());
        if (propertyCollections == null) {
            return Collections.emptyList();
        }
        
        final Class<?> effectiveType = getEffectiveType(to);
        if (effectiveType == null) {
            log.warn("Unsupported object type: {}", to.getClass().getName());
            return Collections.emptyList();
        }
        
        log.trace("Using rules for effective type {}", effectiveType.getName());
        
        return List.copyOf(propertyCollections.get(effectiveType));
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @Unmodifiable public <T> Collection<TranscodingRule> getTranscodingRules(
            @Nonnull final T from) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(from, "Input object cannot be null");
        
        final Class<?> effectiveType = getEffectiveType(from.getClass());
        if (effectiveType == null) {
            log.warn("Unsupported object type: {}", from.getClass().getName());
            return Collections.emptyList();
        }
        
        log.trace("Using rules for effective type {}", effectiveType.getName());
        
        final Function<?,String> namingFunction = namingFunctionRegistry.get(effectiveType);
        
        // Don't know if we can work around this cast or not.
        @SuppressWarnings("unchecked") final String id = ((Function<? super T,String>) namingFunction).apply(from);
        if (id != null) {
            final Multimap<Class<?>,TranscodingRule> propertyCollections = transcodingRegistry.get(id);
            
            return propertyCollections != null ? List.copyOf(propertyCollections.get(effectiveType))
                    : Collections.emptyList();
        }
        log.warn("Object of type {} did not have a canonical name", from.getClass().getName());
        
        return Collections.emptyList();
    }
    
    /**
     * Get the appropriate {@link AttributeTranscoder} objects to use.
     * 
     * @param rule transcoding rule
     * 
     * @return transcoders to install under a copy of each ruleset's {@link #PROP_TRANSCODER} property
     */
    @Nonnull @NonnullElements private Collection<AttributeTranscoder<?>> getAttributeTranscoders(
            @Nonnull final TranscodingRule rule) {
        
        AttributeTranscoder<?> transcoder = rule.get(PROP_TRANSCODER, AttributeTranscoder.class);
        if (transcoder != null) {
            return Collections.singletonList(transcoder);
        }
        
        final String beanNames = rule.get(PROP_TRANSCODER, String.class);
        if (beanNames == null) {
            log.error("{} property is missing or of incorrect type", PROP_TRANSCODER);
            return Collections.emptyList();
        }

        final List<AttributeTranscoder<?>> transcoders = new ArrayList<>();
        
        for (final String id :StringSupport.stringToList(beanNames, " ")) {
            try {
                transcoder = getApplicationContext().getBean(id, AttributeTranscoder.class);
                transcoder.initialize();
                transcoders.add(transcoder);
            } catch (final Exception e) {
                log.error("Unable to locate AttributeTranscoder bean named {}", id, e);
            }
        }
        
        return transcoders;
    }
    
    /**
     * Add a mapping between an {@link IdPAttribute} name and a set of transcoding rules.
     * 
     * @param id name of the {@link IdPAttribute} to map to/from
     * @param transcoder the transcoder for this rule
     * @param ruleset transcoding rules
     */
    private void addMapping(@Nonnull @NotEmpty final String id, @Nonnull final AttributeTranscoder<?> transcoder,
            @Nonnull final Map<String,Object> ruleset) {

        
        final TranscodingRule copy = new TranscodingRule(ruleset);
        copy.getMap().put(PROP_TRANSCODER, transcoder);

        final Class<?> type = transcoder.getEncodedType();
        final String targetName = transcoder.getEncodedName(copy);
        if (targetName != null) {
            
            log.debug("Attribute mapping: {} <-> {} via {}", id, targetName, transcoder.getClass().getSimpleName());
            
            // Install mapping back to IdPAttribute's trimmed name.
            copy.getMap().put(PROP_ID, id);
            
            Multimap<Class<?>,TranscodingRule> rulesetsForIdPName = transcodingRegistry.get(id);
            if (rulesetsForIdPName == null) {
                rulesetsForIdPName = ArrayListMultimap.create();
                transcodingRegistry.put(id, rulesetsForIdPName);
            }
            
            rulesetsForIdPName.put(type, copy);

            Multimap<Class<?>,TranscodingRule> rulesetsForEncodedName = transcodingRegistry.get(targetName);
            if (rulesetsForEncodedName == null) {
                rulesetsForEncodedName = ArrayListMultimap.create();
                transcodingRegistry.put(targetName, rulesetsForEncodedName);
            }
            
            rulesetsForEncodedName.put(type, copy);
            
            if (displayNameRegistry.containsKey(id)) {
                displayNameRegistry.get(id).putAll(copy.getDisplayNames());
            } else {
                displayNameRegistry.put(id, new HashMap<>(copy.getDisplayNames()));
            }

            if (descriptionRegistry.containsKey(id)) {
                descriptionRegistry.get(id).putAll(copy.getDescriptions());
            } else {
                descriptionRegistry.put(id, new HashMap<>(copy.getDescriptions()));
            }
            
        } else {
            log.warn("Transcoding rule for {} into type {} did not produce an encoded name", id, type.getName());
        }
    }
    
    /**
     * Build an appropriate {@link Predicate} to use as an activation condition within the ruleset.
     * 
     * @param ruleset transcoding rules
     * 
     * @return a predicate to install under the ruleset's {@link #PROP_CONDITION}
     */
    @SuppressWarnings("unchecked")
    @Nullable private Predicate<ProfileRequestContext> buildActivationCondition(
            @Nonnull final Map<String,Object> ruleset) {
        
        Predicate<ProfileRequestContext> effectiveCondition = null;
        
        final Object baseCondition = ruleset.get(PROP_CONDITION);
        if (baseCondition instanceof Predicate) {
            effectiveCondition = (Predicate<ProfileRequestContext>) baseCondition;
        } else if (baseCondition instanceof String) {
            try {
                effectiveCondition = getApplicationContext().getBean((String) baseCondition, Predicate.class);
            } catch (final Exception e) {
                log.error("Unable to locate Predicate bean named {}", baseCondition, e);
            }
        } else if (baseCondition != null) {
            log.error("{} property did not contain a Predicate object, ignored", PROP_CONDITION);
        }

        Predicate<ProfileRequestContext> relyingPartyCondition = null;

        final Object relyingParties = ruleset.get(PROP_RELYINGPARTIES);
        if (relyingParties instanceof Collection) {
            relyingPartyCondition = new RelyingPartyIdPredicate((Collection<String>) relyingParties);
        } else if (relyingParties instanceof String) {
            final Collection<String> parsed = StringSupport.normalizeStringCollection(
                    StringSupport.stringToList((String) relyingParties, " "));
            relyingPartyCondition = new RelyingPartyIdPredicate(parsed);
        } else if (relyingParties != null) {
            log.error("{} property did not contain a Collection or String, ignored", PROP_RELYINGPARTIES);
        }
        
        if (effectiveCondition == null) {
            return relyingPartyCondition;
        } else if (relyingPartyCondition != null) {
            return effectiveCondition.and(relyingPartyCondition);
        } else {
            return effectiveCondition;
        }
    }

    /**
     * Convert an input type into the appropriate type (possibly itself) to use in looking up
     * rules in the registry.
     * 
     * @param inputType the type passed into the registry operation
     * 
     * @return the appropriate type to use subsequently or null if not found
     */
    @Nullable private Class<?> getEffectiveType(@Nonnull final Class<?> inputType) {
        
        // Check for explicit support.
        if (namingFunctionRegistry.containsKey(inputType)) {
            return inputType;
        }
        
        // Try each map entry for a match. Optimized around the assumption the
        // map will be fairly small.
        for (final Class<?> candidate : namingFunctionRegistry.keySet()) {
            if (candidate.isAssignableFrom(inputType)) {
                return candidate;
            }
        }
        
        return null;
    }
    
}
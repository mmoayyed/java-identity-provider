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

package net.shibboleth.idp.saml.profile.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.ProfileIdLookup;
import org.opensaml.saml.common.messaging.context.navigate.EntityDescriptorLookupFunction;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.soap.client.security.SOAPClientSecurityProfileIdLookupFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.AttributesMapContainer;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.LockableClassToInstanceMultiMap;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A strategy function that examines SAML metadata associated with a relying party and derives configuration
 * settings based on EntityAttribute extension tags.
 * 
 * <p>The function is tailored with properties that determine what tag it looks for, with subclasses
 * handling the specific type conversion logic.</p>
 * 
 * <p>If a specific property is unavailable, then null is returned.</p>
 * 
 * @param <T> type of property being returned
 * 
 * @since 3.4.0
 */
public abstract class AbstractMetadataDrivenConfigurationLookupStrategy<T> extends AbstractInitializableComponent
        implements Function<BaseContext,T> {

    /** Default metadata lookup for PRC-based usage. */
    @Nonnull private static final Function<ProfileRequestContext,EntityDescriptor> DEFAULT_PRC_METADATA_LOOKUP;

    /** Default profile ID lookup for PRC-based usage. */
    @Nonnull private static final Function<ProfileRequestContext,String> DEFAULT_PRC_PROFILE_ID_LOOKUP;

    /** Default metadata lookup for MC-based usage. */
    @Nonnull private static final Function<MessageContext,EntityDescriptor> DEFAULT_MC_METADATA_LOOKUP;

    /** Default profile ID lookup for MC-based usage. */
    @Nonnull private static final Function<MessageContext,String> DEFAULT_MC_PROFILE_ID_LOOKUP;

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(AbstractMetadataDrivenConfigurationLookupStrategy.class);
    
    /** Require use of URI attribute name format. */
    private boolean strictNameFormat;
    
    /** Cache the lookup in the context tree. */
    private boolean enableCaching;
    
    /** Examine only decoded/mapped tags in object metadata. */
    private boolean ignoreUnmappedEntityAttributes;
    
    /** Prevents prefixing of property name by profile/aliases. */
    private boolean explicitPropertyName;
    
    /** Base name of property to produce. */
    @NonnullAfterInit @NotEmpty private String propertyName;
    
    /** Alternative "full" property identifiers to support. */
    @NonnullAfterInit @NonnullElements private Collection<String> propertyAliases;
    
    /** Optional default to return in the absence of a property. */
    @Nullable private Function<BaseContext,T> defaultValueStrategy;
    
    /** Strategy for obtaining metadata to check. */
    @Nullable private Function<BaseContext,EntityDescriptor> metadataLookupStrategy;

    /** Strategy for obtaining profile ID for property naming. */
    @Nullable @NotEmpty private Function<BaseContext,String> profileIdLookupStrategy;
        
    /** Constructor. */
    public AbstractMetadataDrivenConfigurationLookupStrategy() {
        enableCaching = true;
        defaultValueStrategy = FunctionSupport.constant(null);
    }
    
    /**
     * Sets whether tag matching should examine and require an Attribute NameFormat of the URI type.
     * 
     * <p>Default is false.</p>
     * 
     * @param flag flag to set
     */
    public void setStrictNameFormat(final boolean flag) {
        checkSetterPreconditions();

        strictNameFormat = flag;
    }
    
    /**
     * Sets whether property lookup should be cached in the profile context tree.
     * 
     * <p>Default is true.</p>
     * 
     * @param flag flag to set
     */
    public void setEnableCaching(final boolean flag) {
        checkSetterPreconditions();
        
        enableCaching = flag;
    }

    /**
     * Sets whether property lookup should be based solely on mapped/decoded objects
     * and not on underlying SAML Attributes.
     * 
     * <p>Default is false.</p>
     * 
     * @param flag flag to set
     */
    public void setIgnoreUnmappedEntityAttributes(final boolean flag) {
        checkSetterPreconditions();
        
        ignoreUnmappedEntityAttributes = flag;
    }
    
    /**
     * Sets whether to treat the property name as absolute instead of auto-prefixed
     * by profile or alias values.
     * 
     * <p>Used to allow for direct lookup of a specific tag instead implicitly prefixing the 
     * tag name based on configuration "context".</p>
     * 
     * @param flag flag to set
     * 
     * @since 4.3.0
     */
    public void setExplicitPropertyName(final boolean flag) {
        checkSetterPreconditions();
        
        explicitPropertyName = flag;
    }
    
    /**
     * Sets the "base" name of the property/setting to derive.
     * 
     * @param name base property name
     */
    public void setPropertyName(@Nonnull @NotEmpty final String name) {
        checkSetterPreconditions();
        
        propertyName = Constraint.isNotNull(StringSupport.trimOrNull(name), "Property name cannot be null or empty");
    }
    
    /**
     * Sets profile ID aliases to include when checking for metadata tags (the property name is suffixed to the
     * aliases).
     * 
     * <p>This allows alternative tag names to be checked.</p>
     * 
     * @param aliases alternative profile IDs
     */
    public void setProfileAliases(@Nonnull @NonnullElements final Collection<String> aliases) {
        checkSetterPreconditions();
        
        Constraint.isNotNull(aliases, "Alias collection cannot be null");
        propertyAliases = List.copyOf(StringSupport.normalizeStringCollection(aliases));
    }
    
    /**
     * Sets a default value to return as the function result in the absence of an explicit property.
     * 
     * @param value default value to return 
     */
    public void setDefaultValue(@Nullable final T value) {
        checkSetterPreconditions();
        
        defaultValueStrategy = FunctionSupport.constant(value);
    }
    
    /**
     * Sets a default value function to apply in the absence of an explicit property.
     * 
     * @param strategy default function to apply
     * 
     * @since 4.0.0
     */
    public void setDefaultValueStrategy(@Nonnull final Function<BaseContext,T> strategy) {
        checkSetterPreconditions();
        
        defaultValueStrategy = Constraint.isNotNull(strategy, "Default value strategy cannot be null");
    }
    
    /**
     * Sets lookup strategy for metadata to examine.
     * 
     * @param strategy  lookup strategy
     */
    public void setMetadataLookupStrategy(@Nonnull final Function<BaseContext,EntityDescriptor> strategy) {
        checkSetterPreconditions();
        
        metadataLookupStrategy = Constraint.isNotNull(strategy, "Metadata lookup strategy cannot be null");
    }

    /**
     * Sets lookup strategy for profile ID to base property names on.
     * 
     * @param strategy  lookup strategy
     */
    public void setProfileIdLookupStrategy(@Nonnull final Function<BaseContext,String> strategy) {
        checkSetterPreconditions();
        
        profileIdLookupStrategy = Constraint.isNotNull(strategy, "Profile ID lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (propertyName == null) {
            throw new ComponentInitializationException("Property name cannot be null or empty");
        } else if (propertyAliases == null) {
            propertyAliases = Collections.emptyList();
        }
        
        // Now attach the property name to the end of the alias list entries.
        propertyAliases = propertyAliases.stream()
                .map(s -> s + (s.endsWith("/") ? propertyName : '/' + propertyName))
                .collect(Collectors.toUnmodifiableList());
        
    }

    // Checkstyle: CyclomaticComplexity|MethodLength OFF    
    /** {@inheritDoc} */
    @Nullable public T apply(@Nullable final BaseContext input) {
        checkComponentActive();
        
        CachedConfigurationContext cacheContext = null;
        
        if (enableCaching && input != null) {
            cacheContext = input.getSubcontext(CachedConfigurationContext.class, true);
            if (cacheContext.getPropertyMap().containsKey(propertyName)) {
                log.debug("Returning cached property '{}'", propertyName);
                return (T) cacheContext.getPropertyMap().get(propertyName);
            }
        }
        
        final EntityDescriptor entity;
        final String profileId;
        
        if (metadataLookupStrategy != null) {
            entity = metadataLookupStrategy.apply(input);
        } else if (input instanceof ProfileRequestContext) {
            entity = DEFAULT_PRC_METADATA_LOOKUP.apply((ProfileRequestContext) input);
        } else if (input instanceof MessageContext) {
            entity = DEFAULT_MC_METADATA_LOOKUP.apply((MessageContext) input);
        } else {
            entity = null;
        }
            
        if (entity == null) {
            log.debug("No metadata available for relying party, applying default strategy for '{}'", propertyName);
            return defaultValueStrategy.apply(input);
        }
        
        if (!explicitPropertyName) {
            if (profileIdLookupStrategy != null) {
                profileId = profileIdLookupStrategy.apply(input);
            } else if (input instanceof ProfileRequestContext) {
                profileId = DEFAULT_PRC_PROFILE_ID_LOOKUP.apply((ProfileRequestContext) input);
            } else if (input instanceof MessageContext) {
                profileId = DEFAULT_MC_PROFILE_ID_LOOKUP.apply((MessageContext) input);
            } else {
                profileId = "";
            }
        } else {
            profileId = null;
        }
        
        // Look for "primary" tag name based on profile/property using mapped tags.
        IdPAttribute idpAttribute = findMatchingMappedTag(entity,
                profileId != null ? profileId + '/' + propertyName : propertyName);
        if (idpAttribute != null) {
            log.debug("Found matching tag '{}' for property '{}'", idpAttribute.getId(), propertyName);
            final T result = translate(idpAttribute);
            if (enableCaching) {
                cacheContext.getPropertyMap().put(propertyName, result);
            }
            return result;
        }
        
        // Check aliases.
        for (final String alias : propertyAliases) {
            idpAttribute = findMatchingMappedTag(entity, alias);
            if (idpAttribute != null) {
                log.debug("Found matching tag '{}' for property '{}'", idpAttribute.getId(), propertyName);
                final T result = translate(idpAttribute);
                if (enableCaching) {
                    cacheContext.getPropertyMap().put(propertyName, result);
                }
                return result;
            }
        }
        
        if (ignoreUnmappedEntityAttributes) {
            log.debug("No applicable mapped tag, applying default strategy for '{}'", propertyName);
            final T ret = defaultValueStrategy.apply(input);
            if (enableCaching) {
                cacheContext.getPropertyMap().put(propertyName, ret);
            }
            return ret;
        }
        
        // Look for "primary" tag name based on profile/property.
        Attribute attribute = findMatchingTag(entity,
                profileId != null ? profileId + '/' + propertyName : propertyName);
        if (attribute != null) {
            log.debug("Found matching tag '{}' for property '{}'", attribute.getName(), propertyName);
            final T result = translate(attribute);
            if (enableCaching) {
                cacheContext.getPropertyMap().put(propertyName, result);
            }
            return result;
        }
        
        // Check aliases.
        for (final String alias : propertyAliases) {
            attribute = findMatchingTag(entity, alias);
            if (attribute != null) {
                log.debug("Found matching tag '{}' for property '{}'", attribute.getName(), propertyName);
                final T result = translate(attribute);
                if (enableCaching) {
                    cacheContext.getPropertyMap().put(propertyName, result);
                }
                return result;
            }
        }
        
        log.debug("No applicable tag, applying default strategy for '{}'", propertyName);
        final T ret = defaultValueStrategy.apply(input);
        if (enableCaching) {
            cacheContext.getPropertyMap().put(propertyName, ret);
        }
        return ret;
    }
// Checkstyle: CyclomaticComplexity|MethodLength ON
    
    /**
     * Translate the value(s) into a setting of the appropriate type.
     * 
     * @param tag tag to translate
     * 
     * @return the setting derived from the tag's value(s)
     */
    @Nullable private T translate(@Nonnull final Attribute tag) {
        
        final List<XMLObject> values = tag.getAttributeValues();
        if (values == null || values.isEmpty()) {
            log.debug("Tag '{}' contained no values, no setting returned for '{}'", tag.getName(), propertyName);
            return null;
        }
        
        return doTranslate(tag);
    }
    
    /**
     * Translate the value(s) into a setting of the appropriate type.
     * 
     * @param tag tag to translate
     * 
     * @return the setting derived from the tag's value(s)
     */
    @Nullable private T translate(@Nonnull final IdPAttribute tag) {
        
        final List<IdPAttributeValue> values = tag.getValues();
        if (values == null || values.isEmpty()) {
            log.debug("Tag '{}' contained no values, no setting returned for '{}'", tag.getId(), propertyName);
            return null;
        }
        
        return doTranslate(tag);
    }

    /**
     * Translate the value(s) into a setting of the appropriate type.
     * 
     * <p>Overrides of this function can assume a non-zero collection of values.</p>
     * 
     * @param tag tag to translate
     * 
     * @return the setting derived from the tag's value(s)
     */
    @Nullable protected abstract T doTranslate(@Nonnull final Attribute tag); 

    /**
     * Translate the value(s) into a setting of the appropriate type.
     * 
     * <p>Overrides of this function can assume a non-zero collection of values.</p>
     * 
     * @param tag tag to translate
     * 
     * @return the setting derived from the tag's value(s)
     */
    @Nullable protected abstract T doTranslate(@Nonnull final IdPAttribute tag); 
    
    /**
     * Find first matching attribute in the input object's node metadata.
     * 
     * @param entity the metadata to examine
     * @param name the tag name to search for
     * 
     * @return matching attribute, or null
     */
    @Nullable private IdPAttribute findMatchingMappedTag(@Nonnull final EntityDescriptor entity,
            @Nonnull @NotEmpty final String name) {
        
        // Check for a tag match in the node metadata of the entity and its parent(s).
        IdPAttribute tag = findMatchingMappedTag(entity.getObjectMetadata(), name);
        if (tag != null) {
            return tag;
        }

        XMLObject parent = entity.getParent();
        while (parent instanceof EntitiesDescriptor) {
            tag = findMatchingMappedTag(parent.getObjectMetadata(), name);
            if (tag != null) {
                return tag;
            }
            parent = parent.getParent();
        }
        
        return null;
    }
    
    /**
     * Find a matching entity attribute in the input metadata.
     * 
     * @param entity the metadata to examine
     * @param name the tag name to search for
     * 
     * @return matching attribute or null
     */
    @Nullable private Attribute findMatchingTag(@Nonnull final EntityDescriptor entity,
            @Nonnull @NotEmpty final String name) {
        
        // Check for a tag match in the EntityAttributes extension of the entity and its parent(s).
        Extensions exts = entity.getExtensions();
        if (exts != null) {
            final List<XMLObject> children = exts.getUnknownXMLObjects(EntityAttributes.DEFAULT_ELEMENT_NAME);
            if (!children.isEmpty() && children.get(0) instanceof EntityAttributes) {
                final Attribute tag = findMatchingTag((EntityAttributes) children.get(0), name);
                if (tag != null) {
                    return tag;
                }
            }
        }

        EntitiesDescriptor group = (EntitiesDescriptor) entity.getParent();
        while (group != null) {
            exts = group.getExtensions();
            if (exts != null) {
                final List<XMLObject> children = exts.getUnknownXMLObjects(EntityAttributes.DEFAULT_ELEMENT_NAME);
                if (!children.isEmpty() && children.get(0) instanceof EntityAttributes) {
                    final Attribute tag = findMatchingTag((EntityAttributes) children.get(0), name);
                    if (tag != null) {
                        return tag;
                    }
                }
            }
            group = (EntitiesDescriptor) group.getParent();
        }
        
        return null;
    }
    
    /**
     * Find first matching attribute in the input object's node metadata.
     * 
     * @param input the metadata to examine
     * @param name the tag name to search for
     * 
     * @return matching attribute, or null
     */
    @Nullable private IdPAttribute findMatchingMappedTag(@Nonnull final LockableClassToInstanceMultiMap<?> input,
            @Nonnull @NotEmpty final String name) {

        final List<AttributesMapContainer> containerList = input.get(AttributesMapContainer.class);
        if (null == containerList || containerList.isEmpty() || containerList.get(0).get() == null ||
                containerList.get(0).get().isEmpty()) {
            return null;
        }
        
        final Collection<IdPAttribute> matches = containerList.get(0).get().get(name);
        return matches.isEmpty() ? null : matches.iterator().next();
    }

    /**
     * Find a matching entity attribute in the input metadata.
     * 
     * @param entityAttributes the metadata to examine
     * @param name the tag name to search for
     * 
     * @return matching attribute or null
     */
    @Nullable private Attribute findMatchingTag(@Nonnull final EntityAttributes entityAttributes,
            @Nonnull @NotEmpty final String name) {
        
        for (final Attribute tag : entityAttributes.getAttributes()) {
            if (Objects.equals(tag.getName(), name)
                    && (!strictNameFormat || Objects.equals(tag.getNameFormat(), Attribute.URI_REFERENCE))) {
                return tag;
            }
        }

        return null;
    }
    
    /** A child context that caches derived configuration properties. */
    public static final class CachedConfigurationContext extends BaseContext {
        
        /** Cached property map. */
        @Nonnull private Map<String,Object> propertyMap;
        
        /** Constructor. */
        public CachedConfigurationContext() {
            propertyMap = new HashMap<>();
        }
        
        /**
         * Get cached property map.
         * 
         * @return cached property map
         */
        @Nonnull @Live Map<String,Object> getPropertyMap() {
            return propertyMap;
        }
    }
    
    static {
        // Init PRC defaults.
        
        DEFAULT_PRC_METADATA_LOOKUP = new EntityDescriptorLookupFunction().compose(
                new net.shibboleth.idp.saml.profile.context.navigate.SAMLMetadataContextLookupFunction());
        
        DEFAULT_PRC_PROFILE_ID_LOOKUP = new ProfileIdLookup();

        // Init MC defaults.

        DEFAULT_MC_METADATA_LOOKUP = new EntityDescriptorLookupFunction().compose(
                new net.shibboleth.idp.saml.profile.context.navigate.messaging.SAMLMetadataContextLookupFunction());
        
        DEFAULT_MC_PROFILE_ID_LOOKUP = new SOAPClientSecurityProfileIdLookupFunction();
    }
}

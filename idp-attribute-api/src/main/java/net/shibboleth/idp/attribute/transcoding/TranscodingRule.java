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

package net.shibboleth.idp.attribute.transcoding;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableMap;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Wrapper around a {@link Map} representing a rule for transcoding, used to
 * detect and load the rules at runtime from a Spring context.
 */
public class TranscodingRule {

    /** Underlying map containing the rule. */
    @Nonnull @NonnullElements private final Map<String,Object> rule;
    
    /** Map of locale-specific display names. */
    @Nonnull @NonnullElements private Map<Locale,String> displayNames;
    
    /** Map of locale-specific descriptions. */
    @Nonnull @NonnullElements private Map<Locale,String> descriptions;
    
    /**
     * Constructor.
     * 
     * @param map a map containing the rule
     * 
     * <p>The rule MUST contain at least:</p>
     * <ul>
     *  <li>
     *  {@link AttributeTranscoderRegistry#PROP_ID} - internal attribute ID to map to/from
     *  </li>
     *  <li>
     *  {@link AttributeTranscoderRegistry#PROP_TRANSCODER} - {@link AttributeTranscoder} instance supporting the type
     *  </li>
     * </ul>
     */
    public TranscodingRule(@Nonnull @NonnullElements @ParameterName(name="map") final Map<String,Object> map) {
        rule = new HashMap<>(map);
        processDisplayNames();
        processDescriptions();
    }

    /**
     * Constructor.
     * 
     * @param properties a property set to initialize the map
     * 
     * <p>The rule MUST contain at least:</p>
     * <ul>
     *  <li>
     *  {@link AttributeTranscoderRegistry#PROP_ID} - internal attribute ID to map to/from
     *  </li>
     *  <li>
     *  {@link AttributeTranscoderRegistry#PROP_TRANSCODER} - {@link AttributeTranscoder} class name
     *  </li>
     * </ul>
     */
    public TranscodingRule(@Nonnull @NonnullElements @ParameterName(name="properties") final Properties properties) {
        rule = new HashMap<>(properties.size());
        properties.forEach(
                (k,v) -> {
                    if (k instanceof String && v != null) {
                        rule.put((String) k, v);
                    }
                });
        processDisplayNames();
        processDescriptions();
    }

    /**
     * Access the underlying mapping rule.
     * 
     * @return the map representing the rule
     */
    @Nonnull @NonnullElements @Live public Map<String,Object> getMap() {
        return rule;
    }
    
    /**
     * Get the value of a property key in the rule.
     * 
     * @param <T> type of property value
     * @param key key of property
     * @param type class type of property
     * 
     * @return the mapped value, or null
     */
    @Nullable public <T> T get(@Nonnull @NotEmpty final String key, @Nonnull final Class<T> type) {
        final Object value = rule.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        } else if (type == Boolean.class && value instanceof String) {
            return (T) Boolean.valueOf((String) value);
        } else {
            return null;
        }
    }
    
    /**
     * Get the value of a property key in the rule or a default value if no property exists.
     * 
     * @param <T> type of property value
     * @param key key of property
     * @param type class type of property
     * @param defValue default value
     * 
     * @return the mapped value, or the default
     */
    @Nullable public <T> T getOrDefault(@Nonnull @NotEmpty final String key, @Nonnull final Class<T> type,
            @Nullable final T defValue) {
        
        final T value = get(key, type);
        if (value != null) {
            return value;
        } else {
            return defValue;
        }
    }
    
    /**
     * Get the display names to attach to any {@link IdPAttribute} objects created by this transcoder.
     * 
     * @return map of locale-based names
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Map<Locale,String> getDisplayNames() {
        return displayNames;
    }

    /**
     * Get the descriptions to attach to any {@link IdPAttribute} objects created by this transcoder.
     * 
     * @return map of locale-based descriptions
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Map<Locale,String> getDescriptions() {
        return descriptions;
    }
    
    /**
     * Process any properties that start with {@link AttributeTranscoderRegistry.PROP_DISPLAY_NAME} and
     * create a Locale map around them.
     */
    private void processDisplayNames() {
        
        final ImmutableMap.Builder<Locale,String> builder = ImmutableMap.builder();
        
        for (final Map.Entry<String,Object> entry : rule.entrySet()) {
            if (entry.getValue() instanceof String
                    && entry.getKey().startsWith(AttributeTranscoderRegistry.PROP_DISPLAY_NAME)) {
                
                final String lang = StringSupport.trimOrNull(
                        entry.getKey().substring(AttributeTranscoderRegistry.PROP_DISPLAY_NAME.length()));
                if (lang != null) {
                    builder.put(Locale.forLanguageTag(lang), (String) entry.getValue());
                } else {
                    builder.put(Locale.getDefault(), (String) entry.getValue());
                }
            }
        }
        
        displayNames = builder.build();
    }

    /**
     * Process any properties that start with {@link AttributeTranscoderRegistry.PROP_DESCRIPTION} and
     * create a Locale map around them.
     */
    private void processDescriptions() {
        
        final ImmutableMap.Builder<Locale,String> builder = ImmutableMap.builder();
        
        for (final Map.Entry<String,Object> entry : rule.entrySet()) {
            if (entry.getValue() instanceof String
                    && entry.getKey().startsWith(AttributeTranscoderRegistry.PROP_DESCRIPTION)) {
                
                final String lang = StringSupport.trimOrNull(
                        entry.getKey().substring(AttributeTranscoderRegistry.PROP_DESCRIPTION.length()));
                if (lang != null) {
                    builder.put(Locale.forLanguageTag(lang), (String) entry.getValue());
                } else {
                    builder.put(Locale.getDefault(), (String) entry.getValue());
                }
            }
        }
        
        descriptions = builder.build();
    }
    
    /**
     * Build a new rule from a property set resource.
     * 
     * @param resource a property set to initialize the map
     * 
     * @return the new rule 
     * 
     * @throws IOException if an error occurs
     */
    @Nonnull public static TranscodingRule fromResource(
            @Nonnull @ParameterName(name="resource") final Resource resource) throws IOException {
        
        final Properties props = new Properties();
    
        try (final InputStream is = resource.getInputStream()) {
            props.load(is);
        }
        
        return new TranscodingRule(props);
    }

}
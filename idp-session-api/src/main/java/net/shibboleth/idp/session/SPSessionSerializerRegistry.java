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

package net.shibboleth.idp.session;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A registry of mappings between a {@link SPSession} class and a corresponding {@link StorageSerializer}
 * for that type.
 */
public final class SPSessionSerializerRegistry extends AbstractInitializableComponent {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SPSessionSerializerRegistry.class);
    
    /** Storage for the registry mappings. */
    @Nonnull @NonnullElements
    private Map<Class<? extends SPSession>,StorageSerializer<? extends SPSession>> registry;

    /** Constructor. */
    public SPSessionSerializerRegistry() {
        this(null);
    }
    
    /**
     * Constructor.
     *
     * @param serializers auto-wired serializer entries
     * 
     * @since 4.1.0
     */
    @Autowired
    public SPSessionSerializerRegistry(@Nullable @NonnullElements final Collection<Entry<?>> serializers) {
        registry = new HashMap<>();
        if (serializers != null) {
            serializers.forEach(e -> registry.put(e.getType(), e.getSerializer()));
        }
    }
    
    /**
     * Set the mappings to use.
     * 
     * @param map  map to populate registry with
     */
    public void setMappings(@Nonnull @NonnullElements final
            Map<Class<? extends SPSession>,StorageSerializer<? extends SPSession>> map) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(map, "Map cannot be null");
        
        for (final Map.Entry<Class<? extends SPSession>,StorageSerializer<? extends SPSession>> entry
                : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                registry.put(entry.getKey(), entry.getValue());
            }
        }
    }
    
    /**
     * Get a registered {@link StorageSerializer} for a given {@link SPSession} type, if any.
     * 
     * @param <T> type of SPSession
     * @param type a type of SPSession
     * @return a corresponding StorageSerializer, or null
     */
    @Nullable public <T extends SPSession> StorageSerializer<T> lookup(@Nonnull final Class<T> type) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(type, "SPSession type cannot be null");
        
        final StorageSerializer<T> serializer = (StorageSerializer<T>) registry.get(type);
        if (serializer != null) {
            log.debug("Registry located StorageSerializer of type '{}' for SPSession type '{}'",
                    serializer.getClass().getName(), type);
            return serializer;
        }
        log.debug("Registry failed to locate StorageSerializer for SPSession type '{}'", type);
        return null;
    }

    /**
     * Wrapper type for auto-wiring serializers.
     * 
     * @param <T> session type
     * 
     * @since 4.1.0
     */
    public static class Entry<T extends SPSession> {
        
        /** Session type. */
        @Nonnull private final Class<T> sessionType;
        
        /** Serializer. */
        @Nonnull private final StorageSerializer<T> serializer;
        
        /**
         * Constructor.
         *
         * @param claz session type
         * @param object serializer
         */
        public Entry(@Nonnull @ParameterName(name="claz") final Class<T> claz,
                @Nullable @ParameterName(name="object") final StorageSerializer<T> object) {
            sessionType = Constraint.isNotNull(claz, "Session type cannot be null");
            serializer = object;
        }
        
        /**
         * Gets session type.
         * 
         * @return session type
         */
        @Nonnull Class<T> getType() {
            return sessionType;
        }
        
        /**
         * Gets {@link StorageSerializer}.
         * 
         * @return serializer
         */
        @Nullable StorageSerializer<T> getSerializer() {
            return serializer;
        }
    }

}
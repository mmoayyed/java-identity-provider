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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A registry of mappings between a {@link ServiceSession} class and a corresponding {@link StorageSerializer}
 * for that type.
 */
public final class ServiceSessionSerializerRegistry {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ServiceSessionSerializerRegistry.class);
    
    /** Storage for the registry mappings. */
    private Map<Class<? extends ServiceSession>, StorageSerializer<? extends ServiceSession>> registry;

    /** Constructor. */
    public ServiceSessionSerializerRegistry() {
        registry = new ConcurrentHashMap();
    }
    
    /**
     * Constructor.
     * 
     * @param fromMap  map to populate registry with
     */
    public ServiceSessionSerializerRegistry(@Nonnull @NonnullElements
            Map<Class<? extends ServiceSession>, StorageSerializer<? extends ServiceSession>> fromMap) {
        registry = new ConcurrentHashMap(Constraint.isNotNull(fromMap, "Source map cannot be null"));
    }
    
    /**
     * Get a registered {@link StorageSerializer} for a given {@link ServiceSession} type, if any.
     * 
     * @param type a type of ServiceSession
     * @return a corresponding StorageSerializer, or null
     */
    @Nullable public StorageSerializer<? extends ServiceSession> lookup(
            @Nonnull final Class<? extends ServiceSession> type) {
        Constraint.isNotNull(type, "ServiceSession type cannot be null");
        
        StorageSerializer<? extends ServiceSession> serializer = registry.get(type);
        if (serializer != null) {
            log.debug("Registry located StorageSerializer of type '{}' for ServiceSession type '{}'",
                    serializer.getClass().getName(), type);
            return serializer;
        } else {
            log.debug("Registry failed to locate StorageSerializer for ServiceSession type '{}'", type);
            return null;
        }
    }
    
    /**
     * Register a {@link StorageSerializer} for a given {@link ServiceSession} type.
     * 
     * @param type a type of ServiceSession
     * @param serializer the StorageSerializer to register
     */
    public void register(@Nonnull final Class<? extends ServiceSession> type,
            @Nonnull StorageSerializer<? extends ServiceSession> serializer) {
        Constraint.isNotNull(type, "ServiceSession type cannot be null");
        Constraint.isNotNull(serializer, "StorageSerializer cannot be null");
        
        log.debug("Registering StorageSerializer of type '{}' for ServiceSession type '{}'",
                serializer.getClass().getName(), type);
        registry.put(type, serializer);
    }
    
    /**
     * Deregister a {@link StorageSerializer} for a given {@link ServiceSession} type.
     * 
     * @param type a type of ServiceSession
     */
    public void deregister(@Nonnull final Class<? extends ServiceSession> type) {
        Constraint.isNotNull(type, "ServiceSession type cannot be null");
        
        log.debug("Deregistering StorageSerializer for ServiceSession type '{}'", type);
        registry.remove(type);
    }
}
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

package net.shibboleth.idp.authn.principal;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * Manages and exposes instances of the {@link PrincipalService} interface.
 * 
 * @since 4.1.0
 */
public class PrincipalServiceManager {
    
    /** Service index by class. */
    @Nonnull @NonnullElements private final Map<Class<?>,PrincipalService<?>> classIndexedMap;

    /** Service index by ID. */
    @Nonnull @NonnullElements private final Map<String,PrincipalService<?>> idIndexedMap;

    /**
     * Constructor.
     *
     * @param services instances to manage
     */
    @Autowired
    public PrincipalServiceManager(@Nullable @NonnullElements final Collection<PrincipalService<?>> services) {
        if (services != null) {
            classIndexedMap = new HashMap<>(services.size());
            idIndexedMap = new HashMap<>(services.size());
            services.forEach(ps -> {
                classIndexedMap.put(ps.getType(), ps);
                idIndexedMap.put(ps.getId(), ps);
            });
        } else {
            classIndexedMap = Collections.emptyMap();
            idIndexedMap = Collections.emptyMap();
        }
    }

    /**
     * Get a {@link PrincipalService} by type.
     * 
     * @param <T> class type
     * @param claz class type
     * 
     * @return service for the type, or null
     */
    @Nullable public <T extends Principal> PrincipalService<T> byClass(@Nonnull final Class<T> claz) {
        final PrincipalService<?> service = classIndexedMap.get(claz);
        if (service != null) {
            return service.getType().isAssignableFrom(claz) ? (PrincipalService<T>) service : null;
        }
        return null;
    }
    
    /**
     * Get a {@link PrincipalService} by ID.
     * 
     * @param id identifier
     * 
     * @return named service, or null
     */
    @Nullable public PrincipalService<?> byId(@Nonnull @NotEmpty final String id) {
        return idIndexedMap.get(id);
    }

}
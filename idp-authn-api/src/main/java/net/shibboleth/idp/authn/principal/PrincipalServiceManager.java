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
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Manages and exposes instances of the {@link PrincipalService} interface.
 * 
 * @since 4.1.0
 */
public class PrincipalServiceManager {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PrincipalServiceManager.class);
    
    /** Service index by class. */
    @Nonnull private final Map<Class<?>,PrincipalService<?>> classIndexedMap;

    /** Service index by ID. */
    @Nonnull private final Map<String,PrincipalService<?>> idIndexedMap;

    /**
     * Constructor.
     *
     * @param services instances to manage
     */
    @Autowired
    public PrincipalServiceManager(
            @Nullable @ParameterName(name="services") final Collection<PrincipalService<?>> services) {
        if (services != null) {
            classIndexedMap = new HashMap<>(services.size());
            idIndexedMap = new HashMap<>(services.size());
            services.forEach(ps -> {
                classIndexedMap.put(ps.getType(), ps);
                idIndexedMap.put(ps.getId(), ps);
            });
        } else {
            classIndexedMap = CollectionSupport.emptyMap();
            idIndexedMap = CollectionSupport.emptyMap();
        }
    }
    
    /**
     * Get all of the registered services.
     * 
     * @return all registered services
     */
    @Nonnull @NotLive @Unmodifiable public Collection<PrincipalService<?>> all() {
        final Collection<PrincipalService<?>> values = classIndexedMap.values();
        assert values!=null;
        return CollectionSupport.copyToList(values);
    }

    /**
     * Get a {@link PrincipalService} by type.
     * 
     * @param <T> class type
     * @param claz class type
     * 
     * @return service for the type, or null
     */
    @SuppressWarnings("unchecked")
    @Nullable public <T extends Principal> PrincipalService<T> byClass(@Nonnull final Class<T> claz) {
        final PrincipalService<?> service = classIndexedMap.get(claz);
        if (service != null) {
            return service.getType().isAssignableFrom(claz) ? (PrincipalService<T>) service : null;
        }
        
        log.debug("No service found for Principal type '{}'", claz.getName());
        return null;
    }
    
    /**
     * Manufacture a {@link Principal} from a string of the format "type/value" where
     * type matches the ID of a {@link PrincipalService} and the value is supplied to
     * a single-arg String constructor if one exists.
     * 
     * @param s the delimited form above
     * 
     * @return the new object or null
     */
    @Nullable public Principal principalFromString(@Nonnull @NotEmpty final String s) {
        final int index = s.indexOf('/');
        if (index > 1 && index < s.length() - 1) {
            final PrincipalService<?> psvc = idIndexedMap.get(s.substring(0, index));
            if (psvc != null) {
                try {
                    return psvc.getType().getConstructor(String.class).newInstance(s.substring(index + 1));
                } catch (final ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
                    log.error("No suitable constructor available to create instance of '{}'",
                            psvc.getType().getName(), e);
                }
            } else {
                log.error("No PrincipalService registered under ID '{}'", s.substring(0, index));
            }
        } else {
            log.error("Principal string was not in the expected format");
        }
        
        return null;
    }

}
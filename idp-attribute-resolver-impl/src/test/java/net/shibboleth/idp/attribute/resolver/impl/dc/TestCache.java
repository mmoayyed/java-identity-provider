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

package net.shibboleth.idp.attribute.resolver.impl.dc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;

import net.shibboleth.idp.attribute.Attribute;

/**
 * Simple cache implementation backed by a hash map.
 */
public class TestCache implements Cache<String, Optional<Map<String, Attribute>>>, Iterable<Optional<Map<String, Attribute>>> {

    /** Hash map cache. */
    private final Map<String, Optional<Map<String, Attribute>>> cache =
            new HashMap<String, Optional<Map<String, Attribute>>>();

    /** {@inheritDoc} */
    @Nullable public Optional<Map<String, Attribute>> getIfPresent(Object key) {
        return cache.get(key);
    }

    /** {@inheritDoc} */
    public Optional<Map<String, Attribute>> get(String key,
            Callable<? extends Optional<Map<String, Attribute>>> valueLoader) throws ExecutionException {
        Optional<Map<String, Attribute>> value = cache.get(key);
        if (value == null) {
            try {
                value = valueLoader.call();
                cache.put(key, value);
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }
        return value;
    }

    /** {@inheritDoc} */
    public ImmutableMap<String, Optional<Map<String, Attribute>>> getAllPresent(Iterable<?> keys) {
        throw new UnsupportedOperationException("Method not implemented");
    }

    /** {@inheritDoc} */
    public void put(String key, Optional<Map<String, Attribute>> value) {
        cache.put(key, value);
    }

    /** {@inheritDoc} */
    public void putAll(Map<? extends String, ? extends Optional<Map<String, Attribute>>> m) {
        cache.putAll(m);
    }

    /** {@inheritDoc} */
    public void invalidate(Object key) {
        cache.remove(key);

    }

    /** {@inheritDoc} */
    public void invalidateAll(Iterable<?> keys) {
        throw new UnsupportedOperationException("Method not implemented");
    }

    /** {@inheritDoc} */
    public void invalidateAll() {
        cache.clear();

    }

    /** {@inheritDoc} */
    public long size() {
        return cache.size();
    }

    /** {@inheritDoc} */
    public CacheStats stats() {
        throw new UnsupportedOperationException("Method not implemented");
    }

    /** {@inheritDoc} */
    public ConcurrentMap<String, Optional<Map<String, Attribute>>> asMap() {
        throw new UnsupportedOperationException("Method not implemented");
    }

    /** {@inheritDoc} */
    public void cleanUp() {
    }

    /** {@inheritDoc} */
    public Iterator<Optional<Map<String, Attribute>>> iterator() {
        return cache.values().iterator();
    }

}

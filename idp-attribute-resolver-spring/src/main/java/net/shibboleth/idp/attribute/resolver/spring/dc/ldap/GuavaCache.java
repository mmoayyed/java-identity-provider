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

package net.shibboleth.idp.attribute.resolver.spring.dc.ldap;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.cache.Cache;

import com.google.common.cache.CacheBuilder;

/**
 * Ldaptive cache implementation that leverages the Guava libraries. See {@link com.google.common.cache.CacheBuilder}.
 */
public class GuavaCache implements Cache<SearchRequest> {

    /** cache instance. */
    private final com.google.common.cache.Cache<SearchRequest, SearchResult> cache;

    /**
     * Creates a new guava cache.
     * 
     * @param size maximum size of the cache
     * @param timeToLive expiration time in seconds for items in the cache
     */
    public GuavaCache(final long size, final long timeToLive) {
        cache = CacheBuilder.newBuilder().maximumSize(size).expireAfterWrite(timeToLive, TimeUnit.SECONDS).build();
    }

    /** {@inheritDoc} */
    @Nullable public SearchResult get(@Nullable final SearchRequest request) {
        return cache.getIfPresent(request);
    }

    /** {@inheritDoc} */
    public void put(@Nonnull final SearchRequest request, @Nonnull final SearchResult result) {
        cache.put(request, result);
    }
}

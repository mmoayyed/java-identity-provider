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

package net.shibboleth.idp.tou.storage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Resource;

import net.shibboleth.idp.storage.Cache;
import net.shibboleth.idp.storage.CacheManager;
import net.shibboleth.idp.storage.Configuration;
import net.shibboleth.idp.tou.TOUAcceptance;

import org.springframework.util.Assert;

/** Cache implementation. */
public class CacheStorage implements Storage {

    /** The cache manager. */
    @Resource(name = "cacheManager")
    private CacheManager cacheManager;

    /**
     * The terms of use acceptance partition.
     * 
     * Key: userId. Value: Map Key: version. Value: {@link TOUAcceptance}.
     */
    private ConcurrentMap<String, ConcurrentMap<String, TOUAcceptance>> touAcceptancePartition;

    /** Initializes the cache storage. */
    public void initialize() {
        Cache<String, ConcurrentMap> cache = cacheManager.getCache("tou");

        if (cache == null) {
            cacheManager.defineConfiguration("tou", new Configuration());
            cache = cacheManager.getCache("tou");
        }

        cache.putIfAbsent("touAcceptancePartition",
                new ConcurrentHashMap<String, ConcurrentMap<String, TOUAcceptance>>());
        touAcceptancePartition = cache.get("touAcceptancePartition");
    }

    /** {@inheritDoc} */
    @Override
    public void createToUAcceptance(final String userId, final TOUAcceptance touAcceptance) {
        touAcceptancePartition.putIfAbsent(userId, new ConcurrentHashMap<String, TOUAcceptance>());
        touAcceptancePartition.get(userId).put(touAcceptance.getVersion(), touAcceptance);
    }

    /** {@inheritDoc} */
    @Override
    public void updateToUAcceptance(final String userId, final TOUAcceptance touAcceptance) {
        Assert.state(touAcceptancePartition.containsKey(userId));
        touAcceptancePartition.get(userId).replace(touAcceptance.getVersion(), touAcceptance);
    }

    /** {@inheritDoc} */
    @Override
    public TOUAcceptance readToUAcceptance(final String userId, final String version) {
        if (!touAcceptancePartition.containsKey(userId)) {
            return null;
        }
        return touAcceptancePartition.get(userId).get(version);
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsToUAcceptance(final String userId, final String version) {
        if (!touAcceptancePartition.containsKey(userId)) {
            return false;
        }
        return touAcceptancePartition.get(userId).containsKey(version);
    }
}

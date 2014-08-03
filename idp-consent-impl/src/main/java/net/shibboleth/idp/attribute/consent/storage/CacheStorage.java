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

package net.shibboleth.idp.attribute.consent.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Resource;

import net.shibboleth.idp.attribute.consent.AttributeRelease;
import net.shibboleth.idp.attribute.consent.User;
import net.shibboleth.idp.storage.Cache;
import net.shibboleth.idp.storage.CacheManager;
import net.shibboleth.idp.storage.Configuration;

import org.springframework.util.Assert;

/** Cache implementation. */
public class CacheStorage implements Storage {

    /** The cache manager. */
    @Resource(name = "cacheManager")
    private CacheManager cacheManager;

    /**
     * The user partition.
     * 
     * Key: userId. Value: {@see User}.
     */
    private ConcurrentMap<String, User> userPartition;

    /**
     * The attribute release partition.
     * 
     * Key: userId Value: Map Key: relyingPartyId Value: Map Key: attributeId Value: {@see AttributeRelease}.
     * 
     */
    private ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, AttributeRelease>>>
    attributeReleasePartition;

    /** Initializes the cache storage. */
    public void initialize() {
        Cache<String, ConcurrentMap> cache = cacheManager.getCache("consent");

        if (cache == null) {
            cacheManager.defineConfiguration("consent", new Configuration());
            cache = cacheManager.getCache("consent");
        }

        cache.putIfAbsent("userPartition", new ConcurrentHashMap<String, User>());
        cache.putIfAbsent("attributeReleasePartition",
                new ConcurrentHashMap<String, ConcurrentMap<String, ConcurrentMap<String, AttributeRelease>>>());

        userPartition = cache.get("userPartition");
        attributeReleasePartition = cache.get("attributeReleasePartition");
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsUser(String userId) {
        return userPartition.containsKey(userId);
    }

    /** {@inheritDoc} */
    @Override
    public User readUser(String userId) {
        return userPartition.get(userId);
    }

    /** {@inheritDoc} */
    @Override
    public void updateUser(User user) {
        userPartition.replace(user.getId(), user);
    }

    /** {@inheritDoc} */
    @Override
    public void createUser(User user) {
        userPartition.put(user.getId(), user);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<AttributeRelease> readAttributeReleases(String userId, String relyingPartyId) {
        if (!attributeReleasePartition.containsKey(userId)) {
            return Collections.EMPTY_SET;
        }

        if (!attributeReleasePartition.get(userId).containsKey(relyingPartyId)) {
            return Collections.EMPTY_SET;
        }

        return attributeReleasePartition.get(userId).get(relyingPartyId).values();
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAttributeReleases(String userId, String relyingPartyId) {
        if (attributeReleasePartition.containsKey(userId)) {
            attributeReleasePartition.get(userId).remove(relyingPartyId);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsAttributeRelease(String userId, String relyingPartyId, String attributeId) {
        if (!attributeReleasePartition.containsKey(userId)) {
            return false;
        }

        if (!attributeReleasePartition.get(userId).containsKey(relyingPartyId)) {
            return false;
        }

        return attributeReleasePartition.get(userId).get(relyingPartyId).containsKey(attributeId);
    }

    /** {@inheritDoc} */
    @Override
    public void updateAttributeRelease(String userId, String relyingPartyId, AttributeRelease attributeRelease) {
        Assert.state(attributeReleasePartition.containsKey(userId));
        Assert.state(attributeReleasePartition.get(userId).containsKey(relyingPartyId));
        attributeReleasePartition.get(userId).get(relyingPartyId)
                .replace(attributeRelease.getAttributeId(), attributeRelease);
    }

    /** {@inheritDoc} */
    @Override
    public void createAttributeRelease(String userId, String relyingPartyId, AttributeRelease attributeRelease) {
        attributeReleasePartition.putIfAbsent(userId,
                new ConcurrentHashMap<String, ConcurrentMap<String, AttributeRelease>>());
        attributeReleasePartition.get(userId).putIfAbsent(relyingPartyId,
                new ConcurrentHashMap<String, AttributeRelease>());
        attributeReleasePartition.get(userId).get(relyingPartyId)
                .put(attributeRelease.getAttributeId(), attributeRelease);
    }

}

/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.idp.consent.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Resource;

import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.manager.CacheManager;
import org.springframework.util.Assert;

import edu.internet2.middleware.shibboleth.idp.consent.AttributeRelease;
import edu.internet2.middleware.shibboleth.idp.consent.User;


/**
 *
 */
public class CacheStorage implements Storage {
        
    @Resource(name="cacheManager")
    private CacheManager cacheManager;
    
    // userId: user
    private ConcurrentMap<String, User> userPartition;
    // userId: relyingPartyId: attributeId: attributeRelease
    private ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, AttributeRelease>>> attributeReleasePartition;
    
    public void initialize() {      
        Cache<String, ConcurrentMap> cache = cacheManager.getCache("consent");
        
        if (cache == null) {
            cacheManager.defineConfiguration("consent", new Configuration());
            cache = cacheManager.getCache("consent");
        }
        
        cache.putIfAbsent("userPartition", new ConcurrentHashMap<String, User>());
        cache.putIfAbsent("attributeReleasePartition",
                new ConcurrentHashMap<String, ConcurrentMap<String, ConcurrentMap<String, AttributeRelease>>>());   
        
        userPartition = (ConcurrentMap<String, User>) cache.get("userPartition");
        attributeReleasePartition = (ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, AttributeRelease>>>) cache.get("attributeReleasePartition");    
    }
    
    /** {@inheritDoc} */
    public boolean containsUser(String userId) {
        return userPartition.containsKey(userId);
    }

    /** {@inheritDoc} */
    public User readUser(String userId) {
        return userPartition.get(userId);
    }

    /** {@inheritDoc} */
    public void updateUser(User user) {
        userPartition.replace(user.getId(), user);
    }

    /** {@inheritDoc} */
    public void createUser(User user) {
        userPartition.put(user.getId(), user);
    }

    /** {@inheritDoc} */
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
    public void deleteAttributeReleases(String userId, String relyingPartyId) {
        if (attributeReleasePartition.containsKey(userId)) {
            attributeReleasePartition.get(userId).remove(relyingPartyId);
        }
    }

    /** {@inheritDoc} */
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
    public void updateAttributeRelease(String userId, String relyingPartyId, AttributeRelease attributeRelease) {
        Assert.state(attributeReleasePartition.containsKey(userId));
        Assert.state(attributeReleasePartition.get(userId).containsKey(relyingPartyId));
        attributeReleasePartition.get(userId).get(relyingPartyId).replace(attributeRelease.getAttributeId(), attributeRelease);      
    }

    /** {@inheritDoc} */
    public void createAttributeRelease(String userId, String relyingPartyId, AttributeRelease attributeRelease) {
        attributeReleasePartition.putIfAbsent(userId, new ConcurrentHashMap<String, ConcurrentMap<String, AttributeRelease>>());        
        attributeReleasePartition.get(userId).putIfAbsent(relyingPartyId, new ConcurrentHashMap<String, AttributeRelease>());        
        attributeReleasePartition.get(userId).get(relyingPartyId).put(attributeRelease.getAttributeId(), attributeRelease);
    }

}

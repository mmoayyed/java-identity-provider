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

package edu.internet2.middleware.shibboleth.idp.tou.storage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Resource;

import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.manager.CacheManager;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.tou.ToU;

/**
 *
 */
public class CacheStorage implements Storage {
    
    private final Logger logger = LoggerFactory.getLogger(CacheStorage.class);
    
    @Resource(name="cacheManager")
    private CacheManager cacheManager;
    
    private ConcurrentMap<String, ConcurrentMap<ToU, DateTime>> acceptedToUPartition;
    
    public void initialize() {      
        Cache<String, ConcurrentMap> cache = cacheManager.getCache("tou");
        
        if (cache == null) {
            cacheManager.defineConfiguration("tou", new Configuration());
            cache = cacheManager.getCache("tou");
        }
        
        cache.putIfAbsent("AcceptedToUPartition", new ConcurrentHashMap<String, ConcurrentMap<ToU, DateTime>>());
        acceptedToUPartition = (ConcurrentMap<String, ConcurrentMap<ToU, DateTime>>) cache.get("AcceptedToUPartition");
    }
    
    /** {@inheritDoc} */
    public void createAcceptedToU(String userId, ToU tou, DateTime acceptanceDate) {
        acceptedToUPartition.putIfAbsent(userId, new ConcurrentHashMap<ToU, DateTime>());
        
        if (acceptedToUPartition.get(userId).containsKey(tou)) {
            logger.warn("AcceptedToU already exists, update with new ToU");
        } 
        acceptedToUPartition.get(userId).put(tou, acceptanceDate);        
    }

    /** {@inheritDoc} */
    public boolean containsAcceptedToU(String userId, ToU tou) {
        if (!acceptedToUPartition.containsKey(userId)) {
            return false;
        }
        return acceptedToUPartition.get(userId).containsKey(tou);
    }

}

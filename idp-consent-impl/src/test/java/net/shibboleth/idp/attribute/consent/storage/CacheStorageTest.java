package net.shibboleth.idp.attribute.consent.storage;
/*
 * Copyright 2009 University Corporation for Advanced Internet Development, Inc.
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



import java.util.concurrent.ConcurrentMap;

import javax.annotation.Resource;

import net.shibboleth.idp.attribute.consent.storage.CacheStorage;

import org.infinispan.Cache;
import org.infinispan.manager.CacheManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests the cache storage.
 */
public class CacheStorageTest extends AbstractStorageTest {
    
    @Resource(name="consent.storage.cache")
    private CacheStorage cacheStorage;
    
    @Resource(name="cacheManager")
    private CacheManager cacheManager;
    
    private Cache<String, ConcurrentMap> cache;
    
    public void setup() {
       cache = cacheManager.getCache("consent");
    }
    
    @BeforeMethod
    public void clear() {
        if (cache != null) {
            cache.clear();
        }
        cacheStorage.initialize();
    }
    
    /** {@inheritDoc} */
    @Test(dependsOnMethods={"setup"})
    public void initialization() {
        setStorage(cacheStorage);
    }
    
}

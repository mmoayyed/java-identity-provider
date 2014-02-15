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

import net.shibboleth.idp.storage.Cache;
import net.shibboleth.idp.storage.CacheManager;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests the cache storage.
 */
public class CacheStorageTest extends AbstractStorageTest {

    @Resource(name = "tou.storage.cache")
    private CacheStorage cacheStorage;

    @Resource(name = "cacheManager")
    private CacheManager cacheManager;

    private Cache<String, ConcurrentMap> cache;

    public void setup() {
        if (cacheStorage != null) {
            cache = cacheManager.getCache("tou");
        }
    }

    @BeforeMethod
    public void clear() {
        if (cache != null) {
            cache.clear();
        }
        if (cacheStorage != null) {
            cacheStorage.initialize();
        }
    }

    /** {@inheritDoc} */
    @Override
    @Test(dependsOnMethods = {"setup"}, enabled=false)
    public void initialization() {
        setStorage(cacheStorage);
    }

}

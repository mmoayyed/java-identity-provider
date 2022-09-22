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

package net.shibboleth.idp.authn.revocation.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

import org.opensaml.storage.impl.MemoryStorageService;
import org.opensaml.storage.impl.StorageServiceRevocationCache;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link RevocationCacheCondition} unit test. */
public class RevocationCacheConditionTest extends BaseAuthenticationContextTest {
    
    private MemoryStorageService storageService;
    private StorageServiceRevocationCache revocationCache;
    private RevocationCacheCondition condition; 

    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        storageService = new MemoryStorageService();
        storageService.setId("test");
        storageService.setCleanupInterval(Duration.ZERO);
        storageService.initialize();
        
        revocationCache = new StorageServiceRevocationCache();
        revocationCache.setStorage(storageService);
        revocationCache.setId("test");
        revocationCache.initialize();

        condition = new RevocationCacheCondition();
        condition.setRevocationCache(revocationCache);
        condition.setPrincipalNameLookupStrategy(FunctionSupport.constant("jdoe"));
        condition.initialize();
        
        authenticationFlows.get(1).setRevocationCondition(condition);
    }
    
    @AfterMethod
    public void tearDown() {
        condition.destroy();
        revocationCache.destroy();
        storageService.destroy();
    }
    
    
    @Test public void testNotRevoked() {
        final AuthenticationResult active = authenticationFlows.get(1).newAuthenticationResult(new Subject());
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active));

        Assert.assertTrue(active.test(prc));
    }
    
    @Test public void testRevoked() {
        final AuthenticationResult active = authenticationFlows.get(1).newAuthenticationResult(new Subject());
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active));

        revocationCache.revoke(RevocationCacheCondition.REVOCATION_CONTEXT,
                RevocationCacheCondition.PRINCIPAL_REVOCATION_PREFIX + "jdoe",
                Long.toString(Instant.now().getEpochSecond() + 3600L),
                Duration.ofDays(1));
        
        Assert.assertFalse(active.test(prc));
    }

    @Test public void testPastRevoked() {
        final AuthenticationResult active = authenticationFlows.get(1).newAuthenticationResult(new Subject());
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active));

        revocationCache.revoke(RevocationCacheCondition.REVOCATION_CONTEXT,
                RevocationCacheCondition.PRINCIPAL_REVOCATION_PREFIX + "jdoe",
                Long.toString(Instant.now().getEpochSecond() - 3600L),
                Duration.ofDays(1));
        
        Assert.assertTrue(active.test(prc));
    }

}
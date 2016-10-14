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

package net.shibboleth.idp.authn.impl;


import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.idp.authn.impl.StorageBackedAccountLockoutManager.UsernameIPLockoutKeyStrategy;

import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link StorageBackedAccountLockoutManager} unit test. */
public class StorageBackedAccountLockoutManagerTest extends BaseAuthenticationContextTest {

    private StorageBackedAccountLockoutManager manager;    

    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        final MemoryStorageService ss = new MemoryStorageService();
        ss.setId("test");
        ss.setCleanupInterval(0);
        ss.initialize();
        
        final UsernameIPLockoutKeyStrategy keyStrategy = new UsernameIPLockoutKeyStrategy();
        keyStrategy.setHttpServletRequest((HttpServletRequest) src.getExternalContext().getNativeRequest());
        manager = new StorageBackedAccountLockoutManager();
        manager.setId("test");
        manager.setStorageService(ss);
        manager.setLockoutKeyStrategy(keyStrategy);
        manager.setMaxAttempts(2);
        manager.setCounterInterval(3000);
        manager.setLockoutDuration(5000);
        manager.initialize();
        
        ((MockHttpServletRequest) src.getExternalContext().getNativeRequest()).setRemoteAddr("192.168.1.1");
        prc.getSubcontext(AuthenticationContext.class).getSubcontext(UsernamePasswordContext.class, true).setUsername("jdoe");
    }

    @Test public void noKey() {
        prc.getSubcontext(AuthenticationContext.class).clearSubcontexts();
        Assert.assertFalse(manager.check(prc));
        Assert.assertFalse(manager.increment(prc));
        Assert.assertFalse(manager.clear(prc));
    }
    
    @Test public void one() {
        Assert.assertFalse(manager.check(prc));
        Assert.assertTrue(manager.increment(prc));
        Assert.assertFalse(manager.check(prc));
        Assert.assertTrue(manager.clear(prc));
    }
    
    @Test public void two() {
        Assert.assertTrue(manager.increment(prc));
        Assert.assertTrue(manager.increment(prc));
        Assert.assertFalse(manager.check(prc));
        Assert.assertTrue(manager.clear(prc));
    }
    
    @Test public void threeSlow() throws InterruptedException {
        Assert.assertTrue(manager.increment(prc));
        Assert.assertTrue(manager.increment(prc));
        Thread.sleep(4000);
        Assert.assertTrue(manager.increment(prc));
        Assert.assertFalse(manager.check(prc));
        Assert.assertTrue(manager.clear(prc));
    }

    @Test public void threeFast() throws InterruptedException {
        Assert.assertTrue(manager.increment(prc));
        Assert.assertTrue(manager.increment(prc));
        Assert.assertTrue(manager.increment(prc));
        Assert.assertTrue(manager.check(prc));
        Thread.sleep(2000);
        Assert.assertTrue(manager.check(prc));
    }

    @Test public void waitForUnlock() throws InterruptedException {
        Assert.assertTrue(manager.increment(prc));
        Assert.assertTrue(manager.increment(prc));
        Assert.assertTrue(manager.increment(prc));
        Assert.assertTrue(manager.check(prc));
        Thread.sleep(5000);
        Assert.assertFalse(manager.check(prc));
    }

}
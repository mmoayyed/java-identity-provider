/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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


import java.time.Duration;

import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.idp.authn.impl.StorageBackedAccountLockoutManager.UsernameIPLockoutKeyStrategy;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.ConstantSupplier;

/** {@link StorageBackedAccountLockoutManager} unit test. */
public class StorageBackedAccountLockoutManagerTest extends BaseAuthenticationContextTest {

    private StorageBackedAccountLockoutManager manager;    

    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        final MemoryStorageService ss = new MemoryStorageService();
        ss.setId("test");
        ss.initialize();
        
        final UsernameIPLockoutKeyStrategy keyStrategy = new UsernameIPLockoutKeyStrategy();
        final HttpServletRequest request = (HttpServletRequest) src.getExternalContext().getNativeRequest();
        keyStrategy.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
        manager = new StorageBackedAccountLockoutManager();
        manager.setId("test");
        manager.setStorageService(ss);
        manager.setLockoutKeyStrategy(keyStrategy);
        manager.setMaxAttempts(3);
        manager.setCounterInterval(Duration.ofSeconds(3));
        manager.setLockoutDuration(Duration.ofSeconds(5));
        manager.initialize();
        
        ((MockHttpServletRequest) src.getExternalContext().getNativeRequest()).setRemoteAddr("192.168.1.1");
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        authCtx.ensureSubcontext(UsernamePasswordContext.class).setUsername("jdoe");
    }

    @Test public void noKey() { 
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;

        authCtx.clearSubcontexts();
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
        Thread.sleep(4000);
        Assert.assertTrue(manager.check(prc));
        Thread.sleep(1150);
        Assert.assertFalse(manager.check(prc));
    }

}

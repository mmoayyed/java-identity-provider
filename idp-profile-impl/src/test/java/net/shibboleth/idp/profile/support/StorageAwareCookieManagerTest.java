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

package net.shibboleth.idp.profile.support;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.primitive.NonnullSupplier;

/** {@link CookieManager} unit test. */
@SuppressWarnings("javadoc")
public class StorageAwareCookieManagerTest {
    
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    
    private MemoryStorageService storage;
    
    private StorageAwareCookieManager cm;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        request = new MockHttpServletRequest();
        request.setAttribute(ProfileRequestContext.BINDING_KEY, new RequestContextBuilder().buildProfileRequestContext());
        response = new MockHttpServletResponse();
        
        storage = new MemoryStorageService();
        storage.setId("test");
        storage.setCleanupInterval(Duration.ZERO);
        storage.initialize();
        
        cm = new StorageAwareCookieManager();
        cm.setHttpServletRequestSupplier(new NonnullSupplier<>() { @Nonnull public HttpServletRequest get() {return request;}});
        cm.setHttpServletResponseSupplier(new NonnullSupplier<>() { @Nonnull public HttpServletResponse get() {return response;}});
        cm.setCookiePath("/idp");
        cm.setStorageService(storage);
        cm.setUsernameLookupStrategy(FunctionSupport.constant("jdoe"));
        cm.setMaxAge(600);
        cm.initialize();
    }
    
    @Test public void testInitFailure() {
        final StorageAwareCookieManager cm = new StorageAwareCookieManager();
        try {
            cm.initialize();
            Assert.fail();
        } catch (final ComponentInitializationException e) {
            
        }
    }
    @Test public void testCookieWithPath() throws ComponentInitializationException, IOException {

        cm.addCookie("foo", "bar");

        final Cookie cookie = response.getCookie("foo");
        assert(cookie != null);
        Assert.assertEquals(cookie.getValue(), "bar");
        Assert.assertEquals(cookie.getPath(), "/idp");
        Assert.assertNull(cookie.getDomain());
        Assert.assertTrue(cookie.getSecure());
        Assert.assertEquals(cookie.getMaxAge(), 600);
        
        final StorageRecord<String> record = storage.read(cm.getStorageContext(), "foo!jdoe");
        assert record != null;
        Assert.assertEquals(record.getVersion(), 1);
        Assert.assertEquals(record.getValue(), "bar");
    }

    @Test public void testCookieNoPath() throws ComponentInitializationException, IOException {
        request.setContextPath("/idp");
        
        cm.addCookie("foo", "bar");
        
        final Cookie cookie = response.getCookie("foo");
        assert(cookie != null);
        Assert.assertEquals(cookie.getValue(), "bar");
        Assert.assertEquals(cookie.getPath(), "/idp");
        Assert.assertNull(cookie.getDomain());
        Assert.assertTrue(cookie.getSecure());
        Assert.assertEquals(cookie.getMaxAge(), 600);
        
        final StorageRecord<String> record = storage.read(cm.getStorageContext(), "foo!jdoe");
        assert record != null;
        Assert.assertEquals(record.getVersion(), 1);
        Assert.assertEquals(record.getValue(), "bar");
    }

    @Test public void testCookieUnset() throws ComponentInitializationException, IOException {
        request.setContextPath("/idp");
        request.setCookies(new Cookie("foo", "bar"));

        cm.unsetCookie("foo");
        
        final Cookie cookie = response.getCookie("foo");
        assert(cookie != null);
        Assert.assertNull(cookie.getValue());
        Assert.assertEquals(cookie.getPath(), "/idp");
        Assert.assertNull(cookie.getDomain());
        Assert.assertTrue(cookie.getSecure());
        Assert.assertEquals(cookie.getMaxAge(), 0);

        final StorageRecord<String> record = storage.read(cm.getStorageContext(), "foo!jdoe");
        Assert.assertNull(record);
    }

    @Test public void testCookieRestore() throws ComponentInitializationException, IOException {
        request.setContextPath("/idp");
        
        storage.create(cm.getStorageContext(), "foo!jdoe", "bar", Instant.now().plusSeconds(600).toEpochMilli());
        
        Assert.assertEquals(cm.getCookieValue("foo", "baz"), "bar");
        
        final Cookie cookie = response.getCookie("foo");
        assert(cookie != null);
        Assert.assertEquals(cookie.getValue(), "bar");
        Assert.assertEquals(cookie.getPath(), "/idp");
        Assert.assertNull(cookie.getDomain());
        Assert.assertTrue(cookie.getSecure());
        Assert.assertTrue(cookie.getMaxAge() <= 600);
    }
    
}
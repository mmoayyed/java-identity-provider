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

package net.shibboleth.idp.session.impl;

import java.util.Arrays;
import java.util.Collection;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.TestPrincipal;
import net.shibboleth.idp.authn.UsernamePrincipal;
import net.shibboleth.idp.session.BasicServiceSession;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.ServiceSession;
import net.shibboleth.idp.session.ServiceSessionSerializerRegistry;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.criterion.SessionIdCriterion;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.security.SecureRandomIdentifierGenerationStrategy;

import org.opensaml.storage.impl.MemoryStorageService;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** {@link StorageBackedSessionManager} unit test. */
public class StorageBackedSessionManagerTest {

    private static final long sessionSlop = 60 * 5 * 1000;
    
    private static MemoryStorageService storageService;
    
    private static StorageBackedSessionManager manager;

    private static Collection<AuthenticationFlowDescriptor> flowDescriptors;
    
    private static ServiceSessionSerializerRegistry serializerRegistry;
    
    @BeforeClass public static void setUp() throws ComponentInitializationException {
        storageService = new MemoryStorageService();
        storageService.initialize();

        serializerRegistry = new ServiceSessionSerializerRegistry();
        serializerRegistry.register(BasicServiceSession.class, new BasicServiceSessionSerializer(sessionSlop));
        
        AuthenticationFlowDescriptor foo = new AuthenticationFlowDescriptor("AuthenticationFlow/Foo");
        foo.setLifetime(60 * 1000);
        foo.setInactivityTimeout(60 * 1000);
        AuthenticationFlowDescriptor bar = new AuthenticationFlowDescriptor("AuthenticationFlow/Bar");
        bar.setLifetime(60 * 1000);
        bar.setInactivityTimeout(60 * 1000);
        flowDescriptors = Arrays.asList(foo, bar);
        
        manager = new StorageBackedSessionManager();
        manager.setAuthenticationFlowDescriptors(flowDescriptors);
        manager.setTrackServiceSessions(true);
        manager.setSecondaryServiceIndex(true);
        manager.setStorageService(storageService);
        manager.setSessionSlop(sessionSlop);
        manager.setIDGenerator(new SecureRandomIdentifierGenerationStrategy());
        manager.setServiceSessionSerializerRegistry(serializerRegistry);
        manager.initialize();
    }
    
    @AfterClass public static void tearDown() {
        manager.destroy();
        storageService.destroy();
    }

    @Test(threadPoolSize = 10, invocationCount = 10,  timeOut = 10000)
    public void testSimpleSession() throws ResolverException, SessionException, InterruptedException {
        
        // Test a failed lookup.
        Assert.assertNull(manager.resolveSingle(new CriteriaSet(new SessionIdCriterion("test"))));
        
        // Username should be required.
        try {
            manager.createSession(null, null);
            Assert.fail("A null username should not have worked");
        } catch (ConstraintViolationException e) {
            
        }
        
        // Test basic session content.
        IdPSession session = manager.createSession("joe", null);
        Assert.assertTrue(session.getCreationInstant() <= System.currentTimeMillis());
        Assert.assertEquals(session.getCreationInstant(), session.getLastActivityInstant());
        Assert.assertEquals(session.getPrincipalName(), "joe");
        Assert.assertTrue(session.getAuthenticationResults().isEmpty());
        Assert.assertTrue(session.getServiceSessions().isEmpty());
        
        Thread.sleep(1000);
        
        // checkTimeout should update the last activity time.
        session.checkTimeout();
        Assert.assertNotEquals(session.getCreationInstant(), session.getLastActivityInstant());

        // Do a lookup and compare the results.
        long creation = session.getCreationInstant();
        long lastActivity = session.getLastActivityInstant();
        String sessionId = session.getId();
        session = manager.resolveSingle(new CriteriaSet(new SessionIdCriterion(sessionId)));
        Assert.assertNotNull(session);
        Assert.assertEquals(session.getPrincipalName(), "joe");
        Assert.assertEquals(session.getCreationInstant(), creation);
        Assert.assertEquals(session.getLastActivityInstant(), lastActivity);
        
        // Test a destroy and a failed lookup.
        manager.destroySession(sessionId);
        Assert.assertNull(manager.resolveSingle(new CriteriaSet(new SessionIdCriterion(sessionId))));
    }
    
    @Test
    public void testAddress() throws SessionException, ResolverException {
        
        // Interleave checks of addresses of the two types.
        IdPSession session = manager.createSession("joe", "192.168.1.1");
        Assert.assertTrue(session.checkAddress("192.168.1.1"));
        Assert.assertFalse(session.checkAddress("192.168.1.2"));
        Assert.assertTrue(session.checkAddress("fe80::ca2a:14ff:fe2a:3e04"));
        Assert.assertTrue(session.checkAddress("fe80::ca2a:14ff:fe2a:3e04"));
        Assert.assertFalse(session.checkAddress("fe80::ca2a:14ff:fe2a:3e05"));
        Assert.assertTrue(session.checkAddress("192.168.1.1"));
        
        // Try a bad address type.
        Assert.assertFalse(session.checkAddress("1,1,1,1"));
        
        // Interleave manipulation of a session between two copies to check for resync.
        IdPSession one = manager.createSession("joe", null);
        IdPSession two = manager.resolveSingle(new CriteriaSet(new SessionIdCriterion(one.getId())));
        
        Assert.assertTrue(one.checkAddress("192.168.1.1"));
        Assert.assertFalse(two.checkAddress("192.168.1.2"));
        Assert.assertTrue(two.checkAddress("fe80::ca2a:14ff:fe2a:3e04"));
        Assert.assertFalse(one.checkAddress("fe80::ca2a:14ff:fe2a:3e05"));
    }

    @Test(threadPoolSize = 10, invocationCount = 10,  timeOut = 10000)
    public void testAuthenticationResults() throws ResolverException, SessionException, InterruptedException {
        
        IdPSession session = manager.createSession("joe", null);
        Assert.assertTrue(session.getAuthenticationResults().isEmpty());

        // Add some results.
        AuthenticationResult foo = new AuthenticationResult("AuthenticationFlow/Foo", new UsernamePrincipal("joe"));
        foo.getSubject().getPrincipals().add(new TestPrincipal("test1"));
        AuthenticationResult bar = new AuthenticationResult("AuthenticationFlow/Bar", new UsernamePrincipal("joe"));
        bar.getSubject().getPrincipals().add(new TestPrincipal("test2"));
        AuthenticationResult baz = new AuthenticationResult("AuthenticationFlow/Baz", new UsernamePrincipal("joe"));

        Assert.assertNull(session.addAuthenticationResult(foo));
        Assert.assertNull(session.addAuthenticationResult(bar));
        try {
            session.addAuthenticationResult(baz);
            Assert.fail("An unserializable AuthenticationResult should not have worked");
        } catch (SessionException e) {
            
        }
        
        // Test various methods and removals.
        Assert.assertEquals(session.getAuthenticationResults().size(), 2);
        
        Assert.assertFalse(session.removeAuthenticationResult(baz));
        Assert.assertTrue(session.removeAuthenticationResult(bar));
        
        Assert.assertEquals(session.getAuthenticationResults().size(), 1);
        
        // Test access and compare to original.
        Assert.assertNull(session.getAuthenticationResult("AuthenticationFlow/Bar"));
        AuthenticationResult foo2 = session.getAuthenticationResult("AuthenticationFlow/Foo");
        Assert.assertNotNull(foo2);
        Assert.assertEquals(foo.getAuthenticationInstant(), foo2.getAuthenticationInstant());
        Assert.assertEquals(foo.getLastActivityInstant(), foo2.getLastActivityInstant());
        Assert.assertEquals(foo.getSubject(), foo2.getSubject());
        
        // Load from storage and re-test.
        IdPSession session2 = manager.resolveSingle(new CriteriaSet(new SessionIdCriterion(session.getId())));
        Assert.assertNull(session2.getAuthenticationResult("AuthenticationFlow/Bar"));
        foo2 = session2.getAuthenticationResult("AuthenticationFlow/Foo");
        Assert.assertNotNull(foo2);
        Assert.assertEquals(foo.getAuthenticationInstant(), foo2.getAuthenticationInstant());
        Assert.assertEquals(foo.getLastActivityInstant(), foo2.getLastActivityInstant());
        Assert.assertEquals(foo.getSubject(), foo2.getSubject());
    }
    
    @Test(threadPoolSize = 10, invocationCount = 10,  timeOut = 10000)
    public void testServiceSessions() throws ResolverException, SessionException, InterruptedException {
        
        IdPSession session = manager.createSession("joe", null);
        Assert.assertTrue(session.getServiceSessions().isEmpty());

        // Add some sessions.
        ServiceSession foo = new BasicServiceSession("https://sp.example.org/shibboleth", "AuthenticationFlow/Foo",
                System.currentTimeMillis(), System.currentTimeMillis() + 60 * 60 * 1000);
        ServiceSession bar = new BasicServiceSession("https://sp2.example.org/shibboleth", "AuthenticationFlow/Bar",
                System.currentTimeMillis(), System.currentTimeMillis() + 60 * 60 * 1000);

        Assert.assertNull(session.addServiceSession(foo));
        Assert.assertNull(session.addServiceSession(bar));
        
        // Test various methods and removals.
        Assert.assertEquals(session.getServiceSessions().size(), 2);
        
        Assert.assertTrue(session.removeServiceSession(bar));
        Assert.assertFalse(session.removeServiceSession(bar));
        
        Assert.assertEquals(session.getServiceSessions().size(), 1);
        
        // Test access and compare to original.
        Assert.assertNull(session.getServiceSession("https://sp2.example.org/shibboleth"));
        ServiceSession foo2 = session.getServiceSession("https://sp.example.org/shibboleth");
        Assert.assertNotNull(foo2);
        Assert.assertEquals(foo.getCreationInstant(), foo2.getCreationInstant());
        Assert.assertEquals(foo.getExpirationInstant(), foo2.getExpirationInstant());
        
        // Load from storage and re-test.
        IdPSession session2 = manager.resolveSingle(new CriteriaSet(new SessionIdCriterion(session.getId())));
        Assert.assertNull(session.getServiceSession("https://sp2.example.org/shibboleth"));
        foo2 = session2.getServiceSession("https://sp.example.org/shibboleth");
        Assert.assertNotNull(foo2);
        Assert.assertEquals(foo.getCreationInstant(), foo2.getCreationInstant());
        Assert.assertEquals(foo.getExpirationInstant(), foo2.getExpirationInstant());
    }
    
}
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

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.DefaultAuthenticationResultSerializer;
import net.shibboleth.idp.authn.UsernamePrincipal;
import net.shibboleth.idp.session.BasicServiceSession;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.ServiceSessionSerializerRegistry;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.criterion.SessionIdCriterion;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.security.SecureRandomIdentifierGenerationStrategy;

import org.opensaml.storage.StorageSerializer;
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

    private static ServiceSessionSerializerRegistry serializerRegistry;
    
    @BeforeClass public static void setUp() throws ComponentInitializationException {
        storageService = new MemoryStorageService();
        storageService.initialize();

        serializerRegistry = new ServiceSessionSerializerRegistry();
        serializerRegistry.register(BasicServiceSession.class, new BasicServiceSessionSerializer(sessionSlop));
        
        StorageSerializer<AuthenticationResult> resultSerializer = new DefaultAuthenticationResultSerializer(sessionSlop);
        AuthenticationFlowDescriptor foo = new AuthenticationFlowDescriptor("AuthenticationFlow/Foo");
        foo.setResultSerializer(resultSerializer);
        foo.setLifetime(60 * 1000);
        foo.setInactivityTimeout(60 * 1000);
        AuthenticationFlowDescriptor bar = new AuthenticationFlowDescriptor("AuthenticationFlow/Bar");
        foo.setResultSerializer(resultSerializer);
        foo.setLifetime(60 * 1000);
        foo.setInactivityTimeout(60 * 1000);
        
        manager = new StorageBackedSessionManager();
        manager.setAuthenticationFlowDescriptors(Arrays.asList(foo, bar));
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
        Assert.assertNull(manager.resolveSingle(new CriteriaSet(new SessionIdCriterion("test"))));
        
        try {
            manager.createSession(null, null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            
        }
        
        IdPSession session = manager.createSession("joe", null);
        Assert.assertTrue(session.getCreationInstant() <= System.currentTimeMillis());
        Assert.assertEquals(session.getCreationInstant(), session.getLastActivityInstant());
        Assert.assertEquals(session.getPrincipalName(), "joe");
        Assert.assertTrue(session.getAuthenticationResults().isEmpty());
        Assert.assertTrue(session.getServiceSessions().isEmpty());
        
        Thread.sleep(1000);
        
        session.checkTimeout();
        Assert.assertNotEquals(session.getCreationInstant(), session.getLastActivityInstant());

        long creation = session.getCreationInstant();
        long lastActivity = session.getLastActivityInstant();
        String sessionId = session.getId();
        session = manager.resolveSingle(new CriteriaSet(new SessionIdCriterion(sessionId)));
        Assert.assertNotNull(session);
        Assert.assertEquals(session.getPrincipalName(), "joe");
        Assert.assertEquals(session.getCreationInstant(), creation);
        Assert.assertEquals(session.getLastActivityInstant(), lastActivity);
        
        manager.destroySession(sessionId);
        Assert.assertNull(manager.resolveSingle(new CriteriaSet(new SessionIdCriterion(sessionId))));
    }
}
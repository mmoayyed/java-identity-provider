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

package net.shibboleth.idp.session;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.UsernamePrincipal;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link IdPSession} unit test. */
public class IdPSessionTest {

    /** Tests that everything is properly initialized during object construction. */
    @Test public void testInstantiation() throws Exception {
        long start = System.currentTimeMillis();
        Thread.sleep(50);

        IdPSession session = new IdPSession("test", new byte[] {0, 0, 0});
        Assert.assertNotNull(session.getAuthenticationResults());
        Assert.assertTrue(session.getAuthenticationResults().isEmpty());
        Assert.assertTrue(session.getCreationInstant() > start);
        Assert.assertEquals(session.getId(), "test");
        Assert.assertEquals(session.getLastActivityInstant(), session.getCreationInstant());
        Assert.assertTrue(session.getSecret().length == 3);
        Assert.assertNotNull(session.getServiceSessions());
        Assert.assertTrue(session.getServiceSessions().isEmpty());

        try {
            new IdPSession(null, new byte[] {0, 0, 0});
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }

        try {
            new IdPSession("", new byte[] {0, 0, 0});
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }

        try {
            new IdPSession("  ", new byte[] {0, 0, 0});
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }

        try {
            new IdPSession("test", null);
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }
    }

    /** Tests mutating the last activity instant. */
    @Test public void testLastActivityInstant() throws Exception {
        IdPSession session = new IdPSession("test", new byte[] {0, 0, 0});

        long now = System.currentTimeMillis();
        // this is here to allow the event's last activity time to deviate from the time 'now'
        Thread.sleep(50);

        session.setLastActivityInstantToNow();
        Assert.assertTrue(session.getLastActivityInstant() > now);

        session.setLastActivityInstant(now);
        Assert.assertEquals(session.getLastActivityInstant(), now);
    }

    /** Tests adding service sessions. */
    @Test public void testAddServiceSessions() {
        AuthenticationResult event1 = new AuthenticationResult("foo", new UsernamePrincipal("john"));
        AuthenticationResult event2 = new AuthenticationResult("bar", new UsernamePrincipal("john"));

        ServiceSession svcSession1 = new ServiceSession("svc1", event1);
        ServiceSession svcSession2 = new ServiceSession("svc2", event2);
        ServiceSession svcSession3 = new ServiceSession("svc3", event1);

        IdPSession session = new IdPSession("test", new byte[] {0, 0, 0});
        session.addServiceSession(svcSession1);
        Assert.assertEquals(session.getServiceSessions().size(), 1);
        Assert.assertTrue(session.getServiceSessions().contains(svcSession1));
        Assert.assertEquals(session.getServiceSession("svc1").get(), svcSession1);
        Assert.assertEquals(session.getAuthenticationResults().size(), 1);
        Assert.assertTrue(session.getAuthenticationResults().contains(event1));
        Assert.assertEquals(session.getAuthenticationResult("foo"), event1);

        session.addServiceSession(svcSession2);
        Assert.assertEquals(session.getServiceSessions().size(), 2);
        Assert.assertTrue(session.getServiceSessions().contains(svcSession1));
        Assert.assertEquals(session.getServiceSession("svc1").get(), svcSession1);
        Assert.assertTrue(session.getServiceSessions().contains(svcSession2));
        Assert.assertEquals(session.getServiceSession("svc2").get(), svcSession2);
        Assert.assertEquals(session.getAuthenticationResults().size(), 2);
        Assert.assertTrue(session.getAuthenticationResults().contains(event1));
        Assert.assertEquals(session.getAuthenticationResult("foo"), event1);
        Assert.assertTrue(session.getAuthenticationResults().contains(event2));
        Assert.assertEquals(session.getAuthenticationResult("bar"), event2);

        session.addServiceSession(svcSession3);
        Assert.assertEquals(session.getServiceSessions().size(), 3);
        Assert.assertTrue(session.getServiceSessions().contains(svcSession1));
        Assert.assertEquals(session.getServiceSession("svc1").get(), svcSession1);
        Assert.assertTrue(session.getServiceSessions().contains(svcSession2));
        Assert.assertEquals(session.getServiceSession("svc2").get(), svcSession2);
        Assert.assertTrue(session.getServiceSessions().contains(svcSession3));
        Assert.assertEquals(session.getServiceSession("svc3").get(), svcSession3);
        Assert.assertEquals(session.getAuthenticationResults().size(), 2);
        Assert.assertTrue(session.getAuthenticationResults().contains(event1));
        Assert.assertEquals(session.getAuthenticationResult("foo"), event1);
        Assert.assertTrue(session.getAuthenticationResults().contains(event2));
        Assert.assertEquals(session.getAuthenticationResult("bar"), event2);

        try {
            session.addServiceSession(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            Assert.assertEquals(session.getServiceSessions().size(), 3);
            Assert.assertTrue(session.getServiceSessions().contains(svcSession1));
            Assert.assertEquals(session.getServiceSession("svc1").get(), svcSession1);
            Assert.assertTrue(session.getServiceSessions().contains(svcSession2));
            Assert.assertEquals(session.getServiceSession("svc2").get(), svcSession2);
            Assert.assertTrue(session.getServiceSessions().contains(svcSession3));
            Assert.assertEquals(session.getServiceSession("svc3").get(), svcSession3);
            Assert.assertEquals(session.getAuthenticationResults().size(), 2);
            Assert.assertTrue(session.getAuthenticationResults().contains(event1));
            Assert.assertEquals(session.getAuthenticationResult("foo"), event1);
            Assert.assertTrue(session.getAuthenticationResults().contains(event2));
            Assert.assertEquals(session.getAuthenticationResult("bar"), event2);
        }

        try {
            session.addServiceSession(svcSession1);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            Assert.assertEquals(session.getServiceSessions().size(), 3);
            Assert.assertTrue(session.getServiceSessions().contains(svcSession1));
            Assert.assertEquals(session.getServiceSession("svc1").get(), svcSession1);
            Assert.assertTrue(session.getServiceSessions().contains(svcSession2));
            Assert.assertEquals(session.getServiceSession("svc2").get(), svcSession2);
            Assert.assertTrue(session.getServiceSessions().contains(svcSession3));
            Assert.assertEquals(session.getServiceSession("svc3").get(), svcSession3);
            Assert.assertEquals(session.getAuthenticationResults().size(), 2);
            Assert.assertTrue(session.getAuthenticationResults().contains(event1));
            Assert.assertEquals(session.getAuthenticationResult("foo"), event1);
            Assert.assertTrue(session.getAuthenticationResults().contains(event2));
            Assert.assertEquals(session.getAuthenticationResult("bar"), event2);
        }

    }

    /** Tests removing service sessions. */
    @Test public void testRemoveServiceSession() {
        AuthenticationResult event1 = new AuthenticationResult("foo", new UsernamePrincipal("john"));

        ServiceSession svcSession1 = new ServiceSession("svc1", event1);
        ServiceSession svcSession2 = new ServiceSession("svc2", event1);

        IdPSession session = new IdPSession("test", new byte[] {0, 0, 0});
        session.addServiceSession(svcSession1);
        session.addServiceSession(svcSession2);

        Assert.assertTrue(session.removeServiceSession(svcSession1));
        Assert.assertEquals(session.getServiceSessions().size(), 1);
        Assert.assertFalse(session.getServiceSessions().contains(svcSession1));
        Assert.assertTrue(session.getServiceSessions().contains(svcSession2));
        Assert.assertEquals(session.getServiceSession("svc2").get(), svcSession2);

        Assert.assertFalse(session.removeServiceSession(svcSession1));
        Assert.assertEquals(session.getServiceSessions().size(), 1);
        Assert.assertFalse(session.getServiceSessions().contains(svcSession1));
        Assert.assertTrue(session.getServiceSessions().contains(svcSession2));
        Assert.assertEquals(session.getServiceSession("svc2").get(), svcSession2);

        try {
            session.removeServiceSession(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            Assert.assertEquals(session.getServiceSessions().size(), 1);
            Assert.assertFalse(session.getServiceSessions().contains(svcSession1));
            Assert.assertTrue(session.getServiceSessions().contains(svcSession2));
            Assert.assertEquals(session.getServiceSession("svc2").get(), svcSession2);
        }
    }

    /** Tests remove authentication events. */
    @Test public void testRemoveAuthenticationEvent() {
        AuthenticationResult event1 = new AuthenticationResult("foo", new UsernamePrincipal("john"));
        AuthenticationResult event2 = new AuthenticationResult("bar", new UsernamePrincipal("john"));

        ServiceSession svcSession1 = new ServiceSession("svc1", event1);
        ServiceSession svcSession2 = new ServiceSession("svc2", event2);
        ServiceSession svcSession3 = new ServiceSession("svc3", event1);

        IdPSession session = new IdPSession("test", new byte[] {0, 0, 0});
        session.addServiceSession(svcSession1);
        session.addServiceSession(svcSession2);
        session.addServiceSession(svcSession3);

        session.removeServiceSession(svcSession2);
        Assert.assertEquals(session.getAuthenticationResults().size(), 2);
        Assert.assertTrue(session.getAuthenticationResults().contains(event1));
        Assert.assertEquals(session.getAuthenticationResult("foo"), event1);
        Assert.assertTrue(session.getAuthenticationResults().contains(event2));
        Assert.assertEquals(session.getAuthenticationResult("bar"), event2);

        session.removeAuthenticationEvent(event2);
        Assert.assertEquals(session.getAuthenticationResults().size(), 1);
        Assert.assertTrue(session.getAuthenticationResults().contains(event1));
        Assert.assertEquals(session.getAuthenticationResult("foo"), event1);

        session.removeAuthenticationEvent(event2);
        Assert.assertEquals(session.getAuthenticationResults().size(), 1);
        Assert.assertTrue(session.getAuthenticationResults().contains(event1));
        Assert.assertEquals(session.getAuthenticationResult("foo"), event1);

        try {
            session.removeAuthenticationEvent(event1);
            Assert.fail();
        } catch (IllegalStateException e) {
            Assert.assertEquals(session.getAuthenticationResults().size(), 1);
            Assert.assertTrue(session.getAuthenticationResults().contains(event1));
            Assert.assertEquals(session.getAuthenticationResult("foo"), event1);
        }

        try {
            session.removeAuthenticationEvent(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            Assert.assertEquals(session.getAuthenticationResults().size(), 1);
            Assert.assertTrue(session.getAuthenticationResults().contains(event1));
            Assert.assertEquals(session.getAuthenticationResult("foo"), event1);
        }
    }
}
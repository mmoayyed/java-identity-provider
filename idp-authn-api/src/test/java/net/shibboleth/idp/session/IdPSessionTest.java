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

import net.shibboleth.idp.authn.UsernamePrincipal;

import org.testng.Assert;
import org.testng.annotations.Test;

public class IdPSessionTest {

    @Test
    public void testInstantiation() throws Exception {
        long start = System.currentTimeMillis();
        Thread.sleep(50);

        IdPSession session = new IdPSession("test", new byte[] {0, 0, 0});
        Assert.assertNotNull(session.getAuthenticateEvents());
        Assert.assertTrue(session.getAuthenticateEvents().isEmpty());
        Assert.assertTrue(session.getCreationInstant() > start);
        Assert.assertEquals(session.getId(), "test");
        Assert.assertEquals(session.getLastActivityInstant(), session.getCreationInstant());
        Assert.assertTrue(session.getSecret().length == 3);
        Assert.assertNotNull(session.getServiceSessions());
        Assert.assertTrue(session.getServiceSessions().isEmpty());

        try {
            new IdPSession(null, new byte[] {0, 0, 0});
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }

        try {
            new IdPSession("", new byte[] {0, 0, 0});
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }

        try {
            new IdPSession("  ", new byte[] {0, 0, 0});
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }

        try {
            new IdPSession("test", null);
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testAuthenticationEvents() {
        AuthenticationEvent event1 = new AuthenticationEvent("foo", new UsernamePrincipal("test"));
        AuthenticationEvent event2 = new AuthenticationEvent("foo", new UsernamePrincipal("test"));
        AuthenticationEvent event3 = new AuthenticationEvent("bar", new UsernamePrincipal("test"));

        IdPSession session = new IdPSession("test", new byte[] {0, 0, 0});
        session.addAuthenticationEvent(event1);
        Assert.assertTrue(session.getAuthenticateEvents().size() == 1);
        Assert.assertTrue(session.getAuthenticateEvents().contains(event1));

        session.addAuthenticationEvent(event2);
        Assert.assertTrue(session.getAuthenticateEvents().size() == 1);
        Assert.assertTrue(session.getAuthenticateEvents().contains(event1));

        session.addAuthenticationEvent(event3);
        Assert.assertTrue(session.getAuthenticateEvents().size() == 2);
        Assert.assertTrue(session.getAuthenticateEvents().contains(event1));
        Assert.assertTrue(session.getAuthenticateEvents().contains(event3));

        session.addAuthenticationEvent(null);
        Assert.assertTrue(session.getAuthenticateEvents().size() == 2);
        Assert.assertTrue(session.getAuthenticateEvents().contains(event1));
        Assert.assertTrue(session.getAuthenticateEvents().contains(event3));

        session.removeAuthenticationEvent(event1);
        Assert.assertTrue(session.getAuthenticateEvents().size() == 1);
        Assert.assertFalse(session.getAuthenticateEvents().contains(event1));
        Assert.assertTrue(session.getAuthenticateEvents().contains(event3));

        session.removeAuthenticationEvent(null);
        Assert.assertTrue(session.getAuthenticateEvents().size() == 1);
        Assert.assertFalse(session.getAuthenticateEvents().contains(event1));
        Assert.assertTrue(session.getAuthenticateEvents().contains(event3));

        ServiceSession svcSession = new ServiceSession("example");
        svcSession.setAuthenticationEvent(event3);
        session.addServiceSession(svcSession);

        session.removeAuthenticationEvent(event3);
        Assert.assertTrue(session.getAuthenticateEvents().size() == 0);
        Assert.assertFalse(session.getAuthenticateEvents().contains(event1));
        Assert.assertFalse(session.getAuthenticateEvents().contains(event3));
        Assert.assertNull(svcSession.getAuthenticationEvent());
    }

    @Test
    public void testLastActivityInstant() throws Exception {
        IdPSession session = new IdPSession("test", new byte[] {0, 0, 0});

        long now = System.currentTimeMillis();
        // this is here to allow the event's last activity time to deviate from the 'now' time
        Thread.sleep(50);

        session.setLastActivityInstantToNow();
        Assert.assertTrue(session.getLastActivityInstant() > now);

        session.setLastActivityInstant(now);
        Assert.assertEquals(session.getLastActivityInstant(), now);
    }

    @Test
    public void testServiceSessions() {
        ServiceSession svcSession1 = new ServiceSession("foo");
        ServiceSession svcSession2 = new ServiceSession("foo");
        ServiceSession svcSession3 = new ServiceSession("bar");

        IdPSession session = new IdPSession("test", new byte[] {0, 0, 0});
        session.addServiceSession(svcSession1);
        Assert.assertTrue(session.getServiceSessions().size() == 1);
        Assert.assertTrue(session.getServiceSessions().contains(svcSession1));
        Assert.assertEquals(session.getServiceSession("foo"), svcSession1);

        session.addServiceSession(svcSession2);
        Assert.assertTrue(session.getServiceSessions().size() == 1);
        Assert.assertTrue(session.getServiceSessions().contains(svcSession1));
        Assert.assertEquals(session.getServiceSession("foo"), svcSession1);

        session.addServiceSession(svcSession3);
        Assert.assertTrue(session.getServiceSessions().size() == 2);
        Assert.assertTrue(session.getServiceSessions().contains(svcSession1));
        Assert.assertEquals(session.getServiceSession("foo"), svcSession1);
        Assert.assertTrue(session.getServiceSessions().contains(svcSession3));
        Assert.assertEquals(session.getServiceSession("bar"), svcSession3);

        session.addServiceSession(null);
        Assert.assertTrue(session.getServiceSessions().size() == 2);
        Assert.assertTrue(session.getServiceSessions().contains(svcSession1));
        Assert.assertEquals(session.getServiceSession("foo"), svcSession1);
        Assert.assertTrue(session.getServiceSessions().contains(svcSession3));
        Assert.assertEquals(session.getServiceSession("bar"), svcSession3);

        session.removeServiceSession(svcSession1);
        Assert.assertTrue(session.getServiceSessions().size() == 1);
        Assert.assertFalse(session.getServiceSessions().contains(svcSession1));
        Assert.assertNull(session.getServiceSession("foo"));
        Assert.assertTrue(session.getServiceSessions().contains(svcSession3));
        Assert.assertEquals(session.getServiceSession("bar"), svcSession3);
    }
}
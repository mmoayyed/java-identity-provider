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

package net.shibboleth.idp.session;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.shared.logic.ConstraintViolationException;

import java.time.Instant;

import javax.annotation.Nonnull;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link IdPSession} unit test. */
public class IdPSessionTest {

    /**
     * Tests that everything is properly initialized during object construction.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testInstantiation() throws Exception {
        final Instant start = Instant.now();
        Thread.sleep(50);

        AbstractIdPSession session = new DummyIdPSession("test", "foo");
        Assert.assertNotNull(session.getAuthenticationResults());
        Assert.assertFalse(session.getAuthenticationResults().iterator().hasNext());
        Assert.assertTrue(session.getCreationInstant().isAfter(start));
        Assert.assertEquals(session.getId(), "test");
        Assert.assertEquals(session.getPrincipalName(), "foo");
        Assert.assertEquals(session.getLastActivityInstant(), session.getCreationInstant());
        Assert.assertNotNull(session.getSPSessions());
        Assert.assertFalse(session.getSPSessions().iterator().hasNext());

        try {
            new DummyIdPSession("", "");
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }

        try {
            new DummyIdPSession("  ", "  ");
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }
    }

    /**
     * Tests mutating the last activity instant.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testLastActivityInstant() throws Exception {
        AbstractIdPSession session = new DummyIdPSession("test", "foo");

        final Instant now = Instant.now();
        // this is here to allow the event's last activity time to deviate from the time 'now'
        Thread.sleep(50);

        session.setLastActivityInstant(Instant.now());
        Assert.assertTrue(session.getLastActivityInstant().isAfter(now));

        session.setLastActivityInstant(now);
        Assert.assertEquals(session.getLastActivityInstant(), now);
    }

    /**
     * Tests address binding.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testAddressValidation() throws Exception {
        AbstractIdPSession session = new DummyIdPSession("test", "foo");

        Assert.assertTrue(session.checkAddress("127.0.0.1"));
        Assert.assertTrue(session.checkAddress("127.0.0.1"));
        Assert.assertFalse(session.checkAddress("127.0.0.2"));
        Assert.assertTrue(session.checkAddress("::1"));
        Assert.assertTrue(session.checkAddress("::1"));
        Assert.assertFalse(session.checkAddress("fe80::5a55:caff:fef2:65a3"));
        Assert.assertTrue(session.checkAddress("zorkmid"));
        Assert.assertFalse(session.checkAddress("bugbear"));
    }
    
    /**
     * Tests adding service sessions.
     * 
     * @throws SessionException ...
     */
    @Test public void testAddSPSessions() throws SessionException {
        final Instant now = Instant.now();
        final Instant exp = now.plusSeconds(60);
        
        BasicSPSession svcSession1 = new BasicSPSession("svc1", now, exp);
        BasicSPSession svcSession2 = new BasicSPSession("svc2", now, exp);
        BasicSPSession svcSession3 = new BasicSPSession("svc3", now, exp);

        AbstractIdPSession session = new DummyIdPSession("test", "foo");
        session.addSPSession(svcSession1);
        Assert.assertEquals(session.getSPSessions().size(), 1);
        Assert.assertTrue(session.getSPSessions().contains(svcSession1));
        Assert.assertEquals(session.getSPSession("svc1"), svcSession1);

        session.addSPSession(svcSession2);
        Assert.assertEquals(session.getSPSessions().size(), 2);
        Assert.assertTrue(session.getSPSessions().contains(svcSession1));
        Assert.assertEquals(session.getSPSession("svc1"), svcSession1);
        Assert.assertTrue(session.getSPSessions().contains(svcSession2));
        Assert.assertEquals(session.getSPSession("svc2"), svcSession2);

        session.addSPSession(svcSession3);
        Assert.assertEquals(session.getSPSessions().size(), 3);
        Assert.assertTrue(session.getSPSessions().contains(svcSession1));
        Assert.assertEquals(session.getSPSession("svc1"), svcSession1);
        Assert.assertTrue(session.getSPSessions().contains(svcSession2));
        Assert.assertEquals(session.getSPSession("svc2"), svcSession2);
        Assert.assertTrue(session.getSPSessions().contains(svcSession3));
        Assert.assertEquals(session.getSPSession("svc3"), svcSession3);

        session.addSPSession(svcSession1);
        Assert.assertEquals(session.getSPSessions().size(), 3);
        Assert.assertTrue(session.getSPSessions().contains(svcSession1));
        Assert.assertEquals(session.getSPSession("svc1"), svcSession1);
    }

    /**
     * Tests removing service sessions.
     * 
     * @throws SessionException ...
     */
    @Test public void testRemoveSPSession() throws SessionException {
        final Instant now = Instant.now();
        final Instant exp = now.plusSeconds(60);

        BasicSPSession svcSession1 = new BasicSPSession("svc1", now, exp);
        BasicSPSession svcSession2 = new BasicSPSession("svc2", now, exp);

        AbstractIdPSession session = new DummyIdPSession("test", "foo");
        session.addSPSession(svcSession1);
        session.addSPSession(svcSession2);

        Assert.assertTrue(session.removeSPSession(svcSession1));
        Assert.assertEquals(session.getSPSessions().size(), 1);
        Assert.assertFalse(session.getSPSessions().contains(svcSession1));
        Assert.assertTrue(session.getSPSessions().contains(svcSession2));
        Assert.assertEquals(session.getSPSession("svc2"), svcSession2);

        Assert.assertFalse(session.removeSPSession(svcSession1));
        Assert.assertEquals(session.getSPSessions().size(), 1);
        Assert.assertFalse(session.getSPSessions().contains(svcSession1));
        Assert.assertTrue(session.getSPSessions().contains(svcSession2));
        Assert.assertEquals(session.getSPSession("svc2"), svcSession2);
    }

    /**
     * Tests remove authentication results.
     * 
     * @throws SessionException ...
     */
    @Test public void testRemoveAuthenticationResult() throws SessionException {
        AuthenticationResult event1 = new AuthenticationResult("foo", new UsernamePrincipal("john"));
        AuthenticationResult event2 = new AuthenticationResult("bar", new UsernamePrincipal("john"));
        AuthenticationResult event3 = new AuthenticationResult("baz", new UsernamePrincipal("john"));

        AbstractIdPSession session = new DummyIdPSession("test", "foo");
        session.addAuthenticationResult(event1);
        session.addAuthenticationResult(event2);
        session.addAuthenticationResult(event3);

        session.removeAuthenticationResult(event2);
        Assert.assertEquals(session.getAuthenticationResults().size(), 2);
        Assert.assertTrue(session.getAuthenticationResults().contains(event1));
        Assert.assertEquals(session.getAuthenticationResult("foo"), event1);
        Assert.assertTrue(session.getAuthenticationResults().contains(event3));
        Assert.assertEquals(session.getAuthenticationResult("baz"), event3);

        session.removeAuthenticationResult(event3);
        Assert.assertEquals(session.getAuthenticationResults().size(), 1);
        Assert.assertTrue(session.getAuthenticationResults().contains(event1));
        Assert.assertEquals(session.getAuthenticationResult("foo"), event1);
    }

    /**
     * Dummy concrete class for testing purposes.
     */
    private class DummyIdPSession extends AbstractIdPSession {

        /**
         * Constructor.
         *
         * @param sessionId ...
         * @param canonicalName ...
         */
        public DummyIdPSession(@Nonnull final String sessionId, @Nonnull final String canonicalName) {
            super(sessionId, canonicalName, Instant.now());
        }

        /** {@inheritDoc} */
        public void updateAuthenticationResultActivity(@Nonnull final AuthenticationResult result)
                throws SessionException {

        }
    }
}

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

/** {@link AuthenticationEvent} unit test. */
public class AuthenticationEventTest {

    /** Tests that everything is properly initialized during object construction. */
    @Test public void testInstantiation() throws Exception {
        long start = System.currentTimeMillis();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);

        AuthenticationEvent event = new AuthenticationEvent("test", new UsernamePrincipal("bob"));
        Assert.assertTrue(event.getAuthenticationInstant() > start);
        Assert.assertEquals(event.getAuthenticationWorkflow(), "test");
        Assert.assertEquals(event.getAuthenticatedPrincipal(), new UsernamePrincipal("bob"));
        Assert.assertEquals(event.getLastActivityInstant(), event.getAuthenticationInstant());

        try {
            new AuthenticationEvent(null, new UsernamePrincipal("bob"));
            Assert.fail();
        } catch (AssertionError e) {

        }

        try {
            new AuthenticationEvent("", new UsernamePrincipal("bob"));
            Assert.fail();
        } catch (AssertionError e) {

        }

        try {
            new AuthenticationEvent("  ", new UsernamePrincipal("bob"));
            Assert.fail();
        } catch (AssertionError e) {

        }

        try {
            new AuthenticationEvent("test", null);
            Assert.fail();
        } catch (AssertionError e) {

        }
    }

    /** Tests mutating the last activity instant. */
    @Test public void testLastActivityInstant() throws Exception {
        AuthenticationEvent event = new AuthenticationEvent("test", new UsernamePrincipal("bob"));

        long now = System.currentTimeMillis();
        // this is here to allow the event's last activity time to deviate from the 'now' time
        Thread.sleep(50);

        event.setLastActivityInstantToNow();
        Assert.assertTrue(event.getLastActivityInstant() > now);

        event.setLastActivityInstant(now);
        Assert.assertEquals(event.getLastActivityInstant(), now);
    }
}
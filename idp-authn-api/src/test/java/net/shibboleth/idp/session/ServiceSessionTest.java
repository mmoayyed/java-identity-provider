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

public class ServiceSessionTest {

    @Test
    public void testInstantiation() throws Exception {
        long start = System.currentTimeMillis();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);

        ServiceSession session = new ServiceSession("test");
        Assert.assertNull(session.getAuthenticationEvent());
        Assert.assertTrue(session.getCreationInstant() > start);
        Assert.assertEquals(session.getLastActivityInstant(), session.getCreationInstant());
        Assert.assertEquals(session.getServiceId(), "test");

        try {
            new ServiceSession(null);
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }

        try {
            new ServiceSession("");
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }

        try {
            new ServiceSession("  ");
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testAuthenticationEvent() {
        AuthenticationEvent event = new AuthenticationEvent("test", new UsernamePrincipal("bob"));

        ServiceSession session = new ServiceSession("test");
        session.setAuthenticationEvent(event);
        Assert.assertEquals(session.getAuthenticationEvent(), event);

        session.setAuthenticationEvent(null);
        Assert.assertNull(session.getAuthenticationEvent());
    }

    @Test
    public void testLastActivityInstant() throws Exception {
        ServiceSession session = new ServiceSession("test");

        long now = System.currentTimeMillis();
        // this is here to allow the event's last activity time to deviate from the 'now' time
        Thread.sleep(50);
        
        session.setLastActivityInstantToNow();
        Assert.assertTrue(session.getLastActivityInstant() > now);

        session.setLastActivityInstant(now);
        Assert.assertEquals(session.getLastActivityInstant(), now);
    }
}
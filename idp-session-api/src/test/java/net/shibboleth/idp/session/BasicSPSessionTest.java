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

import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import java.time.Instant;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link BasicSPSession} unit test. */
public class BasicSPSessionTest {

    /**
     * Tests that everything is properly initialized during object construction.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testInstantiation() throws Exception {
        final Instant start = Instant.now();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);

        BasicSPSession session = new BasicSPSession("test", Instant.now(),
                Instant.now().plusSeconds(60));
        Assert.assertEquals(session.getId(), "test");
        Assert.assertTrue(session.getCreationInstant().isAfter(start));
        Assert.assertTrue(session.getExpirationInstant().isAfter(session.getCreationInstant()));

        try {
            new BasicSPSession(null, Instant.ofEpochMilli(0), Instant.ofEpochMilli(0));
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }

        try {
            new BasicSPSession("", Instant.ofEpochMilli(0), Instant.ofEpochMilli(0));
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }

        try {
            new BasicSPSession("  ", Instant.ofEpochMilli(0), Instant.ofEpochMilli(0));
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }
    }

}

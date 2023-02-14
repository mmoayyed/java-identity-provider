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

package net.shibboleth.idp.cas.ticket.serialization.impl;

import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.TicketState;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Unit test for {@link ProxyTicketSerializer}.
 */
@SuppressWarnings("javadoc")
public class ProxyTicketSerializerTest {

    private ProxyTicketSerializer serializer = new ProxyTicketSerializer();

    @Test
    public void testSerializeWithoutTicketState() throws Exception {
        final ProxyTicket pt1 = new ProxyTicket(
                "ST-0123456789-6027f6e93c11b1f587857ee0e7689c27",
                "https://nobody.example.org",
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                "PGT-0123456789-87182857dcbc70f8aa2e0ec87ec3e707");
        final String serialized = serializer.serialize(pt1);
        final ProxyTicket pt2 = serializer.deserialize(1, "notused", pt1.getId(), serialized, null);
        assertEquals(pt2.getId(), pt1.getId());
        assertEquals(pt2.getService(), pt1.getService());
        assertEquals(pt2.getExpirationInstant(), pt1.getExpirationInstant());
        assertEquals(pt2.getPgtId(), pt1.getPgtId());
        assertEquals(pt2.getTicketState(), pt1.getTicketState());
    }

    @Test
    public void testSerializeWithTicketState() throws Exception {
        final ProxyTicket pt1 = new ProxyTicket(
                "ST-0123456789-e1e212143527d57053e7a72d75b3ccd6",
                "https://nobody.example.org",
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                "PGT-0123456789-c0dddd0f73b9494f7fe0b549e8c28002");
        pt1.setTicketState(new TicketState("idpsess-6ebae421b142adb35a3a6303116c3f", "bob",
                Instant.now().truncatedTo(ChronoUnit.MILLIS), "Password"));
        final String serialized = serializer.serialize(pt1);
        final ProxyTicket pt2 = serializer.deserialize(1, "notused", pt1.getId(), serialized, null);
        assertEquals(pt2.getId(), pt1.getId());
        assertEquals(pt2.getService(), pt1.getService());
        assertEquals(pt2.getExpirationInstant(), pt1.getExpirationInstant());
        assertEquals(pt2.getPgtId(), pt1.getPgtId());
        assertEquals(pt2.getTicketState(), pt1.getTicketState());
    }
}
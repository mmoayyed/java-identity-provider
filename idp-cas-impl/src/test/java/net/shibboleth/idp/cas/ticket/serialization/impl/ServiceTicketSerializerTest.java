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

import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketState;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

/**
 * Unit test for {@link ServiceTicketSerializer}.
 */
public class ServiceTicketSerializerTest {

    private ServiceTicketSerializer serializer = new ServiceTicketSerializer();

    @Test
    public void testSerializeWithoutTicketState() throws Exception {
        final ServiceTicket st1 = new ServiceTicket(
                "ST-0123456789-616ea1550eef862761e5931bdccaaba0",
                "https://nobody.example.org",
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                true);
        final String serialized = serializer.serialize(st1);
        final ServiceTicket st2 = serializer.deserialize(1, "notused", st1.getId(), serialized, null);
        assertEquals(st2.getId(), st1.getId());
        assertEquals(st2.getService(), st1.getService());
        assertEquals(st2.getExpirationInstant(), st1.getExpirationInstant());
        assertEquals(st2.isRenew(), st1.isRenew());
        assertEquals(st2.getTicketState(), st1.getTicketState());
    }

    @Test
    public void testSerializeWithTicketState() throws Exception {
        final ServiceTicket st1 = new ServiceTicket(
                "ST-0123456789-e6342d467a4414e599aa3c323528e96f",
                "https://nobody.example.org",
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                true);
        st1.setTicketState(new TicketState("idpsess-d2db22058dc178d3b917363859e", "bob",
                Instant.now().truncatedTo(ChronoUnit.MILLIS), "Password"));
        final String serialized = serializer.serialize(st1);
        final ServiceTicket st2 = serializer.deserialize(1, "notused", st1.getId(), serialized, null);
        assertEquals(st2.getId(), st1.getId());
        assertEquals(st2.getService(), st1.getService());
        assertEquals(st2.getExpirationInstant(), st1.getExpirationInstant());
        assertEquals(st2.isRenew(), st1.isRenew());
        assertEquals(st2.getTicketState(), st1.getTicketState());
    }

    @Test
    public void testSerializeWithConsent() throws Exception {
        final ServiceTicket st1 = new ServiceTicket(
                "ST-0123456789-e6342d467a4414e599aa3c323528e96f",
                "https://nobody.example.org",
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                true);
        final TicketState state = new TicketState("idpsess-d2db22058dc178d3b917363859e", "bob",
                Instant.now().truncatedTo(ChronoUnit.MILLIS), "Password");
        state.setConsentedAttributeIds(Set.of("foo", "bar"));
        st1.setTicketState(state);
        final String serialized = serializer.serialize(st1);
        final ServiceTicket st2 = serializer.deserialize(1, "notused", st1.getId(), serialized, null);
        assertEquals(st2.getId(), st1.getId());
        assertEquals(st2.getService(), st1.getService());
        assertEquals(st2.getExpirationInstant(), st1.getExpirationInstant());
        assertEquals(st2.isRenew(), st1.isRenew());
        assertEquals(st2.getTicketState(), st1.getTicketState());
    }

    @Test
    public void testSerializeWithEmptyConsent() throws Exception {
        final ServiceTicket st1 = new ServiceTicket(
                "ST-0123456789-e6342d467a4414e599aa3c323528e96f",
                "https://nobody.example.org",
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                true);
        final TicketState state = new TicketState("idpsess-d2db22058dc178d3b917363859e", "bob",
                Instant.now().truncatedTo(ChronoUnit.MILLIS), "Password");
        state.setConsentedAttributeIds(Collections.emptySet());
        st1.setTicketState(state);
        final String serialized = serializer.serialize(st1);
        final ServiceTicket st2 = serializer.deserialize(1, "notused", st1.getId(), serialized, null);
        assertEquals(st2.getId(), st1.getId());
        assertEquals(st2.getService(), st1.getService());
        assertEquals(st2.getExpirationInstant(), st1.getExpirationInstant());
        assertEquals(st2.isRenew(), st1.isRenew());
        assertEquals(st2.getTicketState(), st1.getTicketState());
    }

}
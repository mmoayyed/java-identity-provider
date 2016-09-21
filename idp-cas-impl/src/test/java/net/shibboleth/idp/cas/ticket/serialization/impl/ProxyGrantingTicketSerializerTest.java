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

import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.TicketState;
import org.joda.time.Instant;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit test for {@link ProxyGrantingTicketSerializer}.
 */
public class ProxyGrantingTicketSerializerTest {

    private ProxyGrantingTicketSerializer serializer = new ProxyGrantingTicketSerializer();

    @Test
    public void testSerializeWithoutTicketState() throws Exception {
        final ProxyGrantingTicket pgt1 = new ProxyGrantingTicket(
                "ST-0123456789-9d22c36953a31fd12f12d30d76b344d3",
                "https://nobody.example.org",
                Instant.now(),
                null);
        final String serialized = serializer.serialize(pgt1);
        final ProxyGrantingTicket pgt2 = serializer.deserialize(1, "notused", pgt1.getId(), serialized, null);
        assertEquals(pgt2.getId(), pgt1.getId());
        assertEquals(pgt2.getService(), pgt1.getService());
        assertEquals(pgt2.getExpirationInstant(), pgt1.getExpirationInstant());
        assertEquals(pgt2.getParentId(), pgt1.getParentId());
        assertEquals(pgt2.getTicketState(), pgt1.getTicketState());
    }

    @Test
    public void testSerializeWithTicketState() throws Exception {
        final ProxyGrantingTicket pgt1 = new ProxyGrantingTicket(
                "ST-0123456789-fbca86ba09d1be7ec3ac17e6f372be87",
                "https://nobody.example.org",
                Instant.now(),
                null);
        pgt1.setTicketState(new TicketState("idpsess-1e663b80e6c6b2b7ae778cb3ea265", "bob", Instant.now(), "Password"));
        final String serialized = serializer.serialize(pgt1);
        final ProxyGrantingTicket pgt2 = serializer.deserialize(1, "notused", pgt1.getId(), serialized, null);
        assertEquals(pgt2.getId(), pgt1.getId());
        assertEquals(pgt2.getService(), pgt1.getService());
        assertEquals(pgt2.getExpirationInstant(), pgt1.getExpirationInstant());
        assertEquals(pgt2.getParentId(), pgt1.getParentId());
        assertEquals(pgt2.getTicketState(), pgt1.getTicketState());
    }

    @Test
    public void testSerializeWithParentAndTicketState() throws Exception {
        final ProxyGrantingTicket pgt1 = new ProxyGrantingTicket(
                "ST-0123456789-fbca86ba09d1be7ec3ac17e6f372be87",
                "https://nobody.example.org",
                Instant.now(),
                "PGT-0987654321-0040c390cf67e571c7e12fcc78fc0cb3");
        pgt1.setTicketState(new TicketState("idpsess-6d986af1280b7f52f9e7da434cfd0", "bob", Instant.now(), "Password"));
        final String serialized = serializer.serialize(pgt1);
        final ProxyGrantingTicket pgt2 = serializer.deserialize(1, "notused", pgt1.getId(), serialized, null);
        assertEquals(pgt2.getId(), pgt1.getId());
        assertEquals(pgt2.getService(), pgt1.getService());
        assertEquals(pgt2.getExpirationInstant(), pgt1.getExpirationInstant());
        assertEquals(pgt2.getParentId(), pgt1.getParentId());
        assertEquals(pgt2.getTicketState(), pgt1.getTicketState());
    }
}
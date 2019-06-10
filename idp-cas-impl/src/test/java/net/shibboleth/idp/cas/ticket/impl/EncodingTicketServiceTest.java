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

package net.shibboleth.idp.cas.ticket.impl;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import net.shibboleth.ext.spring.resource.ResourceHelper;
import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.utilities.java.support.security.DataSealer;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.security.RandomIdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.security.impl.BasicKeystoreKeyStrategy;
import org.apache.commons.codec.binary.Base32;
import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit test for {@link EncodingTicketService}.
 *
 * @author Marvin S. Addison
 */
public class EncodingTicketServiceTest {

    private EncodingTicketService ticketService;

    private IdentifierGenerationStrategy sessionIdGenerator = new RandomIdentifierGenerationStrategy(32);

    private IdentifierGenerationStrategy pgtIdGenerator = new TicketIdentifierGenerationStrategy("PGT", 32);

    private SecureRandom secureRandom;


    public EncodingTicketServiceTest() {
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Secure random creation failed", e);
        }
    }

    @BeforeTest
    public void setUp() throws Exception {
        final BasicKeystoreKeyStrategy strategy = new BasicKeystoreKeyStrategy();
        strategy.setKeystoreResource(ResourceHelper.of(new ClassPathResource("credentials/sealer.jks")));
        strategy.setKeyVersionResource(ResourceHelper.of(new ClassPathResource("credentials/sealer.kver")));
        strategy.setKeystorePassword("password");
        strategy.setKeyAlias("secret");
        strategy.setKeyPassword("password");
        strategy.initialize();
        final Base32 codec = new Base32(0, null, false, (byte) '-');
        final DataSealer sealer = new DataSealer();
        sealer.setKeyStrategy(strategy);
        sealer.setRandom(secureRandom);
        sealer.setEncoder(codec);
        sealer.setDecoder(codec);
        sealer.initialize();
        ticketService = new EncodingTicketService(new MemoryStorageService(), sealer);
    }

    @Test
    public void testCreateRemoveServiceTicketSuccess() throws Exception {
        final TicketState state = newState("aloysius");
        final String service = "https://www.example.com/s1/";
        final Instant expiry = Instant.now().plusSeconds(5);
        final String id = String.valueOf(System.currentTimeMillis());
        final ServiceTicket st1 = ticketService.createServiceTicket(id, expiry, service, state, true);
        assertNotNull(st1);
        assertTrue(st1.getId().startsWith("ST-"));
        final ServiceTicket st2 = ticketService.removeServiceTicket(st1.getId());
        assertNotNull(st2);
        assertEquals(st1.getId(), st2.getId());
        assertEquals(expiry.truncatedTo(ChronoUnit.MILLIS), st2.getExpirationInstant());
        assertEquals(service, st2.getService());
        assertTrue(st2.isRenew());
        assertEquals(state.getSessionId(), st2.getSessionId());
        assertEquals(state, st2.getTicketState());
        // Confirm removing multiple times is possible
        final ServiceTicket st3 = ticketService.removeServiceTicket(st1.getId());
        assertNotNull(st3);
        assertEquals(st1.getId(), st3.getId());
    }

    @Test
    public void testCreateRemoveServiceTicketInvalid() throws Exception {
        final ServiceTicket st1 = ticketService.createServiceTicket(
                String.valueOf(System.currentTimeMillis()),
                Instant.now().plusSeconds(5),
                "https://www.example.com/s2/",
                newState("bartholomew"),
                true);
        assertNotNull(st1);
        assertNull(ticketService.removeServiceTicket("ST-abc123"));
    }

    @Test
    public void testCreateServiceTicketSuccessWithLongID() throws Exception {
        final TicketState state = newState("charlemagne@vt.edu");
        final String service = "https://www.google.com/maps/place/Mountain+Lake+Lodge/"
                + "@37.3554696,-80.539459,17z/data=!3m1!4b1!4m5!3m4!1s0x884dc6a58faa2119:0x17f3cc6a2c82b614!"
                + "8m2!3d37.3554696!4d-80.537265";
        final Instant expiry = Instant.now().plusSeconds(5);
        final String id = String.valueOf(System.currentTimeMillis());
        final ServiceTicket st1 = ticketService.createServiceTicket(id, expiry, service, state, true);
        assertNotNull(st1);
        assertTrue(st1.getId().matches("ST-[A-Za-z0-9]+-*"));
        assertTrue(st1.getId().length() < 600);
    }

    @Test
    public void testCreateRemoveProxyTicketSuccess() throws Exception {
        final ProxyGrantingTicket pgt = newPGT(newState("donegal"), "https://www.example.com/s1/");
        final String service = "https://www.example.com/s2/";
        final Instant expiry = Instant.now().plusSeconds(5);
        final String id = String.valueOf(System.currentTimeMillis());
        final ProxyTicket pt1 = ticketService.createProxyTicket(id, expiry, pgt, service);
        assertNotNull(pt1);
        assertTrue(pt1.getId().startsWith("PT-"));
        final ProxyTicket pt2 = ticketService.removeProxyTicket(pt1.getId());
        assertNotNull(pt2);
        assertEquals(pt1.getId(), pt2.getId());
        assertEquals(expiry.truncatedTo(ChronoUnit.MILLIS), pt2.getExpirationInstant());
        assertEquals(service, pt2.getService());
        assertEquals(pgt.getId(), pt2.getPgtId());
        assertEquals(pgt.getTicketState(), pt2.getTicketState());
        // Confirm removing multiple times is possible
        final ProxyTicket pt3 = ticketService.removeProxyTicket(pt1.getId());
        assertNotNull(pt3);
        assertEquals(pt1.getId(), pt3.getId());
    }

    @Test
    public void testCreateRemoveProxyTicketInvalid() throws Exception {
        final ProxyGrantingTicket pgt = newPGT(newState("esquire"), "https://www.example.com/s1/");
        final String service = "https://www.example.com/s2/";
        final Instant expiry = Instant.now().plusSeconds(5);
        final String id = String.valueOf(System.currentTimeMillis());
        final ProxyTicket pt1 = ticketService.createProxyTicket(id, expiry, pgt, service);
        assertNotNull(pt1);
        assertNull(ticketService.removeProxyTicket("PT-123"));
    }

    private TicketState newState(final String principal) {
        return new TicketState(sessionIdGenerator.generateIdentifier(), principal,
                Instant.now().truncatedTo(ChronoUnit.MILLIS), "authn/Password");
    }

    private ProxyGrantingTicket newPGT(final TicketState state, final String service) {
        final ProxyGrantingTicket pgt = new ProxyGrantingTicket(
                pgtIdGenerator.generateIdentifier(), service, Instant.now().plusSeconds(300), "PGT-12345");
        pgt.setTicketState(state);
        return pgt;
    }
}
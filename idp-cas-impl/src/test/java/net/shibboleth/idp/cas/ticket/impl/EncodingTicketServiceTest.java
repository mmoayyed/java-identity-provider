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

import java.security.SecureRandom;

import net.shibboleth.ext.spring.resource.ResourceHelper;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.utilities.java.support.security.BasicKeystoreKeyStrategy;
import net.shibboleth.utilities.java.support.security.DataSealer;
import org.joda.time.Instant;
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


    @BeforeTest
    public void setUp() throws Exception {
        final BasicKeystoreKeyStrategy strategy = new BasicKeystoreKeyStrategy();
        strategy.setKeystoreResource(ResourceHelper.of(new ClassPathResource("credentials/sealer.jks")));
        strategy.setKeyVersionResource(ResourceHelper.of(new ClassPathResource("credentials/sealer.kver")));
        strategy.setKeystorePassword("password");
        strategy.setKeyAlias("secret");
        strategy.setKeyPassword("password");
        strategy.initialize();
        final DataSealer sealer = new DataSealer();
        sealer.setKeyStrategy(strategy);
        sealer.setRandom(SecureRandom.getInstance("SHA1PRNG"));
        sealer.initialize();
        ticketService = new EncodingTicketService(new MemoryStorageService(), sealer);
    }

    @Test
    public void testCreateRemoveServiceTicketSuccess() throws Exception {
        final TicketState state = new TicketState(
                "21ea878e966f3d447a9f419866e1c976f69ec2297848902363e1cafd276ce66c",
                "aloysius",
                new Instant(),
                "authn/Password");
        final String service = "https://www.example.com/s1/";
        final Instant expiry = new Instant().plus(5000);
        final String id = String.valueOf(System.currentTimeMillis());
        final ServiceTicket st1 = ticketService.createServiceTicket(id, expiry, service, state, true);
        assertNotNull(st1);
        assertTrue(st1.getId().startsWith("ST-"));
        final ServiceTicket st2 = ticketService.removeServiceTicket(st1.getId());
        assertNotNull(st2);
        assertEquals(st1.getId(), st2.getId());
        assertEquals(expiry, st2.getExpirationInstant());
        assertEquals(service, st2.getService());
        assertTrue(st2.isRenew());
        assertEquals(state.getSessionId(), st2.getSessionId());
        assertEquals(state, st2.getTicketState());
    }


    @Test
    public void testCreateRemoveServiceTicketInvalid() throws Exception {
        final TicketState state = new TicketState(
                "cf53c3912207ea7ce7e659333fc203b3290707e41e23b7f0f589500f60d129d9",
                "bartholomew",
                new Instant(),
                "authn/X509");
        final ServiceTicket st1 = ticketService.createServiceTicket(
                String.valueOf(System.currentTimeMillis()),
                new Instant().plus(5000),
                "https://www.example.com/s2/",
                state,
                true);
        assertNotNull(st1);
        assertTrue(st1.getId().startsWith("ST-"));
        assertNull(ticketService.removeServiceTicket("ST-abc123"));
    }

    @Test
    public void testCreateServiceTicketSuccessWithLongID() throws Exception {
        final TicketState state = new TicketState(
                "46d14956eaca97fc9762243af3c1fd0424d0031c3c7e44115b83cec80f29ebe47f1cdb89f73ccc075f71558f5338ebd5",
                "charlemagne@vt.edu",
                new Instant(),
                "authn/Password");
        final String service = "https://www.google.com/maps/place/Mountain+Lake+Lodge/"
                + "@37.3554696,-80.539459,17z/data=!3m1!4b1!4m5!3m4!1s0x884dc6a58faa2119:0x17f3cc6a2c82b614!"
                + "8m2!3d37.3554696!4d-80.537265";
        final Instant expiry = new Instant().plus(5000);
        final String id = String.valueOf(System.currentTimeMillis());
        final ServiceTicket st1 = ticketService.createServiceTicket(id, expiry, service, state, true);
        assertNotNull(st1);
        assertTrue(st1.getId().startsWith("ST-"));
        assertTrue(st1.getId().length() < 500);
    }
}
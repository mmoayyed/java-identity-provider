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

package net.shibboleth.idp.cas.ticket.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.annotation.Nonnull;

import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketIdentifierGenerationStrategy;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.shared.security.DataSealer;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;
import net.shibboleth.shared.security.IdentifierGenerationStrategy.ProviderType;
import net.shibboleth.shared.security.RandomIdentifierParameterSpec;
import net.shibboleth.shared.security.impl.BasicKeystoreKeyStrategy;
import net.shibboleth.shared.spring.resource.ResourceHelper;

import org.apache.commons.codec.binary.Base32;
import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit test for {@link EncodingTicketService}.
 *
 * @author Marvin S. Addison
 */
@SuppressWarnings("javadoc")
public class EncodingTicketServiceTest {

    private EncodingTicketService ticketService;

    private IdentifierGenerationStrategy sessionIdGenerator;

    private IdentifierGenerationStrategy pgtIdGenerator;

    @BeforeClass
    public void setUp() throws Exception {
        pgtIdGenerator = new TicketIdentifierGenerationStrategy("PGT", 32);
        sessionIdGenerator = IdentifierGenerationStrategy.getInstance(ProviderType.RANDOM,
                new RandomIdentifierParameterSpec(null, 32, null));
        
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
        assert id != null && expiry != null;
        final ServiceTicket st1 = ticketService.createServiceTicket(id, expiry, service, state, true);
        assert st1 != null;
        assertTrue(st1.getId().startsWith("ST-"));
        final ServiceTicket st2 = ticketService.removeServiceTicket(st1.getId());
        assert st2 != null;
        assertEquals(st1.getId(), st2.getId());
        assertEquals(expiry.truncatedTo(ChronoUnit.MILLIS), st2.getExpirationInstant());
        assertEquals(service, st2.getService());
        assertTrue(st2.isRenew());
        assertEquals(state.getSessionId(), st2.getSessionId());
        assertEquals(state, st2.getTicketState());
        // Confirm removing multiple times is possible
        final ServiceTicket st3 = ticketService.removeServiceTicket(st1.getId());
        assert st3 != null;
        assertEquals(st1.getId(), st3.getId());
    }

    @Test
    public void testCreateRemoveServiceTicketInvalid() throws Exception {
        final Instant expiry = Instant.now().plusSeconds(5);
        final String nowString = String.valueOf(System.currentTimeMillis());
        assert expiry!=null && nowString!=null;
        final ServiceTicket st1 = ticketService.createServiceTicket(
                nowString,
                expiry,
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
        assert expiry!=null && id!=null;
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
        assert id != null && expiry !=null;
        final ProxyTicket pt1 = ticketService.createProxyTicket(id, expiry, pgt, service);
        assert pt1 != null;
        assertTrue(pt1.getId().startsWith("PT-"));
        final ProxyTicket pt2 = ticketService.removeProxyTicket(pt1.getId());
        assert pt2 != null;
        assertEquals(pt1.getId(), pt2.getId());
        assertEquals(expiry.truncatedTo(ChronoUnit.MILLIS), pt2.getExpirationInstant());
        assertEquals(service, pt2.getService());
        assertEquals(pgt.getId(), pt2.getPgtId());
        assertEquals(pgt.getTicketState(), pt2.getTicketState());
        // Confirm removing multiple times is possible
        final ProxyTicket pt3 = ticketService.removeProxyTicket(pt1.getId());
        assert pt3 != null;
        assertEquals(pt1.getId(), pt3.getId());
    }

    @Test
    public void testCreateRemoveProxyTicketInvalid() throws Exception {
        final ProxyGrantingTicket pgt = newPGT(newState("esquire"), "https://www.example.com/s1/");
        final String service = "https://www.example.com/s2/";
        final Instant expiry = Instant.now().plusSeconds(5);
        final String id = String.valueOf(System.currentTimeMillis());
        assert id != null && expiry!=null && pgt!=null;
        final ProxyTicket pt1 = ticketService.createProxyTicket(id, expiry, pgt, service);
        assert pt1 != null;
        assertNull(ticketService.removeProxyTicket("PT-123"));
    }

    @Test
    public void testCreateFetchRemoveEncodedProxyGrantingTicket() {
        final String principal = "aleph";
        final String serviceUrl = "https://www.example.com/service1";
        final String pgtUrl = "https://www.example.com/pgt1";
        final Instant then = Instant.now().plusSeconds(5);
        final String nowString = String.valueOf(System.currentTimeMillis()); 
        assert then != null && nowString!=null;;
        final ServiceTicket st = ticketService.createServiceTicket(
            nowString,
            then,
            serviceUrl,
            newState(principal),
            true);
        final Instant expiry = Instant.now().plusSeconds(3600);
        assert expiry!=null;
        final ProxyGrantingTicket pgt = ticketService.createProxyGrantingTicket("notused", expiry, st, pgtUrl);
        assertTrue(pgt.getId().startsWith("PGT-E-"));
        final ProxyGrantingTicket pgt2 = ticketService.fetchProxyGrantingTicket(pgt.getId());
        assert pgt2 != null;
        assertEquals(pgt2.getService(), serviceUrl);
        assertEquals(pgt2.getProxyCallbackUrl(), pgtUrl);
        final TicketState ts2 = pgt2.getTicketState();
        assert ts2 != null;
        assertEquals(ts2.getPrincipalName(), principal);
        final ProxyGrantingTicket pgt3 = ticketService.removeProxyGrantingTicket(pgt.getId());
        assert pgt3 != null;
        assertNotNull(pgt3);
        assertEquals(pgt3.getService(), serviceUrl);
        assertEquals(pgt2.getProxyCallbackUrl(), pgtUrl);
        final TicketState ts3 = pgt3.getTicketState();
        assert ts3 != null;
        assertEquals(ts3.getPrincipalName(), principal);
        // Removing encoded tickets is the same as fetching so they are still available (no backing storage)
        assertNotNull(ticketService.fetchProxyGrantingTicket(pgt.getId()));
    }

    @Nonnull private TicketState newState(@Nonnull final String principal) {
        final Instant truncatedNow = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        assert truncatedNow!=null;
        return new TicketState(sessionIdGenerator.generateIdentifier(), principal, truncatedNow, "authn/Password");
    }

    @Nonnull private ProxyGrantingTicket newPGT(@Nonnull final TicketState state, @Nonnull final String service) {
        final Instant then = Instant.now().plusSeconds(300);
        assert then != null;
        final ProxyGrantingTicket pgt = new ProxyGrantingTicket(
            pgtIdGenerator.generateIdentifier(),
            service,
            then,
            service + "/proxy",
            "PGT-12345");
        pgt.setTicketState(state);
        return pgt;
    }
}
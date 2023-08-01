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

import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketIdentifierGenerationStrategy;
import net.shibboleth.idp.cas.ticket.TicketState;
import org.opensaml.storage.impl.MemoryStorageService;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.annotation.Nonnull;

/**
 * Unit test for {@link SimpleTicketService} class.
 *
 * @author Marvin S. Addison
 */
@SuppressWarnings("javadoc")
public class SimpleTicketServiceTest {

    private static final String TEST_SESSION_ID = "jHXRo42W0ATPEN+X5Zk1cw==";

    private static final String TEST_SERVICE = "https://example.com/widget";

    private static final String TEST_PGTURL = "https://proxy.example.com/";

    private SimpleTicketService ticketService;

    @BeforeClass
    public void setUp() throws Exception {
        final MemoryStorageService ss = new MemoryStorageService();
        ss.setId("shibboleth.StorageService");
        ss.initialize();
        ticketService = new SimpleTicketService(ss);
    }


    @Test
    public void testCreateRemoveServiceTicket() throws Exception {
        final ServiceTicket st = createServiceTicket();
        assert st != null;
        final TicketState ts = st.getTicketState();
        assert ts != null;
        assertNotNull(ts.getSessionId());
        assertNotNull(ts.getPrincipalName());
        final ServiceTicket st2 = ticketService.removeServiceTicket(st.getId());
        assert st2 != null;
        assertEquals(st, st2);
        assertEquals(st.getExpirationInstant(), st2.getExpirationInstant());
        assertEquals(st.getService(), st2.getService());
        assertEquals(st.getTicketState(), st2.getTicketState());
        assertNull(ticketService.removeServiceTicket(st.getId()));
    }

    @Test
    public void testCreateFetchRemoveProxyGrantingTicket() throws Exception {
        final ProxyGrantingTicket pgt = createProxyGrantingTicket();
        assert pgt != null;
        final TicketState ts = pgt.getTicketState();
        assert ts != null;
        assertNotNull(ts.getSessionId());
        assertNotNull(ts.getPrincipalName());
        final ProxyGrantingTicket pgt2 = ticketService.fetchProxyGrantingTicket(pgt.getId());
        assert pgt2 != null;
        assertEquals(pgt, pgt2);
        assertEquals(pgt.getExpirationInstant(), pgt2.getExpirationInstant());
        assertEquals(pgt.getService(), pgt2.getService());
        assertEquals(pgt.getProxyCallbackUrl(), pgt2.getProxyCallbackUrl());
        assertEquals(pgt.getTicketState(), pgt2.getTicketState());
        assertEquals(ticketService.removeProxyGrantingTicket(pgt.getId()), pgt);
        assertNull(ticketService.removeProxyGrantingTicket(pgt.getId()));
    }

    @Test
    public void testCreateRemoveProxyTicket() throws Exception {
        final ProxyTicket pt = ticketService.createProxyTicket(
                new TicketIdentifierGenerationStrategy("PT", 25).generateIdentifier(),
                expiry(),
                createProxyGrantingTicket(),
                TEST_SERVICE);
        assert pt != null;
        final TicketState ts = pt.getTicketState();
        assert ts != null;
        assertNotNull(ts.getSessionId());
        assertNotNull(ts.getPrincipalName());
        final ProxyTicket pt2 = ticketService.removeProxyTicket(pt.getId());
        assert null != pt2;
        assertEquals(pt, pt2);
        assertEquals(pt.getExpirationInstant(), pt2.getExpirationInstant());
        assertEquals(pt.getService(), pt2.getService());
        assertEquals(pt.getTicketState(), pt2.getTicketState());
        assertNull(ticketService.removeProxyTicket(pt.getId()));
    }

    @Nonnull private ServiceTicket createServiceTicket() {
        return ticketService.createServiceTicket(
                new TicketIdentifierGenerationStrategy("ST", 25).generateIdentifier(),
                expiry(),
                TEST_SERVICE,
                new TicketState(TEST_SESSION_ID, "bob", expiry(), "Password"),
                false);
    }

    @Nonnull private ProxyGrantingTicket createProxyGrantingTicket() {
        return ticketService.createProxyGrantingTicket(
                new TicketIdentifierGenerationStrategy("PGT", 50).generateIdentifier(),
                expiry(),
                createServiceTicket(),
                TEST_PGTURL);
    }

    @Nonnull private static Instant expiry() {
        final Instant result = Instant.now().plusSeconds(10).truncatedTo(ChronoUnit.MILLIS);
        assert result != null;
        return result;
    }
}

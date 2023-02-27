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

package net.shibboleth.idp.cas.flow.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.time.Instant;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.annotations.Test;

import net.shibboleth.idp.cas.config.ValidateConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.shared.component.ComponentInitializationException;

/**
 * Unit test for {@link ValidateTicketAction} class.
 *
 * @author Marvin S. Addison
 */
@SuppressWarnings("javadoc")
public class ValidateTicketActionTest extends AbstractFlowActionTest {

    private static final String TEST_SERVICE = "https://example.com/widget";

    @Test
    public void testInvalidTicketFormat() throws Exception {
        final RequestContext context = new TestContextBuilder(ValidateConfiguration.PROFILE_ID)
                .addProtocolContext(new TicketValidationRequest(TEST_SERVICE, "AB-1234-012346abcdef"), null)
                .addRelyingPartyContext(TEST_SERVICE, true, new ValidateConfiguration())
                .build();
        final  Event event =  newAction(ticketService).execute(context);
        assert event != null;

        assertEquals(event.getId(), ProtocolError.InvalidTicketFormat.name());
    }

    @Test
    public void testServiceMismatch() throws Exception {
        final ServiceTicket ticket = createServiceTicket(TEST_SERVICE, false);
        final RequestContext context = new TestContextBuilder(ValidateConfiguration.PROFILE_ID)
                .addProtocolContext(new TicketValidationRequest("mismatch", ticket.getId()), null)
                .addRelyingPartyContext(ticket.getService(), true, new ValidateConfiguration())
                .build();
        final  Event event =  newAction(ticketService).execute(context);
        assert event != null;
        assertEquals(event.getId(), ProtocolError.ServiceMismatch.name());
    }

    @Test
    public void testTicketExpired() throws Exception {
        final int ticketTTLMillis = 10;
        final TicketState state = new TicketState(TEST_SESSION_ID, TEST_PRINCIPAL_NAME, Instant.now(), "Password");
        final ServiceTicket ticket = ticketService.createServiceTicket(
            generateServiceTicketId(), Instant.now().plusMillis(ticketTTLMillis), TEST_SERVICE, state, false);
        final RequestContext context = new TestContextBuilder(ValidateConfiguration.PROFILE_ID)
                .addProtocolContext(new TicketValidationRequest(TEST_SERVICE, ticket.getId()), null)
                .addRelyingPartyContext(ticket.getService(), true, new ValidateConfiguration())
                .build();
        // Wait briefly to let ticket expire
        Thread.sleep(ticketTTLMillis + 5);
        final  Event event =  newAction(ticketService).execute(context);
        assert event != null;
        assertEquals(event.getId(), ProtocolError.TicketExpired.name());
    }

    @Test
    public void testTicketRetrievalError() throws Exception {
        final TicketService throwingTicketService = mock(TicketService.class);
        when(throwingTicketService.removeServiceTicket(any(String.class))).thenThrow(new RuntimeException("Broken"));
        final RequestContext context = new TestContextBuilder(ValidateConfiguration.PROFILE_ID)
                .addProtocolContext(new TicketValidationRequest(TEST_SERVICE, "ST-12345"), null)
                .addRelyingPartyContext(TEST_SERVICE, true, new ValidateConfiguration())
                .build();
        final  Event event =  newAction(throwingTicketService).execute(context);
        assert event != null;
        assertEquals(event.getId(), ProtocolError.TicketRetrievalError.name());
    }

    @Test
    public void testServiceTicketValidateSuccess() throws Exception {
        final ServiceTicket ticket = createServiceTicket(TEST_SERVICE, false);
        final RequestContext context = new TestContextBuilder(ValidateConfiguration.PROFILE_ID)
                .addProtocolContext(new TicketValidationRequest(TEST_SERVICE, ticket.getId()), null)
                .addRelyingPartyContext(ticket.getService(), true, new ValidateConfiguration())
                .build();
        final ValidateTicketAction action = newAction(ticketService);
        final  Event event =  action.execute(context);
        assert event != null;
        assertEquals(event.getId(), Events.ServiceTicketValidated.name());
        assertNotNull(action.getCASResponse(getProfileContext(context)));
    }

    @Test
    public void testServiceTicketValidateSuccessWithJSessionID() throws Exception {
        final ServiceTicket ticket = createServiceTicket(TEST_SERVICE + ";jsessionid=abc123", false);
        final RequestContext context = new TestContextBuilder(ValidateConfiguration.PROFILE_ID)
                .addProtocolContext(new TicketValidationRequest(TEST_SERVICE, ticket.getId()), null)
                .addRelyingPartyContext(ticket.getService(), true, new ValidateConfiguration())
                .build();
        final ValidateTicketAction action = newAction(ticketService);
        final  Event event =  action.execute(context);
        assert event != null;
        assertEquals(event.getId(), Events.ServiceTicketValidated.name());
        assertNotNull(action.getCASResponse(getProfileContext(context)));
    }

    @Test
    public void testProxyTicketValidateSuccess() throws Exception {
        final ServiceTicket st = createServiceTicket(TEST_SERVICE, false);
        final ProxyGrantingTicket pgt = createProxyGrantingTicket(st, TEST_SERVICE + "/proxy");
        final ProxyTicket pt = createProxyTicket(pgt, "proxyA");
        final RequestContext context = new TestContextBuilder(ValidateConfiguration.PROFILE_ID)
                .addProtocolContext(new TicketValidationRequest("proxyA", pt.getId()), null)
                .addRelyingPartyContext(pt.getService(), true, new ValidateConfiguration())
                .build();
        final ValidateTicketAction action = newAction(ticketService);
        final  Event event =  action.execute(context);
        assert event != null;
        assertEquals(event.getId(), Events.ProxyTicketValidated.name());
        assertNotNull(action.getCASResponse(getProfileContext(context)));
    }

    private static ValidateTicketAction newAction(final TicketService service) {
        final ValidateTicketAction action = new ValidateTicketAction(service);
        try {
            action.initialize();
        } catch (ComponentInitializationException e) {
            throw new RuntimeException("Initialization error", e);
        }
        return action;
    }
}

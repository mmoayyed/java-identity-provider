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

package net.shibboleth.idp.cas.flow;

import net.shibboleth.idp.cas.config.ServiceTicketConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import org.springframework.webflow.execution.RequestContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Unit test for {@link ValidateTicketAction} class.
 *
 * @author Marvin S. Addison
 */
public class ValidateTicketActionTest extends AbstractFlowActionTest {

    private static final String TEST_SERVICE = "https://example.com/widget";

    private RequestContext context;

    @BeforeTest
    public void setUp() throws Exception {
        context = new TestContextBuilder(ServiceTicketConfiguration.PROFILE_ID).build();
    }

    @Test
    public void testInvalidTicketFormat() throws Exception {
        final TicketValidationRequest request = new TicketValidationRequest(TEST_SERVICE, "AB-1234-012346abcdef");
        FlowStateSupport.setTicketValidationRequest(context, request);
        assertEquals(newAction(ticketService).execute(context).getId(), ProtocolError.InvalidTicketFormat.id());
    }

    @Test
    public void testServiceMismatch() throws Exception {
        final ServiceTicket ticket = createServiceTicket(TEST_SERVICE, false);
        final TicketValidationRequest request = new TicketValidationRequest("mismatch", ticket.getId());
        FlowStateSupport.setTicketValidationRequest(context, request);
        assertEquals(newAction(ticketService).execute(context).getId(), ProtocolError.ServiceMismatch.id());
    }

    @Test
    public void testTicketExpired() throws Exception {
        final ServiceTicket ticket = createServiceTicket(TEST_SERVICE, false);
        final TicketValidationRequest request = new TicketValidationRequest(TEST_SERVICE, ticket.getId());
        FlowStateSupport.setTicketValidationRequest(context, request);
        // Remove the ticket prior to validation to simulate expiration
        ticketService.removeServiceTicket(ticket.getId());
        assertEquals(newAction(ticketService).execute(context).getId(), ProtocolError.TicketExpired.id());
    }

    @Test
    public void testTicketRetrievalError() throws Exception {
        final TicketService throwingTicketService = mock(TicketService.class);
        when(throwingTicketService.removeServiceTicket(any(String.class))).thenThrow(new RuntimeException("Broken"));
        final TicketValidationRequest request = new TicketValidationRequest(TEST_SERVICE, "ST-12345");
        FlowStateSupport.setTicketValidationRequest(context, request);
        assertEquals(
                newAction(throwingTicketService).execute(context).getId(),
                ProtocolError.TicketRetrievalError.id());
    }

    @Test
    public void testSuccess() throws Exception {
        final ServiceTicket ticket = createServiceTicket(TEST_SERVICE, false);
        final TicketValidationRequest request = new TicketValidationRequest(TEST_SERVICE, ticket.getId());
        FlowStateSupport.setTicketValidationRequest(context, request);
        assertEquals(newAction(ticketService).execute(context).getId(), Events.ServiceTicketValidated.id());
        assertNotNull(FlowStateSupport.getTicketValidationResponse(context));
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

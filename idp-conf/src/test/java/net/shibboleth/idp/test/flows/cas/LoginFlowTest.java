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

package net.shibboleth.idp.test.flows.cas;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import javax.annotation.Nonnull;
import javax.servlet.http.Cookie;

import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.criterion.SessionIdCriterion;
import net.shibboleth.idp.session.impl.StorageBackedSessionManager;
import net.shibboleth.idp.test.flows.AbstractFlowTest;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.annotations.Test;

/**
 * Tests the flow behind the <code>/login</code> endpoint.
 *
 * @author Marvin S. Addison
 */
@ContextConfiguration(locations = {
        "/test/test-cas-beans.xml",
})
public class LoginFlowTest extends AbstractFlowTest {

    /** Flow id. */
    @Nonnull
    private static String FLOW_ID = "cas/login";

    @Autowired
    private TicketService ticketService;

    @Autowired
    private StorageBackedSessionManager sessionManager;


    @Test
    public void testGateway() throws Exception {
        final String service = "https://gateway.example.org/";
        externalContext.getMockRequestParameterMap().put("service", service);
        externalContext.getMockRequestParameterMap().put("gateway", "true");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        assertEquals(result.getOutcome().getId(), "gatewayRedirect");
        assertEquals(externalContext.getExternalRedirectUrl(), service);
    }

    @Test
    public void testLoginStartSession() throws Exception {
        final String service = "https://start.example.org/";
        externalContext.getMockRequestParameterMap().put("service", service);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        assertEquals(result.getOutcome().getId(), "redirectToService");
        final String url = externalContext.getExternalRedirectUrl();
        assertTrue(url.contains("ticket=ST-"));
        final String ticketId = url.substring(url.indexOf("ticket=") + 7);
        final Ticket st = ticketService.removeServiceTicket(ticketId);
        assertNotNull(st);
        final IdPSession session = sessionManager.resolveSingle(
                new CriteriaSet(new SessionIdCriterion(st.getSessionId())));
        assertNotNull(session);
    }

    @Test
    public void testLoginExistingSession() throws Exception {
        final String service = "https://existing.example.org/";
        final IdPSession existing = sessionManager.createSession("aurora");
        externalContext.getMockRequestParameterMap().put("service", service);
        request.setCookies(new Cookie("shib_idp_session", existing.getId()));
        initializeThreadLocals();

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        assertEquals(result.getOutcome().getId(), "redirectToService");
        final String url = externalContext.getExternalRedirectUrl();
        assertTrue(url.contains("ticket=ST-"));
        final String ticketId = url.substring(url.indexOf("ticket=") + 7);
        final Ticket st = ticketService.removeServiceTicket(ticketId);
        assertNotNull(st);
        final IdPSession session = sessionManager.resolveSingle(
                new CriteriaSet(new SessionIdCriterion(st.getSessionId())));
        assertNotNull(session);
        assertEquals(session.getId(), existing.getId());
    }

    @Test
    public void testErrorNoService() throws Exception {
        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        final String responseBody = response.getContentAsString();
        assertEquals(result.getOutcome().getId(), "error");
        assertTrue(responseBody.contains("serviceNotSpecified"));
    }
}

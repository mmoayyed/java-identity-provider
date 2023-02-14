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

import javax.annotation.Nonnull;

import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketIdentifierGenerationStrategy;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionManager;
import net.shibboleth.idp.test.flows.AbstractFlowTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.time.Instant;

/**
 * Tests the flow behind the <code>/proxyValidate</code> endpoint.
 *
 * @author Marvin S. Addison
 */
@ContextConfiguration(locations = {
        "/test/test-cas-beans.xml",
})
@SuppressWarnings("javadoc")
public class ProxyValidateFlowTest extends AbstractFlowTest {

    /** Flow id. */
    @Nonnull
    private static String FLOW_ID = "cas/proxyValidate";

    @Autowired
    @Qualifier("shibboleth.CASTicketService")
    private TicketService ticketService;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private TestProxyValidator testProxyValidator;

    @Test
    public void testSuccess() throws Exception {
        final String principal = "john";
        final IdPSession session = sessionManager.createSession(principal);
        final ProxyTicket ticket = createProxyTicket(session.getId(), principal);

        externalContext.getMockRequestParameterMap().put("service", ticket.getService());
        externalContext.getMockRequestParameterMap().put("ticket", ticket.getId());

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        assertEquals(result.getOutcome().getId(), "ValidateSuccess");
        assertTrue(responseBody.contains("<cas:authenticationSuccess>"));
        assertTrue(responseBody.contains("<cas:user>john</cas:user>"));
        assertFalse(responseBody.contains("<cas:proxyGrantingTicket>"));
        assertTrue(responseBody.contains("<cas:proxy>https://service.example.org/proxy</cas:proxy>"));
    }

    @Test
    public void testFailureTicketExpired() throws Exception {
        externalContext.getMockRequestParameterMap().put("service", "https://test.example.org/");
        externalContext.getMockRequestParameterMap().put("ticket", "PT-123-ABC");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        assertEquals(result.getOutcome().getId(), "ProtocolErrorView");
        final String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("<cas:authenticationFailure code=\"INVALID_TICKET\""));
        assertTrue(responseBody.contains("E_TICKET_EXPIRED"));
    }

    @Test
    public void testSuccessWithProxy() throws Exception {
        final String principal = "john";
        final IdPSession session = sessionManager.createSession(principal);
        final ProxyTicket ticket = createProxyTicket(session.getId(), principal);

        externalContext.getMockRequestParameterMap().put("service", ticket.getService());
        externalContext.getMockRequestParameterMap().put("ticket", ticket.getId());
        externalContext.getMockRequestParameterMap().put("pgtUrl", "https://proxy.example.com/");

        testProxyValidator.setResponseCode(200);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        assertEquals(result.getOutcome().getId(), "ValidateSuccess");
        assertTrue(responseBody.contains("<cas:authenticationSuccess>"));
        assertTrue(responseBody.contains("<cas:user>john</cas:user>"));
        assertTrue(responseBody.contains("<cas:proxyGrantingTicket>"));
        assertTrue(responseBody.contains("<cas:proxy>https://service.example.org/proxy</cas:proxy>"));
    }

    // This test must execute after testSuccessWithProxy to prevent concurrency problems
    // on shared testProxyAuthenticator component
    @Test(dependsOnMethods = "testSuccessWithProxy")
    public void testProxyCallbackAuthnFailure() throws Exception {
        final String principal = "john";
        final IdPSession session = sessionManager.createSession(principal);
        final ProxyTicket ticket = createProxyTicket(session.getId(), principal);

        externalContext.getMockRequestParameterMap().put("service", ticket.getService());
        externalContext.getMockRequestParameterMap().put("ticket", ticket.getId());
        externalContext.getMockRequestParameterMap().put("pgtUrl", "https://proxy.example.com/");

        testProxyValidator.setResponseCode(404);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        assertEquals(result.getOutcome().getId(), "ProtocolErrorView");
        assertTrue(responseBody.contains("<cas:authenticationFailure code=\"INVALID_REQUEST\""));
        assertTrue(responseBody.contains("E_PROXY_CALLBACK_AUTH_FAILURE"));
    }

    @Test
    public void testFailureBrokenProxyChain() throws Exception {
        final String principal = "john";
        final int pgtTTLMillis = 20;
        final IdPSession session = sessionManager.createSession(principal);
        final ServiceTicket st = ticketService.createServiceTicket(
            new TicketIdentifierGenerationStrategy("ST", 25).generateIdentifier(),
            Instant.now().plusSeconds(5),
            "https://service.example.org/",
            new TicketState(session.getId(), principal, Instant.now(), "Password"),
            false);
        final ProxyGrantingTicket pgt1 = ticketService.createProxyGrantingTicket(
            new TicketIdentifierGenerationStrategy("PGT", 50).generateIdentifier(),
            Instant.now().plusMillis(pgtTTLMillis),
            st,
            "https://proxy1.example.org/");
        final ProxyTicket pt1 = ticketService.createProxyTicket(
            new TicketIdentifierGenerationStrategy("PT", 25).generateIdentifier(),
            Instant.now().plusSeconds(5),
            pgt1,
            "https://proxied1.example.org/");
        final ProxyGrantingTicket pgt2 = ticketService.createProxyGrantingTicket(
            new TicketIdentifierGenerationStrategy("PGT", 50).generateIdentifier(),
            Instant.now().plusSeconds(3600),
            pt1,
            "https://proxy2.example.org/");
        final ProxyTicket pt2 = ticketService.createProxyTicket(
            new TicketIdentifierGenerationStrategy("PT", 25).generateIdentifier(),
            Instant.now().plusSeconds(5),
            pgt2,
            "https://proxied2.example.org/");

        externalContext.getMockRequestParameterMap().put("service", pt2.getService());
        externalContext.getMockRequestParameterMap().put("ticket", pt2.getId());

        // Wait for PGT#1 to expire
        Thread.sleep(pgtTTLMillis + 5);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        assertEquals(result.getOutcome().getId(), "ProtocolErrorView");
        assertTrue(responseBody.contains("<cas:authenticationFailure code=\"INVALID_TICKET\""));
        assertTrue(responseBody.contains("E_BROKEN_PROXY_CHAIN"));
    }

    private ProxyTicket createProxyTicket(final String sessionId, final String principal) {
        final ServiceTicket st = ticketService.createServiceTicket(
                new TicketIdentifierGenerationStrategy("ST", 25).generateIdentifier(),
                Instant.now().plusSeconds(5),
                "https://service.example.org/",
                new TicketState(sessionId, principal, Instant.now(), "Password"),
                false);
        final ProxyGrantingTicket pgt = ticketService.createProxyGrantingTicket(
                new TicketIdentifierGenerationStrategy("PGT", 50).generateIdentifier(),
                Instant.now().plusSeconds(10),
                st,
                "https://service.example.org/proxy");
        return ticketService.createProxyTicket(
                new TicketIdentifierGenerationStrategy("PT", 25).generateIdentifier(),
                Instant.now().plusSeconds(5),
                pgt,
                "https://proxiedA.example.org/");
    }
}

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

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.cas.flow.ValidateProxyCallbackAction;
import net.shibboleth.idp.cas.proxy.ProxyAuthenticator;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionManager;
import net.shibboleth.idp.test.flows.AbstractFlowTest;
import org.joda.time.DateTime;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.X509Credential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;

import java.net.URI;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Tests the flow behind the <code>/serviceValidate</code> endpoint.
 *
 * @author Marvin S. Addison
 */
@ContextConfiguration(locations = {
        "/test/test-cas-beans.xml",
})
public class ServiceValidateFlowTest extends AbstractFlowTest {

    /** Flow id. */
    @Nonnull
    private static String FLOW_ID = "cas/serviceValidate";

    @Autowired
    private TicketService ticketService;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private TestProxyAuthenticator testProxyAuthenticator;

    @Test
    public void testSuccess() throws Exception {
        final String principal = "john";
        final IdPSession session = sessionManager.createSession(principal);
        session.addAuthenticationResult(
                new AuthenticationResult("authn/Password", new UsernamePrincipal(principal)));

        final ServiceTicket ticket = ticketService.createServiceTicket(
                "ST-1415133132-ompog68ygxKyX9BPwPuw0hESQBjuA",
                DateTime.now().plusSeconds(5).toInstant(),
                session.getId(),
                "https://test.example.org/",
                false);

        externalContext.getMockRequestParameterMap().put("service", ticket.getService());
        externalContext.getMockRequestParameterMap().put("ticket", ticket.getId());

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        assertEquals(result.getOutcome().getId(), "validateSuccess");
        assertTrue(responseBody.contains("<cas:authenticationSuccess>"));
        assertTrue(responseBody.contains("<cas:user>john</cas:user>"));
        assertFalse(responseBody.contains("<cas:proxyGrantingTicket>"));
        assertFalse(responseBody.contains("<cas:proxies>"));
    }

    @Test
    public void testFailureTicketExpired() throws Exception {
        externalContext.getMockRequestParameterMap().put("service", "https://test.example.org/");
        externalContext.getMockRequestParameterMap().put("ticket", "ST-123-ABC");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        assertEquals(result.getOutcome().getId(), "validateFailure");
        final String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("<cas:authenticationFailure code=\"INVALID_TICKET\""));
        assertTrue(responseBody.contains("E_TICKET_EXPIRED"));
    }

    @Test
    public void testFailureSessionExpired() throws Exception {
        final ServiceTicket ticket = ticketService.createServiceTicket(
                "ST-1415133227-o5ly5eArKccYkb2P+80uRE7Gq9xSAqWtOg",
                DateTime.now().plusSeconds(5).toInstant(),
                "No-Such-Session-Id",
                "https://test.example.org/",
                false);

        externalContext.getMockRequestParameterMap().put("service", ticket.getService());
        externalContext.getMockRequestParameterMap().put("ticket", ticket.getId());

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        assertEquals(result.getOutcome().getId(), "validateFailure");
        final String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("<cas:authenticationFailure code=\"INVALID_TICKET\""));
        assertTrue(responseBody.contains("E_SESSION_EXPIRED"));
    }

    @Test
    public void testSuccessWithProxy() throws Exception {
        final String principal = "john";
        final IdPSession session = sessionManager.createSession(principal);
        session.addAuthenticationResult(
                new AuthenticationResult("authn/Password", new UsernamePrincipal(principal)));

        final ServiceTicket ticket = ticketService.createServiceTicket(
                "ST-1415133132-ompog68ygxKyX9BPwPuw0hESQBjuA",
                DateTime.now().plusSeconds(5).toInstant(),
                session.getId(),
                "https://test.example.org/",
                false);

        externalContext.getMockRequestParameterMap().put("service", ticket.getService());
        externalContext.getMockRequestParameterMap().put("ticket", ticket.getId());
        externalContext.getMockRequestParameterMap().put("pgtUrl", "https://proxy.example.com/");

        testProxyAuthenticator.setFailureFlag(false);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        assertEquals(result.getOutcome().getId(), "validateSuccess");
        assertTrue(responseBody.contains("<cas:authenticationSuccess>"));
        assertTrue(responseBody.contains("<cas:user>john</cas:user>"));
        assertTrue(responseBody.contains("<cas:proxyGrantingTicket>"));
        assertFalse(responseBody.contains("<cas:proxies>"));
    }

    @Test
    public void testProxyCallbackAuthnFailure() throws Exception {
        final String principal = "john";
        final IdPSession session = sessionManager.createSession(principal);
        session.addAuthenticationResult(
                new AuthenticationResult("authn/Password", new UsernamePrincipal(principal)));

        final ServiceTicket ticket = ticketService.createServiceTicket(
                "ST-1415133132-ompog68ygxKyX9BPwPuw0hESQBjuA",
                DateTime.now().plusSeconds(5).toInstant(),
                session.getId(),
                "https://test.example.org/",
                false);

        externalContext.getMockRequestParameterMap().put("service", ticket.getService());
        externalContext.getMockRequestParameterMap().put("ticket", ticket.getId());
        externalContext.getMockRequestParameterMap().put("pgtUrl", "https://proxy.example.com/");

        testProxyAuthenticator.setFailureFlag(true);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        assertEquals(result.getOutcome().getId(), "validateFailure");
        assertTrue(responseBody.contains("<cas:authenticationFailure code=\"INVALID_REQUEST\""));
        assertTrue(responseBody.contains("E_PROXY_CALLBACK_AUTH_FAILURE"));
    }

    private static ProxyAuthenticator<TrustEngine<X509Credential>> mockProxyAuthenticator(final Exception toBeThrown)
            throws Exception {
        final ProxyAuthenticator<TrustEngine<X509Credential>> authenticator = mock(ProxyAuthenticator.class);
        if (toBeThrown != null) {
            doThrow(toBeThrown).when(authenticator).authenticate(any(URI.class), any(TrustEngine.class));
        }
        return authenticator;
    }
}

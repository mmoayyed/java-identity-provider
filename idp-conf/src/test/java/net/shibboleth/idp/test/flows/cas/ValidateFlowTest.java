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

import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketServiceEx;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionManager;
import net.shibboleth.idp.session.SessionResolver;
import net.shibboleth.idp.session.criterion.SessionIdCriterion;
import net.shibboleth.idp.test.flows.AbstractFlowTest;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests the flow behind the <code>/validate</code> endpoint.
 *
 * @author Marvin S. Addison
 */
@ContextConfiguration(locations = {
        "/test/test-cas-beans.xml",
})
public class ValidateFlowTest extends AbstractFlowTest {

    /** Flow id. */
    @Nonnull
    private static String FLOW_ID = "cas/validate";

    @Autowired
    @Qualifier("shibboleth.CASTicketService")
    private TicketServiceEx ticketService;

    @Autowired
    private SessionResolver sessionResolver;

    @Autowired
    private SessionManager sessionManager;

    @Test
    public void testSuccess() throws Exception {
        final String principal = "john";
        final IdPSession session = sessionManager.createSession(principal);
        final ServiceTicket ticket = ticketService.createServiceTicket(
                "ST-1415133132-ompog68ygxKyX9BPwPuw0hESQBjuA",
                DateTime.now().plusSeconds(5).toInstant(),
                "https://test.example.org/",
                new TicketState(session.getId(), principal, Instant.now(), "Password"),
                false);

        externalContext.getMockRequestParameterMap().put("service", ticket.getService());
        externalContext.getMockRequestParameterMap().put("ticket", ticket.getId());
        overrideEndStateOutput(FLOW_ID, "ValidateSuccess");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        final FlowExecutionOutcome outcome = result.getOutcome();
        assertEquals(outcome.getId(), "ValidateSuccess");
        assertTrue(responseBody.contentEquals("yes\njohn\n"));
        assertEquals("text/plain;charset=utf-8", response.getContentType());

        final IdPSession updatedSession = sessionResolver.resolveSingle(
                new CriteriaSet(new SessionIdCriterion(session.getId())));
        assertNotNull(updatedSession);
        assertEquals(updatedSession.getSPSessions().size(), 0);
    }

    @Test
    public void testFailureTicketExpired() throws Exception {
        externalContext.getMockRequestParameterMap().put("service", "https://test.example.org/");
        externalContext.getMockRequestParameterMap().put("ticket", "ST-123-ABC");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        assertEquals(result.getOutcome().getId(), "ProtocolErrorView");
        final String responseBody = response.getContentAsString();
        assertTrue(responseBody.contentEquals("no\n\n"));
        assertEquals("text/plain;charset=utf-8", response.getContentType());
    }
}

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

package net.shibboleth.idp.cas.flow.impl;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.cas.config.LoginConfiguration;
import net.shibboleth.idp.cas.protocol.ServiceTicketRequest;
import net.shibboleth.idp.cas.protocol.ServiceTicketResponse;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketState;

import net.shibboleth.idp.session.IdPSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.webflow.execution.RequestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit test for {@link GrantServiceTicketAction}.
 *
 * @author Marvin S. Addison
 */
@SuppressWarnings("javadoc")
public class GrantServiceTicketActionTest extends AbstractFlowActionTest {

    @Autowired
    private GrantServiceTicketAction action;


    @DataProvider(name = "messages")
    public Object[][] provideMessages() {
        final ServiceTicketRequest renewedRequest = new ServiceTicketRequest("https://www.example.com/beta");
        renewedRequest.setRenew(true);
        return new Object[][] {
                { new ServiceTicketRequest("https://www.example.com/alpha") },
                { renewedRequest },
        };
    }

    @Test(dataProvider = "messages")
    public void testExecute(final ServiceTicketRequest request) throws Exception {
        final IdPSession session = mockSession("1234567890", true);
        final AuthenticationResult result = new AuthenticationResult("Password", new UsernamePrincipal("bob"));
        final RequestContext context = new TestContextBuilder(LoginConfiguration.PROFILE_ID)
                .addProtocolContext(request, null)
                .addAuthenticationContext(result)
                .addSessionContext(session)
                .addSubjectContext(TEST_PRINCIPAL_NAME)
                .addRelyingPartyContext(request.getService(), true, new LoginConfiguration())
                .build();
        assertNull(action.execute(context));
        final ServiceTicketResponse response = action.getCASResponse(getProfileContext(context));
        assertNotNull(response);
        assertNotNull(response.getTicket());
        assertEquals(response.getService(), request.getService());
        final ServiceTicket ticket = ticketService.removeServiceTicket(response.getTicket());
        assert ticket!=null;
        assertEquals(ticket.isRenew(), request.isRenew());
        assertEquals(ticket.getId(), response.getTicket());
        assertEquals(ticket.getService(), response.getService());
        final TicketState ts = ticket.getTicketState();
        assert ts != null;
        assertEquals(ts.getPrincipalName(), TEST_PRINCIPAL_NAME);
    }
}

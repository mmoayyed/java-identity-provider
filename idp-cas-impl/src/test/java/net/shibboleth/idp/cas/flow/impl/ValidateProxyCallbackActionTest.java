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

import java.net.URI;
import java.security.cert.CertificateException;
import java.time.Instant;

import javax.annotation.Nonnull;

import net.shibboleth.idp.cas.config.ValidateConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;
import net.shibboleth.idp.cas.proxy.ProxyValidator;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketState;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit test for {@link ValidateProxyCallbackAction}.
 *
 * @author Marvin S. Addison
 */
@SuppressWarnings("javadoc")
public class ValidateProxyCallbackActionTest extends AbstractFlowActionTest {

    private Object nullObj;
    
    @Test
    public void testValidateProxySuccess() throws Exception {
        @SuppressWarnings("null")
        final ValidateProxyCallbackAction action = new ValidateProxyCallbackAction(
                mockProxyAuthenticator((Exception) nullObj), ticketService);
        action.initialize();
        final RequestContext context = newRequestContext("https://test.example.org/");
        assertNull(action.execute(context));
        final TicketValidationResponse response = action.getCASResponse(getProfileContext(context));
        assertNotNull(response);
        assertNotNull(response.getPgtIou());
    }

    @Test
    public void testValidateProxyFailure() throws Exception {
        final ValidateProxyCallbackAction action = new ValidateProxyCallbackAction(
                mockProxyAuthenticator(new CertificateException()), ticketService);
        action.initialize();
        final  Event event =  action.execute(newRequestContext("https://test.example.org/"));
        assert event != null;
        assertEquals(event.getId(),ProtocolError.ProxyCallbackAuthenticationFailure.name());
    }

    @Nonnull private static ProxyValidator mockProxyAuthenticator(@Nonnull final Exception toBeThrown)
            throws Exception {
        final ProxyValidator validator = mock(ProxyValidator.class);
        assert validator!= null;
        if (toBeThrown != null) {
            doThrow(toBeThrown).when(validator).validate(any(ProfileRequestContext.class), any(URI.class));
        }
        return validator;
    }

    @Nonnull private static RequestContext newRequestContext(final String pgtURL) {
        final String service = "https://test.example.com/";
        final String ticketId = "ST-123-ABCCEF";
        final ServiceTicket st = new ServiceTicket(ticketId, service, Instant.now(), false);
        st.setTicketState(new TicketState("SessionID-123", "bob", Instant.now(), "bob"));
        final TicketValidationRequest request = new TicketValidationRequest(service, ticketId);
        request.setPgtUrl(pgtURL);
        final RequestContext context = new TestContextBuilder(ValidateConfiguration.PROFILE_ID)
                .addProtocolContext(request, new TicketValidationResponse())
                .addTicketContext(st)
                .addRelyingPartyContext(service, true, new ValidateConfiguration())
                .build();
        return context;
    }

}
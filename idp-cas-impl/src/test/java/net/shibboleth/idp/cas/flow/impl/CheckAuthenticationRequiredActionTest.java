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

import net.shibboleth.idp.cas.config.impl.LoginConfiguration;
import net.shibboleth.idp.cas.protocol.ServiceTicketRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.webflow.execution.RequestContext;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Unit test for {@link CheckAuthenticationRequiredAction} class.
 *
 * @author Marvin S. Addison
 */
public class CheckAuthenticationRequiredActionTest extends AbstractFlowActionTest {

    @Autowired
    private CheckAuthenticationRequiredAction action;

    @Test
    public void testGatewayRequested() throws Exception {
        final ServiceTicketRequest request = new ServiceTicketRequest("a");
        request.setGateway(true);
        final RequestContext context = new TestContextBuilder(LoginConfiguration.PROFILE_ID)
                .addProtocolContext(request, null)
                .build();
        assertEquals(action.execute(context).getId(), Events.GatewayRequested.name());
    }

    @Test
    public void testSessionNotFound() throws Exception {
        final RequestContext context = new TestContextBuilder(LoginConfiguration.PROFILE_ID)
                .addProtocolContext(new ServiceTicketRequest("b"), null)
                .build();
        assertEquals(action.execute(context).getId(), Events.SessionNotFound.name());
    }

    @Test
    public void testSessionExpired() throws Exception {
        final RequestContext context = new TestContextBuilder(LoginConfiguration.PROFILE_ID)
                .addProtocolContext(new ServiceTicketRequest("c"), null)
                .addSessionContext(mockSession("ABCDE", false))
                .build();
        assertEquals(action.execute(context).getId(), Events.SessionNotFound.name());
    }

    @Test
    public void testSessionFound() throws Exception {
        final RequestContext context = new TestContextBuilder(LoginConfiguration.PROFILE_ID)
                .addProtocolContext(new ServiceTicketRequest("d"), null)
                .addSessionContext(mockSession("12345", true))
                .build();
        assertEquals(action.execute(context).getId(), Events.SessionFound.name());
    }

    @Test
    public void testRenewRequested() throws Exception {
        final ServiceTicketRequest request = new ServiceTicketRequest("e");
        request.setRenew(true);
        final RequestContext context = new TestContextBuilder(LoginConfiguration.PROFILE_ID)
                .addProtocolContext(request, null)
                .addSessionContext(mockSession("98765", true))
                .build();
        assertEquals(action.execute(context).getId(), Events.RenewRequested.name());
    }
}

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
        final RequestContext context = new TestContextBuilder(ServiceTicketConfiguration.PROFILE_ID).build();
        final ServiceTicketRequest request = new ServiceTicketRequest("a");
        request.setGateway(true);
        FlowStateSupport.setServiceTicketRequest(context, request);
        assertEquals(action.execute(context).getId(), Events.GatewayRequested.id());
    }

    @Test
    public void testSessionNotFound() throws Exception {
        final RequestContext context = new TestContextBuilder(ServiceTicketConfiguration.PROFILE_ID).build();
        final ServiceTicketRequest request = new ServiceTicketRequest("b");
        FlowStateSupport.setServiceTicketRequest(context, request);
        assertEquals(action.execute(context).getId(), Events.SessionNotFound.id());
    }

    @Test
    public void testSessionExpired() throws Exception {
        final RequestContext context = new TestContextBuilder(ServiceTicketConfiguration.PROFILE_ID)
                .addSessionContext(mockSession("ABCDE", false))
                .build();
        final ServiceTicketRequest request = new ServiceTicketRequest("b");
        FlowStateSupport.setServiceTicketRequest(context, request);
        assertEquals(action.execute(context).getId(), Events.SessionNotFound.id());
    }

    @Test
    public void testSessionFound() throws Exception {
        final RequestContext context = new TestContextBuilder(ServiceTicketConfiguration.PROFILE_ID)
                .addSessionContext(mockSession("12345", true))
                .build();
        final ServiceTicketRequest request = new ServiceTicketRequest("c");
        FlowStateSupport.setServiceTicketRequest(context, request);
        assertEquals(action.execute(context).getId(), Events.SessionFound.id());
    }

    @Test
    public void testRenewRequested() throws Exception {
        final RequestContext context = new TestContextBuilder(ServiceTicketConfiguration.PROFILE_ID)
                .addSessionContext(mockSession("98765", true))
                .build();
        final ServiceTicketRequest request = new ServiceTicketRequest("d");
        request.setRenew(true);
        FlowStateSupport.setServiceTicketRequest(context, request);
        assertEquals(action.execute(context).getId(), Events.RenewRequested.id());
    }
}

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
import net.shibboleth.idp.cas.protocol.ProxyTicketRequest;
import net.shibboleth.idp.cas.protocol.ServiceTicketRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.service.ServiceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.webflow.execution.RequestContext;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class BuildRelyingPartyContextActionTest extends AbstractFlowActionTest {

    @Autowired
    private BuildRelyingPartyContextAction action;

    @Test
    public void testExecuteFromServiceTicketRequest() {
        final String serviceURL = "https://serviceA.example.org:8443/landing";
        final RequestContext requestContext = new TestContextBuilder(ServiceTicketConfiguration.PROFILE_ID).build();
        FlowStateSupport.setServiceTicketRequest(requestContext, new ServiceTicketRequest(serviceURL));
        action.execute(requestContext);
        final ServiceContext sc = getProfileContext(requestContext).getSubcontext(ServiceContext.class);
        assertNotNull(sc);
        assertNotNull(sc.getService());
        assertEquals(serviceURL, sc.getService().getName());
        assertEquals("allowedToProxy", sc.getService().getGroup());
        assertTrue(sc.getService().isAuthorizedToProxy());
    }

    @Test
    public void testExecuteFromTicketValidationRequest() {
        final String serviceURL = "http://serviceB.example.org/";
        final RequestContext requestContext = new TestContextBuilder(ServiceTicketConfiguration.PROFILE_ID).build();
        FlowStateSupport.setTicketValidationRequest(requestContext, new TicketValidationRequest(serviceURL, "ST-123"));
        action.execute(requestContext);
        final ServiceContext sc = getProfileContext(requestContext).getSubcontext(ServiceContext.class);
        assertNotNull(sc);
        assertNotNull(sc.getService());
        assertEquals(serviceURL, sc.getService().getName());
        assertEquals("notAllowedToProxy", sc.getService().getGroup());
        assertFalse(sc.getService().isAuthorizedToProxy());
    }

    @Test
    public void testExecuteFromProxyTicketRequest() {
        final String serviceURL = "http://mallory.untrusted.org/";
        final RequestContext requestContext = new TestContextBuilder(ServiceTicketConfiguration.PROFILE_ID).build();
        FlowStateSupport.setProxyTicketRequest(requestContext, new ProxyTicketRequest("PGT-123", serviceURL));
        action.execute(requestContext);
        final ServiceContext sc = getProfileContext(requestContext).getSubcontext(ServiceContext.class);
        assertNotNull(sc);
        assertNotNull(sc.getService());
        assertEquals(serviceURL, sc.getService().getName());
        assertEquals(BuildRelyingPartyContextAction.UNVERIFIED_GROUP, sc.getService().getGroup());
        assertFalse(sc.getService().isAuthorizedToProxy());
    }
}
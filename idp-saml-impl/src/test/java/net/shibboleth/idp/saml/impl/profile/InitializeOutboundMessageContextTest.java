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

package net.shibboleth.idp.saml.impl.profile;

import net.shibboleth.idp.saml.impl.profile.InitializeOutboundMessageContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.RequestContextBuilder;
import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml1.core.Request;
import org.opensaml.saml.saml1.profile.SAML1ActionTestingSupport;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link InitializeOutboundMessageContext} unit test. */
public class InitializeOutboundMessageContextTest extends OpenSAMLInitBaseTestCase {

    private ProfileRequestContext prc;
    
    private Request attributeQuery;
    
    private InitializeOutboundMessageContext action;

    @BeforeMethod public void setUp() throws ComponentInitializationException {
        attributeQuery = SAML1ActionTestingSupport.buildAttributeQueryRequest(
                SAML1ActionTestingSupport.buildSubject("jdoe"));
        prc = new RequestContextBuilder().setInboundMessage(attributeQuery).buildProfileRequestContext();
        prc.setOutboundMessageContext(null);
        action = new InitializeOutboundMessageContext();
        action.setId("test");
        action.initialize();
    }

    @Test public void testNoInboundContext() throws ProfileException {
        final ProfileRequestContext localprc = new ProfileRequestContext();

        action.execute(localprc);
        ActionTestingSupport.assertEvent(localprc, EventIds.INVALID_MSG_CTX);
        Assert.assertNull(localprc.getOutboundMessageContext());
    }

    @Test public void testNoPeerEntityContext() throws ProfileException {
        action.execute(prc);

        ActionTestingSupport.assertEvent(prc, EventIds.INVALID_MSG_CTX);
        Assert.assertNull(prc.getOutboundMessageContext());
    }

    @Test public void testPeerEntityContextNoIssuer() throws ProfileException {
        prc.getInboundMessageContext().getSubcontext(SAMLPeerEntityContext.class, true);
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertNotNull(prc.getOutboundMessageContext());
        final SAMLPeerEntityContext ctx =
                prc.getOutboundMessageContext().getSubcontext(SAMLPeerEntityContext.class, false);
        Assert.assertNotNull(ctx);
        Assert.assertNull(ctx.getEntityId());
    }

    @Test public void testPeerEntityContextIssuer() throws ProfileException {
        prc.getInboundMessageContext().getSubcontext(SAMLPeerEntityContext.class, true);
        attributeQuery.getAttributeQuery().setResource("issuer");
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertNotNull(prc.getOutboundMessageContext());
        final SAMLPeerEntityContext ctx =
                prc.getOutboundMessageContext().getSubcontext(SAMLPeerEntityContext.class, false);
        Assert.assertNotNull(ctx);
        Assert.assertEquals(ctx.getEntityId(), "issuer");
    }
    
    // TODO more tests

}
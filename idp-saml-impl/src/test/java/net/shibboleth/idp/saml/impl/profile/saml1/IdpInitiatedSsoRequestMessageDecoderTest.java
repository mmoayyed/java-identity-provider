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

package net.shibboleth.idp.saml.impl.profile.saml1;

import net.shibboleth.idp.saml.impl.profile.BaseIdpInitiatedSsoRequestMessageDecoder;
import net.shibboleth.idp.saml.impl.profile.IdpInitatedSsoRequest;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.common.messaging.context.SamlBindingContext;
import org.opensaml.saml.common.messaging.context.SamlMessageInfoContext;
import org.opensaml.saml.common.messaging.context.SamlPeerEntityContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test the {@link IdpInitiatedSsoRequestMessageDecoder}.
 */
public class IdpInitiatedSsoRequestMessageDecoderTest {
    
    private IdpInitiatedSsoRequestMessageDecoder decoder;
    
    private MockHttpServletRequest request;
    
    private String entityId = "http://sp.example.org";
    
    private String acsUrl = "http://sp.example.org/acs";
    
    private String relayState = "myRelayState";
    
    private String sessionID = "abc123";
    
    private String messageID;
    
    private Long time;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        // Note: protocol takes time in seconds, so divide by 1000.
        // Components usually produce milliseconds, so later multiply or divide by 1000 in assertions as appropriate.
        time = System.currentTimeMillis()/1000;
        
        messageID = "_" + sessionID + "!" + time.toString();
        
        request = new MockHttpServletRequest();
        request.setRequestedSessionId(sessionID);
        
        decoder = new IdpInitiatedSsoRequestMessageDecoder();
        decoder.setHttpServletRequest(request);
        decoder.initialize();
    }
    
    @Test
    public void testOldStyleParams() throws MessageDecodingException {
        request.addParameter(BaseIdpInitiatedSsoRequestMessageDecoder.PROVIDER_ID_PARAM,  entityId);
        request.addParameter(BaseIdpInitiatedSsoRequestMessageDecoder.SHIRE_PARAM,  acsUrl);
        request.addParameter(BaseIdpInitiatedSsoRequestMessageDecoder.TARGET_PARAM,  relayState);
        request.addParameter(BaseIdpInitiatedSsoRequestMessageDecoder.TIME_PARAM,  time.toString());
        
        decoder.decode();
        
        MessageContext<IdpInitatedSsoRequest> messageContext = decoder.getMessageContext();
        Assert.assertNotNull(messageContext);
        IdpInitatedSsoRequest ssoRequest = messageContext.getMessage();
        Assert.assertNotNull(ssoRequest);
        
        Assert.assertEquals(ssoRequest.getEntityId(), entityId, "Incorrect decoded entityId value");
        Assert.assertEquals(ssoRequest.getAcsUrl(), acsUrl, "Incorrect decoded ACS URL value");
        Assert.assertEquals(ssoRequest.getRelayState(), relayState, "Incorrect decoded relay state value");
        Assert.assertEquals(Long.valueOf(ssoRequest.getTime()/1000), time, "Incorrect decoded time value");
        
        Assert.assertEquals(messageContext.getSubcontext(SamlPeerEntityContext.class, true).getEntityId(), entityId,
                "Incorrect decoded entityId value in peer context");
        
        SamlBindingContext bindingContext = messageContext.getSubcontext(SamlBindingContext.class, true);
        Assert.assertEquals(bindingContext.getRelayState(), relayState, "Incorrect decoded relay state value in binding context");
        Assert.assertEquals(bindingContext.getBindingUri(), "urn:mace:shibboleth:1.0:profiles:AuthnRequest",
                "Incorrect binding URI in binding context");
        
        SamlMessageInfoContext msgInfoContext = messageContext.getSubcontext(SamlMessageInfoContext.class, true);
        Assert.assertEquals(msgInfoContext.getMessageIssueInstant(), new DateTime(time*1000, ISOChronology.getInstanceUTC()),
                "Incorrect decoded issue instant value in message info context");
        Assert.assertEquals(msgInfoContext.getMessageId(), messageID, "Incorrect decoded message ID value in message info context");
    }
    
    @Test
    public void testNewStyleParams() throws MessageDecodingException {
        request.addParameter(BaseIdpInitiatedSsoRequestMessageDecoder.ENTITY_ID_PARAM,  entityId);
        request.addParameter(BaseIdpInitiatedSsoRequestMessageDecoder.ACS_URL_PARAM,  acsUrl);
        request.addParameter(BaseIdpInitiatedSsoRequestMessageDecoder.RELAY_STATE_PARAM,  relayState);
        request.addParameter(BaseIdpInitiatedSsoRequestMessageDecoder.TIME_PARAM,  time.toString());
        
        decoder.decode();
        
        MessageContext<IdpInitatedSsoRequest> messageContext = decoder.getMessageContext();
        Assert.assertNotNull(messageContext);
        IdpInitatedSsoRequest ssoRequest = messageContext.getMessage();
        Assert.assertNotNull(ssoRequest);
        
        Assert.assertEquals(ssoRequest.getEntityId(), entityId, "Incorrect decoded entityId value");
        Assert.assertEquals(ssoRequest.getAcsUrl(), acsUrl, "Incorrect decoded ACS URL value");
        Assert.assertEquals(ssoRequest.getRelayState(), relayState, "Incorrect decoded relay state value");
        Assert.assertEquals(Long.valueOf(ssoRequest.getTime()/1000), time, "Incorrect decoded time value");
        
        Assert.assertEquals(messageContext.getSubcontext(SamlPeerEntityContext.class, true).getEntityId(), entityId,
                "Incorrect decoded entityId value in peer context");
        
        SamlBindingContext bindingContext = messageContext.getSubcontext(SamlBindingContext.class, true);
        Assert.assertEquals(bindingContext.getRelayState(), relayState, "Incorrect decoded relay state value in binding context");
        Assert.assertEquals(bindingContext.getBindingUri(), "urn:mace:shibboleth:1.0:profiles:AuthnRequest",
                "Incorrect binding URI in binding context");
        
        SamlMessageInfoContext msgInfoContext = messageContext.getSubcontext(SamlMessageInfoContext.class, true);
        Assert.assertEquals(msgInfoContext.getMessageIssueInstant(), new DateTime(time*1000, ISOChronology.getInstanceUTC()),
                "Incorrect decoded issue instant value in message info context");
        Assert.assertEquals(msgInfoContext.getMessageId(), messageID, "Incorrect decoded message ID value in message info context");
        
    }

}

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

package net.shibboleth.idp.saml.saml2.profile.impl;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.saml2.profile.config.impl.BrowserSSOProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.component.ComponentInitializationException;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.NameIDType;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link EnforceDisallowedSSOFeatures} unit test. */
@SuppressWarnings("javadoc")
public class EnforceDisallowedSSOFeaturesTest extends OpenSAMLInitBaseTestCase {

    private MockRequestContext src; 
    
    private ProfileRequestContext prc;
    
    private BrowserSSOProfileConfiguration profileConfig;
    
    private EnforceDisallowedSSOFeatures action;
    
    private SAMLObjectBuilder<NameIDPolicy> nidBuilder;
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        nidBuilder = (SAMLObjectBuilder<NameIDPolicy>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<NameIDPolicy>ensureBuilder(
                        NameIDPolicy.DEFAULT_ELEMENT_NAME);
        
        src = (MockRequestContext) new RequestContextBuilder().buildRequestContext();
        prc = (ProfileRequestContext) src.getConversationScope().get(ProfileRequestContext.BINDING_KEY);
        profileConfig = new BrowserSSOProfileConfiguration();
        prc.ensureSubcontext(RelyingPartyContext.class).setProfileConfig(profileConfig);
        
        action = new EnforceDisallowedSSOFeatures();
        action.initialize();
    }
    
    @Test public void testNoRequest() {
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_MSG_CTX);
    }

    @Test public void testGeneric() {
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildAuthnRequest());
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

    @Test public void testForceAuthn() {
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildAuthnRequest());
        AuthnRequest ar = (AuthnRequest)imc.getMessage();
        assert ar!=null;
        ar.setForceAuthn(true);

        Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        profileConfig.setDisallowedFeatures(BrowserSSOProfileConfiguration.FEATURE_FORCEAUTHN);
        event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.ACCESS_DENIED);
        
        ar = (AuthnRequest)imc.getMessage();
        assert ar!=null;
        ar.setForceAuthn(false);
        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

    @Test public void testFormat() {
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildAuthnRequest());
        
        final NameIDPolicy policy = nidBuilder.buildObject();
        policy.setFormat(NameIDType.EMAIL);
        
        final AuthnRequest ar = (AuthnRequest)imc.getMessage();
        assert ar!=null;
        ar.setNameIDPolicy(policy);

        Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        profileConfig.setDisallowedFeatures(BrowserSSOProfileConfiguration.FEATURE_NAMEIDFORMAT);
        event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.ACCESS_DENIED);

        policy.setFormat(NameIDType.UNSPECIFIED);
        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        policy.setFormat(NameIDType.ENCRYPTED);
        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        policy.setFormat(null);
        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
}

    @Test public void testSPNameQualifier() {
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildAuthnRequest());
        
        final NameIDPolicy policy = nidBuilder.buildObject();
        policy.setSPNameQualifier(ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        
        final AuthnRequest ar = (AuthnRequest)imc.getMessage();
        assert ar!=null;
        ar.setNameIDPolicy(policy);

        Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        profileConfig.setDisallowedFeatures(BrowserSSOProfileConfiguration.FEATURE_SPNAMEQUALIFIER);
        event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.ACCESS_DENIED);
        
        policy.setSPNameQualifier(null);
        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

}
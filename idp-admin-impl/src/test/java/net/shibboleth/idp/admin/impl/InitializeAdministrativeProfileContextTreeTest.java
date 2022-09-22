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

package net.shibboleth.idp.admin.impl;

import java.util.Collections;
import java.util.function.Supplier;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.admin.BasicAdministrativeFlowDescriptor;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.primitive.LangBearingString;

/** {@link InitializeAdministrativeProfileContextTree} unit test. */
@SuppressWarnings("javadoc")
public class InitializeAdministrativeProfileContextTreeTest extends OpenSAMLInitBaseTestCase {

    private RequestContext src;
    
    private ProfileRequestContext prc;

    private InitializeAdministrativeProfileContextTree action;
    
    private BasicAdministrativeFlowDescriptor descriptor;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        prc.removeSubcontext(RelyingPartyContext.class);

        descriptor = new BasicAdministrativeFlowDescriptor("foo");
        descriptor.setLoggingId("log");
        descriptor.setNonBrowserSupported(true);
        descriptor.setDisplayNames(Collections.singletonList(new LangBearingString("name", "en")));
        descriptor.setDescriptions(Collections.singletonList(new LangBearingString("description", "en")));
        descriptor.setLogos(Collections.singletonList(new BasicAdministrativeFlowDescriptor.Logo("http://logo", null, 10, 10)));
        
        action = new InitializeAdministrativeProfileContextTree();
        action.setAdministrativeFlowDescriptor(descriptor);
        action.setHttpServletRequestSupplier(new Supplier<> () {public HttpServletRequest get() {
            return (HttpServletRequest) src.getExternalContext().getNativeRequest();
            }
        });
        action.initialize();
    }
    
    @Test public void testNoDescriptor() throws ComponentInitializationException {
        
        action = new InitializeAdministrativeProfileContextTree();
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, IdPEventIds.INVALID_PROFILE_CONFIG);
    }

    @Test public void testBadDescriptor() throws ComponentInitializationException {
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, IdPEventIds.INVALID_PROFILE_CONFIG);
    }

    @Test public void testAction() throws Exception {

        prc.setProfileId("foo");
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        Assert.assertEquals(prc.getLoggingId(), "log");
        
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class);
        Assert.assertNotNull(rpc);
        
        Assert.assertEquals(rpc.getRelyingPartyId(), "foo");
        Assert.assertNotNull(rpc.getProfileConfig());
        Assert.assertSame(rpc.getProfileConfig(), descriptor);
        
        final RelyingPartyUIContext ui = rpc.getSubcontext(RelyingPartyUIContext.class);
        Assert.assertNotNull(ui);
        Assert.assertEquals(ui.getServiceName(), "name");
        Assert.assertEquals(ui.getServiceDescription(), "description");
        Assert.assertEquals(ui.getLogo(), "http://logo");
    }
    
}

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

package net.shibboleth.idp.authn.duo.impl;


import javax.servlet.http.HttpServletRequest;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.duo.DuoAuthAPI;
import net.shibboleth.idp.authn.duo.context.DuoAuthenticationContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.primitive.NonnullSupplier;

/** {@link ExtractDuoAuthenticationFromHeaders} unit test. */
public class ExtractDuoAuthenticationFromHeadersTest extends BaseAuthenticationContextTest {
    
    private ExtractDuoAuthenticationFromHeaders action; 
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        action = new ExtractDuoAuthenticationFromHeaders();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new NonnullSupplier<> () {public HttpServletRequest get() { return request;}});
        action.initialize();
    }
    
    @Test public void testNoServletNoAuto() throws ComponentInitializationException {
        action = new ExtractDuoAuthenticationFromHeaders();
        action.setAutoAuthenticationSupported(false);
        action.initialize();
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        final DuoAuthenticationContext duoCtx = authCtx.getSubcontext(DuoAuthenticationContext.class, false);
        Assert.assertNull(duoCtx);
    }

    @Test public void testNoAuto() throws ComponentInitializationException {
        action = new ExtractDuoAuthenticationFromHeaders();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new NonnullSupplier<> () {public HttpServletRequest get() { return request;}});
        action.setAutoAuthenticationSupported(false);
        action.initialize();
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);

        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        final DuoAuthenticationContext duoCtx = authCtx.getSubcontext(DuoAuthenticationContext.class, false);
        Assert.assertNull(duoCtx);
    }
    
    @Test public void testNoServletAuto() throws ComponentInitializationException {
        action = new ExtractDuoAuthenticationFromHeaders();
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        final DuoAuthenticationContext duoCtx = authCtx.getSubcontext(DuoAuthenticationContext.class, false);
        Assert.assertNotNull(duoCtx);
        Assert.assertEquals(duoCtx.getFactor(), DuoAuthAPI.DUO_FACTOR_AUTO);
        Assert.assertEquals(duoCtx.getDeviceID(), DuoAuthAPI.DUO_DEVICE_AUTO);
        Assert.assertNull(duoCtx.getPasscode());
    }
    
    @Test public void testFactorAutoDevice() {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addHeader(DuoAuthAPI.DUO_FACTOR_HEADER_NAME, DuoAuthAPI.DUO_FACTOR_PUSH);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        final DuoAuthenticationContext duoCtx = authCtx.getSubcontext(DuoAuthenticationContext.class, false);
        Assert.assertNotNull(duoCtx);
        Assert.assertEquals(duoCtx.getFactor(), DuoAuthAPI.DUO_FACTOR_PUSH);
        Assert.assertEquals(duoCtx.getDeviceID(), DuoAuthAPI.DUO_DEVICE_AUTO);
        Assert.assertNull(duoCtx.getPasscode());
    }
    
    @Test public void testDeviceAutoFactor() {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addHeader(DuoAuthAPI.DUO_DEVICE_HEADER_NAME, "foo");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        final DuoAuthenticationContext duoCtx = authCtx.getSubcontext(DuoAuthenticationContext.class, false);
        Assert.assertNotNull(duoCtx);
        Assert.assertEquals(duoCtx.getFactor(), DuoAuthAPI.DUO_FACTOR_AUTO);
        Assert.assertEquals(duoCtx.getDeviceID(), "foo");
        Assert.assertNull(duoCtx.getPasscode());
    }
    
    @Test public void testNoPasscode() {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addHeader(DuoAuthAPI.DUO_FACTOR_HEADER_NAME, DuoAuthAPI.DUO_FACTOR_PASSCODE);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);

        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        final DuoAuthenticationContext duoCtx = authCtx.getSubcontext(DuoAuthenticationContext.class, false);
        Assert.assertNull(duoCtx);
    }

    @Test public void testPasscode() {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addHeader(DuoAuthAPI.DUO_FACTOR_HEADER_NAME, DuoAuthAPI.DUO_FACTOR_PASSCODE);
        ((MockHttpServletRequest) action.getHttpServletRequest()).addHeader(DuoAuthAPI.DUO_PASSCODE_HEADER_NAME, "foo");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        final DuoAuthenticationContext duoCtx = authCtx.getSubcontext(DuoAuthenticationContext.class, false);
        Assert.assertNotNull(duoCtx);
        Assert.assertEquals(duoCtx.getFactor(), DuoAuthAPI.DUO_FACTOR_PASSCODE);
        Assert.assertNull(duoCtx.getDeviceID());
        Assert.assertEquals(duoCtx.getPasscode(), "foo");
    }

}
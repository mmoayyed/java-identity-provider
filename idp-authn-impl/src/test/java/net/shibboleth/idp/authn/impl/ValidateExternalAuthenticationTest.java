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

package net.shibboleth.idp.authn.impl;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.AuthenticationErrorContext;
import net.shibboleth.idp.authn.context.ExternalAuthenticationContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.idp.authn.principal.TestPrincipal;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link ValidateExternalAuthentication} unit test. */
public class ValidateExternalAuthenticationTest extends BaseAuthenticationContextTest {
    
    private ExternalAuthentication ext;
    
    private ValidateExternalAuthentication action;
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        prc.getSubcontext(AuthenticationContext.class).setAttemptedFlow(authenticationFlows.get(0));
        
        ext = new ExternalAuthenticationImpl();

        action = new ValidateExternalAuthentication();
        action.setHttpServletRequest((HttpServletRequest) src.getExternalContext().getNativeRequest());
        action.initialize();
    }

    @Test public void testMissingFlow() {
        prc.getSubcontext(AuthenticationContext.class).setAttemptedFlow(null);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
    }
    
    @Test public void testMissingContext() {
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    @Test public void testNoCredentials() {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.addSubcontext(new ExternalAuthenticationContext(ext), true);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testPrincipalName() {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        final ExternalAuthenticationContext eac =
                (ExternalAuthenticationContext) ac.addSubcontext(new ExternalAuthenticationContext(ext), true);
        eac.setPrincipalName("foo");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertFalse(ac.getAuthenticationResult().isPreviousResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(
                UsernamePrincipal.class).iterator().next().getName(), "foo");
    }
    
    @Test public void testPrincipal() {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        final ExternalAuthenticationContext eac =
                (ExternalAuthenticationContext) ac.addSubcontext(new ExternalAuthenticationContext(ext), true);
        eac.setPrincipal(new TestPrincipal("foo"));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertFalse(ac.getAuthenticationResult().isPreviousResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(
                TestPrincipal.class).iterator().next().getName(), "foo");
    }

    @Test public void testSubject() {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        final ExternalAuthenticationContext eac =
                (ExternalAuthenticationContext) ac.addSubcontext(new ExternalAuthenticationContext(ext), true);
        final Subject subject = new Subject();
        eac.setSubject(subject);
        subject.getPrincipals().add(new TestPrincipal("foo"));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertFalse(ac.getAuthenticationResult().isPreviousResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(
                TestPrincipal.class).iterator().next().getName(), "foo");
    }

    @Test public void testAuthnInstant() {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        final ExternalAuthenticationContext eac =
                (ExternalAuthenticationContext) ac.addSubcontext(new ExternalAuthenticationContext(ext), true);
        eac.setPrincipalName("foo");
        final Instant ts = Instant.now().minusSeconds(3600);
        eac.setAuthnInstant(ts);
        eac.setPreviousResult(true);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertTrue(ac.getAuthenticationResult().isPreviousResult());
        Assert.assertEquals(ts, ac.getAuthenticationResult().getAuthenticationInstant());
    }

    @Test public void testAuthnAuthorities() {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        final ExternalAuthenticationContext eac =
                (ExternalAuthenticationContext) ac.addSubcontext(new ExternalAuthenticationContext(ext), true);
        eac.setPrincipalName("foo");
        eac.getAuthenticatingAuthorities().addAll(Arrays.asList("foo", "bar", "baz"));
        eac.setPreviousResult(true);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertTrue(ac.getAuthenticationResult().isPreviousResult());
        final Set<ProxyAuthenticationPrincipal> prin =
                ac.getAuthenticationResult().getSubject().getPrincipals(ProxyAuthenticationPrincipal.class);
        Assert.assertEquals(prin.size(), 1);
        Assert.assertEquals(prin.iterator().next().getAuthorities(), Arrays.asList("foo", "bar", "baz"));
    }

    @Test public void testException() {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        final ExternalAuthenticationContext eac =
                (ExternalAuthenticationContext) ac.addSubcontext(new ExternalAuthenticationContext(ext), true);
        eac.setAuthnException(new LoginException("foo"));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.AUTHN_EXCEPTION);
        Assert.assertNull(ac.getAuthenticationResult());
        Assert.assertNotNull(ac.getSubcontext(AuthenticationErrorContext.class));
        Assert.assertEquals(ac.getSubcontext(AuthenticationErrorContext.class).getExceptions().size(), 1);
    }

    @Test public void testError() {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        final ExternalAuthenticationContext eac =
                (ExternalAuthenticationContext) ac.addSubcontext(new ExternalAuthenticationContext(ext), true);
        eac.setAuthnError("foo");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.AUTHN_EXCEPTION);
        Assert.assertNull(ac.getAuthenticationResult());
    }

}
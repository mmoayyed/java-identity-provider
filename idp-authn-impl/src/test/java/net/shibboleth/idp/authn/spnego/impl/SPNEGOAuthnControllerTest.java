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

package net.shibboleth.idp.authn.spnego.impl;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.ExternalAuthenticationException;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.ExternalAuthenticationContext;
import net.shibboleth.idp.authn.impl.ExternalAuthenticationImpl;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.codec.Base64Support;
import net.shibboleth.shared.codec.EncodingException;
import net.shibboleth.shared.component.ComponentInitializationException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;
import org.mockito.ArgumentMatchers;
import org.opensaml.messaging.context.BaseContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SPNEGOAuthnControllerTest {

    private static final String TEST_CONVERSATION_KEY = "e1s1";

    private static String NTLMSSP_HEADER_DATA;

    private static String NEGOTIATE_HEADER_DATA;

    private SPNEGOAuthnController controller = new SPNEGOAuthnController();

    private GSSContextAcceptor mockGSSContextAcceptor;
    
    
    @BeforeClass
    public void init() throws EncodingException {        
        NTLMSSP_HEADER_DATA = Base64Support.encode(new byte[] {(byte) 0x4E, (byte) 0x54,
             (byte) 0x4C, (byte) 0x4D, (byte) 0x53, (byte) 0x53, (byte) 0x50}, false);
        NEGOTIATE_HEADER_DATA = Base64Support.encode("testdata".getBytes(), false);
    }

    private SPNEGOAuthnController mockedGSSController = new SPNEGOAuthnController() {
        @Override
        @Nonnull
        protected GSSContextAcceptor createGSSContextAcceptor(@Nonnull final SPNEGOContext spnegoCtx)
                throws GSSException {
            return mockGSSContextAcceptor;

        }
    };

    @BeforeMethod
    public void setup() {
        mockGSSContextAcceptor = mock(GSSContextAcceptor.class);
    }

    @Test(expectedExceptions = {ExternalAuthenticationException.class})
    public void withoutConversationKeyParameter_startSPNEGO_shouldThrowExternalAuthenticationException() throws Exception {
        controller.startSPNEGO(TEST_CONVERSATION_KEY,
                (HttpServletRequest) buildConversationRequestContext(null).getExternalContext().getNativeRequest(), null);
    }

    @Test(expectedExceptions = ExternalAuthenticationException.class)
    public void givenMismatchedKeys_startSPNEGO_shouldThrowExternalAuthenticationException() throws Exception {
        controller.startSPNEGO("e1s2",
                (HttpServletRequest) buildConversationRequestContext(TEST_CONVERSATION_KEY).getExternalContext().getNativeRequest(), null);
    }

    @Test(expectedExceptions = ExternalAuthenticationException.class)
    public void givenNullKey_startSPNEGO_shouldReturnAuthenticationException() throws Exception {
        RequestContext req = buildConversationRequestContext(null);
        controller.startSPNEGO(TEST_CONVERSATION_KEY,
                (MockHttpServletRequest) req.getExternalContext().getNativeRequest(),
                (MockHttpServletResponse) req.getExternalContext().getNativeResponse());
    }

    @Test
    public void withoutSPNEGOContext_startSPNEGO_shouldReturnAuthenticationError() throws Exception {
        RequestContext req = buildConversationRequestContext(TEST_CONVERSATION_KEY);
        ModelAndView mv = controller.startSPNEGO(TEST_CONVERSATION_KEY,
                (MockHttpServletRequest) req.getExternalContext().getNativeRequest(),
                (MockHttpServletResponse) req.getExternalContext().getNativeResponse());
        assertAuthenticationError(req, mv, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    @Test
    public void withoutKerberosSettings_startSPNEGO_shouldReturnAuthenticationError() throws Exception {
        RequestContext req = buildConversationRequestContext(TEST_CONVERSATION_KEY);
        final ProfileRequestContext prc =
                (ProfileRequestContext) req.getConversationScope().get(ProfileRequestContext.BINDING_KEY);
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        SPNEGOContext sc = new SPNEGOContext();
        ac.addSubcontext(sc);
        ModelAndView mv = controller.startSPNEGO(TEST_CONVERSATION_KEY,
                (MockHttpServletRequest) req.getExternalContext().getNativeRequest(),
                (MockHttpServletResponse) req.getExternalContext().getNativeResponse());
        assertAuthenticationError(req, mv, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    @Test
    public void givenKerberosSettings_startSPNEGO_shouldReturnModelAndView() throws Exception {
        RequestContext req = buildKerberosContextRequestContext();
        ModelAndView mv = controller.startSPNEGO(TEST_CONVERSATION_KEY,
                (MockHttpServletRequest) req.getExternalContext().getNativeRequest(),
                (MockHttpServletResponse) req.getExternalContext().getNativeResponse());
        assertModelAndView(mv, req);
    }

    @Test
    public void givenKerberosSettings_startSPNEGO_shouldPreserveQueryString() throws Exception {
        RequestContext req = buildKerberosContextRequestContext();
        ((MockHttpServletRequest) req.getExternalContext().getNativeRequest()).setQueryString("dummy query string");
        ModelAndView mv = controller.startSPNEGO(TEST_CONVERSATION_KEY,
                (MockHttpServletRequest) req.getExternalContext().getNativeRequest(),
                (MockHttpServletResponse) req.getExternalContext().getNativeResponse());
        assertModelAndView(mv, req);
    }

    @Test
    public void givenKerberosSettings_startSPNEGO_shouldReplyUnauthorizedNegotiate() throws Exception {
        RequestContext req = buildKerberosContextRequestContext();
        controller.startSPNEGO(TEST_CONVERSATION_KEY,
                (MockHttpServletRequest) req.getExternalContext().getNativeRequest(),
                (MockHttpServletResponse) req.getExternalContext().getNativeResponse());
        assertResponseUnauthorizedNegotiate(req);
    }

    @Test
    public void withoutNegotiateToken_continueSPNEGO_shouldReturnModelAndView() throws Exception {
        RequestContext req = buildConversationRequestContext(TEST_CONVERSATION_KEY);
        ModelAndView mv =
                controller.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate",
                        (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                        (HttpServletResponse) req.getExternalContext().getNativeResponse());
        assertModelAndView(mv, req);
    }

    @Test
    public void withoutNegotiateToken_continueSPNEGO_shouldPreserveQueryString() throws Exception {
        RequestContext req = buildConversationRequestContext(TEST_CONVERSATION_KEY);
        ((MockHttpServletRequest) req.getExternalContext().getNativeRequest()).setQueryString("dummy query string");
        ModelAndView mv =
                controller.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate",
                        (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                        (HttpServletResponse) req.getExternalContext().getNativeResponse());
        assertModelAndView(mv, req);
    }

    @Test
    public void withoutNegotiateToken_continueSPNEGO_shouldReplyUnauthorizedNegotiate() throws Exception {
        RequestContext req = buildConversationRequestContext(TEST_CONVERSATION_KEY);
        controller.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate",
                (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                (HttpServletResponse) req.getExternalContext().getNativeResponse());
        assertResponseUnauthorizedNegotiate(req);
    }

    @Test
    public void withoutSPNEGOContext_continueSPNEGO_shouldReturnAuthenticationError() throws Exception {
        RequestContext req = buildConversationRequestContext(TEST_CONVERSATION_KEY);
        ModelAndView mv =
                controller.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate " + NEGOTIATE_HEADER_DATA,
                        (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                        (HttpServletResponse) req.getExternalContext().getNativeResponse());
        assertAuthenticationError(req, mv, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    @Test
    public void withoutKerberosSettings_continueSPNEGO_shouldReturnAuthenticationError() throws Exception {
        final RequestContext req = buildConversationRequestContext(TEST_CONVERSATION_KEY);
        ModelAndView mv =
                controller.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate " + NEGOTIATE_HEADER_DATA,
                        (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                        (HttpServletResponse) req.getExternalContext().getNativeResponse());
        assertAuthenticationError(req, mv, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    @Test
    public void givenFailedGSSContextAcceptorInstantiation_continueSPNEGO_shouldReturnAuthenticationException() throws Exception {
        final GSSException expected = new GSSException(0);
        SPNEGOAuthnController failedGSSController = new SPNEGOAuthnController() {
            @Override
            @Nonnull
            protected GSSContextAcceptor createGSSContextAcceptor(@Nonnull final SPNEGOContext spnegoCtx)
                    throws GSSException {
                throw expected;
            }
        };
        final RequestContext req = buildKerberosContextRequestContext();
        ModelAndView mv =
                failedGSSController.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate " + NEGOTIATE_HEADER_DATA,
                        (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                        (HttpServletResponse) req.getExternalContext().getNativeResponse());

        Assert.assertSame(((Exception) ((ServletRequest) req.getExternalContext().getNativeRequest())
                .getAttribute(ExternalAuthentication.AUTHENTICATION_EXCEPTION_KEY)).getCause(), expected);
        assertAuthenticationExceptionCause(req, mv, GSSException.class);
    }

    @Test
    public void givenSuccessfulGSSContextAcceptorInstantiation_continueSPNEGO_shouldHaveSetAcceptorInSPNEGOContext() throws Exception {
        final GSSContext mockGSSContext = mock(GSSContext.class);
        when(mockGSSContextAcceptor.acceptSecContext(ArgumentMatchers.<byte[]> any(), anyInt(), anyInt())).thenReturn(
                "tokenBytes".getBytes());
        when(mockGSSContextAcceptor.getContext()).thenReturn(mockGSSContext);
        when(mockGSSContext.isEstablished()).thenReturn(false);
        final RequestContext req = buildSPNEGORequestContext(NEGOTIATE_HEADER_DATA);
        mockedGSSController.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate " + NEGOTIATE_HEADER_DATA,
                (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                (HttpServletResponse) req.getExternalContext().getNativeResponse());

        final AuthenticationContext authnContext =
                ((BaseContext) req.getConversationScope().get(ProfileRequestContext.BINDING_KEY)).getSubcontext(AuthenticationContext.class);
        final SPNEGOContext spnegoContext = authnContext != null ? authnContext.getSubcontext(SPNEGOContext.class) : null;
        assert spnegoContext != null;
        Assert.assertEquals(spnegoContext.getContextAcceptor(), mockGSSContextAcceptor);
    }

    @Test
    public void givenHeaderAuthorizationNegotiate_withNTLMdata_continueSPNEGO_shouldReturnAuthenticationError() throws Exception {
        final RequestContext req = buildSPNEGORequestContext(NTLMSSP_HEADER_DATA);
        final ModelAndView mv =
                controller.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate " + NTLMSSP_HEADER_DATA,
                        (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                        (HttpServletResponse) req.getExternalContext().getNativeResponse());
        assertAuthenticationError(req, mv, SPNEGOAuthnController.NTLM_UNSUPPORTED);
    }

    @Test
    public void whenAcceptSecContextThrowsException_continueSPNEGO_shouldReturnAuthenticationException() throws Exception {
        final RuntimeException e = new RuntimeException();
        when(mockGSSContextAcceptor.acceptSecContext(ArgumentMatchers.<byte[]> any(), anyInt(), anyInt())).thenThrow(e);
        final RequestContext req = buildSPNEGORequestContext(NEGOTIATE_HEADER_DATA);
        final ModelAndView mv =
                mockedGSSController.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate " + NEGOTIATE_HEADER_DATA,
                        (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                        (HttpServletResponse) req.getExternalContext().getNativeResponse());

        Assert.assertSame(((Exception) ((HttpServletRequest) req.getExternalContext().getNativeRequest())
                .getAttribute(ExternalAuthentication.AUTHENTICATION_EXCEPTION_KEY)).getCause(), e);
        assertAuthenticationExceptionCause(req, mv, RuntimeException.class);
    }

    @Test
    public void withoutGSSContext_continueSPNEGO_shouldReturnModelAndView() throws Exception {
        when(mockGSSContextAcceptor.acceptSecContext(ArgumentMatchers.<byte[]> any(), anyInt(), anyInt())).thenReturn(
                "tokenBytes".getBytes());
        when(mockGSSContextAcceptor.getContext()).thenReturn(null);
        final RequestContext req = buildSPNEGORequestContext(NEGOTIATE_HEADER_DATA);
        final ModelAndView modelAndView =
                mockedGSSController.continueSPNEGO(TEST_CONVERSATION_KEY, "",
                        (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                        (HttpServletResponse) req.getExternalContext().getNativeResponse());
        assertModelAndView(modelAndView, req);
    }

    @Test
    public void withoutGSSContext_continueSPNEGO_shouldReplyUnauthorizedNegotiate() throws Exception {
        when(mockGSSContextAcceptor.acceptSecContext(ArgumentMatchers.<byte[]> any(), anyInt(), anyInt())).thenReturn(
                "tokenBytes".getBytes());
        when(mockGSSContextAcceptor.getContext()).thenReturn(null);
        final RequestContext req = buildSPNEGORequestContext(NEGOTIATE_HEADER_DATA);
        mockedGSSController.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate " + NEGOTIATE_HEADER_DATA,
                (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                (HttpServletResponse) req.getExternalContext().getNativeResponse());
        assertResponseUnauthorizedNegotiate(req, Base64Support.encode("tokenBytes".getBytes(), false));
    }

    @Test
    public void givenGSSContextNotEstablished_continueSPNEGO_shouldReturnModelAndView() throws Exception {
        final GSSContext mockGSSContext = mock(GSSContext.class);
        when(mockGSSContextAcceptor.acceptSecContext(ArgumentMatchers.<byte[]> any(), anyInt(), anyInt())).thenReturn(
                "tokenBytes".getBytes());
        when(mockGSSContextAcceptor.getContext()).thenReturn(mockGSSContext);
        when(mockGSSContext.isEstablished()).thenReturn(false);
        final RequestContext req = buildSPNEGORequestContext(NEGOTIATE_HEADER_DATA);
        final ModelAndView modelAndView =
                mockedGSSController.continueSPNEGO(TEST_CONVERSATION_KEY, "",
                        (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                        (HttpServletResponse) req.getExternalContext().getNativeResponse());
        assertModelAndView(modelAndView, req);
    }

    @Test
    public void givenGSSContextNotEstablished_continueSPNEGO_shouldReplyUnauthorizedNegotiate() throws Exception {
        final GSSContext mockGSSContext = mock(GSSContext.class);
        when(mockGSSContextAcceptor.acceptSecContext(ArgumentMatchers.<byte[]> any(), anyInt(), anyInt())).thenReturn(
                "tokenBytes".getBytes());
        when(mockGSSContextAcceptor.getContext()).thenReturn(mockGSSContext);
        when(mockGSSContext.isEstablished()).thenReturn(false);
        final RequestContext req = buildSPNEGORequestContext(NEGOTIATE_HEADER_DATA);
        mockedGSSController.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate " + NEGOTIATE_HEADER_DATA,
                (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                (HttpServletResponse) req.getExternalContext().getNativeResponse());
        assertResponseUnauthorizedNegotiate(req, Base64Support.encode("tokenBytes".getBytes(), false));
    }

    @Test
    public void givenGSSContextEstablished_andGSSException_continueSPNEGO_shouldReturnAuthenticationError() throws Exception {
        final GSSContext mockGSSContext = mock(GSSContext.class);
        final GSSException gssException = new GSSException(0);
        when(mockGSSContextAcceptor.acceptSecContext(ArgumentMatchers.<byte[]> any(), anyInt(), anyInt())).thenReturn(
                "tokenBytes".getBytes());
        when(mockGSSContextAcceptor.getContext()).thenReturn(mockGSSContext);
        when(mockGSSContext.isEstablished()).thenReturn(true);
        when(mockGSSContext.getSrcName()).thenThrow(gssException);
        final RequestContext req = buildSPNEGORequestContext(NEGOTIATE_HEADER_DATA);
        final ModelAndView mv =
                mockedGSSController.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate " + NEGOTIATE_HEADER_DATA,
                        (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                        (HttpServletResponse) req.getExternalContext().getNativeResponse());
        Assert.assertSame(((Exception) ((HttpServletRequest) req.getExternalContext().getNativeRequest())
                .getAttribute(ExternalAuthentication.AUTHENTICATION_EXCEPTION_KEY)).getCause(), gssException);
        assertAuthenticationExceptionCause(req, mv, GSSException.class);
    }

    @Test
    public void givenGSSContextEstablished_continueSPNEGO_shouldReturnNull() throws Exception {
        final GSSContext mockGSSContext = mock(GSSContext.class);
        final GSSName mockGssName = mock(GSSName.class);
        when(mockGSSContextAcceptor.acceptSecContext(ArgumentMatchers.<byte[]> any(), anyInt(), anyInt())).thenReturn(
                "tokenBytes".getBytes());
        when(mockGSSContextAcceptor.getContext()).thenReturn(mockGSSContext);
        when(mockGSSContext.isEstablished()).thenReturn(true);
        when(mockGSSContext.getSrcName()).thenReturn(mockGssName);
        when(mockGssName.toString()).thenReturn("testname@realm");
        final RequestContext req = buildSPNEGORequestContext(NEGOTIATE_HEADER_DATA);
        Assert.assertNull(mockedGSSController.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate " + NEGOTIATE_HEADER_DATA,
                (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                (HttpServletResponse) req.getExternalContext().getNativeResponse()));
    }

    @Test
    public void givenGSSContextEstablished_continueSPNEGO_shouldSetAuthenticationSubjectAttribute() throws Exception {
        GSSContext mockGSSContext = mock(GSSContext.class);
        GSSName mockGssName = mock(GSSName.class);
        when(mockGSSContextAcceptor.acceptSecContext(ArgumentMatchers.<byte[]> any(), anyInt(), anyInt())).thenReturn(
                "tokenBytes".getBytes());
        when(mockGSSContextAcceptor.getContext()).thenReturn(mockGSSContext);
        when(mockGSSContext.isEstablished()).thenReturn(true);
        when(mockGSSContext.getSrcName()).thenReturn(mockGssName);
        when(mockGssName.toString()).thenReturn("testname@realm");
        RequestContext req = buildSPNEGORequestContext(NEGOTIATE_HEADER_DATA);
        mockedGSSController.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate " + NEGOTIATE_HEADER_DATA,
                (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                (HttpServletResponse) req.getExternalContext().getNativeResponse());
        Subject s = (Subject) ((HttpServletRequest) req.getExternalContext().getNativeRequest()).getAttribute(ExternalAuthentication.SUBJECT_KEY);
        Assert.assertEquals(s.getClass(), Subject.class);
        Assert.assertTrue(s.getPrincipals(KerberosPrincipal.class).contains(new KerberosPrincipal("testname@realm")));
        Assert.assertTrue(s.getPrincipals(UsernamePrincipal.class).contains(new UsernamePrincipal("testname@realm")));
    }

    @Test
    public void givenGSSContextEstablishedButNoGSSNameIsNull_continueSPNEGO_shouldSetAuthenticationSubjectAttribute() throws Exception {
        GSSContext mockGSSContext = mock(GSSContext.class);
        when(mockGSSContextAcceptor.acceptSecContext(ArgumentMatchers.<byte[]> any(), anyInt(), anyInt())).thenReturn(
                "tokenBytes".getBytes());
        when(mockGSSContextAcceptor.getContext()).thenReturn(mockGSSContext);
        when(mockGSSContext.isEstablished()).thenReturn(true);
        when(mockGSSContext.getSrcName()).thenReturn(null);
        RequestContext req = buildSPNEGORequestContext(NEGOTIATE_HEADER_DATA);
        ModelAndView mv = mockedGSSController.continueSPNEGO(TEST_CONVERSATION_KEY, "Negotiate " + NEGOTIATE_HEADER_DATA,
                (HttpServletRequest) req.getExternalContext().getNativeRequest(),
                (HttpServletResponse) req.getExternalContext().getNativeResponse());
        Assert.assertNull(mv);
        Assert.assertEquals(
                ((HttpServletRequest) req.getExternalContext().getNativeRequest()).getAttribute(ExternalAuthentication.AUTHENTICATION_EXCEPTION_KEY).getClass(),
                ExternalAuthenticationException.class);
    }

    private RequestContext buildSPNEGORequestContext(String negotiateHeaderData) throws ComponentInitializationException {
        RequestContext req = buildKerberosContextRequestContext();
        ((MockHttpServletRequest) req.getExternalContext().getNativeRequest()).addHeader(HttpHeaders.AUTHORIZATION, "Negotiate " + negotiateHeaderData);
        return req;
    }

    private RequestContext buildKerberosContextRequestContext() throws ComponentInitializationException {
        RequestContext req = buildConversationRequestContext(TEST_CONVERSATION_KEY);
        buildKerberosProfileRequestContext(req);
        return req;
    }

    private ProfileRequestContext buildKerberosProfileRequestContext(RequestContext rc) {
        ProfileRequestContext prc = (ProfileRequestContext) rc.getConversationScope().get(ProfileRequestContext.BINDING_KEY);
        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        SPNEGOContext sc = new SPNEGOContext();
        KerberosSettings ks = new KerberosSettings();
        List<KerberosRealmSettings> realms = new ArrayList<>();
        realms.add(new KerberosRealmSettings());
        ks.setRealms(realms);
        sc.setKerberosSettings(ks);
        ac.addSubcontext(sc);
        return prc;
    }

    private RequestContext buildConversationRequestContext(String conversationKey) throws ComponentInitializationException {
        final RequestContext rc = new RequestContextBuilder().buildRequestContext();
        if (conversationKey != null) {
            ((MockHttpServletRequest) rc.getExternalContext().getNativeRequest()).addParameter(
                    ExternalAuthentication.CONVERSATION_KEY, conversationKey);
        }
        
        final ProfileRequestContext prc =
                (ProfileRequestContext) rc.getConversationScope().get(ProfileRequestContext.BINDING_KEY); 
        ((MockServletContext) rc.getExternalContext().getNativeContext()).setAttribute(ExternalAuthentication.SWF_KEY, prc);
        
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, true);
        ac.setAttemptedFlow(new AuthenticationFlowDescriptor());
        
        final ExternalAuthenticationContext eac = (ExternalAuthenticationContext) ac.addSubcontext(
                new ExternalAuthenticationContext(new ExternalAuthenticationImpl()));
        eac.setFlowExecutionUrl("foo");
        
        return rc;
    }

    private void assertAuthenticationError(RequestContext request, ModelAndView mv, String expectedError) {
        Assert.assertNull(mv);
        Assert.assertEquals(
                ((ServletRequest) request.getExternalContext().getNativeRequest()).getAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY).toString(),
                expectedError);
    }

    private void assertAuthenticationExceptionCause(RequestContext request, ModelAndView mv,
            Class<?> exceptedExceptionClass) {
        Assert.assertNull(mv);
        Assert.assertEquals(((Exception) ((ServletRequest) request.getExternalContext().getNativeRequest())
                .getAttribute(ExternalAuthentication.AUTHENTICATION_EXCEPTION_KEY)).getCause().getClass(),
                exceptedExceptionClass);
    }

    private void assertModelAndView(ModelAndView modelAndView, RequestContext request) {
        Assert.assertEquals(modelAndView.getViewName(), "spnego-unavailable");
        Map<String, Object> model = modelAndView.getModel();
        Assert.assertTrue(model.containsKey("encoder"), "Model doesn't contain \"encoder\"");
        Assert.assertEquals(model.get("encoder").getClass(), Class.class);
        Assert.assertTrue(model.containsKey("errorUrl"), "Model doesn't contain \"errorUrl\"");
        Assert.assertEquals(model.get("errorUrl").getClass(), String.class);
        if (((HttpServletRequest) request.getExternalContext().getNativeRequest()).getQueryString() != null) {
            Assert.assertTrue(((String) model.get("errorUrl")).endsWith(
                    "/error?" + ((HttpServletRequest) request.getExternalContext().getNativeRequest()).getQueryString()));
        } else {
            Assert.assertTrue(((String) model.get("errorUrl")).endsWith("/error"));
        }
        Assert.assertTrue(model.containsKey("request"), "Model doesn't contain \"request\"");
        Assert.assertTrue(model.get("request") instanceof HttpServletRequest);
    }

    private void assertResponseUnauthorizedNegotiate(RequestContext request) {
        final HttpServletResponse response = (HttpServletResponse) request.getExternalContext().getNativeResponse();
        Assert.assertEquals(response.getStatus(), 401);
        Assert.assertEquals(response.getHeader("WWW-Authenticate"), "Negotiate");
    }

    private void assertResponseUnauthorizedNegotiate(RequestContext request, String base64token) {
        final HttpServletResponse response = (HttpServletResponse) request.getExternalContext().getNativeResponse();
        Assert.assertEquals(response.getStatus(), 401);
        Assert.assertEquals(response.getHeader("WWW-Authenticate"), "Negotiate " + base64token);
    }

}
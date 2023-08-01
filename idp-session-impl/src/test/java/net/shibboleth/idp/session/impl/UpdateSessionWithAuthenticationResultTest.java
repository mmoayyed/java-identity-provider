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

package net.shibboleth.idp.session.impl;

import java.time.Instant;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageSerializer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.impl.DefaultAuthenticationResultSerializer;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.idp.session.criterion.SessionIdCriterion;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;
import net.shibboleth.shared.servlet.impl.HttpServletRequestResponseContext;

/** {@link UpdateSessionWithAuthenticationResult} unit test. */
@SuppressWarnings("javadoc")
public class UpdateSessionWithAuthenticationResultTest extends SessionManagerBaseTestCase {
    
    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    private AuthenticationContext ac;
    
    private AuthenticationFlowDescriptor flowDescriptor;
    
    private UpdateSessionWithAuthenticationResult action;
    
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        ac = prc.ensureSubcontext(AuthenticationContext.class);

        action = new UpdateSessionWithAuthenticationResult();
        action.setSessionManager(sessionManager);
        action.initialize();
    }
    
    /** {@inheritDoc} */
    protected void adjustProperties() throws ComponentInitializationException {
        StorageSerializer<AuthenticationResult> resultSerializer = new DefaultAuthenticationResultSerializer();
        resultSerializer.initialize();

        flowDescriptor = new AuthenticationFlowDescriptor();
        flowDescriptor.setId("test1");
        flowDescriptor.setResultSerializer(resultSerializer);
        flowDescriptor.initialize();
        sessionManager.setAuthenticationFlowDescriptors(CollectionSupport.arrayAsList(flowDescriptor));
    }

    @Test public void testNoResult() {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNull(prc.getSubcontext(SessionContext.class));
    }

    @Test public void testNoFlow() throws SessionException {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        prc.ensureSubcontext(SubjectContext.class).setPrincipalName("joe");
        ac.setAttemptedFlow(flowDescriptor);
        ac.setAuthenticationResult(new AuthenticationResult("test2", new UsernamePrincipal("joe")));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.IO_ERROR);
        SessionContext sessionCtx = prc.getSubcontext(SessionContext.class);
        assert sessionCtx!=null;
        final IdPSession idpSession = sessionCtx.getIdPSession();
        assert idpSession!=null;
        
        Assert.assertEquals(idpSession.getPrincipalName(), "joe");
        Assert.assertEquals(idpSession.getAuthenticationResults().size(), 0);
        Assert.assertNotNull(getResponse().getCookies()[0]);
    }

    @Test public void testNotCacheable() throws SessionException {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ac.setAttemptedFlow(flowDescriptor);
        ac.setAuthenticationResult(new AuthenticationResult("test1", new UsernamePrincipal("joe")));
        ac.setResultCacheable(false);
        
        SessionContext sessionCtx = prc.ensureSubcontext(SessionContext.class);
        assert sessionCtx!=null;
        sessionCtx.setIdPSession(sessionManager.createSession("joe"));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final IdPSession idpSession = sessionCtx.getIdPSession();
        assert idpSession!=null;

        Assert.assertEquals(idpSession.getAuthenticationResults().size(), 0);
    }
    
    @Test public void testNewSession() throws SessionException {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        prc.ensureSubcontext(SubjectContext.class).setPrincipalName("joe");
        ac.setAttemptedFlow(flowDescriptor);
        ac.setAuthenticationResult(new AuthenticationResult("test1", new UsernamePrincipal("joe")));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        SessionContext sessionCtx = prc.getSubcontext(SessionContext.class);
        Assert.assertNotNull(sessionCtx);
        assert sessionCtx!=null;
        final IdPSession idpSession = sessionCtx.getIdPSession();
        assert idpSession!=null;

        Assert.assertEquals(idpSession.getPrincipalName(), "joe");
        Assert.assertSame(idpSession.getAuthenticationResult("test1"), ac.getAuthenticationResult());
        Assert.assertNotNull(getResponse().getCookies()[0]);
    }
    
    @Test public void testAddToSession() throws SessionException {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ac.setAttemptedFlow(flowDescriptor);
        ac.setAuthenticationResult(new AuthenticationResult("test1", new UsernamePrincipal("joe")));
        
        SessionContext sessionCtx = prc.ensureSubcontext(SessionContext.class);
        sessionCtx.setIdPSession(sessionManager.createSession("joe"));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final IdPSession idpSession = sessionCtx.getIdPSession();
        assert idpSession!=null;

        Assert.assertSame(idpSession.getAuthenticationResult("test1"), ac.getAuthenticationResult());
    }

    @Test public void testUpdateSessionNoResult() throws SessionException {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ac.setAuthenticationResult(new AuthenticationResult("test1", new UsernamePrincipal("joe")));
        
        SessionContext sessionCtx = prc.ensureSubcontext(SessionContext.class);
        sessionCtx.setIdPSession(sessionManager.createSession("joe"));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final IdPSession idpSession = sessionCtx.getIdPSession();
        assert idpSession!=null;

        Assert.assertEquals(idpSession.getAuthenticationResults().size(), 0);
    }
    
    @Test public void testUpdateSession() throws SessionException, ResolverException {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ac.setAuthenticationResult(new AuthenticationResult("test1", new UsernamePrincipal("joe")));
        
        final SessionContext sessionCtx = prc.ensureSubcontext(SessionContext.class);
        sessionCtx.setIdPSession(sessionManager.createSession("joe"));
        final IdPSession idpSession = sessionCtx.getIdPSession();
        assert idpSession!=null;
        final AuthenticationResult ar = ac.getAuthenticationResult();
        assert ar != null;
        idpSession.addAuthenticationResult(ar);
        
        // Limit granularity to milliseconds for storage roundtrip.
        final Instant ts = Instant.ofEpochMilli(System.currentTimeMillis()).plusSeconds(300);
        assert ts!=null;
        ar.setLastActivityInstant(ts);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertSame(idpSession.getAuthenticationResult("test1"), ac.getAuthenticationResult());
        
        final String idpSessionId = idpSession.getId();
        assert idpSessionId!=null;
        IdPSession session2 = sessionManager.resolveSingle(new CriteriaSet(new SessionIdCriterion(idpSessionId)));
        assert session2!= null;
        AuthenticationResult result = session2.getAuthenticationResult("test1");
        assert result!=null;
        Assert.assertEquals(result.getLastActivityInstant(), ts);
    }

}
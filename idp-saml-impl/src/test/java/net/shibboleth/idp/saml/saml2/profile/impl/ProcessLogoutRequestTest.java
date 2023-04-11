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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import jakarta.servlet.http.Cookie;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.saml2.profile.config.impl.SingleLogoutProfileConfiguration;
import net.shibboleth.idp.saml.session.SAML1SPSession;
import net.shibboleth.idp.saml.session.SAML2SPSession;
import net.shibboleth.idp.saml.session.impl.SAML1SPSessionSerializer;
import net.shibboleth.idp.saml.session.impl.SAML2SPSessionSerializer;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.SPSessionSerializerRegistry;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.idp.session.criterion.HttpServletRequestCriterion;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;
import net.shibboleth.shared.servlet.impl.HttpServletRequestResponseContext;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.profile.SAMLEventIds;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.SessionIndex;
import org.opensaml.saml.saml2.profile.SAML2ObjectSupport;
import org.opensaml.storage.StorageSerializer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link ProcessLogoutRequest} unit test. */
@SuppressWarnings({"javadoc", "null"})
public class ProcessLogoutRequestTest extends SessionManagerBaseTestCase {

    private SAMLObjectBuilder<SessionIndex> sessionIndexBuilder;
    
    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    private ProcessLogoutRequest action;
    
    @Nonnull private MockHttpServletRequest getMockHttpServletRequest() {
        final MockHttpServletRequest result = (MockHttpServletRequest) HttpServletRequestResponseContext.getRequest();
        assert result  != null;
        return result;
    }
    
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        sessionIndexBuilder = (SAMLObjectBuilder<SessionIndex>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<SessionIndex>ensureBuilder(
                        SessionIndex.DEFAULT_ELEMENT_NAME);

        final SingleLogoutProfileConfiguration logoutConfig = new SingleLogoutProfileConfiguration();
        logoutConfig.setQualifiedNameIDFormats(Collections.singletonList(NameID.UNSPECIFIED));
        src = new RequestContextBuilder().setRelyingPartyProfileConfigurations(
                CollectionSupport.singletonList(logoutConfig)).buildRequestContext();
        
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        
        action = new ProcessLogoutRequest();
        action.setSessionResolver(sessionManager);
        action.initialize();
    }
    
    /** {@inheritDoc} */
    @Override
    protected void adjustProperties() throws ComponentInitializationException {
        sessionManager.setTrackSPSessions(true);
        sessionManager.setSecondaryServiceIndex(true);
        sessionManager.setSessionSlop(Duration.ofSeconds(900 * 60));
        final SPSessionSerializerRegistry registry = new SPSessionSerializerRegistry();
        final Map<Class<? extends SPSession>,StorageSerializer<? extends SPSession>> mappings = new HashMap<>();
        mappings.put(SAML1SPSession.class, new SAML1SPSessionSerializer(Duration.ofSeconds(900 * 60)));
        mappings.put(SAML2SPSession.class, new SAML2SPSessionSerializer(Duration.ofSeconds(900 * 60)));
        registry.setMappings(mappings);
        registry.initialize();
        sessionManager.setSPSessionSerializerRegistry(registry);
    }
    
    @Test public void testNoMessage() {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
    }

    @Test public void testNoNameID() {
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutRequest(null));
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_MESSAGE);
        Assert.assertNull(prc.getSubcontext(SessionContext.class));
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
        Assert.assertNull(prc.getSubcontext(LogoutContext.class));
    }

    @Test public void testNoSession() {
        final NameID nameId = SAML2ActionTestingSupport.buildNameID("jdoe");
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutRequest(nameId));
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, SAMLEventIds.SESSION_NOT_FOUND);
        Assert.assertNull(prc.getSubcontext(SessionContext.class));
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
        Assert.assertNull(prc.getSubcontext(LogoutContext.class));
    }
    
    @Test public void testSessionNoSPSessions() throws SessionException {
        final Cookie cookie = createSession("joe");

        final NameID nameId = SAML2ActionTestingSupport.buildNameID("joe");
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutRequest(nameId));
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        getMockHttpServletRequest().setCookies(cookie);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, SAMLEventIds.SESSION_NOT_FOUND);
        Assert.assertNull(prc.getSubcontext(SessionContext.class));
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
        Assert.assertNull(prc.getSubcontext(LogoutContext.class));
    }

    @Test public void testBadQualifiers() throws SessionException, ResolverException {
        final Cookie cookie = createSession("joe");

        final NameID nameId = SAML2ActionTestingSupport.buildNameID("joe");
        nameId.setSPNameQualifier("affiliation");
        final NameID nameIdForSession = SAML2ActionTestingSupport.buildNameID("joe");
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutRequest(nameId));

        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        getMockHttpServletRequest().setCookies(cookie);
        
        final Instant creation = Instant.now();
        final Instant expiration = creation.plusSeconds(3600);
        
        final IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        assert session!=null && expiration!=null;
        session.addSPSession(new SAML2SPSession(ActionTestingSupport.INBOUND_MSG_ISSUER, creation, expiration,
                nameIdForSession, "index", "foo", false));
                
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, SAMLEventIds.SESSION_NOT_FOUND);
        Assert.assertNull(prc.getSubcontext(SessionContext.class));
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
        Assert.assertNull(prc.getSubcontext(LogoutContext.class));
        
        final String id = session.getId();
        assert id!=null;
        sessionManager.destroySession(id, false);
    }

    @Test public void testDefaultedRequestQualifiers() throws SessionException, ResolverException {
        final Cookie cookie = createSession("joe");

        final NameID nameId = SAML2ActionTestingSupport.buildNameID("joe");
        nameId.setNameQualifier(ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        nameId.setSPNameQualifier(ActionTestingSupport.INBOUND_MSG_ISSUER);
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutRequest(nameId));
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        getMockHttpServletRequest().setCookies(cookie);
        
        final Instant creation = Instant.now();
        final Instant expiration = creation.plusSeconds(3600);
        
        final IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        assert session!=null && expiration!=null;
        final NameID nameIdForSession = SAML2ActionTestingSupport.buildNameID("joe");
        session.addSPSession(new SAML2SPSession(ActionTestingSupport.INBOUND_MSG_ISSUER, creation, expiration,
                nameIdForSession, "index", "foo", false));
                
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectContext subjectCtx = prc.getSubcontext(SubjectContext.class);
        assert subjectCtx!=null;
        Assert.assertEquals(subjectCtx.getPrincipalName(), "joe");
        
        final SessionContext sessionCtx = prc.getSubcontext(SessionContext.class);
        assert sessionCtx!=null;
        final IdPSession idpSession=sessionCtx.getIdPSession();
        assert idpSession!=null;
        Assert.assertEquals(session.getId(), idpSession.getId());
        
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class);
        assert logoutCtx!=null;
        Assert.assertEquals(logoutCtx.getIdPSessions().size(), 1);
        Assert.assertSame(logoutCtx.getIdPSessions().iterator().next(), sessionCtx.getIdPSession());
        Assert.assertEquals(logoutCtx.getSessionMap().size(), 0);

        final String id = session.getId();
        assert id!=null;
        sessionManager.destroySession(id, false);
    }

    @Test public void testDefaultedSessionQualifiers() throws SessionException, ResolverException {
        final Cookie cookie = createSession("joe");

        final NameID nameId = SAML2ActionTestingSupport.buildNameID("joe");
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutRequest(nameId));
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        getMockHttpServletRequest().setCookies(cookie);
        
        final Instant creation = Instant.now();
        final Instant expiration = creation.plusSeconds(3600);
        
        final IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        assert session!=null && expiration!=null;
        final NameID nameIdForSession = SAML2ActionTestingSupport.buildNameID("joe");
        nameIdForSession.setNameQualifier(ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        nameIdForSession.setSPNameQualifier(ActionTestingSupport.INBOUND_MSG_ISSUER);
        session.addSPSession(new SAML2SPSession(ActionTestingSupport.INBOUND_MSG_ISSUER, creation, expiration,
                nameIdForSession, "index", "foo", false));
                
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectContext subjectCtx = prc.getSubcontext(SubjectContext.class);
        assert subjectCtx!=null;
        Assert.assertEquals(subjectCtx.getPrincipalName(), "joe");
        
        final SessionContext sessionCtx = prc.getSubcontext(SessionContext.class);
        assert sessionCtx!=null;
        final IdPSession idpSession=sessionCtx.getIdPSession();
        assert idpSession!=null;
        Assert.assertEquals(session.getId(), idpSession.getId());
        
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class);
        assert logoutCtx!=null;
        Assert.assertEquals(logoutCtx.getIdPSessions().size(), 1);
        Assert.assertSame(logoutCtx.getIdPSessions().iterator().next(), sessionCtx.getIdPSession());
        Assert.assertEquals(logoutCtx.getSessionMap().size(), 0);
        
        final String id = session.getId();
        assert id!=null;
        sessionManager.destroySession(id, false);
    }
    
    @Test public void testSessionOneSPSession() throws SessionException, ResolverException {
        final Cookie cookie = createSession("joe");

        final NameID nameId = SAML2ActionTestingSupport.buildNameID("joe");
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutRequest(nameId));

        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        getMockHttpServletRequest().setCookies(cookie);
        
        final Instant creation = Instant.now();
        final Instant expiration = creation.plusSeconds(3600);
        
        final IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        assert session!=null && expiration!=null;
        session.addSPSession(new SAML2SPSession(ActionTestingSupport.INBOUND_MSG_ISSUER, creation, expiration,
                nameId, "index", "foo", false));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectContext subjectCtx = prc.getSubcontext(SubjectContext.class);
        assert subjectCtx!=null;
        Assert.assertEquals(subjectCtx.getPrincipalName(), "joe");
        
        final SessionContext sessionCtx = prc.getSubcontext(SessionContext.class);
        assert sessionCtx!=null;
        final IdPSession idpSession=sessionCtx.getIdPSession();
        assert idpSession!=null;
        Assert.assertEquals(session.getId(), idpSession.getId());
        
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class);
        assert logoutCtx!=null;
        Assert.assertEquals(logoutCtx.getIdPSessions().size(), 1);
        Assert.assertSame(logoutCtx.getIdPSessions().iterator().next(), sessionCtx.getIdPSession());
        Assert.assertEquals(logoutCtx.getSessionMap().size(), 0);
        
        final String id = session.getId();
        assert id!=null;
        sessionManager.destroySession(id, false);
    }
    
    @Test public void testSessionTwoSPSessions() throws SessionException, ResolverException {
        final Cookie cookie = createSession("joe");

        final NameID nameId = SAML2ActionTestingSupport.buildNameID("joe");
        final NameID nameId2 = SAML2ActionTestingSupport.buildNameID("joe2");
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutRequest(nameId));

        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        getMockHttpServletRequest().setCookies(cookie);
        
        final Instant creation = Instant.now();
        final Instant expiration = creation.plusSeconds(3600);
        
        final IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        assert session!=null && expiration!=null;
        session.addSPSession(new SAML2SPSession(ActionTestingSupport.INBOUND_MSG_ISSUER, creation, expiration,
                nameId, "index", "foo", false));
        session.addSPSession(new SAML2SPSession(ActionTestingSupport.INBOUND_MSG_ISSUER + "/2", creation, expiration,
                nameId2, "index2", "foo", false));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectContext subjectCtx = prc.getSubcontext(SubjectContext.class);
        assert subjectCtx!=null;
        Assert.assertEquals(subjectCtx.getPrincipalName(), "joe");
        
        final SessionContext sessionCtx = prc.getSubcontext(SessionContext.class);
        assert sessionCtx!=null;
        final IdPSession idpSession=sessionCtx.getIdPSession();
        assert idpSession!=null;
        Assert.assertEquals(session.getId(), idpSession.getId());
        
        final LogoutContext logoutCtx = prc.ensureSubcontext(LogoutContext.class);
        Assert.assertEquals(logoutCtx.getIdPSessions().size(), 1);
        Assert.assertSame(logoutCtx.getIdPSessions().iterator().next(), sessionCtx.getIdPSession());
        Assert.assertEquals(logoutCtx.getSessionMap().size(), 1);
        
        final SAML2SPSession sp = (SAML2SPSession) logoutCtx.getSessions(ActionTestingSupport.INBOUND_MSG_ISSUER + "/2").iterator().next();
        Assert.assertNotNull(sp);
        Assert.assertEquals(sp.getCreationInstant(), creation.truncatedTo(ChronoUnit.MILLIS));
        Assert.assertEquals(sp.getExpirationInstant(), expiration.truncatedTo(ChronoUnit.MILLIS));
        Assert.assertTrue(SAML2ObjectSupport.areNameIDsEquivalent(nameId2, sp.getNameID()));
        Assert.assertEquals(sp.getSessionIndex(), "index2");
        
        final String id = session.getId();
        assert id!=null;
        sessionManager.destroySession(id, false);
    }

    @Test public void testTwoSPSessionsWrongRequester() throws SessionException, ResolverException {
        final Cookie cookie = createSession("joe");

        final NameID nameId = SAML2ActionTestingSupport.buildNameID("joe");
        final NameID nameId2 = SAML2ActionTestingSupport.buildNameID("joe2");
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutRequest(nameId2));

        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        getMockHttpServletRequest().setCookies(cookie);
        
        final Instant creation = Instant.now();
        final Instant expiration = creation.plusSeconds(3600);
        
        final IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        assert session!=null && expiration!=null;
        session.addSPSession(new SAML2SPSession(ActionTestingSupport.INBOUND_MSG_ISSUER, creation, expiration,
                nameId, "index", "foo", false));
        session.addSPSession(new SAML2SPSession(ActionTestingSupport.INBOUND_MSG_ISSUER + "/2", creation, expiration,
                nameId2, "index2", "foo", false));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, SAMLEventIds.SESSION_NOT_FOUND);
        Assert.assertNull(prc.getSubcontext(SessionContext.class));
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
        Assert.assertNull(prc.getSubcontext(LogoutContext.class));
        
        final String id = session.getId();
        assert id!=null;
        sessionManager.destroySession(id, false);
    }

    @Test public void testTwoSessionsOneMatch() throws SessionException, ResolverException {
        final Cookie cookie = createSession("joe");
        final Cookie cookie2 = createSession("joe");

        final NameID nameId = SAML2ActionTestingSupport.buildNameID("joe");
        final SessionIndex sessionIndex = sessionIndexBuilder.buildObject();
        sessionIndex.setValue("index");
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutRequest(nameId));
        final LogoutRequest lr = (LogoutRequest) imc.getMessage();
        assert lr!= null;
        lr.getSessionIndexes().add(sessionIndex);

        final Instant creation = Instant.now();
        final Instant expiration = creation.plusSeconds(3600);

        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        getMockHttpServletRequest().setCookies(cookie);
        
        final IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        assert session!=null && expiration!=null;
        session.addSPSession(new SAML2SPSession(ActionTestingSupport.INBOUND_MSG_ISSUER, creation, expiration,
                nameId, "index", "foo", false));
        
        
        getMockHttpServletRequest().setCookies(cookie2);
        
        final IdPSession session2 = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        assert session2!=null;
        session2.addSPSession(new SAML2SPSession(ActionTestingSupport.INBOUND_MSG_ISSUER, creation, expiration,
                nameId, "index2", "foo", false));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectContext subjectCtx = prc.getSubcontext(SubjectContext.class);
        assert subjectCtx!=null;
        Assert.assertEquals(subjectCtx.getPrincipalName(), "joe");
        
        final SessionContext sessionCtx = prc.getSubcontext(SessionContext.class);
        assert sessionCtx!=null;
        final IdPSession idpSession=sessionCtx.getIdPSession();
        assert idpSession!=null;
        Assert.assertEquals(session.getId(), idpSession.getId());
        
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class);
        assert logoutCtx!=null;
        Assert.assertEquals(logoutCtx.getIdPSessions().size(), 1);
        Assert.assertSame(logoutCtx.getIdPSessions().iterator().next(), sessionCtx.getIdPSession());
        Assert.assertEquals(logoutCtx.getSessionMap().size(), 0);
        
        final String id = session.getId();
        assert id!=null;
        sessionManager.destroySession(id, false);
        final String id2 = session2.getId();
        assert id2!=null;
        sessionManager.destroySession(id, false);
        sessionManager.destroySession(id2, false);
    }

    @Test public void testTwoSessions() throws SessionException, ResolverException {
        final Cookie cookie = createSession("joe");
        final Cookie cookie2 = createSession("joe");

        final NameID nameId = SAML2ActionTestingSupport.buildNameID("joe");
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutRequest(nameId));

        final Instant creation = Instant.now();
        final Instant expiration = creation.plusSeconds(3600);

        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        getMockHttpServletRequest().setCookies(cookie);
        
        final IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        assert session!=null && expiration!=null;
        session.addSPSession(new SAML2SPSession(ActionTestingSupport.INBOUND_MSG_ISSUER, creation, expiration,
                nameId, "index", "foo", false));
        
        
        getMockHttpServletRequest().setCookies(cookie2);
        
        final IdPSession session2 = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        assert session2!=null;
        session2.addSPSession(new SAML2SPSession(ActionTestingSupport.INBOUND_MSG_ISSUER, creation, expiration,
                nameId, "index2", "foo", false));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectContext subjectCtx = prc.getSubcontext(SubjectContext.class);
        assert subjectCtx!=null;
        Assert.assertEquals(subjectCtx.getPrincipalName(), "joe");
        
        final SessionContext sessionCtx = prc.getSubcontext(SessionContext.class);
        Assert.assertNull(sessionCtx);
        
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class);
        assert logoutCtx!=null;
        Assert.assertEquals(logoutCtx.getIdPSessions().size(), 2);
        Assert.assertEquals(logoutCtx.getSessionMap().size(), 0);

        final String id = session.getId();
        assert id!=null;
        sessionManager.destroySession(id, false);
        final String id2 = session2.getId();
        assert id2!=null;
        sessionManager.destroySession(id, false);
        sessionManager.destroySession(id2, false);
    }
    
}
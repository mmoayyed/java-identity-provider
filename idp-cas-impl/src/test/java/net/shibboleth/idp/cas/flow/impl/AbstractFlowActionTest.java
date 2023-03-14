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

package net.shibboleth.idp.cas.flow.impl;

import java.security.KeyException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;

import javax.annotation.Nonnull;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketIdentifierGenerationStrategy;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.spring.IdPPropertiesApplicationContextInitializer;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.security.DataSealerKeyStrategy;

import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.webflow.execution.RequestContext;
import org.testng.annotations.BeforeSuite;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Abstract base class for all flow action tests.
 *
 * @author Marvin S. Addison
 */
@ContextConfiguration(
        locations = {
                "/spring/test-flow-beans.xml",
        },
        initializers = IdPPropertiesApplicationContextInitializer.class)
@WebAppConfiguration
@TestPropertySource(properties = {"idp.initializer.failFast = false"})
public abstract class AbstractFlowActionTest extends AbstractTestNGSpringContextTests {

    protected static final String TEST_SESSION_ID = "+TkSGIRofZyue/p8F4M7TA==";

    protected static final String TEST_PRINCIPAL_NAME = "omega";

    @Autowired
    protected TicketService ticketService;

    private TicketIdentifierGenerationStrategy serviceTicketGenerator =
            new TicketIdentifierGenerationStrategy("ST", 25);

    private TicketIdentifierGenerationStrategy proxyTicketGenerator =
            new TicketIdentifierGenerationStrategy("PT", 25);


    private TicketIdentifierGenerationStrategy proxyGrantingTicketGenerator =
            new TicketIdentifierGenerationStrategy("PGT", 50);


    protected static ProfileRequestContext getProfileContext(final RequestContext context) {
        return (ProfileRequestContext) context.getConversationScope().get(ProfileRequestContext.BINDING_KEY);
    }

    protected static IdPSession mockSession(
            final String sessionId, final boolean expiredFlag, final AuthenticationResult ... results) {
        final IdPSession mockSession = mock(IdPSession.class);
        when(mockSession.getId()).thenReturn(sessionId);
        when(mockSession.getPrincipalName()).thenReturn(TEST_PRINCIPAL_NAME);
        try {
            when(mockSession.checkTimeout()).thenReturn(expiredFlag);
        } catch (SessionException e) {
            throw new RuntimeException("Session exception", e);
        }
        when(mockSession.getAuthenticationResults()).thenReturn(new HashSet<>(Arrays.asList(results)));
        return mockSession;
    }

    @Nonnull protected static Instant expiry() {
        final Instant result = Instant.now().plusSeconds(30);
        assert result!=null;
        return result;
    }

    @Nonnull protected String generateServiceTicketId() {
        return serviceTicketGenerator.generateIdentifier();
    }

    @Nonnull protected String generateProxyTicketId() {
        return proxyTicketGenerator.generateIdentifier();
    }

    @Nonnull protected String generateProxyGrantingTicketId() {
        return proxyGrantingTicketGenerator.generateIdentifier();
    }

    @Nonnull protected ServiceTicket createServiceTicket(@Nonnull final String service, final boolean renew) {
        final Instant now=Instant.now();
        assert now!=null;
        final TicketState state = new TicketState(TEST_SESSION_ID, TEST_PRINCIPAL_NAME, now, "Password");
        return ticketService.createServiceTicket(generateServiceTicketId(), expiry(), service, state, renew);
    }

    @Nonnull protected ProxyTicket createProxyTicket(@Nonnull final ProxyGrantingTicket pgt, @Nonnull final String service) {
        return ticketService.createProxyTicket(generateProxyTicketId(), expiry(), pgt, service);
    } 

    @Nonnull protected ProxyGrantingTicket createProxyGrantingTicket(@Nonnull final ServiceTicket st, @Nonnull final String pgtUrl) {
        return ticketService.createProxyGrantingTicket(generateProxyGrantingTicketId(), expiry(), st, pgtUrl);
    }

    @Nonnull protected ProxyGrantingTicket createProxyGrantingTicket(@Nonnull final ProxyTicket pt, @Nonnull final String pgtUrl) {
        return ticketService.createProxyGrantingTicket(generateProxyGrantingTicketId(), expiry(), pt, pgtUrl);
    }

    /**
     *  Initialize OpenSAML.
     *
     * @throws InitializationException ...
     */
    @BeforeSuite
    public void initOpenSAML() throws InitializationException {
        InitializationService.initialize();
    }

    /**
     * Test implementation of {@link DataSealerKeyStrategy} that emits a static key for all inquiries.
     */
    public static class MockDataSealerKeyStrategy implements DataSealerKeyStrategy {
        /** Static key. */
        @Nonnull private final SecretKey key;

        /**
         * Constructor.
         */
        public MockDataSealerKeyStrategy() {
            final byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            key = new SecretKeySpec(bytes, "AES");
        }

        /** {@inheritDoc} */
        public @Nonnull Pair<String, SecretKey> getDefaultKey() throws KeyException {
            return new Pair<>("default", key);
        }

        /** {@inheritDoc} */
        public @Nonnull SecretKey getKey(final @Nonnull String s) throws KeyException {
            return key;
        }
    }
}

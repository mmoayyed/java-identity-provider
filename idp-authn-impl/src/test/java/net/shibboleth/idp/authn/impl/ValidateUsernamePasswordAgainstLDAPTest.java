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

import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.UsernamePrincipal;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.AuthenticationErrorContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;

import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.AuthenticationResultCode;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.BindAuthenticationHandler;
import org.ldaptive.auth.SearchDnResolver;
import org.ldaptive.provider.ConnectionException;
import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.EventContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;

/** {@link ValidateUsernamePasswordAgainstLDAP} unit test. */
public class ValidateUsernamePasswordAgainstLDAPTest extends InitializeAuthenticationContextTest {

    private static final String DATA_PATH = "src/test/resources/data/net/shibboleth/idp/authn/impl/";
    
    private ValidateUsernamePasswordAgainstLDAP action;

    private InMemoryDirectoryServer directoryServer;

    private SearchDnResolver dnResolver;

    private BindAuthenticationHandler authHandler;

    private Authenticator authenticator;

    /**
     * Creates an UnboundID in-memory directory server. Leverages LDIF found in test resources.
     * 
     * @throws LDAPException if the in-memory directory server cannot be created
     */
    @BeforeClass public void setupDirectoryServer() throws LDAPException {

        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=shibboleth,dc=net");
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", 10389));
        config.addAdditionalBindCredentials("cn=Directory Manager", "password");
        directoryServer = new InMemoryDirectoryServer(config);
        directoryServer.importFromLDIF(true, DATA_PATH + "loginLDAPTest.ldif");
        directoryServer.startListening();
    }

    /**
     * Creates an Authenticator configured to use the in-memory directory server.
     */
    @BeforeClass public void setupAuthenticator() {

        dnResolver = new SearchDnResolver(new DefaultConnectionFactory("ldap://localhost:10389"));
        dnResolver.setBaseDn("ou=people,dc=shibboleth,dc=net");
        dnResolver.setUserFilter("(uid={user})");

        authHandler = new BindAuthenticationHandler(new DefaultConnectionFactory("ldap://localhost:10389"));

        authenticator = new Authenticator(dnResolver, authHandler);
    }

    /**
     * Shutdown the in-memory directory server.
     */
    @AfterClass public void teardownDirectoryServer() {
        directoryServer.shutDown(true);
    }

    @BeforeMethod public void setUp() throws Exception {
        super.setUp();

        action = new ValidateUsernamePasswordAgainstLDAP();
        action.setUnknownUsernameErrors(ImmutableList.of("DN_RESOLUTION_FAILURE"));
        action.setInvalidPasswordErrors(ImmutableList.of("INVALID_CREDENTIALS"));
        action.setHttpServletRequest(new MockHttpServletRequest());
    }

    @Test public void testMissingFlow() throws Exception {
        action.setAuthenticator(authenticator);
        action.initialize();

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, EventIds.INVALID_PROFILE_CTX);
    }

    @Test public void testMissingUser() throws Exception {
        prc.getSubcontext(AuthenticationContext.class, false).setAttemptedFlow(authenticationFlows.get(0));
        action.setAuthenticator(authenticator);
        action.initialize();

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testMissingUser2() throws Exception {
        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        ac.getSubcontext(UsernamePasswordContext.class, true);
        action.setAuthenticator(authenticator);
        action.initialize();

        action.execute(prc);
        Assert.assertNull(ac.getAuthenticationResult());
        Assert.assertNull(ac.getSubcontext(AuthenticationErrorContext.class, false));
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testBadConfig() throws Exception {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "foo");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        action.setAuthenticator(new Authenticator(new SearchDnResolver(), authHandler));
        action.initialize();

        doExtract(prc);

        action.execute(prc);
        Assert.assertNull(ac.getAuthenticationResult());
        Assert.assertNull(ac.getSubcontext(AuthenticationErrorContext.class, false));
        EventContext<AuthenticationResponse> ctx = prc.getSubcontext(EventContext.class);
        Assert.assertNotNull(ctx);
        Assert.assertEquals(ctx.getEvent().getAuthenticationResultCode(), AuthenticationResultCode.DN_RESOLUTION_FAILURE);
    }

    @Test public void testBadConfig2() throws Exception {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "bar");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        action.setAuthenticator(new Authenticator(dnResolver, new BindAuthenticationHandler(new DefaultConnectionFactory("ldap://localhost:3891"))));
        action.initialize();

        doExtract(prc);

        try {
            action.execute(prc);
            Assert.fail("Should have thrown exception");
        } catch (AuthenticationException e){ 
            Assert.assertEquals(e.getCause().getClass(), ConnectionException.class);
        }
        Assert.assertNull(ac.getAuthenticationResult());
        Assert.assertNull(ac.getSubcontext(AuthenticationErrorContext.class, false));
        Assert.assertNull(prc.getSubcontext(EventContext.class));
    }

    @Test public void testBadUsername() throws Exception {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "foo");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "bar");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        action.setAuthenticator(authenticator);
        action.initialize();

        doExtract(prc);

        action.execute(prc);
        Assert.assertNull(ac.getAuthenticationResult());
        Assert.assertNull(ac.getSubcontext(AuthenticationErrorContext.class, false));
        EventContext<AuthenticationResponse> ctx = prc.getSubcontext(EventContext.class);
        Assert.assertNotNull(ctx);
        Assert.assertEquals(ctx.getEvent().getAuthenticationResultCode(), AuthenticationResultCode.DN_RESOLUTION_FAILURE);
    }

    @Test public void testEmptyPassword() throws Exception {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        action.setAuthenticator(authenticator);
        action.initialize();

        doExtract(prc);

        action.execute(prc);
        Assert.assertNull(ac.getAuthenticationResult());
        Assert.assertNull(ac.getSubcontext(AuthenticationErrorContext.class, false));
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testBadPassword() throws Exception {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "bar");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        action.setAuthenticator(authenticator);
        action.initialize();

        doExtract(prc);

        action.execute(prc);
        Assert.assertNull(ac.getAuthenticationResult());
        Assert.assertNull(ac.getSubcontext(AuthenticationErrorContext.class, false));
        EventContext<AuthenticationResponse> ctx = prc.getSubcontext(EventContext.class);
        Assert.assertNotNull(ctx);
        Assert.assertEquals(ctx.getEvent().getAuthenticationResultCode(), AuthenticationResultCode.AUTHENTICATION_HANDLER_FAILURE);
    }

    @Test public void testAuthorized() throws Exception {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));

        action.setAuthenticator(authenticator);
        action.initialize();

        doExtract(prc);

        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        AuthenticationResult result = ac.getAuthenticationResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getSubject().getPrincipals(UsernamePrincipal.class).iterator().next().getName(), "PETER_THE_PRINCIPAL");
    }

    private void doExtract(ProfileRequestContext prc) throws Exception {
        ExtractUsernamePasswordFromFormRequest extract = new ExtractUsernamePasswordFromFormRequest();
        extract.setHttpServletRequest(action.getHttpServletRequest());
        extract.initialize();
        extract.execute(prc);
    }
}
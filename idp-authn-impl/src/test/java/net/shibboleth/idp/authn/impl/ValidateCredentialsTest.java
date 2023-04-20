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

import static org.testng.Assert.assertEquals;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.AuthenticationResultCode;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.SimpleBindAuthenticationHandler;
import org.ldaptive.jaas.LdapPrincipal;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.TemplateSearchDnResolver;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.AuthenticationErrorContext;
import net.shibboleth.idp.authn.context.LDAPResponseContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.ConstantSupplier;
import net.shibboleth.shared.testing.InMemoryDirectory;
import net.shibboleth.shared.testing.VelocityEngine;

/** Unit test for multiple credential validation. */
@SuppressWarnings("javadoc")
public class ValidateCredentialsTest extends BaseAuthenticationContextTest {

    private static final String DATA_PATH = "/net/shibboleth/idp/authn/impl/";

    private ValidateCredentials action;

    private InMemoryDirectory directoryServer;

    private TemplateSearchDnResolver dnResolver;

    private SimpleBindAuthenticationHandler authHandler;

    private Authenticator authenticator;

    /**
     * Creates an UnboundID in-memory directory server. Leverages LDIF found in test resources.
     */
    @BeforeClass public void setupDirectoryServer() {
        directoryServer =
            new InMemoryDirectory(
                new String[] {"dc=shibboleth,dc=net"},
                new ClassPathResource(DATA_PATH + "loginLDAPTest.ldif"),
                10389);
        directoryServer.start();
    }

    /**
     * Creates an Authenticator configured to use the in-memory directory server.
     */
    @BeforeClass public void setupAuthenticator() {

        dnResolver = new TemplateSearchDnResolver(new DefaultConnectionFactory("ldap://localhost:10389"),
                VelocityEngine.newVelocityEngine(), "(uid=$usernamePasswordContext.username)");
        dnResolver.setBaseDn("ou=people,dc=shibboleth,dc=net");

        authHandler = new SimpleBindAuthenticationHandler(new DefaultConnectionFactory("ldap://localhost:10389"));

        authenticator = new Authenticator(dnResolver, authHandler);
    }

    /**
     * Shutdown the in-memory directory server.
     * 
     * @throws Exception 
     */
    @AfterClass public void teardownDirectoryServer() throws Exception {
        if (directoryServer.openConnectionCount() > 0) {
            Thread.sleep(100);
        }
        assertEquals(directoryServer.openConnectionCount(), 0);
        directoryServer.stop(true);
    }

    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();

        final LDAPCredentialValidator ldap = new LDAPCredentialValidator();
        ldap.setId("ldap");
        assert authenticator != null;
        ldap.setAuthenticator(authenticator);
        ldap.initialize();

        final HTPasswdCredentialValidator htpasswd = new HTPasswdCredentialValidator();
        htpasswd.setId("htpasswd");
        htpasswd.setResource(new ClassPathResource(DATA_PATH + "htpasswd.txt"));
        htpasswd.initialize();
        
        action = new ValidateCredentials();
        action.setValidators(CollectionSupport.listOf(ldap, htpasswd));

        final Map<String, Collection<String>> mappings = new HashMap<>();
        mappings.put("UnknownUsername", CollectionSupport.singleton("DN_RESOLUTION_FAILURE"));
        mappings.put("InvalidPassword", CollectionSupport.singleton("INVALID_CREDENTIALS"));
        mappings.put("InvalidPassword", CollectionSupport.singleton(AuthnEventIds.INVALID_CREDENTIALS));
        mappings.put("ExpiringPassword", CollectionSupport.singleton("ACCOUNT_WARNING"));
        mappings.put("ExpiredPassword", CollectionSupport.listOf("PASSWORD_EXPIRED", "CHANGE_AFTER_RESET"));
        action.setClassifiedMessages(mappings);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
    }

    @Test public void testBadUsername() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "foo");
        getMockHttpServletRequest(action).addParameter("password", "bar");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        Assert.assertNull(ac.getAuthenticationResult());
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        assert lrc != null;
        final AuthenticationResponse lar = lrc.getAuthenticationResponse();
        assert lar != null;
        Assert.assertEquals(lar.getAuthenticationResultCode(),
                AuthenticationResultCode.DN_RESOLUTION_FAILURE);

        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        assert aec != null;

        ActionTestingSupport.assertEvent(event, "UnknownUsername");
        Assert.assertEquals(aec.getClassifiedErrors().size(), 1);
        Assert.assertTrue(aec.isClassifiedError("UnknownUsername"));
    }

    @Test public void testEmptyPassword() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        Assert.assertNull(ac.getAuthenticationResult());
        
        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        assert aec != null;
        ActionTestingSupport.assertEvent(event, "InvalidPassword");
        Assert.assertEquals(aec.getClassifiedErrors().size(), 1);
        Assert.assertTrue(aec.isClassifiedError("InvalidPassword"));
    }

    @Test public void testBadPassword() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "bar");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        Assert.assertNull(ac.getAuthenticationResult());
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        assert lrc != null;
        final AuthenticationResponse lar = lrc.getAuthenticationResponse();
        assert lar != null;
        Assert.assertEquals(lar.getAuthenticationResultCode(),
                AuthenticationResultCode.AUTHENTICATION_HANDLER_FAILURE);

        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        assert aec != null;
        ActionTestingSupport.assertEvent(event, "InvalidPassword");
        Assert.assertEquals(aec.getClassifiedErrors().size(), 2);
        Assert.assertTrue(aec.isClassifiedError("InvalidPassword"));
        Assert.assertTrue(aec.isClassifiedError(AuthnEventIds.INVALID_CREDENTIALS));
    }

    @Test public void testAuthorized() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        Assert.assertNotNull(ac.getSubcontext(UsernamePasswordContext.class));
        
        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertNull(aec);

        AuthenticationResult result = ac.getAuthenticationResult();
        assert result != null;
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        assert lrc != null;
        final AuthenticationResponse lar = lrc.getAuthenticationResponse();
        assert lar != null;
        Assert.assertEquals(lar.getAuthenticationResultCode(),
                AuthenticationResultCode.AUTHENTICATION_HANDLER_SUCCESS);

        UsernamePrincipal up = result.getSubject().getPrincipals(UsernamePrincipal.class).iterator().next();
        Assert.assertNotNull(up);
        Assert.assertEquals(up.getName(), "PETER_THE_PRINCIPAL");
        LdapPrincipal lp = result.getSubject().getPrincipals(LdapPrincipal.class).iterator().next();
        Assert.assertNotNull(lp);
        Assert.assertEquals(lp.getName(), "PETER_THE_PRINCIPAL");
        Assert.assertNotNull(lp.getLdapEntry());
    }

    @Test public void testAuthorized2() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL2");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        action.setCleanupHook(new ValidateCredentials.UsernamePasswordCleanupHook());
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        Assert.assertNull(ac.getSubcontext(UsernamePasswordContext.class));
        
        AuthenticationResult result = ac.getAuthenticationResult();
        assert result != null;
        UsernamePrincipal up = result.getSubject().getPrincipals(UsernamePrincipal.class).iterator().next();
        Assert.assertNotNull(up);
        Assert.assertEquals(up.getName(), "PETER_THE_PRINCIPAL2");
        Assert.assertTrue(result.getSubject().getPrincipals(LdapPrincipal.class).isEmpty());

        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        assert lrc != null;
        final AuthenticationResponse lar = lrc.getAuthenticationResponse();
        assert lar != null;
        Assert.assertEquals(lar.getAuthenticationResultCode(),
                AuthenticationResultCode.DN_RESOLUTION_FAILURE);

        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        assert aec != null;
        Assert.assertEquals(aec.getClassifiedErrors().size(), 1);
        Assert.assertTrue(aec.isClassifiedError("UnknownUsername"));
    }
    
    @Test public void testBadPassword2() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL2");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        action.setRequireAll(true);
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        Assert.assertNull(ac.getAuthenticationResult());
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        assert lrc != null;
        final AuthenticationResponse lar = lrc.getAuthenticationResponse();
        assert lar != null;
        Assert.assertEquals(lar.getAuthenticationResultCode(),
                AuthenticationResultCode.DN_RESOLUTION_FAILURE);

        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        assert aec != null;
        ActionTestingSupport.assertEvent(event, AuthnEventIds.UNKNOWN_USERNAME);
        Assert.assertEquals(aec.getClassifiedErrors().size(), 1);
        Assert.assertTrue(aec.isClassifiedError(AuthnEventIds.UNKNOWN_USERNAME));
    }
    
    @Test public void testAuthorizedAll() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        action.setRequireAll(true);
        action.setCleanupHook(new ValidateCredentials.UsernamePasswordCleanupHook());
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        Assert.assertNull(ac.getSubcontext(UsernamePasswordContext.class));
        
        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertNull(aec);
        
        AuthenticationResult result = ac.getAuthenticationResult();
        assert result != null;
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        assert lrc != null;
        final AuthenticationResponse lar = lrc.getAuthenticationResponse();
        assert lar != null;
        Assert.assertEquals(lar.getAuthenticationResultCode(),
                AuthenticationResultCode.AUTHENTICATION_HANDLER_SUCCESS);

        final Set<UsernamePrincipal> ups = result.getSubject().getPrincipals(UsernamePrincipal.class);
        Assert.assertEquals(ups.size(), 1);
        Assert.assertNotNull(ups.iterator().next());
        Assert.assertEquals(ups.iterator().next().getName(), "PETER_THE_PRINCIPAL");
        LdapPrincipal lp = result.getSubject().getPrincipals(LdapPrincipal.class).iterator().next();
        Assert.assertNotNull(lp);
        Assert.assertEquals(lp.getName(), "PETER_THE_PRINCIPAL");
        Assert.assertNotNull(lp.getLdapEntry());
    }

    private void doExtract() throws ComponentInitializationException {
        final ExtractUsernamePasswordFromFormRequest extract = new ExtractUsernamePasswordFromFormRequest();
        extract.setHttpServletRequestSupplier(action.getHttpServletRequestSupplier());
        extract.initialize();
        extract.execute(src);
    }
    
}
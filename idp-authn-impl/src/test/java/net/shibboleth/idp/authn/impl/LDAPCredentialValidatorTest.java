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

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.auth.AccountState;
import org.ldaptive.auth.AuthenticationResultCode;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.SimpleBindAuthenticationHandler;
import org.ldaptive.auth.SearchDnResolver;
import org.ldaptive.auth.ext.PasswordPolicyAccountState;
import org.ldaptive.control.PasswordPolicyControl;
import org.ldaptive.jaas.LdapPrincipal;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.TemplateSearchDnResolver;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.AuthenticationErrorContext;
import net.shibboleth.idp.authn.context.AuthenticationWarningContext;
import net.shibboleth.idp.authn.context.LDAPResponseContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.InMemoryDirectory;
import net.shibboleth.shared.testing.VelocityEngine;

import static org.testng.Assert.assertEquals;

/** Unit test for LDAP credential validation. */
public class LDAPCredentialValidatorTest extends BaseAuthenticationContextTest {

    private static final String DATA_PATH = "/net/shibboleth/idp/authn/impl/";

    private LDAPCredentialValidator validator;
    
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
     */
    @AfterClass public void teardownDirectoryServer() {
        assertEquals(directoryServer.openConnectionCount(), 0);
        directoryServer.stop(true);
    }

    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();

        validator = new LDAPCredentialValidator();
        validator.setId("ldaptest");
        
        action = new ValidateCredentials();
        action.setValidators(Collections.singletonList(validator));

        final Map<String, Collection<String>> mappings = new HashMap<>();
        mappings.put("UnknownUsername", Collections.singleton("DN_RESOLUTION_FAILURE"));
        mappings.put("InvalidPassword", Collections.singleton("INVALID_CREDENTIALS"));
        mappings.put("ExpiringPassword", Collections.singleton("ACCOUNT_WARNING"));
        mappings.put("ExpiredPassword", Arrays.asList("PASSWORD_EXPIRED", "CHANGE_AFTER_RESET"));
        action.setClassifiedMessages(mappings);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new Supplier<> () {public HttpServletRequest get() { return request;}});
    }

    @Test public void testMissingFlow() throws ComponentInitializationException {
        validator.setAuthenticator(authenticator);
        validator.initialize();
        
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    @Test public void testMissingUser() throws ComponentInitializationException {
        prc.getSubcontext(AuthenticationContext.class).setAttemptedFlow(authenticationFlows.get(0));

        validator.setAuthenticator(authenticator);
        validator.initialize();
        
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testMissingUser2() throws ComponentInitializationException {
        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        ac.getSubcontext(UsernamePasswordContext.class, true);
        
        validator.setAuthenticator(authenticator);
        validator.initialize();
        
        action.initialize();

        final Event event = action.execute(src);
        Assert.assertNull(ac.getAuthenticationResult());
        Assert.assertNull(ac.getSubcontext(LDAPResponseContext.class));
        Assert.assertNull(ac.getSubcontext(AuthenticationErrorContext.class));
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testUnmatchedUser() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "foo");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "bar");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        ac.getSubcontext(UsernamePasswordContext.class, true);
        
        validator.setAuthenticator(authenticator);
        validator.setMatchExpression(Pattern.compile("foo.+"));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
    }

    @Test public void testBadConfig() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "foo");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        validator.setAuthenticator(new Authenticator(new SearchDnResolver(), authHandler));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        Assert.assertNull(ac.getAuthenticationResult());
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        Assert.assertNotNull(lrc.getAuthenticationResponse());
        Assert.assertEquals(lrc.getAuthenticationResponse().getAuthenticationResultCode(),
                AuthenticationResultCode.DN_RESOLUTION_FAILURE);

        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertNotNull(aec);
        ActionTestingSupport.assertEvent(event, "UnknownUsername");
        Assert.assertEquals(aec.getClassifiedErrors().size(), 1);
        Assert.assertTrue(aec.isClassifiedError("UnknownUsername"));
    }

    @Test public void testBadConfig2() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "bar");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        validator.setAuthenticator(new Authenticator(dnResolver,
                new SimpleBindAuthenticationHandler(new DefaultConnectionFactory("ldap://unknown:389"))));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);

        Assert.assertNull(ac.getAuthenticationResult());
        Assert.assertNull(ac.getSubcontext(LDAPResponseContext.class));
        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertNotNull(aec);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.AUTHN_EXCEPTION);
        Assert.assertEquals(aec.getExceptions().size(), 1);
        Assert.assertEquals(aec.getClassifiedErrors().size(), 0);
    }

    @Test public void testBadUsername() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "foo");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "bar");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        validator.setAuthenticator(authenticator);
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        Assert.assertNull(ac.getAuthenticationResult());
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        Assert.assertNotNull(lrc.getAuthenticationResponse());
        Assert.assertEquals(lrc.getAuthenticationResponse().getAuthenticationResultCode(),
                AuthenticationResultCode.DN_RESOLUTION_FAILURE);

        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertNotNull(aec);
        ActionTestingSupport.assertEvent(event, "UnknownUsername");
        Assert.assertEquals(aec.getClassifiedErrors().size(), 1);
        Assert.assertTrue(aec.isClassifiedError("UnknownUsername"));
    }

    @Test public void testEmptyPassword() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        validator.setAuthenticator(authenticator);
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        Assert.assertNull(ac.getAuthenticationResult());
        Assert.assertNull(ac.getSubcontext(LDAPResponseContext.class));
        Assert.assertNull(ac.getSubcontext(AuthenticationErrorContext.class));
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_CREDENTIALS);
    }

    @Test public void testBadPassword() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "bar");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        validator.setAuthenticator(authenticator);
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        Assert.assertNull(ac.getAuthenticationResult());
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        Assert.assertNotNull(lrc.getAuthenticationResponse());
        Assert.assertEquals(lrc.getAuthenticationResponse().getAuthenticationResultCode(),
                AuthenticationResultCode.AUTHENTICATION_HANDLER_FAILURE);

        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertNotNull(aec);
        ActionTestingSupport.assertEvent(event, "InvalidPassword");
        Assert.assertEquals(aec.getClassifiedErrors().size(), 1);
        Assert.assertTrue(aec.isClassifiedError("InvalidPassword"));
    }

    @Test public void testExpiredPassword() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "bar");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));

        Authenticator errorAuthenticator = new Authenticator(dnResolver, authHandler);
        errorAuthenticator.setResponseHandlers(
            response -> response.setAccountState(
                new PasswordPolicyAccountState(PasswordPolicyControl.Error.PASSWORD_EXPIRED)));
        validator.setAuthenticator(errorAuthenticator);
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        Assert.assertNull(ac.getAuthenticationResult());
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        Assert.assertNotNull(lrc.getAuthenticationResponse());
        Assert.assertEquals(lrc.getAuthenticationResponse().getAuthenticationResultCode(),
                AuthenticationResultCode.AUTHENTICATION_HANDLER_FAILURE);

        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertNotNull(aec);
        ActionTestingSupport.assertEvent(event, "ExpiredPassword");
        Assert.assertEquals(aec.getClassifiedErrors().size(), 2);
        Assert.assertTrue(aec.isClassifiedError("ExpiredPassword"));
        Assert.assertTrue(aec.isClassifiedError("InvalidPassword"));
    }

    @Test public void testChangeAfterReset() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));

        Authenticator errorAuthenticator = new Authenticator(dnResolver, authHandler);
        errorAuthenticator.setResponseHandlers(
            response -> response.setAccountState(
                new PasswordPolicyAccountState(PasswordPolicyControl.Error.CHANGE_AFTER_RESET)));
        validator.setAuthenticator(errorAuthenticator);
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        AuthenticationResult result = ac.getAuthenticationResult();
        Assert.assertNotNull(result);
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        Assert.assertNotNull(lrc.getAuthenticationResponse());
        Assert.assertEquals(lrc.getAuthenticationResponse().getAuthenticationResultCode(),
                AuthenticationResultCode.AUTHENTICATION_HANDLER_SUCCESS);

        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertNull(aec);
        AuthenticationWarningContext awc = ac.getSubcontext(AuthenticationWarningContext.class);
        Assert.assertNotNull(awc);
        ActionTestingSupport.assertEvent(event, "ExpiredPassword");
        Assert.assertEquals(awc.getClassifiedWarnings().size(), 1);
        Assert.assertTrue(awc.isClassifiedWarning("ExpiredPassword"));

        UsernamePrincipal up = result.getSubject().getPrincipals(UsernamePrincipal.class).iterator().next();
        Assert.assertNotNull(up);
        Assert.assertEquals(up.getName(), "PETER_THE_PRINCIPAL");
        LdapPrincipal lp = result.getSubject().getPrincipals(LdapPrincipal.class).iterator().next();
        Assert.assertNotNull(lp);
        Assert.assertEquals(lp.getName(), "PETER_THE_PRINCIPAL");
        Assert.assertNotNull(lp.getLdapEntry());
    }

    @Test public void testExpiringPassword() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));

        Authenticator warningAuthenticator = new Authenticator(dnResolver, authHandler);
        warningAuthenticator.setResponseHandlers(
            response -> response.setAccountState(
                new AccountState(new AccountState.DefaultWarning(ZonedDateTime.now(), 10))));
        validator.setAuthenticator(warningAuthenticator);
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);

        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertNull(aec);

        AuthenticationResult result = ac.getAuthenticationResult();
        Assert.assertNotNull(result);
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        Assert.assertNotNull(lrc.getAuthenticationResponse());
        Assert.assertEquals(lrc.getAuthenticationResponse().getAuthenticationResultCode(),
                AuthenticationResultCode.AUTHENTICATION_HANDLER_SUCCESS);

        ActionTestingSupport.assertEvent(event, "ExpiringPassword");

        AuthenticationWarningContext awc = ac.getSubcontext(AuthenticationWarningContext.class);
        Assert.assertNotNull(awc);
        Assert.assertEquals(awc.getClassifiedWarnings().size(), 1);
        Assert.assertTrue(awc.isClassifiedWarning("ExpiringPassword"));

        UsernamePrincipal up = result.getSubject().getPrincipals(UsernamePrincipal.class).iterator().next();
        Assert.assertNotNull(up);
        Assert.assertEquals(up.getName(), "PETER_THE_PRINCIPAL");
        LdapPrincipal lp = result.getSubject().getPrincipals(LdapPrincipal.class).iterator().next();
        Assert.assertNotNull(lp);
        Assert.assertEquals(lp.getName(), "PETER_THE_PRINCIPAL");
        Assert.assertNotNull(lp.getLdapEntry());
    }

    @Test public void testAuthorized() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.setAuthenticator(authenticator);
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertNull(aec);

        AuthenticationResult result = ac.getAuthenticationResult();
        Assert.assertNotNull(result);
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        Assert.assertNotNull(lrc.getAuthenticationResponse());
        Assert.assertEquals(lrc.getAuthenticationResponse().getAuthenticationResultCode(),
                AuthenticationResultCode.AUTHENTICATION_HANDLER_SUCCESS);

        UsernamePrincipal up = result.getSubject().getPrincipals(UsernamePrincipal.class).iterator().next();
        Assert.assertNotNull(up);
        Assert.assertEquals(up.getName(), "PETER_THE_PRINCIPAL");
        LdapPrincipal lp = result.getSubject().getPrincipals(LdapPrincipal.class).iterator().next();
        Assert.assertNotNull(lp);
        Assert.assertEquals(lp.getName(), "PETER_THE_PRINCIPAL");
        Assert.assertNotNull(lp.getLdapEntry());
    }

    @Test public void testComputedAndAuthorized() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "change");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        validator.setAuthenticator(authenticator);
        validator.setPasswordLookupStrategy(
                new Function<ProfileRequestContext,char[]>() {
                    public char[] apply(final ProfileRequestContext input) {
                        return (input.getSubcontext(
                                AuthenticationContext.class).getSubcontext(
                                        UsernamePasswordContext.class).getPassword() + "it").toCharArray();
                    }
                });
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertNull(aec);

        AuthenticationResult result = ac.getAuthenticationResult();
        Assert.assertNotNull(result);
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        Assert.assertNotNull(lrc.getAuthenticationResponse());
        Assert.assertEquals(lrc.getAuthenticationResponse().getAuthenticationResultCode(),
                AuthenticationResultCode.AUTHENTICATION_HANDLER_SUCCESS);

        UsernamePrincipal up = result.getSubject().getPrincipals(UsernamePrincipal.class).iterator().next();
        Assert.assertNotNull(up);
        Assert.assertEquals(up.getName(), "PETER_THE_PRINCIPAL");
        LdapPrincipal lp = result.getSubject().getPrincipals(LdapPrincipal.class).iterator().next();
        Assert.assertNotNull(lp);
        Assert.assertEquals(lp.getName(), "PETER_THE_PRINCIPAL");
        Assert.assertNotNull(lp.getLdapEntry());
    }

    @Test public void testDefaultFilterSyntax() throws ComponentInitializationException {
        TemplateSearchDnResolver testResolver = new TemplateSearchDnResolver(new DefaultConnectionFactory("ldap://localhost:10389"),
                VelocityEngine.newVelocityEngine(), "(uid={user})");
        testResolver.setBaseDn("ou=people,dc=shibboleth,dc=net");


        Authenticator defaultFilterAuthenticator = new Authenticator(testResolver, authHandler);
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        validator.setAuthenticator(defaultFilterAuthenticator);
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertNull(aec);

        AuthenticationResult result = ac.getAuthenticationResult();
        Assert.assertNotNull(result);
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        Assert.assertNotNull(lrc.getAuthenticationResponse());
        Assert.assertEquals(lrc.getAuthenticationResponse().getAuthenticationResultCode(),
                AuthenticationResultCode.AUTHENTICATION_HANDLER_SUCCESS);

        UsernamePrincipal up = result.getSubject().getPrincipals(UsernamePrincipal.class).iterator().next();
        Assert.assertNotNull(up);
        Assert.assertEquals(up.getName(), "PETER_THE_PRINCIPAL");
        LdapPrincipal lp = result.getSubject().getPrincipals(LdapPrincipal.class).iterator().next();
        Assert.assertNotNull(lp);
        Assert.assertEquals(lp.getName(), "PETER_THE_PRINCIPAL");
        Assert.assertNotNull(lp.getLdapEntry());
    }

    @Test public void testCombinedFilterSyntax() throws ComponentInitializationException {
        TemplateSearchDnResolver testResolver = new TemplateSearchDnResolver(new DefaultConnectionFactory("ldap://localhost:10389"),
                VelocityEngine.newVelocityEngine(), "(|(mail=$usernamePasswordContext.username)(uid={user}))");
        testResolver.setBaseDn("ou=people,dc=shibboleth,dc=net");


        Authenticator defaultFilterAuthenticator = new Authenticator(testResolver, authHandler);
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        validator.setAuthenticator(defaultFilterAuthenticator);
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertNull(aec);

        AuthenticationResult result = ac.getAuthenticationResult();
        Assert.assertNotNull(result);
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        Assert.assertNotNull(lrc.getAuthenticationResponse());
        Assert.assertEquals(lrc.getAuthenticationResponse().getAuthenticationResultCode(),
                AuthenticationResultCode.AUTHENTICATION_HANDLER_SUCCESS);

        UsernamePrincipal up = result.getSubject().getPrincipals(UsernamePrincipal.class).iterator().next();
        Assert.assertNotNull(up);
        Assert.assertEquals(up.getName(), "PETER_THE_PRINCIPAL");
        LdapPrincipal lp = result.getSubject().getPrincipals(LdapPrincipal.class).iterator().next();
        Assert.assertNotNull(lp);
        Assert.assertEquals(lp.getName(), "PETER_THE_PRINCIPAL");
        Assert.assertNotNull(lp.getLdapEntry());
    }

    @Test public void testMatchAndAuthorized() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.setAuthenticator(authenticator);
        validator.setMatchExpression(Pattern.compile(".+_THE_.+"));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        AuthenticationErrorContext aec = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertNull(aec);

        AuthenticationResult result = ac.getAuthenticationResult();
        Assert.assertNotNull(result);
        LDAPResponseContext lrc = ac.getSubcontext(LDAPResponseContext.class);
        Assert.assertNotNull(lrc.getAuthenticationResponse());
        Assert.assertEquals(lrc.getAuthenticationResponse().getAuthenticationResultCode(),
                AuthenticationResultCode.AUTHENTICATION_HANDLER_SUCCESS);

        UsernamePrincipal up = result.getSubject().getPrincipals(UsernamePrincipal.class).iterator().next();
        Assert.assertNotNull(up);
        Assert.assertEquals(up.getName(), "PETER_THE_PRINCIPAL");
        LdapPrincipal lp = result.getSubject().getPrincipals(LdapPrincipal.class).iterator().next();
        Assert.assertNotNull(lp);
        Assert.assertEquals(lp.getName(), "PETER_THE_PRINCIPAL");
        Assert.assertNotNull(lp.getLdapEntry());
    }

    @Test public void testAuthorizedAndKeepContext() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.setAuthenticator(authenticator);
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

    private void doExtract() throws ComponentInitializationException {
        final ExtractUsernamePasswordFromFormRequest extract = new ExtractUsernamePasswordFromFormRequest();
        extract.setHttpServletRequestSupplier(action.getHttpServletRequestSupplier());
        extract.initialize();
        extract.execute(src);
    }
    
}
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.AuthenticationErrorContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.authn.principal.impl.ExactPrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.testing.TestPrincipal;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.net.URISupport;
import net.shibboleth.shared.testing.InMemoryDirectory;

import static org.testng.Assert.assertEquals;

/** Unit test for JAAS validation. */
public class JAASCredentialValidatorTest extends BaseAuthenticationContextTest {

    private static final String DATA_PATH = "src/test/resources/net/shibboleth/idp/authn/impl/";

    private static final String DATA_CLASSPATH = "/net/shibboleth/idp/authn/impl/";

    private JAASCredentialValidator validator;
    
    private ValidateCredentials action;

    private InMemoryDirectory directoryServer;

    /**
     * Creates an UnboundID in-memory directory server. Leverages LDIF found in test resources.
     */
    @BeforeClass public void setupDirectoryServer() {
        directoryServer =
            new InMemoryDirectory(
                new String[] {"dc=shibboleth,dc=net"},
                new ClassPathResource(DATA_CLASSPATH + "loginLDAPTest.ldif"),
                10389);
        directoryServer.start();
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

        validator = new JAASCredentialValidator();
        validator.setId("jaastest");
        
        action = new ValidateCredentials();
        action.setValidators(Collections.singletonList(validator));
        
        final Map<String,Collection<String>> mappings = new HashMap<>();
        mappings.put("UnknownUsername", Collections.singleton("DN_RESOLUTION_FAILURE"));
        mappings.put("InvalidPassword", Collections.singleton("INVALID_CREDENTIALS"));
        action.setClassifiedMessages(mappings);
        
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new Supplier<> () {public HttpServletRequest get() { return request;}});
    }

    @Test public void testMissingFlow() throws ComponentInitializationException {
        validator.initialize();
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    @Test public void testMissingUser() throws ComponentInitializationException {
        prc.getSubcontext(AuthenticationContext.class).setAttemptedFlow(authenticationFlows.get(0));
        
        validator.initialize();
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testMissingUser2() throws ComponentInitializationException {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        ac.getSubcontext(UsernamePasswordContext.class, true);
        
        validator.initialize();
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testNoConfig() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "foo");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        validator.initialize();
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_CREDENTIALS);
        AuthenticationErrorContext errorCtx = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertEquals(errorCtx.getExceptions().size(), 1);
        Assert.assertTrue(errorCtx.getExceptions().get(0) instanceof LoginException);
    }

    @Test public void testBadConfig() throws ComponentInitializationException, URISyntaxException, IOException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "foo");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        validator.setLoginConfigNames(Collections.singletonList("ShibBadAuth"));
        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigParameters(URISupport.fileURIFromAbsolutePath(getCurrentDir()
                + '/' + DATA_PATH + "jaas.config"));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_CREDENTIALS);
        AuthenticationErrorContext errorCtx = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertEquals(errorCtx.getExceptions().size(), 1);
        Assert.assertTrue(errorCtx.getExceptions().get(0) instanceof LoginException);
    }

    @Test public void testUnsupportedConfig() throws ComponentInitializationException, URISyntaxException, IOException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "foo");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        final RequestedPrincipalContext rpc = ac.getSubcontext(RequestedPrincipalContext.class, true);
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(Collections.<Principal>singletonList(new TestPrincipal("test1")));

        validator.setLoginConfigurations(Collections.singletonList(new Pair<String,Collection<Principal>>("ShibUserPassAuth",
                Collections.singletonList(new TestPrincipal("test2")))));
        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigParameters(URISupport.fileURIFromAbsolutePath(getCurrentDir()
                + '/' + DATA_PATH + "jaas.config"));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
    }
    
    @Test public void testUnmatchedUser() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "foo");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        ac.getSubcontext(UsernamePasswordContext.class, true);
        
        validator.setMatchExpression(Pattern.compile("foo.+"));
        validator.initialize();
        
        action.initialize();
        
        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
    }

    @Test public void testBadUsername() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "foo");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigResource(new ClassPathResource(DATA_CLASSPATH + "jaas.config"));
        
        validator.initialize();
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "UnknownUsername");
        AuthenticationErrorContext errorCtx = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertTrue(errorCtx.getExceptions().get(0) instanceof LoginException);
        Assert.assertTrue(errorCtx.isClassifiedError("UnknownUsername"));
        Assert.assertFalse(errorCtx.isClassifiedError("InvalidPassword"));
    }

    @Test public void testBadPassword() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigResource(new ClassPathResource(DATA_CLASSPATH + "jaas.config"));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "InvalidPassword");
        AuthenticationErrorContext errorCtx = ac.getSubcontext(AuthenticationErrorContext.class);
        Assert.assertTrue(errorCtx.getExceptions().get(0) instanceof LoginException);
        Assert.assertFalse(errorCtx.isClassifiedError("UnknownUsername"));
        Assert.assertTrue(errorCtx.isClassifiedError("InvalidPassword"));
    }

    @Test public void testAuthorized() throws ComponentInitializationException, URISyntaxException, IOException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigParameters(URISupport.fileURIFromAbsolutePath(getCurrentDir()
                + '/' + DATA_PATH + "jaas.config"));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
    }

    @Test public void testAuthorizedAndKeep() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigResource(new ClassPathResource(DATA_CLASSPATH + "jaas.config"));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
    }

    @Test public void testSupported() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        final RequestedPrincipalContext rpc = ac.getSubcontext(RequestedPrincipalContext.class, true);
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(Collections.<Principal>singletonList(new TestPrincipal("test1")));

        validator.setLoginConfigurations(Collections.singletonList(new Pair<String,Collection<Principal>>("ShibUserPassAuth",
                Collections.<Principal>singletonList(new TestPrincipal("test1")))));
        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigResource(new ClassPathResource(DATA_CLASSPATH + "jaas.config"));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(TestPrincipal.class).iterator()
                .next().getName(), "test1");
    }
    
    @Test public void testMultiConfigAuthorized() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.setLoginConfigNames(Arrays.asList("ShibBadAuth", "ShibUserPassAuth"));
        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigResource(new ClassPathResource(DATA_CLASSPATH + "jaas.config"));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
    }
    
    @Test public void testMatchAndAuthorized() throws ComponentInitializationException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("username", "PETER_THE_PRINCIPAL");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigResource(new ClassPathResource(DATA_CLASSPATH + "jaas.config"));
        validator.setMatchExpression(Pattern.compile(".+_THE_.+"));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
    }
    
    private void doExtract() throws ComponentInitializationException {
        final ExtractUsernamePasswordFromFormRequest extract = new ExtractUsernamePasswordFromFormRequest();
        extract.setHttpServletRequestSupplier(action.getHttpServletRequestSupplier());
        extract.initialize();
        extract.execute(src);
    }

    private String getCurrentDir() throws IOException {

        final String currentDir = new java.io.File(".").getCanonicalPath();

        return currentDir.replace(File.separatorChar, '/');
    }
    
}
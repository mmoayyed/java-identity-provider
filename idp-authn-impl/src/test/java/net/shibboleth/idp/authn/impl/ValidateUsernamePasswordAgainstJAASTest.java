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
import java.security.URIParameter;

import javax.security.auth.login.LoginException;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.UsernamePrincipal;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.AuthenticationErrorContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.utilities.java.support.net.UriSupport;

import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.action.EventIds;
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

/** {@link ValidateUsernamePasswordAgainstJAAS} unit test. */
public class ValidateUsernamePasswordAgainstJAASTest extends InitializeAuthenticationContextTest {

    private static final String DATA_PATH = "src/test/resources/data/net/shibboleth/idp/authn/impl/";
    
    private ValidateUsernamePasswordAgainstJAAS action;

    private InMemoryDirectoryServer directoryServer;

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
     * Shutdown the in-memory directory server.
     */
    @AfterClass public void teardownDirectoryServer() {
        directoryServer.shutDown(true);
    }

    @BeforeMethod public void setUp() throws Exception {
        super.setUp();

        action = new ValidateUsernamePasswordAgainstJAAS();
        action.setUnknownUsernameErrors(ImmutableList.of("DN_RESOLUTION_FAILURE"));
        action.setInvalidPasswordErrors(ImmutableList.of("INVALID_CREDENTIALS"));
    }

    @Test public void testMissingFlow() throws Exception {
        action.initialize();

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, EventIds.INVALID_PROFILE_CTX);
    }

    @Test public void testMissingUser() throws Exception {
        prc.getSubcontext(AuthenticationContext.class, false).setAttemptedFlow(authenticationFlows.get(0));
        action.initialize();

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testMissingUser2() throws Exception {
        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        ac.getSubcontext(UsernamePasswordContext.class, true);
        action.initialize();

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testBadConfig() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("username", "foo");
        request.addParameter("password", "bar");
        prc.setHttpRequest(request);

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        action.initialize();

        doExtract(prc);

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_CREDENTIALS);
        AuthenticationErrorContext errorCtx = ac.getSubcontext(AuthenticationErrorContext.class, false);
        Assert.assertEquals(errorCtx.getExceptions().size(), 1);
        Assert.assertTrue(errorCtx.getExceptions().get(0) instanceof LoginException);
    }

    @Test public void testBadUsername() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("username", "foo");
        request.addParameter("password", "bar");
        prc.setHttpRequest(request);

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        action.setLoginConfigType("JavaLoginConfig");
        System.out.println(getCurrentDir());
        action.setLoginConfigParameters(new URIParameter(UriSupport.fileURIFromAbsolutePath(getCurrentDir()
                + '/' + DATA_PATH + "jaas.config")));
        action.initialize();

        doExtract(prc);

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_CREDENTIALS);
        AuthenticationErrorContext errorCtx = ac.getSubcontext(AuthenticationErrorContext.class, false);
        Assert.assertTrue(errorCtx.getExceptions().get(0) instanceof LoginException);
        Assert.assertTrue(errorCtx.isUnknownUsername());
        Assert.assertFalse(errorCtx.isInvalidPassword());
    }

    @Test public void testBadPassword() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("username", "PETER_THE_PRINCIPAL");
        request.addParameter("password", "bar");
        prc.setHttpRequest(request);

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        action.setLoginConfigType("JavaLoginConfig");
        System.out.println(getCurrentDir());
        action.setLoginConfigParameters(new URIParameter(UriSupport.fileURIFromAbsolutePath(getCurrentDir()
                + '/' + DATA_PATH + "jaas.config")));
        action.initialize();

        doExtract(prc);

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_CREDENTIALS);
        AuthenticationErrorContext errorCtx = ac.getSubcontext(AuthenticationErrorContext.class, false);
        Assert.assertTrue(errorCtx.getExceptions().get(0) instanceof LoginException);
        Assert.assertFalse(errorCtx.isUnknownUsername());
        Assert.assertTrue(errorCtx.isInvalidPassword());
    }

    @Test public void testAuthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("username", "PETER_THE_PRINCIPAL");
        request.addParameter("password", "changeit");
        prc.setHttpRequest(request);

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));

        action.setLoginConfigType("JavaLoginConfig");
        System.out.println(getCurrentDir());
        action.setLoginConfigParameters(new URIParameter(UriSupport.fileURIFromAbsolutePath(getCurrentDir()
                + '/' + DATA_PATH + "jaas.config")));
        action.initialize();

        doExtract(prc);

        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
    }

    private void doExtract(ProfileRequestContext prc) throws Exception {
        ExtractUsernamePasswordFromFormRequest extract = new ExtractUsernamePasswordFromFormRequest();
        extract.initialize();
        extract.execute(prc);
    }

    private String getCurrentDir() throws IOException {

        final String currentDir = new java.io.File(".").getCanonicalPath();

        return currentDir.replace(File.separatorChar, '/');
    }
}
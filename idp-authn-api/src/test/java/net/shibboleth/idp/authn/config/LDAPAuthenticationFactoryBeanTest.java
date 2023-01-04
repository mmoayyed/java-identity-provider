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

package net.shibboleth.idp.authn.config;

import java.time.Duration;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.ldaptive.Credential;
import org.ldaptive.auth.AuthenticationRequest;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.User;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;

import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.utilities.java.support.velocity.VelocityEngine;

/** Unit test for LDAP authentication factory bean. */
public class LDAPAuthenticationFactoryBeanTest {

    private static final String DATA_PATH = "src/test/resources/net/shibboleth/idp/authn/config/";

    private InMemoryDirectoryServer directoryServer;

    private LDAPAuthenticationFactoryBean factoryBean;

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
     * Shutdown the in-memory directory server.
     */
    @AfterClass public void teardownDirectoryServer() {
        directoryServer.shutDown(true);
    }

    @BeforeMethod
    public void setupAuthenticator() throws Exception {
        factoryBean = new LDAPAuthenticationFactoryBean();
        factoryBean.setLdapUrl("ldap://localhost:10389");
        factoryBean.setBaseDn("ou=people,dc=shibboleth,dc=net");
        factoryBean.setUserFilter("(uid={user})");
        factoryBean.setSubtreeSearch(false);
        factoryBean.setVelocityEngine(VelocityEngine.newVelocityEngine());
        factoryBean.setAuthenticatorType("anonSearchAuthenticator");
        factoryBean.setTrustType("disabled");
        factoryBean.setConnectionStrategyType("ACTIVE_PASSIVE");
        factoryBean.setUseStartTLS(false);
        factoryBean.setConnectTimeout(Duration.ofSeconds(3));
        factoryBean.setResponseTimeout(Duration.ofSeconds(3));
        factoryBean.setDisablePooling(true);
        factoryBean.setBlockWaitTime(Duration.ofSeconds(3));
        factoryBean.setPrunePeriod(Duration.ofMinutes(5));
        factoryBean.setIdleTime(Duration.ofMinutes(10));
        factoryBean.setMinPoolSize(3);
        factoryBean.setMaxPoolSize(5);
        factoryBean.setValidateOnCheckout(false);
        factoryBean.setValidatePeriodically(true);
        factoryBean.setValidatePeriod(Duration.ofMinutes(5));
        factoryBean.setValidateDn("");
        factoryBean.setValidateFilter("(objectClass=*)");
        factoryBean.setBindPoolPassivatorType("anonymousBind");
        authenticator = factoryBean.createInstance();
    }

    @Test public void testAuthnSuccess() throws Exception {
        final AuthenticationResponse response = authenticator.authenticate(
            createAuthenticationRequest("PETER_THE_PRINCIPAL", "changeit"));
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getResult());
    }

    @Test public void testAuthnFailure() throws Exception {
        final AuthenticationResponse response = authenticator.authenticate(
            createAuthenticationRequest("PETER_THE_PRINCIPAL", "wrong"));
        Assert.assertNotNull(response);
        Assert.assertFalse(response.getResult());
    }

    private AuthenticationRequest createAuthenticationRequest(final String username, final String password) {
        final UsernamePasswordContext upc = new UsernamePasswordContext();
        upc.setUsername(username);
        upc.setPassword(password);
        return new AuthenticationRequest(
            new User(upc.getUsername(), new VelocityContext(Map.of("usernamePasswordContext", upc))),
            new Credential(upc.getPassword()));
    }
}
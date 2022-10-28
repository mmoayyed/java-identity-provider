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

package net.shibboleth.idp.test;

import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.annotation.Nonnull;

import com.unboundid.util.ssl.SSLUtil;
import org.ldaptive.ssl.CredentialConfigFactory;
import org.ldaptive.ssl.SSLContextInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFReader;

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.Positive;
import net.shibboleth.shared.logic.Constraint;

/**
 * Manages an instance of the in-memory directory server.
 */
public class InMemoryDirectory {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InMemoryDirectory.class);

    /** Directory server. */
    @Nonnull private final InMemoryDirectoryServer directoryServer;

    /**
     * Constructor with STARTTLS support.
     * 
     * @param ldif the LDIF resource to be imported
     * @param port port to listen on
     * @param keystore to use for startTLS
     * 
     * @throws LDAPException if the in-memory directory server cannot be created
     * @throws IOException if the LDIF resource cannot be imported
     */
    public InMemoryDirectory(@ParameterName(name="ldif") @Nonnull final Resource ldif,
            @ParameterName(name="port") @Positive final int port,
            @ParameterName(name="keystore") @Nonnull final Resource keystore) throws LDAPException,
            IOException {
        Constraint.isNotNull(ldif, "LDIF resource cannot be null");
        final InMemoryDirectoryServerConfig config =
                new InMemoryDirectoryServerConfig("dc=example,dc=org", "ou=system");
        try {
            final KeyStore ks = KeyStore.getInstance("JKS");
            final String ksPass = "changeit";
            ks.load(keystore.getInputStream(), ksPass.toCharArray());
            final SSLContextInitializer sslInit =
                CredentialConfigFactory.createKeyStoreCredentialConfig(ks, ksPass).createSSLContextInitializer();
            final SSLUtil sslUtil = new SSLUtil(sslInit.getKeyManagers(), sslInit.getTrustManagers());
            config.setListenerConfigs(
                InMemoryListenerConfig.createLDAPConfig(
                    "default", InetAddress.getByName("localhost"), port, sslUtil.createSSLSocketFactory()));
        } catch (final GeneralSecurityException e) {
            throw new IOException("Error reading keystore", e);
        }
        config.addAdditionalBindCredentials("cn=Directory Manager", "password");
        directoryServer = new InMemoryDirectoryServer(config);
        directoryServer.importFromLDIF(true, new LDIFReader(ldif.getInputStream()));
    }

    /**
     * Starts the directory server.
     * 
     * @throws LDAPException if the in-memory directory server cannot be started
     */
    public void start() throws LDAPException {
        directoryServer.startListening();
        log.info("In-memory directory server started");
    }

    /**
     * Stops the directory server without closing existing connections. Resources should be configured so that LDAP
     * connections are closed when the spring application context shuts down.
     */
    public void stop() {
        directoryServer.shutDown(false);
        log.info("In-memory directory server stopped");
    }
}

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
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.annotation.Nonnull;

import org.ldaptive.ssl.CredentialConfigFactory;
import org.ldaptive.ssl.SslConfig;
import org.ldaptive.ssl.TLSSocketFactory;

import org.springframework.core.io.Resource;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFReader;

import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Manages an instance of the in-memory directory server.
 */
public class InMemoryDirectory {

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
    public InMemoryDirectory(@Nonnull final Resource ldif, @Positive final int port, @Nonnull final Resource keystore) throws LDAPException,
            IOException {
        Constraint.isNotNull(ldif, "LDIF resource cannot be null");
        final InMemoryDirectoryServerConfig config =
                new InMemoryDirectoryServerConfig("dc=example,dc=org", "ou=system");
        try {
            final KeyStore ks = KeyStore.getInstance("JKS");
            final String ksPass = "changeit";
            ks.load(keystore.getInputStream(), ksPass.toCharArray());
            final TLSSocketFactory socketFactory = new TLSSocketFactory();
            socketFactory.setSslConfig(new SslConfig(CredentialConfigFactory.createKeyStoreCredentialConfig(ks, ksPass)));
            socketFactory.initialize();
            
            config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", null, port,
                    socketFactory));
        } catch (GeneralSecurityException e) {
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
    }

    /**
     * Stops the directory server.
     */
    public void stop() {
        directoryServer.shutDown(true);
    }
}

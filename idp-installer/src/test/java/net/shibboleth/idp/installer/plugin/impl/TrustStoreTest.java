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

package net.shibboleth.idp.installer.plugin.impl;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.installer.plugin.impl.TrustStore.Signature;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

@SuppressWarnings("javadoc")
public class TrustStoreTest {
    
    private final static String pluginId = "net.shibboleth.plugin.test";
    
    private Path dir;
    
    
    @BeforeClass public void setup() throws IOException {
        dir = Files.createTempDirectory("TrustStoreTest");
        Files.createDirectories(dir.resolve("credentials").resolve(pluginId));
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    @AfterClass public void teardown() throws IOException {
        
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            /** {@inheritDoc} */
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            
            /** {@inheritDoc} */
            public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    @AfterMethod  public void deleteStore() throws IOException {
        Files.deleteIfExists(dir.resolve("credentials").resolve(pluginId).resolve("truststore.asc"));
    }

    @Test public void signatureAbsentTest() throws ComponentInitializationException, IOException {
        final TrustStore ts = new TrustStore();
        ts.setIdpHome(dir);
        ts.setPluginId(pluginId);
        ts.initialize();
        try (InputStream sigStream = TrustStoreTest.class.getResourceAsStream("/net/shibboleth/idp/installer/plugin/shib.ico.asc")) {
            final Signature signature = TrustStore.signatureOf(sigStream);
            assertFalse(ts.contains(signature));
        }
    }
    
    private void populateKeyStore() throws IOException {
        try (final InputStream trustStream = TrustStoreTest.class.getResourceAsStream("/net/shibboleth/idp/installer/plugin/keys.txt");
                final OutputStream outStream = Files.newOutputStream(dir.resolve("credentials").resolve(pluginId).resolve("truststore.asc"))) {
           trustStream.transferTo(outStream);
        }
    }

    @Test public void signaturePresentTest() throws ComponentInitializationException, IOException {
        populateKeyStore();
        final TrustStore ts = new TrustStore();
        ts.setIdpHome(dir);
        ts.setPluginId(pluginId);
        ts.initialize();
        try( final InputStream sigStream = TrustStoreTest.class.getResourceAsStream("/net/shibboleth/idp/installer/plugin/shib.ico.asc")) {
            final Signature signature = TrustStore.signatureOf(sigStream);
            assertTrue(ts.contains(signature));
        }        
    }

    @Test public void signingTest()  throws ComponentInitializationException, IOException {
        populateKeyStore();
        final TrustStore ts = new TrustStore();
        ts.setIdpHome(dir);
        ts.setPluginId(pluginId);
        ts.initialize();
        try( final InputStream sigStream = TrustStoreTest.class.getResourceAsStream("/net/shibboleth/idp/installer/plugin/shib.ico.asc");
               final InputStream badSigStream = TrustStoreTest.class.getResourceAsStream("/net/shibboleth/idp/installer/plugin/shib.ico.asc.bad");
               final InputStream dataStream = TrustStoreTest.class.getResourceAsStream("/net/shibboleth/idp/installer/plugin/shib.ico");
               final InputStream dataStream2 = TrustStoreTest.class.getResourceAsStream("/net/shibboleth/idp/installer/plugin/shib.ico")) {

            Signature badSig = TrustStore.signatureOf(badSigStream);
            assertTrue(ts.contains(badSig));
            assertFalse(ts.checkSignature(dataStream, badSig));
            assertTrue(ts.checkSignature(dataStream2, TrustStore.signatureOf(sigStream)));
         }
    }

    @Test public void loadSave() throws IOException, ComponentInitializationException {
        populateKeyStore();
        final Signature signature;
        try( final InputStream sigStream = TrustStoreTest.class.getResourceAsStream("/net/shibboleth/idp/installer/plugin/shib.ico.asc")) {
            signature = TrustStore.signatureOf(sigStream);
        }
        TrustStore ts = new TrustStore();
        ts.setIdpHome(dir);
        ts.setPluginId(pluginId);
        ts.initialize();
        assertTrue(ts.contains(signature));
        ts.saveStore();

        ts = new TrustStore();
        ts.setIdpHome(dir);
        ts.setPluginId(pluginId);
        ts.initialize();
        assertTrue(ts.contains(signature));

        ts = new TrustStore();
        ts.setIdpHome(dir);
        ts.setTrustStore(dir.resolve("credentials").resolve(pluginId).resolve("truststore.asc").toString());
        ts.setPluginId(pluginId);
        ts.initialize();
    }
}

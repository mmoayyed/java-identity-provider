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

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.security.Security;
import java.util.List;
import java.util.function.Predicate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.plugin.AbstractPluginDescription;
import net.shibboleth.utilities.java.support.plugin.PluginDescription;

@SuppressWarnings("javadoc")
public class PluginInstallerTest {

    private final Logger log = LoggerFactory.getLogger(PluginInstallerTest.class);
    
    private final Predicate<String> loggingAcceptCert = new  Predicate<>() {
        public boolean test(String what) {
            log.debug("Accepting the certificate\n{}", what);
            return true;
        }
    };

    private final Predicate<Pair<URL, Path>> loggingAcceptDownLoad = new  Predicate<>() {
        public boolean test(Pair<URL, Path> what) {
            log.debug("Accepting the download from {} to {}", what.getFirst(), what.getSecond());
            return true;
        }
    };

    @BeforeClass public void setup() throws IOException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test(enabled = false) public void testListing() throws ComponentInitializationException, IOException {
        
        try (final PluginInstaller inst = new PluginInstaller()) {
            inst.setIdpHome(new ClassPathResource("idphome-test").getFile().toPath());
            inst.initialize();
            List<PluginDescription> plugins = inst.getInstalledPlugins();
            assertEquals(plugins.get(0).getPluginId(), "org.example.Plugin");
        }
    }
    
    @Test(enabled = false) public void testUnpackZip() throws ComponentInitializationException, IOException {
        try (final PluginInstaller inst = new PluginInstaller()) {
            inst.setIdpHome(new ClassPathResource("idphome-test").getFile().toPath());
            inst.setAcceptCert(loggingAcceptCert);
            inst.setAcceptDownload(loggingAcceptDownLoad);
            inst.initialize();
            final File f = new File("H:\\Perforce\\Juno\\New\\plugins\\java-idp-plugin-scripting\\rhino-dist\\target");
            inst.installPlugin(f.toPath(),"shibboleth-idp-plugin-rhino-0.0.1-SNAPSHOT.zip");
        }
    }
    
    @Test(enabled = false) public void testUnpackTgz() throws ComponentInitializationException, IOException {
        try (final PluginInstaller inst = new PluginInstaller()) {
            inst.setIdpHome(new ClassPathResource("idphome-test").getFile().toPath());
            inst.setAcceptCert(loggingAcceptCert);
            inst.setAcceptDownload(loggingAcceptDownLoad);
            inst.initialize();
            final File f = new File("H:\\Perforce\\Juno\\New\\plugins\\java-idp-plugin-scripting\\nashorn-dist\\target");
            inst.installPlugin(f.toPath(),"shibboleth-idp-plugin-nashorn-0.0.1-SNAPSHOT.tar.gz");
        }
    }

    @Test(enabled = false) public void testDownload() throws ComponentInitializationException, IOException {
        try (final PluginInstaller inst = new PluginInstaller()) {
            inst.setIdpHome(new ClassPathResource("idphome-test").getFile().toPath());
            inst.setAcceptCert(loggingAcceptCert);
            inst.setAcceptDownload(loggingAcceptDownLoad);
            inst.initialize();
            final URL url = new URL("http://iis.steadingsoftware.net/plugins/");
            inst.installPlugin(url,"shibboleth-idp-plugin-nashorn-0.0.1-SNAPSHOT.tar.gz");
        }
    }

    public static class Wibble extends AbstractPluginDescription {

        /** {@inheritDoc} */
        public String getPluginId() {
            
            return "org.example.Plugin";
        }

        /** {@inheritDoc} */
        public List<URL> getUpdateURLs() throws IOException {
            return null;
        }

        /** {@inheritDoc} */
        public int getMajorVersion() {
            return 0;
        }

        /** {@inheritDoc} */
        public int getMinorVersion() {
            return 0;
        }
        
    }

}

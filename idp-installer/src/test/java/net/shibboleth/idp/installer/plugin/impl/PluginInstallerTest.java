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

import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.security.Security;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.plugin.AbstractIdPPlugin;
import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

@SuppressWarnings("javadoc")
public class PluginInstallerTest extends BasePluginTest {

    private final Logger log = LoggerFactory.getLogger(PluginInstallerTest.class);
    
    private final Predicate<String> loggingAcceptCert = new  Predicate<>() {
        public boolean test(String what) {
            log.debug("Accepting the certificate\n{}", what);
            return true;
        }
    };

    private final Predicate<String> loggingAcceptDownLoad = new  Predicate<>() {
        public boolean test(String what) {
            log.debug("Accepting the download from {} ", what);
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
            inst.setIdpHome(getIdpHome());
            inst.initialize();
            final Map<String, Object> result = inst.getInstalledPlugins().stream().collect(Collectors.toMap(IdPPlugin::getPluginId,
                    e->e));
            
            assertTrue(result.containsKey("org.example.Plugin"));
            assertTrue(result.containsKey("net.shibboleth.plugin.test"));
        }
    }

    @Test(enabled = false, dependsOnMethods ={"testListing", }) public void testRemove() throws ComponentInitializationException, IOException
    {
        try (final PluginInstaller inst = new PluginInstaller()) {
            inst.setIdpHome(getIdpHome());
            inst.setPluginId("org.example.Plugin");
            inst.initialize();
            inst.removeJars();
        }
    }

    @Test(enabled = false) public void testUnpackZip() throws ComponentInitializationException, IOException {
        try (final PluginInstaller inst = new PluginInstaller()) {
            inst.setIdpHome(getIdpHome());
            inst.setAcceptCert(loggingAcceptCert);
            inst.setAcceptDownload(loggingAcceptDownLoad);
            inst.initialize();
            final URL where = new URL("https://build.shibboleth.net/nexus/service/local/repositories/releases/content/net/shibboleth/idp/plugin/scripting/idp-plugin-nashorn-dist/0.1.0/");
            inst.setPluginId("net.shibboleth.idp.plugin.nashorn");
            inst.installPlugin(where,"idp-plugin-nashorn-dist-0.1.0.zip");
        }
    }
    
    @Test(enabled = false) public void testUnpackZipFile() throws ComponentInitializationException, IOException {
        try (final PluginInstaller inst = new PluginInstaller()) {
            inst.setIdpHome(getIdpHome());
            inst.setAcceptCert(loggingAcceptCert);
            inst.setAcceptDownload(loggingAcceptDownLoad);
            inst.initialize();
            final Path dir = Path.of("H:\\Perforce\\Juno\\New\\plugins\\java-idp-plugin-scripting\\rhino-dist\\target");
            inst.installPlugin(dir,"shibboleth-idp-plugin-rhino-0.1.0-SNAPSHOT.zip");
        }
    }

    
    @Test(enabled = false) public void testUnpackTgz() throws ComponentInitializationException, IOException {
        try (final PluginInstaller inst = new PluginInstaller()) {
            inst.setPluginId("net.shibboleth.idp.plugin.rhino");
            inst.setIdpHome(getIdpHome());
            inst.setAcceptCert(loggingAcceptCert);
            inst.setAcceptDownload(loggingAcceptDownLoad);
            inst.initialize();
            final URL where = new URL("https://build.shibboleth.net/nexus/service/local/repositories/releases/content/net/shibboleth/idp/plugin/scripting/idp-plugin-rhino-dist/0.1.0/");
            inst.installPlugin(where,"idp-plugin-rhino-dist-0.1.0.zip");
        }
    }


    public static class Wibble extends AbstractIdPPlugin {

        /** {@inheritDoc} */
        public String getPluginId() {
            
            return "org.example.Plugin";
        }

        /** {@inheritDoc} */
        public List<URL> getUpdateURLs() throws IOException {
            return Collections.emptyList();
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

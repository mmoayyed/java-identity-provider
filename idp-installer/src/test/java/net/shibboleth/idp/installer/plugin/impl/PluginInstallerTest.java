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
import java.security.Security;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.plugin.AbstractPluginDescription;
import net.shibboleth.utilities.java.support.plugin.PluginDescription;

@SuppressWarnings("javadoc")
public class PluginInstallerTest {

    @BeforeClass public void setup() throws IOException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test public void testListing() throws ComponentInitializationException, IOException {
        
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
            inst.initialize();
            final File f = new File("H:\\Perforce\\Juno\\New\\plugins\\java-idp-plugin-scripting\\nashorn-dist\\target");
            inst.installPlugin(f.toPath(),"shibboleth-idp-plugin-nashorn-0.0.1-SNAPSHOT.zip");
        }
    }
    
    @Test(enabled = false) public void testUnpackTgz() throws ComponentInitializationException, IOException {
        try (final PluginInstaller inst = new PluginInstaller()) {
            inst.setIdpHome(new ClassPathResource("idphome-test").getFile().toPath());
            inst.setAcceptCert(Predicates.alwaysTrue());
            inst.initialize();
            final File f = new File("H:\\Perforce\\Juno\\New\\plugins\\java-idp-plugin-scripting\\nashorn-dist\\target");
            inst.installPlugin(f.toPath(),"shibboleth-idp-plugin-nashorn-0.0.1-SNAPSHOT.tar.gz");
        }
    }

    @Test(enabled = false) public void testDownload() throws ComponentInitializationException, IOException {
        try (final PluginInstaller inst = new PluginInstaller()) {
            inst.setIdpHome(new ClassPathResource("idphome-test").getFile().toPath());
            inst.setAcceptCert(Predicates.alwaysTrue());
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

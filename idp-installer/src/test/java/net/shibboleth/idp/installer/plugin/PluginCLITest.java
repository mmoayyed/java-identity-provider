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

package net.shibboleth.idp.installer.plugin;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.http.client.HttpClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import net.shibboleth.ext.spring.cli.AbstractCommandLine;
import net.shibboleth.ext.spring.resource.HTTPResource;
import net.shibboleth.idp.installer.plugin.impl.PluginInstaller;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;

@SuppressWarnings("javadoc")
public class PluginCLITest extends BasePluginTest {
    
    private final String RHINO_DISTRO = "https://build.shibboleth.net/nexus/service/local/repositories/releases/content/net/shibboleth/idp/plugin/scripting/idp-plugin-rhino-dist/0.1.0/idp-plugin-rhino-dist-0.1.0.tar.gz";

    private File plugin;

    @BeforeSuite public void setUp() throws IOException
    {
        System.setProperty("net.shibboleth.idp.cli.idp.home",getIdpHome().toString());
        final Resource pluginInstaller = new ClassPathResource("conf/admin/plugin-installer.xml");
        plugin = getIdpHome().resolve("conf").resolve("admin").resolve("plugin-installer.xml").toFile();
        plugin.createNewFile();
        
        try (final InputStream is = pluginInstaller.getInputStream();
             final BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(plugin))) {
            is.transferTo(os);
        }
    }

    @Test(enabled = true) public void testList() throws IOException {
        assertEquals(PluginInstallerCLI.runMain(new String[] { plugin.getAbsolutePath(), "-fl"}),
                AbstractCommandLine.RC_OK);
    }

    @Test(enabled = true) public void testWrong() {
        assertEquals(PluginInstallerCLI.runMain(new String[] { plugin.getAbsolutePath(), "-i", "a"}),
                AbstractCommandLine.RC_INIT);
    }

    @Test(enabled = false, dependsOnMethods = {"testRhinoLocal"}) public void testRhinoWeb() {
            assertEquals(PluginInstallerCLI.runMain(new String[] { plugin.getAbsolutePath(),
                    "-i", RHINO_DISTRO,
                    "-p", "net.shibboleth.idp.plugin.rhino"}),
                    AbstractCommandLine.RC_OK);
    }

    @Test(enabled = false) public void testRhinoLocal() {
        Path unpack = null;
        try {
            unpack = Files.createTempDirectory("rhinoLocal");
            final HttpClient client = new HttpClientBuilder().buildClient();
            Resource from = new HTTPResource(client, RHINO_DISTRO);
            try (final InputStream in = from.getInputStream(); 
                 final OutputStream out = new BufferedOutputStream(new FileOutputStream(unpack.resolve("rhino.tar.gz").toFile()))) {
                    in.transferTo(out);
            }
            from = new HTTPResource(client, RHINO_DISTRO + ".asc");
            try (final InputStream in = from.getInputStream(); 
                    final OutputStream out = new BufferedOutputStream(new FileOutputStream(unpack.resolve("rhino.tar.gz.asc").toFile()))) {
                       in.transferTo(out);
               }
            
            assertEquals(PluginInstallerCLI.runMain(new String[] { plugin.getAbsolutePath(),
                    "-p", "net.shibboleth.idp.plugin.rhino",
                    "-i", unpack.resolve("rhino.tar.gz").toString()}),
                    AbstractCommandLine.RC_IO);
            //
            // Populate the new key store
            //
            final Path trustStorePath = getIdpHome().
                    resolve("credentials").
                    resolve("net.shibboleth.idp.plugin.rhino").
                    resolve("truststore.asc");
            from = new ClassPathResource("credentials/truststore.asc");
            try (final InputStream in = from.getInputStream(); 
                    final OutputStream out = new BufferedOutputStream(new FileOutputStream(trustStorePath.toFile(), true))) {
                in.transferTo(out);
            }
            //
            // try again
            //
            assertEquals(PluginInstallerCLI.runMain(new String[] { plugin.getAbsolutePath(), 
                    "-i", unpack.resolve("rhino.tar.gz").toString(),
                    "-p", "net.shibboleth.idp.plugin.rhino"}),
                    AbstractCommandLine.RC_OK);
        } catch (Exception e) {
            fail("Failed" + e);
        } finally {
            if (unpack != null) {
                PluginInstaller.deleteTree(unpack);
            }
        }
    }
}

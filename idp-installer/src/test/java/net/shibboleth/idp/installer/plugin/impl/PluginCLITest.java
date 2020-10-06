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
import net.shibboleth.idp.installer.ProgressReportingOutputStream;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;

@SuppressWarnings("javadoc")
public class PluginCLITest extends BasePluginTest {
    
    private final String RHINO_DISTRO = "https://build.shibboleth.net/nexus/service/local/repositories/releases/content/net/shibboleth/idp/plugin/scripting/idp-plugin-rhino-dist/0.1.3/idp-plugin-rhino-dist-0.1.3.tar.gz";

    @BeforeSuite public void setUp() throws IOException
    {
        System.setProperty("idp.home",getIdpHome().toString());
    }

    @Test(enabled = true) public void testLicense() {
        assertEquals(PluginInstallerCLI.runMain(new String[] { "--license", "net.shibboleth.plugin.test"} ), AbstractCommandLine.RC_OK);
    }

    @Test(enabled = false) public void testList() throws IOException {
        assertEquals(PluginInstallerCLI.runMain(new String[] { "-fl", } ), AbstractCommandLine.RC_OK);
    }

    @Test(enabled = false) public void testWrong() {
        assertEquals(PluginInstallerCLI.runMain(new String[] { "-i", "a"}), AbstractCommandLine.RC_INIT);
    }

    @Test(enabled = false, dependsOnMethods = {"testRhinoLocal"}) public void testRhinoWeb() {
            assertEquals(PluginInstallerCLI.runMain(new String[] { 
                    "-i", RHINO_DISTRO,
                    "-p", "net.shibboleth.idp.plugin.rhino"}),
                    AbstractCommandLine.RC_OK);
    }

    @Test(enabled = false, dependsOnMethods = {"testRhinoWeb"})  public void testUpdate() {
        assertEquals(PluginInstallerCLI.runMain(new String[] {
                "-u", "net.shibboleth.idp.plugin.rhino"}),
                AbstractCommandLine.RC_OK);
    }

    @Test(enabled = false, dependsOnMethods = {"testUpdate"})  public void testForceUpdate() {
        assertEquals(PluginInstallerCLI.runMain(new String[] {
                "-u", "net.shibboleth.idp.plugin.rhino",
                "-fu", "0.1.3" }),
                AbstractCommandLine.RC_OK);
    }

    @Test(enabled = false) public void testRhinoLocal() throws Exception {
        Path unpack = null;
        try {
            unpack = Files.createTempDirectory("rhinoLocal");
            final HttpClient client = new HttpClientBuilder().buildClient();
            Resource from = new HTTPResource(client, RHINO_DISTRO);
            try (final InputStream in = from.getInputStream(); 
                 final OutputStream out = new ProgressReportingOutputStream(new FileOutputStream(unpack.resolve("rhino.tar.gz").toFile()))) {
                    in.transferTo(out);
            }
            from = new HTTPResource(client, RHINO_DISTRO + ".asc");
            try (final InputStream in = from.getInputStream(); 
                    final OutputStream out = new ProgressReportingOutputStream(new FileOutputStream(unpack.resolve("rhino.tar.gz.asc").toFile()))) {
                       in.transferTo(out);
               }

            final Path credentials = getIdpHome().resolve("credentials").resolve("net.shibboleth.idp.plugin.rhino");
            Files.createDirectories(credentials);
            //
            // Populate the new key store
            //
            final Path trustStorePath = credentials.resolve("truststore.asc");
            from = new ClassPathResource("credentials/truststore.asc");
            try (final InputStream in = from.getInputStream(); 
                    final OutputStream out = new ProgressReportingOutputStream(new FileOutputStream(trustStorePath.toFile(), true))) {
                in.transferTo(out);
            }
            //
            // try again
            //
            assertEquals(PluginInstallerCLI.runMain(new String[] { 
                    "-i", unpack.resolve("rhino.tar.gz").toString(),
                    "-p", "net.shibboleth.idp.plugin.rhino"}),
                    AbstractCommandLine.RC_OK);
        } finally {
            if (unpack != null) {
                PluginInstallerSupport.deleteTree(unpack);
            }
        }
    }
}

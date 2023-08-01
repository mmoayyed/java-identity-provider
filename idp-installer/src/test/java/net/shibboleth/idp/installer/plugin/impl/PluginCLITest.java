/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.hc.client5.http.classic.HttpClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.idp.installer.ProgressReportingOutputStream;
import net.shibboleth.shared.cli.AbstractCommandLine;
import net.shibboleth.shared.httpclient.HttpClientBuilder;
import net.shibboleth.shared.spring.httpclient.resource.HTTPResource;

@SuppressWarnings("javadoc")
public class PluginCLITest extends BasePluginTest {
    
    private final static boolean RunInstallTests = false; 
    
    private final String PLUGIN_DISTRO = "http://test.shibboleth.net/downloads/identity-provider/plugins/metadatagen/1.0.0/idp-plugin-metadatagen-dist-1.0.0.tar.gz";
    
    private final String PLUGIN_ID = "net.shibboleth.idp.plugin.metadatagen";

    @BeforeSuite public void setUp() throws IOException
    {
        System.setProperty("idp.home",getIdpHome().toString());
        final Path credentials = getIdpHome().resolve("credentials").resolve(PLUGIN_ID);
        Files.createDirectories(credentials);
        //
        // Populate the new key store
        //
        final Path trustStorePath = credentials.resolve("truststore.asc");
        final Resource from = new ClassPathResource("credentials/truststore.asc");
        try (final InputStream in = from.getInputStream();
             final OutputStream out = new ProgressReportingOutputStream(new FileOutputStream(trustStorePath.toFile(), true))) {
            in.transferTo(out);
        }
    }

    @Test(enabled = true) public void testLicense() {
        assertEquals(PluginInstallerCLI.runMain(new String[] { "--license", "net.shibboleth.plugin.test"} ), AbstractCommandLine.RC_OK);
    }

    @Test(enabled = true) public void testList() throws IOException {
        assertEquals(PluginInstallerCLI.runMain(new String[] { "-fl", } ), AbstractCommandLine.RC_OK);
    }

    @Test(enabled = true) public void testListAvailable() throws IOException {
        assertEquals(PluginInstallerCLI.runMain(new String[] { "-L", } ), AbstractCommandLine.RC_OK);
    }

    @Test(enabled = RunInstallTests) public void testInstallById() throws IOException {
        assertEquals(PluginInstallerCLI.runMain(new String[] { "-I", PLUGIN_ID} ), AbstractCommandLine.RC_OK);
    }

    @Test(enabled = true,dependsOnMethods = {/*"testWeb"*/}) public void testListWithOverride() throws IOException {
        ClassPathResource resource = new ClassPathResource("/net/shibboleth/idp/plugin/allPlugins.props");
        String url = resource.getURL().toString();
        final String parms[]=  { "-fl", "--updateURL", url};
        final int rc = PluginInstallerCLI.runMain(parms);
        assertEquals(rc, AbstractCommandLine.RC_OK);
    }

    @Test(enabled = true) public void testWrong() {
        assertEquals(PluginInstallerCLI.runMain(new String[] { "-i", "a"}), AbstractCommandLine.RC_INIT);
    }

    @Test(enabled = RunInstallTests) public void testWeb() {
            assertEquals(PluginInstallerCLI.runMain(new String[] { 
                    "-i", PLUGIN_DISTRO,
                    "--noCheck",
                    "--noRebuild"
                    }),
                    AbstractCommandLine.RC_OK);
    }

    @Test(enabled = RunInstallTests, dependsOnMethods = {"testWeb"})
    public void testUpdate() {
        assertEquals(PluginInstallerCLI.runMain(new String[] {
                "-u", PLUGIN_ID,
                "--noCheck",
                "--noRebuild"
                }),
                AbstractCommandLine.RC_OK);
    }

    @Test(enabled = RunInstallTests, dependsOnMethods = {"testUpdate"})
    public void testForceUpdate() {
        assertEquals(PluginInstallerCLI.runMain(new String[] {
                "-u", PLUGIN_ID,
                "-fu", "0.0.2" }),
                AbstractCommandLine.RC_OK);
    }

    @Test(enabled = RunInstallTests, dependsOnMethods = {"testForceUpdate"})
    public void testListContents() {
        assertEquals(PluginInstallerCLI.runMain(new String[] {
                "-cl", PLUGIN_ID,
                }),
                AbstractCommandLine.RC_OK);
    }

    @Test(enabled = RunInstallTests, dependsOnMethods = {"testListContents"}, ignoreMissingDependencies = true)
    public void testUninstall() {
        assertEquals(PluginInstallerCLI.runMain(new String[] {
                "-r", PLUGIN_ID,
                "--noRebuild"
                }),
                AbstractCommandLine.RC_OK);
    }

    @Test(enabled = RunInstallTests) public void testLocal() throws Exception {
        Path unpack = null;
        try {
            Resource from;
            unpack = Files.createTempDirectory("unpackLocal");
            final HttpClient client = new HttpClientBuilder().buildClient();
            from = new HTTPResource(client, PLUGIN_DISTRO);
            try (final InputStream in = from.getInputStream(); 
                 final OutputStream out = new ProgressReportingOutputStream(new FileOutputStream(unpack.resolve("local.tar.gz").toFile()))) {
                    in.transferTo(out);
            }
            from = new HTTPResource(client, PLUGIN_DISTRO + ".asc");
            try (final InputStream in = from.getInputStream(); 
                    final OutputStream out = new ProgressReportingOutputStream(new FileOutputStream(unpack.resolve("local.tar.gz.asc").toFile()))) {
                       in.transferTo(out);
               }

            assertEquals(PluginInstallerCLI.runMain(new String[] {
                    "-i", unpack.resolve("local.tar.gz").toString(),
                    "--noCheck",
                    }),
                    AbstractCommandLine.RC_OK);
        } finally {
            if (unpack != null) {
                InstallerSupport.deleteTree(unpack);
            }
        }
    }
}

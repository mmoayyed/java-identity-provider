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

package net.shibboleth.idp.dependencies;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.AfterClass;
import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.dependencies.ParsedPom.PomArtifact;
import net.shibboleth.idp.installer.plugin.impl.PluginInstallerSupport;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.XMLParserException;

/**
 * Test that what we see is what we wanted in abuild - we do this by reading the pom
 */
public class DependencyTest extends OpenSAMLInitBaseTestCase {

    /** Set this up if you want to run the tests from eclipse. */
    private static String LOCAL_MAVEN_HOME = null;
    
    /** Parse for us to use. */
    private ParserPool parserPool;
    
    /** If this is set to false we do not do any work which requires maven (which is everything). */
    private boolean mavenAvailable;
    
    /** Work space.  Deleted on exit. */
    private Path workingDir;
    
    /** The project parent dependencies */
    private List<PomArtifact> dependencies = new ArrayList<>();
    
    /** The IDP Version/artifact info */
    private PomArtifact idpArtefact;
    
    private PrintWriter report;

    
    /**  We have as an assumption that the CWD is idp-installer.  Test this.
     * @throws IOException
     */
    @BeforeClass public void testWorkingDir() throws IOException {
        final Path path = Path.of(".");
        final String myPath = path.toFile().getCanonicalPath();
        final String indirectPath = path.resolve("..").resolve("idp-installer").toFile().getCanonicalPath();
        
        assertTrue(path.resolve("..").resolve("idp-war").toFile().exists());
        assertEquals(myPath, indirectPath);
    }
    
    /** Set up maven.
     * This relies on a couple of dodgy tests when running from maven from the command line and
     * on the user setting up {@link #LOCAL_MAVEN_HOME} when running from eclipse.
     */
    @BeforeClass public void setupMavenEnvironment() {
        if (System.getProperty("maven.home") != null) {
            mavenAvailable = true;
            return;
        }
        String home = System.getenv("MAVEN_HOME");
        if (home == null) {
            home = System.getenv("_");
        }
        if (home == null) {
            home = LOCAL_MAVEN_HOME;
        }

        if (home != null) {
            System.setProperty("maven.home", home);
            mavenAvailable = true;
        }
    }
    
    /** Set up the environment for running the test.
     * Parse the IdP parent pom.
     * From that find the description of parent pom & download it.
     * Parse that.
     * @throws IOException 
     * @throws XMLParserException
     * @throws MavenInvocationException 
     */

    @BeforeClass(dependsOnMethods = {"setupMavenEnvironment", "testWorkingDir"}) public void setup() throws IOException, XMLParserException, MavenInvocationException {
        if (!mavenAvailable) {
            return;
        }
        workingDir = Files.createTempDirectory("dependencyTest");
        parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        final ParsedPom idpParent = new ParsedPom(parserPool, Path.of("../idp-parent/pom.xml"), true);
        idpArtefact = idpParent.getOurInfo();
        assertNotNull(idpParent.getParent());
        final Path parentPath = downloadPom(idpParent.getParent());
        final ParsedPom projectParent = new ParsedPom(parserPool, parentPath, false);
        dependencies.addAll(projectParent.getCompileDependencies());
        for (final PomArtifact bom : projectParent.getBomDependencies()) {
            final Path bomPath = downloadPom(bom);
            final ParsedPom bomContents = new ParsedPom(parserPool, bomPath, false);
            dependencies.addAll(bomContents.getCompileDependencies());
        }
        final File out = new File("target/dependencyReport.txt");
        final FileOutputStream outStream = new FileOutputStream(out);
        report = new PrintWriter(new BufferedOutputStream(outStream));
    }
    
    /** Clean up after ourselves. */
    @AfterClass public void teardown() {
        PluginInstallerSupport.deleteTree(workingDir);
    }
    
    /** The guts of the first test.  Are all the files what we expected?
     * @throws IOException
     */
    @Test public void testDependencies() throws IOException {
        if (!mavenAvailable) {
            return;
        }
        final Path lib = Path.of("../idp-war/target/idp-war-"+ idpArtefact.getVersion()).resolve("WEB-INF").resolve("lib");
        assertTrue(Files.exists(lib), "idp-war must have been built");
        final Map<String, String> names = new HashMap<>();
        int wrongVersion = 0;
        int found = 0;
        int nonUsed = 0;
        int dupNames = Files.list(lib).mapToInt(e -> addName(names, lib.relativize(e).toString())).sum();
        report.format("WAR CONTENTS\n=== ========\n");
        Collections.sort(dependencies);
        for (PomArtifact artifact : dependencies) {
            final String id = artifact.getArtifactId();
            final String ver = artifact.getVersion();
            final String version = names.get(id);
            if (version == null) {
                report.format("%-22s\t: %-12s\tNOT found in war\n", id, ver);
                nonUsed++;
            } else if (version.equals(ver)) {
                report.format("%-22s\t: %-12s\tFound in war\n", id, ver);
                found++;
            } else {
                report.format("%-22s\t: %-12s\tVERSION MISMATCH - found %s\n", id, ver, version);
                wrongVersion++;
            }
        }
        if (dupNames != 0) {
            report.format("\n%d Duplicate names\n", dupNames); 
        }
        
        report.format("\n%d dependencies, %d found, %d not found %d mismatched", dependencies.size(), found, nonUsed, wrongVersion);
        report.flush();
        report.close();
        assertEquals(wrongVersion,  0, "Mismatched version");
        assertEquals(dupNames,  0, "Multiple similarly named jars");
    }
    
    /** Trivial accumulator to pull a name in the lib directory apart and insert it into the map.
     * @param names The map to accumulate into
     * @param jarPath the file we are looking at.
     * @return 1 if here was a previous entry.
     */
    private int addName(Map<String, String> names, String jarPath) {
        final int last = jarPath.lastIndexOf("-");
        final String base = jarPath.substring(0, last);
        String versionExtension = jarPath.substring(last+1);
        final String oldVersion; 
        if (versionExtension.endsWith(".jar")) {
            versionExtension = versionExtension.substring(0, versionExtension.length()-4);
        }
        oldVersion = names.put(base, versionExtension);
        if (oldVersion == null) {
            return 0;
        }
        report.format("%-22s\t: Duplicate version %-12s\t& %s\n", base, versionExtension, oldVersion);
        return 1;
    }

    /** tell maven to download the artifact and returns it's path.
     * @param artifact what to look for
     * @return the pom as a {@link Path}
     * @throws MavenInvocationException 
     */
    private Path downloadPom(final PomArtifact artifact) throws MavenInvocationException {
        final String fullArtifactName = new StringBuilder(artifact.getGroupId())
                    .append(':')
                    .append(artifact.getArtifactId())
                    .append(':')
                    .append(artifact.getVersion())
                    .append(":pom")
                    .toString();
        
        final Properties props = new Properties(2);
        props.setProperty("artifact",fullArtifactName);
        props.setProperty("outputDirectory", workingDir.toString());
       
        InvocationRequest request = new DefaultInvocationRequest().setProperties(props).setGoals( Arrays.asList( "dependency:copy" ) );

        Invoker invoker = new DefaultInvoker();
        invoker.execute( request );
        
        return workingDir.resolve(artifact.getArtifactId() + "-" + artifact.getVersion() + ".pom");
    }
}

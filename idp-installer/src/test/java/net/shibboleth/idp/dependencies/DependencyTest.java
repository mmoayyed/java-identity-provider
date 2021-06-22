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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.dependencies.ParsedPom.PomArtifact;
import net.shibboleth.idp.installer.plugin.impl.PluginInstallerSupport;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.XMLParserException;

/**
 * Test that what we see is what we wanted in abuild - we do this by reading the pom
 */
public class DependencyTest extends OpenSAMLInitBaseTestCase {

    /** Set this up if you want to run the tests from eclipse. */
    private static String LOCAL_MAVEN_HOME = null;

    /** A list of things which get added to real versions. */
    private static final List<String> extensionGarnish = List.of("-SNAPSHOT", "-GA", "-jre", "-empty-to-avoid-conflict-with-guava");

    /** Parse for us to use. */
    private ParserPool parserPool;
    
    /** Work space.  Deleted on exit. */
    private Path workingDir;
    
    /** The project parent dependencies */
    private List<PomArtifact> dependencies = new ArrayList<>();
    
    /** The IDP Version/artifact info */
    private PomArtifact idpArtefact;
    
    private PrintWriter report;

    private PomArtifact parentArtefact;
    
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
            return;
        }
        String home = LOCAL_MAVEN_HOME;
        if (home == null) {
            home = System.getenv("MAVEN_HOME");
        }
        if (home == null) {
            home = System.getenv("_");
        }
        if (home == null) {
            throw new SkipException("Maven Not Located");
        }

        System.setProperty("maven.home", home);
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
        workingDir = Files.createTempDirectory("dependencyTest");
        parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        ParsedPom idpParent = new ParsedPom(parserPool, Path.of("../idp-parent/pom.xml"), "idp-parent/pom.xml", null, Collections.emptyList());
        idpArtefact = idpParent.getOurInfo();
        parentArtefact = idpParent.getParent(); 
        assertNotNull(parentArtefact);
        final Path parentPath = downloadPom(parentArtefact);
        final Set<PomArtifact> allManagedDeps = new HashSet<>();
        final ParsedPom projectParent = new ParsedPom(parserPool, parentPath, "parent/pom.xml", new Properties(), Collections.emptyList());
        allManagedDeps.addAll(projectParent.getManagedDependencies());
        idpParent = new ParsedPom(parserPool, Path.of("../idp-parent/pom.xml"), "idp-parent/pom.xml", projectParent.getProperties(), projectParent.getCompileDependencies());
        allManagedDeps.addAll(idpParent.getManagedDependencies());
        dependencies.addAll(projectParent.getCompileDependencies());
        dependencies.addAll(idpParent.getCompileDependencies());
        for (final PomArtifact bom : projectParent.getBomDependencies()) {
            final Path bomPath = downloadPom(bom);
            final ParsedPom bomContents = new ParsedPom(parserPool, bomPath, bom.getArtifactId()+".pom", projectParent.getProperties(), projectParent.getCompileDependencies());
            dependencies.addAll(bomContents.getCompileDependencies());
            allManagedDeps.addAll(bomContents.getManagedDependencies());
        }
        for (final PomArtifact bom : idpParent.getBomDependencies()) {
            final Path bomPath = downloadPom(bom);
            final ParsedPom bomContents = new ParsedPom(parserPool, bomPath, bom.getArtifactId()+".pom", projectParent.getProperties(), projectParent.getCompileDependencies());
            dependencies.addAll(bomContents.getCompileDependencies());
            allManagedDeps.addAll(bomContents.getManagedDependencies());
        }
        final ParsedPom warDist = new ParsedPom(parserPool, Path.of("../idp-war-distribution/pom.xml"), "idp-war-distribution/pom.xml", projectParent.getProperties(), allManagedDeps);
        dependencies.addAll(warDist.getRuntimeDependencies());
        final File out = new File("target/dependencyReport.txt");
        final FileOutputStream outStream = new FileOutputStream(out);
        report = new PrintWriter(new BufferedOutputStream(outStream));
        report.format("Dependency Analysis, started at %s\n", Instant.now().toString());
    }
    
    /** Clean up after ourselves. */
    @AfterClass public void teardown() {
        PluginInstallerSupport.deleteTree(workingDir);
    }
    
    /** The guts of the first test.  Are all the files what we expected?
     * @throws IOException if the file doesn't exist
     * @throws MavenInvocationException if we fail to download a pom or a dependency
     */
    @Test(enabled=false) public void testDependencies() throws IOException, MavenInvocationException {
        final Path lib = Path.of("../idp-war-distribution/target/idp-war-distribution-"+ idpArtefact.getVersion()).resolve("WEB-INF").resolve("lib");
        assertTrue(Files.exists(lib), "idp-war must have been built");
        final Map<String, String> names = new HashMap<>();
        int wrongVersion = 0;
        int found = 0;
        int nonUsed = 0;
        int dupNames = 0;
        final int similarNames = Files.list(lib).mapToInt(e -> addName(names, lib.relativize(e).toString())).sum();
        report.format("WAR CONTENTS\n=== ========\n");
        Collections.sort(dependencies);
        // ArtifactId->(Ver->[source, source])
        final Map<String, Map<String, Set<String>>> dependencySource = new HashMap<>();
        PomArtifact last = null;
        for (PomArtifact artifact : dependencies) {
            final String id = artifact.getArtifactId();
            final String ver = artifact.getVersion();
            final String sourcePomFilename = "(from " + artifact.getSourcePomFilename() + ")";
            final String version = names.remove(id);
            if (artifact.equals(last)) {
                report.format("%-22s\t: %-12s\tDuplicate artifact: %-22s\n", id, ver, sourcePomFilename);
                dupNames++;
            } else if (version == null) {
                report.format("%-22s\t: %-12s\tNot found in war    %-22s\n", id, ver, sourcePomFilename);
                nonUsed++;
            } else if (version.equals(ver)) {
                report.format("%-22s\t: %-12s\tFound in war        %-22s\n", id, ver, sourcePomFilename);
                found++;
                analyzeChild(dependencySource, artifact);
            } else {
                report.format("%-22s\t: %-12s\tVersion Mismatch- found %s %s\n", id, ver, version, sourcePomFilename);
                analyzeChild(dependencySource, artifact.withVersion(version));
                if (!ver.equals(PomArtifact.BAD_VERSION)) {
                    wrongVersion++;
                }
            }
            last = artifact;
        }
        if (dupNames != 0) {
            report.format("\n%d Duplicate names\n", dupNames); 
        }
        if (similarNames != 0) {
            report.format("\n%d Artifacts with multiple versions\n", similarNames);
        }

        report.format("\n%d dependencies, %d found, %d not found, %d mismatched\n\nDependency Sources\n", dependencies.size(), found, nonUsed, wrongVersion);

        final List<String> contributedDeps = new ArrayList<>(names.keySet());
        Collections.sort(contributedDeps);
        int noSource = 0, verMismatch = 0;

        for (final String dependency: contributedDeps) {
            final Map<String, Set<String>> map = dependencySource.get(dependency);
            final String version = names.get(dependency);
            if (map == null) {
                if (!dependency.startsWith("idp-")) {
                    report.format("%-22s\t: %-12s\tNo source artefact found\n", dependency, version);
                    noSource++;
                }
            } else {
                final Set<String> sources = map.remove(version);
                if (sources == null) {
                    report.format("%-22s\t: %-12s\tNO Dependency contributes this version\n", dependency, version);
                    verMismatch ++;
                } else {
                    reportContributions(dependency, version, sources);
                }
                final List<String> versions = new ArrayList<>(map.keySet());
                Collections.sort(versions);
                for (final String ver:versions) {
                    reportContributions(dependency, ver, map.get(ver));
                }
            }
        }
        report.format("%d Version mismatches, %d lost artifacts\n", verMismatch, noSource);
        report.format("Completed at %s\n", Instant.now().toString());
        report.flush();
        report.close();
        assertEquals(wrongVersion,  0, "Mismatched version");
        assertEquals(similarNames,  0, "Multiple similarly named jars");
    }
    
    /** report the contributions of the provided dependency & version.
     * @param dependency
     * @param version
     * @param sources
     */
    private void reportContributions(final String dependency, final String version, final Collection<String> sources) {
        List<String> srcs = new ArrayList<>(sources);
        Collections.sort(srcs);
        report.format("%-22s\t: %-12s\tContributed by ", dependency, version);
        for (int i = 0; i < (srcs.size()-1); i++) {
            report.format("%s,", srcs.get(i));
            if ((i&3)==3) {
                report.format("\n                                      \t");
            }
        }
        report.format("%s\n", srcs.get(srcs.size()-1));
    }

    /** Given an artifact do an "mvn dependency:copy-dependencies" on it.
     * Then analyse the output file into the map.  The dependency name
     * yields a map.  Looking this up with a version yields a set of the sources.
     * @param dependencySource where to accumulate the results
     * @param artifact what to start with.
     * @throws MavenInvocationException  if maven fails.
     * @throws IOException if a file doesbn't exist.
     */
    private void analyzeChild(final Map<String, Map<String, Set<String>>> dependencySource,
            final PomArtifact artifact) throws MavenInvocationException, IOException {
        final File pomFile = outputPom(artifact);
        final String artifactName = artifact.getArtifactId()+"-"+artifact.getVersion();
        final Path outputDir = workingDir.resolve(artifactName);

        final Properties props = new Properties(2);
        props.setProperty("includeScope","runtime");
        props.setProperty("outputDirectory", outputDir.toString());
        InvocationRequest request = new DefaultInvocationRequest().setProperties(props).setPomFile(pomFile).setGoals( Arrays.asList( "dependency:copy-dependencies" ) );

        Invoker invoker = new DefaultInvoker();
        invoker.execute( request );
        if (Files.exists(outputDir)) {
            Files.list(outputDir).forEach(e -> addDep(dependencySource, outputDir.relativize(e).toString(), artifact));
        }
    }

    /** Add the artifact as a source of this file.
     * @param dependencySources where to accumulate the answers 
     * @param dep the file name of the dependency which was down-loaded
     * @param artifact the artifact which provoked the download
     */
    private void addDep(final Map<String, Map<String, Set<String>>> dependencySources,
            final String dep,
            final PomArtifact artifact) {
        final Pair<String,String> depId = splitFileName(dep);
        
        if (artifact.getArtifactId().equals(depId.getFirst()) && artifact.getVersion().equals(depId.getSecond())) {
            // it's us.  Not interesting
            return;
        }
        
        // for each version, what contributed this dependency
        Map<String, Set<String>> depEntry = dependencySources.get(depId.getFirst());
        if (depEntry == null) {
            depEntry = new HashMap<>();
            dependencySources.put(depId.getFirst(), depEntry);
        }
        Set<String> provider = depEntry.get(depId.getSecond());
        if (provider == null) {
            provider = new HashSet<>();
            depEntry.put(depId.getSecond(), provider);
        }
        provider.add(artifact.getArtifactId()+"-"+artifact.getVersion());
    }

    /** Create a pom file which has one dependency - this artifact.
     * @param artifact the artifact.
     * @return the file.
     * @throws FileNotFoundException 
     */
    private File outputPom(PomArtifact artifact) throws FileNotFoundException {
        final File file = workingDir.resolve(new StringBuilder(artifact.getArtifactId())
                .append("-")
                .append(artifact.getVersion())
                .append(".xml").
                toString()).toFile();
        final FileOutputStream outStream = new FileOutputStream(file);
        final PrintWriter pom = new PrintWriter(new BufferedOutputStream(outStream));
        pom.format("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "     xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "\n"
                + "    <parent>\n"
                + "        <groupId>%s</groupId>\n"
                + "        <artifactId>%s</artifactId>\n"
                + "        <version>%s</version>\n"
                + "    </parent>\n"
                + "\n", parentArtefact.getGroupId(), parentArtefact.getArtifactId(), parentArtefact.getVersion());
        pom.format("    <groupId>shibboleth.net.dependency</groupId>\n"
                + "    <version>0.0.1</version>\n"
                + "    <name>Shibboleth Dependency</name>\n"
                + "    <artifactId>idp-dep-%s</artifactId>\n"
                + "    <packaging>jar</packaging>\n\n", artifact.getArtifactId());
        pom.format("    <dependencies>\n"
                + "    <dependency>\n"
                + "            <groupId>%s</groupId><artifactId>%s</artifactId><version>%s</version>\n"
                + "    </dependency>\n"
                + "    </dependencies>\n\n", artifact.getGroupId(),artifact.getArtifactId(), artifact.getVersion());
        pom.format("    <repositories>\n"
                + "        <repository>\n"
                + "            <id>shib-release</id>\n"
                + "            <url>https://build.shibboleth.net/nexus/content/groups/public</url>\n"
                + "            <snapshots>\n"
                + "                <enabled>false</enabled>\n"
                + "            </snapshots>\n"
                + "        </repository>\n"
                + "        <repository>\n"
                + "            <id>shib-snapshot</id>\n"
                + "            <url>https://build.shibboleth.net/nexus/content/repositories/snapshots</url>\n"
                + "            <releases>\n"
                + "                <enabled>false</enabled>\n"
                + "            </releases>\n"
                + "        </repository>\n"
                + "    </repositories>\n"
                + "</project>\n");
        pom.flush();
        pom.close();
        return file;
    }

    /** Split the file name into the artifact (first) and version (second).
     * @param inName the file name
     * @return a pair.
     */
    private Pair<String, String> splitFileName(final String inName) {
        final String name;
        if (inName.endsWith(".jar")) {
            name = inName.substring(0, inName.length()-4);
        } else {
            name = inName;
        }
        int last = name.lastIndexOf("-");
        for (String otherGarnish : extensionGarnish) {
            if (name.endsWith(otherGarnish)) {
                last = name.substring(0, name.length()-otherGarnish.length()).lastIndexOf("-");
                break;
            }
        }
        final String base = name.substring(0, last);
        String versionExtension = name.substring(last+1);
        return new Pair<>(base, versionExtension);
    }

    /** Trivial accumulator to pull a name in the lib directory apart and insert it into the map.
     * @param names The map to accumulate into
     * @param jarPath the file we are looking at.
     * @return 1 if there as a artifact with the same name.
     */
    private int addName(Map<String, String> names, String jarPath) {
        final Pair<String, String> nm = splitFileName(jarPath);
        final String oldName = names.put(nm.getFirst(), nm.getSecond());
        if (oldName == null) {
            return 0;
        }
        return 1;
    }

    /** tell maven to download the artifact and returns it's path.
     * @param artifact what to look for
     * @return the pom as a {@link Path}
     * @throws MavenInvocationException 
     */
    private Path downloadPom(final PomArtifact artifact) throws MavenInvocationException {
        final Path output =  workingDir.resolve(artifact.getArtifactId() + ".pom");
        assertFalse(Files.exists(output));
        final String fullArtifactName = new StringBuilder(artifact.getGroupId())
                    .append(':')
                    .append(artifact.getArtifactId())
                    .append(':')
                    .append(artifact.getVersion())
                    .append(":pom")
                    .toString();
        
        final Properties props = new Properties(3);
        props.setProperty("artifact",fullArtifactName);
        props.setProperty("mdep.stripVersion","true");
        props.setProperty("outputDirectory", workingDir.toString());
       
        InvocationRequest request = new DefaultInvocationRequest().setProperties(props).setGoals( Arrays.asList( "dependency:copy" ) );

        Invoker invoker = new DefaultInvoker();
        invoker.execute( request );
        assertTrue(Files.exists(output));
        
        return output;
    }
}

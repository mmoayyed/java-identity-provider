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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.dependencies.ParsedPom.PomArtifact;
import net.shibboleth.idp.installer.plugin.impl.PluginInstallerSupport;
import net.shibboleth.idp.installer.plugin.impl.TrustStore;
import net.shibboleth.idp.installer.plugin.impl.TrustStore.Signature;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.ParserPool;

/**
 * Test that what we see is what we wanted in abuild - we do this by reading the pom
 */
public class DependencyTest extends OpenSAMLInitBaseTestCase implements PomLoader {

    /** Set this up if you want to run the tests from eclipse. */
    private static String LOCAL_MAVEN_HOME = "c:/Program Files (x86)/apache-maven-3.6.1";

    /** A list of things which get added to real versions. */
    private static final List<String> extensionGarnish = List.of("-SNAPSHOT", "-GA", "-jre", "-empty-to-avoid-conflict-with-guava");

    /** Parse for us to use. */
    private ParserPool parserPool;
    
    /** Work space.  Deleted on exit. */
    private Path workingDir;
    
    /** The parsed idp-parent pom. */
    private ParsedPom idpParent;
    
    /** The parsed java-parent pom. */
    private ParsedPom projectParent;

    /** where we are writing to (target/dependencyReport.txt).*/
    private PrintWriter report;

    /** The trust stores for our signature test. */
    private final Map<String, Optional<TrustStore>> trustStrores = new HashMap<>();
    
    /** The ArtefactId to GroupId mapping */
    private final Map<String, String> artifactToGroup = new HashMap<>();

    /** Is this a snapshot build (the idp version ends with -SNAPSHOT.*/
    private boolean isSnapShot;
    
   /**  We have as an assumption that the CWD is idp-installer.  Test this.
     * @throws IOException if the directory isn't what we expect it to be
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
    
    /** Parse the idp-parent pom and all related.
     * @throws Exception if a folder or files has issues, 
     *    if the pom is badly formed, or if the download fails
     */

    @BeforeClass(dependsOnMethods = {"setupMavenEnvironment", "testWorkingDir"}) public void parsePom() throws Exception {
        workingDir = Files.createTempDirectory("dependencyTest");
        parserPool = XMLObjectProviderRegistrySupport.getParserPool();

        idpParent = new ParsedPom(parserPool, this, Path.of("../idp-parent/pom.xml"), "idp-parent/pom.xml", null, Collections.emptyMap());
        final Path lib = Path.of("../idp-war-distribution/target/idp-war-distribution-"+ idpParent.getOurInfo().getVersion()).resolve("WEB-INF").resolve("lib");
        if (!Files.exists(lib)) {
            throw new SkipException("War distribution target not found");
        }
        assertNotNull(idpParent.getParent());
        final Path parentPath = downloadPom(idpParent.getParent());
        projectParent = new ParsedPom(parserPool, this, parentPath, "parent/pom.xml", new Properties(), Collections.emptyMap());
        idpParent = new ParsedPom(parserPool, this, Path.of("../idp-parent/pom.xml"), "idp-parent/pom.xml", projectParent.getProperties(), projectParent.getManagedDependencies());
        assertTrue(projectParent.getCompileDependencies().isEmpty(), "project parent contributes compile dependencies");
        assertTrue(projectParent.getRuntimeDependencies().isEmpty(), "project parent contributes run time dependencies");
        isSnapShot = idpParent.getOurInfo().getVersion().endsWith("-SNAPSHOT");
    }

    /** Populate the {@link #artifactToGroup} map.
     * First from the parse pom  and then by hand.
     */
    @BeforeClass(dependsOnMethods = "parsePom")  public void setupGroupMapping() {
        for (final PomArtifact dep : idpParent.getCompileDependencies()) {
            artifactToGroup.put(dep.getArtifactId(), dep.getGroupId());
        }
        for (final PomArtifact dep : idpParent.getRuntimeDependencies()) {
            final String comp = artifactToGroup.get(dep.getArtifactId());
                if (comp != null) {
                    assertEquals(comp, dep.getGroupId(), "Group mismatch dependency for " + dep.getArtifactId()); 
                } else {
                    artifactToGroup.put(dep.getArtifactId(), dep.getGroupId());
            }
        }
        for (final PomArtifact artifact : idpParent.getManagedDependencies().values()) {
            if (!artifactToGroup.containsKey(artifact.getArtifactId())) {
                artifactToGroup.put(artifact.getArtifactId(), artifact.getGroupId());
            }
        }
        for (final PomArtifact artifact : projectParent.getManagedDependencies().values()) {
            if (!artifactToGroup.containsKey(artifact.getArtifactId())) {
                artifactToGroup.put(artifact.getArtifactId(), artifact.getGroupId());
            }
        }

        addMapping("annotations", "org.jetbrains");
        addMapping("antlr", "antlr");
        addMapping("byte-buddy", "net.bytebuddy");
        addMapping("checker-qual", "org.checkerframework");
        addMapping("classmate", "com.fasterxml");
        addMapping("commons-cli", "commons-cli");
        addMapping("commons-compiler", "org.codehaus.janino");
        addMapping("commons-lang3", "org.apache.commons");
        addMapping("commons-pool2", "org.apache.commons");
        addMapping("dom4j", "org.dom4j");
        addMapping("error_prone_annotations", "com.google.errorprone");
        addMapping("failureaccess", "com.google.guava");
        addMapping("hibernate-commons-annotations", "org.hibernate.common");
        addMapping("istack-commons-runtime", "com.sun.istack");
        addMapping("j2objc-annotations", "com.google.j2objc");
        addMapping("jandex", "org.jboss");
        addMapping("jboss-logging", "org.jboss.logging");
        addMapping("jboss-transaction-api_1.2_spec", "org.jboss.spec.javax.transaction");
        addMapping("javassist", "org.javassist");
        addMapping("javax.persistence-api", "javax.persistence");
        addMapping("listenablefuture", "com.google.guava");
        addMapping("spymemcached", "net.spy");
        addMapping("spring-binding", "org.springframework.webflow");
        addMapping("stax2-api", "org.codehaus.woodstox");
        addMapping("txw2", "org.glassfish.jaxb");
        addMapping("woodstox-core", "com.fasterxml.woodstox");
    }

    /** Add the pair to the artifact to group mapping.  With test for duplicate
     * @param artifactId the atifact
     * @param groupId the groupid
     */
    private void addMapping(String artifactId, String groupId) {
        final String old = artifactToGroup.put(artifactId, groupId);
        if (old != null) {
            report.format("Duplicate group declaration for %s\n" , artifactId);
        }

    }

    /** Create the reporter print stream
     * @throws FileNotFoundException  if we cannot
     */
    @BeforeClass(dependsOnMethods = {"testWorkingDir"}) public void initialize() throws FileNotFoundException {
        final File out = new File("target/dependencyReport.txt");
        final FileOutputStream outStream = new FileOutputStream(out);
        report = new PrintWriter(new BufferedOutputStream(outStream));
        report.format("Dependency Analysis, started at %s\n", Instant.now().toString());
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /** Clean up after ourselves. */
    @AfterClass public void teardown() {
        report.flush();
        report.close();
        PluginInstallerSupport.deleteTree(workingDir);
    }
 
    /** The Body of the signature test.  Are all the files what we expected?
     * @throws IOException if the enumeration failed.
     */
    @Test(enabled=false) public void testSignatures() throws IOException {
        final Path lib = Path.of("../idp-war-distribution/target/idp-war-distribution-"+ idpParent.getOurInfo().getVersion()).resolve("WEB-INF").resolve("lib");
        
        final int sigFails = Files.list(lib).mapToInt(e -> checkSignature(e)).sum();
        assertEquals(sigFails, 0, "Signature Failures");
    }

    /** Given the Path and the parent dir check the signature.
     * @param jarFile the file to check
     * @return 1 if anything went wrong
     */
    private int checkSignature(Path jarFile) {
        final Pair<String,String> name = splitFileName(jarFile.getFileName().toString());
        final String group = artifactToGroup.get(name.getFirst());
        if (group == null) {
            report.format("%-30s: %-12s Could not determine group\n", name.getFirst(), name.getSecond());
            return 1;
        }
        final PomArtifact jarAsArtifact = idpParent.new PomArtifact(group, name.getFirst(), name.getSecond());
        if (idpParent.getGeneratedArtifacts().contains(jarAsArtifact)) {
            report.format("%-30s: %-12s Generated by IdP build.  Not checked\n", name.getFirst(), name.getSecond());
            return 0;
        }
        if (isSnapShot && name.getSecond().endsWith("-SNAPSHOT")) {
            report.format("%-30s: %-12s SnapShot version on a snapshot build.  Not Checked\n", name.getFirst(), name.getSecond());
            return 0;
        }
        final TrustStore store = getTrustStore(group);
        if (store == null) {
            report.format("%-30s: %-12s No truststore for group %s\n", name.getFirst(), name.getSecond(), group);
            return 1;
        }
        final Signature sig = getSignature(jarAsArtifact);
        if (sig == null) {
            report.format("%-30s: %-12s Could not find signature (group : %s)\n",
                    name.getFirst(), name.getSecond(), group);
            return 1;
        }
        if (!store.contains(sig)) {
            report.format("%-30s: %-12s KeyId (%s) not found in truststore for %s\n", name.getFirst(), name.getSecond(), sig.toString(), group);
            return 1;
        }

        try (final BufferedInputStream stream = new BufferedInputStream(new FileInputStream(jarFile.toFile()))) {
            if (!store.checkSignature(stream, sig)) {
                report.format("%-30s: %-12s Signature Mismatch : %s in Trustore %s\n",
                        name.getFirst(), name.getSecond(), store.getKeyInfo(sig), group);
                return 1;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
        report.format("%-30s: %-12s Signature Match in trustore %s : %s \n", 
                name.getFirst(), name.getSecond(), group, store.getKeyInfo(sig));
        return 0;
    }
 
    /** Locate and load the signature for this artefact
     * @param artifact what to load
     * @return the Signature or null if we couldn't locate it.
     */
    private Signature getSignature(final PomArtifact artifact) {
        Path path;
        try {
            path = download(artifact, "jar.asc");
        } catch (MavenInvocationException e1) {
            e1.printStackTrace();
            return null;
        }
        if (!Files.exists(path)) {
            return null;
        }
        try (final InputStream stream = new BufferedInputStream(new FileInputStream(path.toFile()))){
            return TrustStore.signatureOf(stream);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Locate the truststore in the cache or load &amp; cache it (or a negative lookup).
     * @param group the group to load
     * @return a truststore or null if there wasn't one.
     */
    private TrustStore getTrustStore(final String group) {
        final Optional<TrustStore> opt = trustStrores.get(group);
        if (opt != null) {
            if (opt.isEmpty()) {
                return null;
            }
            return opt.get();
        }

        try (final InputStream input = getClass().getResourceAsStream("/net/shibboleth/idp/dependencies/stores/"+group)) {
            if (input == null) {
                trustStrores.put(group, Optional.empty());
                return null;
            }
            final TrustStore store = new TrustStore();
            store.setTrustStore(input);
            store.initialize();
            trustStrores.put(group,  Optional.of(store));
            return store;
        } catch (IOException | ComponentInitializationException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** The Body of the Dependency test.  Are all the files what we expected? Who produced what?
     * @throws IOException if the file doesn't exist
     * @throws MavenInvocationException if we fail to download a pom or a dependency
     */
    @Test(enabled=false) public void testDependencies() throws IOException, MavenInvocationException {
        if (!idpParent.getDuplicates().isEmpty()) {
            report.format("Duplicates found parsing the poms\n");
            for (final Pair<PomArtifact,PomArtifact> poms : idpParent.getDuplicates()) {
                final PomArtifact f = poms.getFirst();
                final PomArtifact s = poms.getSecond();

                report.format("%-30s: %10s (from %s) and %s (from %s)\n", f.getMapKey(),
                        f.getVersion(), f.getSourcePomFilename(),
                        s.getVersion(), s.getSourcePomFilename());
            }
        }
        final Path lib = Path.of("../idp-war-distribution/target/idp-war-distribution-"+ idpParent.getOurInfo().getVersion()).resolve("WEB-INF").resolve("lib");
        final Map<String, String> names = new HashMap<>();
        int wrongVersion = 0;
        int found = 0;
        int nonUsed = 0;
        int dupNames = 0;
        final int similarNames = Files.list(lib).mapToInt(e -> addName(names, lib.relativize(e).toString())).sum();
        report.format("Dependencies found in war file\n\n");

        List<PomArtifact> dependencies = new ArrayList<>(idpParent.getCompileDependencies().size() + idpParent.getRuntimeDependencies().size());
        dependencies.addAll(idpParent.getCompileDependencies());
        dependencies.addAll(idpParent.getRuntimeDependencies());
        Collections.sort(dependencies);
        // ArtifactId->(Ver->[source, source])
        final Map<String, Map<String, Set<String>>> dependencySource = new HashMap<>();
        PomArtifact last = null;
        for (PomArtifact artifact : dependencies) {
            final String id = artifact.getArtifactId();
            final String ver = artifact.getVersion();
            final String sourcePomFilename = "(from " + artifact.getSourcePomFilename() + ")";
            final String version = names.remove(id);
            if (idpParent.getGeneratedArtifacts().contains(artifact)) {
                if (!artifact.equals(last)) {
                    report.format("%-30s: %12s\tGenerated by parent war\n", id, ver);
                }
            } else if (artifact.equals(last)) {
                report.format("%-30s\t: %12s\tRuntime & Compile: %-22s\n", id, ver, sourcePomFilename);
                dupNames++;
            } else if (version == null) {
                report.format("%-30s\t: %12s\tNot found in war    %-22s\n", id, ver, sourcePomFilename);
                nonUsed++;
            } else if (version.equals(ver)) {
                report.format("%-30s\t: %12s\tFound in war        %-22s\n", id, ver, sourcePomFilename);
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
        int noSource = 0;

        report.format("Found in but not explicitly defined as a dependency:\n\n");

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
                    noSource ++;
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
        report.format("%d Orphans artifact(s)\n", noSource);
        report.format("%d Similar artifact names(s)\n", similarNames);
        report.format("%d Wrong Versions(s)\n", wrongVersion);
        report.format("Completed at %s\n", Instant.now().toString());
        assertEquals(wrongVersion,  0, "Mismatched version");
        assertEquals(similarNames,  0, "Multiple similarly named jars");
        assertEquals(noSource,  0, "Orphaned Artefacts");
        assertTrue(idpParent.getDuplicates().isEmpty(), "Duplicate dependencies");
    }
    
    /** report the contributions of the provided dependency &amp; version.
     * @param dependency the artifact ID  
     * @param version the version we are considering
     * @param sources what caused this to exist
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
     * @throws FileNotFoundException if the created pom file doesnt exist?
     */
    private File outputPom(PomArtifact artifact) throws FileNotFoundException {
        final File file = workingDir.resolve(new StringBuilder(artifact.getArtifactId())
                .append("-")
                .append(artifact.getVersion())
                .append(".xml").
                toString()).toFile();
        final PomArtifact parentArtefact = idpParent.getParent();
        try (final PrintWriter pom = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)))) {
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
        }
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

    /** {@inheritDoc} */
    @Override
    public Path downloadPom(final PomArtifact artifact) throws MavenInvocationException {
        final Path path = download(artifact, "pom");
        assertTrue(Files.exists(path));
        return path;
    }

    /** Tell Maven to download the POM for artifact and returns it's path.
     * @param artifact what to look for
     * @param type the type to dowb load ('pom' or 'jar.asc' and so on
     * @return the pom as a {@link Path}
     * @throws MavenInvocationException if the download failed
     */
    public Path download(final PomArtifact artifact, final String type) throws MavenInvocationException {
        final Path output =  workingDir.resolve(artifact.getArtifactId() + "." + type);
        assertFalse(Files.exists(output));
        final String fullArtifactName = new StringBuilder(artifact.getGroupId())
                    .append(':')
                    .append(artifact.getArtifactId())
                    .append(':')
                    .append(artifact.getVersion())
                    .append(':')
                    .append(type)
                    .toString();
        
        final Properties props = new Properties(3);
        props.setProperty("artifact",fullArtifactName);
        props.setProperty("mdep.stripVersion","true");
        props.setProperty("outputDirectory", workingDir.toString());
       
        InvocationRequest request = new DefaultInvocationRequest().setProperties(props).setGoals( Arrays.asList( "dependency:copy" ) );

        Invoker invoker = new DefaultInvoker();
        invoker.execute( request );
        
        return output;
    }
}

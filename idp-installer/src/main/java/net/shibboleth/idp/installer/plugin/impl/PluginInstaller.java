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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Copy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;

import net.shibboleth.ext.spring.resource.HTTPResource;
import net.shibboleth.idp.installer.BuildWar;
import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.idp.installer.plugin.impl.TrustStore.Signature;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.plugin.PluginDescription;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resource.Resource;

/**
 *  The class where the heavy lifting of managing a plugin happens. 
 */
public final class PluginInstaller extends AbstractInitializableComponent implements AutoCloseable {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PluginInstaller.class);

    /** Where we are installing to. */
    @NonnullAfterInit private Path idpHome;
    
    /** What we are dealing with. */
    private String pluginId;
    
    /** Where we have unpacked into. */
    private Path unpackDirectory;
    
    /** Where we have downloaded. */
    private Path downloadDirectory;
    
    /** The plugin's story about itself. */
    private PluginDescription description;

    /** The callback before we install a certificate into the TrustStore. */
    @Nonnull private Predicate<String> acceptCert = Predicates.alwaysFalse();

    /** The callback before we download a file. */
    @Nonnull private Predicate<Pair<URL,Path>> acceptDownload = Predicates.alwaysFalse();

    /** The actual distribution. */
    private Path distribution;

    /** What to use to download things. */
    private HttpClient httpClient;

    /** set IdP Home.
     * @param home Where we are working from
     */
    public void setIdpHome(@Nonnull final Path home) {
        idpHome = Constraint.isNotNull(home, "IdPHome should be non-null");
    }

    /** Set the plugin in.
     * @param id The pluginId to set.
     */
    public void setPluginId(@Nonnull @NotEmpty final String id) {
        pluginId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Plugin id should be be non-null");
    }

    /** Set the acceptCert predicate.
     * @param what what to set.
     */
    public void setAcceptCert(@Nonnull final Predicate<String> what) {
        acceptCert = Constraint.isNotNull(what, "Accept Certificate Predicate should be non-null");
    }

    /** Set the acceptCert predicate.
     * @param what what to set.
     */
    public void setAcceptDownload(@Nonnull final Predicate<Pair<URL,Path>> what) {
        acceptDownload  = Constraint.isNotNull(what, "Accept Download Predicate should be non-null");
    }

    /** Set the httpClient.
     * @param what what to set.
     */
    public void setHttpClient(final HttpClient what) {
        httpClient = Constraint.isNotNull(what, "HttpClient should be non-null");
    }

    /** Install the plugin from the provided URL.  Involves downloading
     *  the file and then doing a {@link #installPlugin(Path, String)}.
     * @param baseURL where we get the files from
     * @param fileName the name
     * @throws BuildException if badness is detected.
     */
    public void installPlugin(@Nonnull final URL baseURL,
                              @Nonnull @NotEmpty final String fileName) throws BuildException {
        download(baseURL, fileName);
        installPlugin(downloadDirectory, fileName);
    }

    /** Install the plugin from a local path.
     * <ul><li> Check signature</li>
     * <li>Unpack to temp folder</li>
     * <li>Install from the folder</li></ul>
     * @param base the directory where the files are
     * @param fileName the name
     * @throws BuildException if badness is detected.
     */
    public void installPlugin(@Nonnull final Path base,
                              @Nonnull @NotEmpty final String fileName) throws BuildException {
        if (!Files.exists(base.resolve(fileName))) {
            log.error("Could not find distribution {}", base.resolve(fileName));
            throw new BuildException("Could not find distribution");
        }
        if (!Files.exists(base.resolve(fileName + ".asc"))) {
            log.error("Could not find distribution {}", base.resolve(fileName + ".asc"));
            throw new BuildException("Could not find signature for distribution");
        }

        unpack(base, fileName);
        setupPluginId();
        checkSignature(base, fileName);
        getDescription();
        log.info("Installing Plugin {} version {}.{}.{}", pluginId,
                description.getMajorVersion(),description.getMinorVersion(), description.getPatchVersion());

        if (!description.getAdditionalPropertyFiles().isEmpty()) {
            log.error("Additional property files not supported");
            throw new BuildException("Uninstallable plugin");
        }
        if (!description.getPropertyMerges().isEmpty()) {
            log.error("Prroperty merges not supported");
            throw new BuildException("Uninstallable plugin");
        }

        final Path myWebApp = idpHome.resolve("dist").resolve("edit-webapp-" + pluginId);
        deleteTree(myWebApp);
        installWebapp(myWebApp);
        installFiles();
        downloadExternals();

        final BuildWar builder = new BuildWar(idpHome);
        try {
            builder.initialize();
        } catch (final ComponentInitializationException e) {
            throw new BuildException(e);
        }
        builder.execute();
    }

    /** Download any files that should not be shipped.
     * @throws BuildException if badness is detected.
     */
    private void downloadExternals() throws BuildException {
        try {
            for (final Pair<URL, Path> pair : description.getExternalFilePathsToCopy()) {
                final Path to = idpHome.resolve(pair.getSecond());
                if (Files.exists(to)) {
                    log.warn("{} exists, not copied", to);
                    continue;
                }
                if (!acceptDownload.test(new Pair<>(pair.getFirst(), to))) {
                    log.info("Did not download {} to {}", pair.getFirst(), to);
                    continue;
                }
                buildHttpClient();
                createParent(to);
                log.debug("Copying from {} to {}", pair.getFirst(), to);
                final Resource from  = new HTTPResource(httpClient, pair.getFirst());
                try (final InputStream in = new BufferedInputStream(from.getInputStream());
                     final OutputStream out =  new BufferedOutputStream(new FileOutputStream(to.toFile()))) {

                    in.transferTo(out);

                } catch (final IOException e) {
                    log.error("Could not copy from {} to {}",  from, to, e);
                    throw new BuildException(e);
                }
            }
        } catch (final IOException e) {
            throw new BuildException(e);
        }
    }

    /** Get hold of the {@link PluginDescription} for this plugin.
     * @throws BuildException if badness is happens.
     */
    private void getDescription() throws BuildException {
        final List<URL> urls = new ArrayList<>();
        final Path libDir = distribution.resolve("edit-webapp").resolve("WEB-INF").resolve("lib");

        try {
            for (final Path jar : Files.newDirectoryStream(libDir)) {
                urls.add(jar.toUri().toURL());
            }
           try (final URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new))){

               final ServiceLoader<PluginDescription> plugins = ServiceLoader.load(PluginDescription.class, loader);
               for (final PluginDescription plugin:plugins) {
                   log.debug("Found Service announcing itself as {}", plugin.getPluginId() );
                   if (pluginId.equals(plugin.getPluginId())) {
                       description = plugin;
                       return;
                   }
               }
           }
           log.error("Could not locate description for {} in distribution {}", pluginId, libDir);
           throw new BuildException("Could not locate PluginDescription");
        } catch (final IOException e) {
            log.error("Could not get description of {} from {}", pluginId, libDir, e);
            throw new BuildException(e);
        }
    }

    /** Copy the files the distribution tells us to.
     * @throws BuildException if badness is happens.
     */
    private void installFiles() throws BuildException {
        for (final Path p : description.getFilePathsToCopy()) {
            final Path from = distribution.resolve(p);
            final Path to = idpHome.resolve(p);
            if (Files.exists(to)) {
                log.debug("File {} exists, skipping", to);
                continue;
            }
            if (!Files.exists(from)) {
                log.warn("Source File {} does not exists, skipping", from);
                continue;
            }
            try {
                createParent(to);
                log.debug("Copying from {} to {}", from, to);
                try (final InputStream in = new BufferedInputStream(new FileInputStream(from.toFile()));
                     final OutputStream out =  new BufferedOutputStream(new FileOutputStream(to.toFile()))) {
                    in.transferTo(out);
                }
            } catch (final IOException e) {
                log.error("Could not copy from {} to {}",  from, to, e);
                throw new BuildException(e);
            }
        }
    }

    /** If the parent dir of the provided path doesn't exist, create it.
     * @param file where the file will go
     * @throws IOException if the directory couldn't be created
     * @throws BuildException if the parent wasnt a directory
     */
    private void createParent(final Path file) throws IOException, BuildException {
        final Path parent = file.resolve("..");
        if (!Files.exists(parent)) {
            log.debug("Creating parent directory {}", parent);
            Files.createDirectories(parent);
        } else if (!Files.isDirectory(parent)) {
            log.error("{} exists and is not a directory", parent);
            throw new BuildException("Parent of target file was not a directory");
        } else {
            log.trace("Parent directory {} existed", parent);
        }  
    }

    /** Copy the webapp folder from the distribution to the per plugin
     * location inside dist.
     * @param myWebApp Where to put it.
     * @throws BuildException if badness is detected.
     */
    private void installWebapp(final Path myWebApp) throws BuildException {
        final Path from = distribution.resolve("edit-webapp");
        log.debug("Copying distribution from {} to {}", from, myWebApp);
        final Copy copy = InstallerSupport.getCopyTask(from, myWebApp);
        copy.execute();
    }

    /** Method to download a zip file to the {{@link #downloadDirectory}.
     * @param baseURL Where the zip/tgz and signature file is
     * @param fileName the name.
     * @throws BuildException if badness is detected.
     */
    private void download(final URL baseURL, final String fileName) throws BuildException {
        buildHttpClient();
        try {
            downloadDirectory = Files.createTempDirectory("plugin-installer-download");
            final Resource baseResource = new HTTPResource(httpClient, baseURL);
            download(baseResource, fileName);
            download(baseResource, fileName + ".asc");
        } catch (final IOException e) {
            log.error("Error in download", e);
            throw new BuildException(e);
        }
    }

    /** Build the Http Client if it doesn't exist. */
    private void buildHttpClient() {
        if (httpClient == null) {
            log.debug("No HttpClient built, creating default");
            try {
                httpClient = new HttpClientBuilder().buildClient();
            } catch (final Exception e) {
                log.error("Could not create HttpClient", e);
                throw new BuildException(e);
            }
        }
    }

    /** Download helper method.
     * @param baseResource where to go for the file
     * @param fileName the file name
     * @throws IOException as required
     */
    private void download(final Resource baseResource, final String fileName) throws IOException {
        final Resource fileResource = baseResource.createRelativeResource(fileName);
        final Path filePath = downloadDirectory.resolve(fileName);
        log.debug("Downloading from {} to {}", fileResource.getDescription(), filePath);
        try (final OutputStream fileOut = new BufferedOutputStream(
                new FileOutputStream(filePath.toFile()))) {
            fileResource.getInputStream().transferTo(fileOut);
        }
    }

    /** Method to unpack a zip or tgz file into out {{@link #unpackDirectory}.
     * @param base Where the zip/tgz file is
     * @param fileName the name.
     * @throws BuildException if badness is detected.
     */
    // CheckStyle:  CyclomaticComplexity OFF
    private void unpack(final Path base, final String fileName) throws BuildException {
        Constraint.isNull(unpackDirectory, "cannot unpack multiple times");
        try {
            unpackDirectory = Files.createTempDirectory("plugin-installer-unpack");
            
            final Path fullName = base.resolve(fileName);
            try (final ArchiveInputStream inStream = getStreamFor(fullName, isZip(fileName))) {
                
                ArchiveEntry entry = null;
                while ((entry = inStream.getNextEntry()) != null) {
                    if (!inStream.canReadEntryData(entry)) {
                        log.warn("Could not read next entry from {}", inStream);
                        continue;
                    }
                    final File output = unpackDirectory.resolve(entry.getName()).toFile();
                    log.trace("Unpacking {} to {}", entry.getName(), output);
                    if (entry.isDirectory()) {
                        if (!output.isDirectory() && !output.mkdirs()) {
                            log.error("Failed to create directory {}", output);
                            throw new BuildException("failed to create unpacked directory");
                        }
                    } else {
                        final File parent = output.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs()) {
                            log.error("Failed to create parent directory {}", parent);
                            throw new BuildException("failed to create unpacked directory");
                        }
                        try (OutputStream outStream = Files.newOutputStream(output.toPath())) {
                            IOUtils.copy(inStream, outStream);
                        }
                    }
                }
            }
            final Iterator<Path> contents = Files.newDirectoryStream(unpackDirectory).iterator();
            if (!contents.hasNext()) {
                log.error("No contents unpacked from {}", fullName);
                throw new BuildException("Distro was empty");
            }
            distribution = contents.next();
            if (contents.hasNext()) {
                log.error("Too many packages in distributions {}", fullName);
                throw new BuildException("Too many packages in distributions");
            }
        } catch (final IOException e) {
            throw new BuildException(e);
        }
    }
    // CheckStyle:  CyclomaticComplexity ON
    
    /** does the file name end in .zip?
     * @param fileName the name to consider
     * @return true if it ends with .zip
     * @throws BuildException if the name is too short
     */
    private boolean isZip(final String fileName) throws BuildException {
        if (fileName.length() <= 7) {
            log.error("Improbably small file name: {}", fileName);
            throw new BuildException("Improbably small file name");
        }
        if (".zip".equalsIgnoreCase(fileName.substring(fileName.length()-4))) {
            return true;
        }
        if (!".tar.gz".equalsIgnoreCase(fileName.substring(fileName.length()-7))) {
            log.warn("FileName {} did not end with .zip or .tar.gz, assuming tar-gz", fileName);
        }
        return false;
    }

    /** Create the correct {@link ArchiveInputStream} for the input.
     * @param fullName the path of the zip file to unpack.
     * @param isZip if true then this is a zip file, otherwise a tgz file
     * @return the the appropriate  {@link ArchiveInputStream} 
     * @throws IOException  if we trip over an unpack
     */
    private ArchiveInputStream getStreamFor(final Path fullName, final boolean isZip) throws IOException {
        final InputStream inStream = new BufferedInputStream(new FileInputStream(fullName.toFile()));
        if (isZip) {
            return new ZipArchiveInputStream(inStream);
        }
        return new TarArchiveInputStream(new GzipCompressorInputStream(inStream));
    }

    /** Look into the distribution and suck out the plugin id.
     * @throws BuildException if badness is detected.
     */
    private void setupPluginId() throws BuildException {
        final File propertyFile = distribution.resolve("bootstrap").resolve("id.property").toFile();
        if (!propertyFile.exists()) {
            log.error("Could not locate identity of plugin at {}", propertyFile);
            throw new BuildException("Could not locate identity of plugin");
        }
        try (final InputStream inStream = new BufferedInputStream(new FileInputStream(propertyFile))) {
            final Properties idProperties = new Properties();
            idProperties.load(inStream);
            final String id = StringSupport.trimOrNull(idProperties.getProperty("pluginid"));
            if (id == null) {
                log.error("identity property file {} did not contain 'pluginid' property", propertyFile);
                throw new BuildException("No property in ID file");
            }
            setPluginId(id);
        } catch (final IOException e) {
            log.error("Could not load plugin identity at {}", propertyFile, e);
            throw new BuildException(e);
        }
    }

    /** Check the signature of the plugin.
     * @param base Where the zip/tgz file is
     * @param fileName the name.
     * @throws BuildException if badness is detected.
     */
    private void checkSignature(final Path base, final String fileName) throws BuildException {
        try (final InputStream sigStream = new BufferedInputStream(
                new FileInputStream(base.resolve(fileName + ".asc").toFile()))) {
            final TrustStore trust = new TrustStore();
            trust.setIdpHome(idpHome);
            trust.setPluginId(pluginId);
            trust.initialize();
            final Signature sig = TrustStore.signatureOf(sigStream);
            if (!trust.contains(sig)) {
                log.info("TrustStore does not contain signature {}", sig);
                final File certs = distribution.resolve("bootstrap").resolve("keys.txt").toFile();
                if (!certs.exists()) {
                    log.info("No embedded keys file, signature check fails");
                    throw new BuildException("No Certificate found to check signiture o distribution");
                }
                try (final InputStream keysStream = new BufferedInputStream(
                        new FileInputStream(certs))) {
                    trust.importCertificateFromStream(sig, keysStream, acceptCert);
                }
                if (!trust.contains(sig)) {
                    log.info("Certificate not added to Trust Store");
                    throw new BuildException("Could not check signature of distribution");
                }
            }

            try (final InputStream distroStream = new BufferedInputStream(
                new FileInputStream(base.resolve(fileName).toFile()))) {
                if (!trust.checkSignature(distroStream, sig)) {
                    log.info("Signature checked for {} failed", fileName);
                    throw new BuildException("Signature check failed");
                }
            }

        } catch (final ComponentInitializationException | IOException e) {
            log.error("Could not manage truststore for [{}, {}] ", idpHome, pluginId, e);
            throw new BuildException(e);
        }
    }

    /**
     * Return a list of the installed plugins.
     * @return All the plugins.
     */
    public List<PluginDescription> getInstalledPlugins() {
        try {
            final List<URL> urls = new ArrayList<>();
            
            for (final Path webApp : Files.newDirectoryStream(idpHome.resolve("dist"), "edit-webapp-*")) {
                for (final Path jar : Files.newDirectoryStream(webApp.resolve("WEB-INF").resolve("lib"))) {
                    urls.add(jar.toUri().toURL());
                }
            }
            
           try (final URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new))){
               
               return ServiceLoader.load(PluginDescription.class, loader).
                   stream().
                   map(ServiceLoader.Provider::get).
                   collect(Collectors.toList());
           }
            
        } catch (final IOException e) {
            throw new BuildException(e);
        }
    }
    
    /** Delete a directory tree. 
     * @param directory what to delete
     */
    private void deleteTree(@Nullable final Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        log.debug("Deleting directory {}", directory);
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override 
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override 
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            log.error("Couldn't delete {}", directory, e);
        }
    }
    
    /** {@inheritDoc} */
    public void close() {
        deleteTree(downloadDirectory);
        deleteTree(unpackDirectory);
    }
    
}


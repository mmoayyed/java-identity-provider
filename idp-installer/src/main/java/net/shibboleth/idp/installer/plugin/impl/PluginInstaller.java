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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;

import net.shibboleth.ext.spring.resource.HTTPResource;
import net.shibboleth.idp.installer.BuildWar;
import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.idp.installer.ProgressReportingOutputStream;
import net.shibboleth.idp.installer.plugin.impl.TrustStore.Signature;
import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.module.ModuleContext;
import net.shibboleth.idp.module.ModuleException;
import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.idp.plugin.PluginVersion;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resource.Resource;

/**
 *  The class where the heavy lifting of managing a plugin happens. 
 */
public final class PluginInstaller extends AbstractInitializableComponent implements AutoCloseable {

    /** Class logger. */
    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(PluginInstaller.class);

    /** Where we are installing to. */
    @NonnullAfterInit private Path idpHome;
    
    /** What we are dealing with. */
    private String pluginId;
    
    /** Where we have unpacked into. */
    private Path unpackDirectory;
    
    /** Where we have downloaded. */
    private Path downloadDirectory;
    
    /** The plugin's story about itself. */
    private IdPPlugin description;

    /** The callback before we install a certificate into the TrustStore. */
    @Nonnull private Predicate<String> acceptCert = Predicates.alwaysFalse();

    /** The callback before we download a file. */
    @Nonnull private Predicate<String> acceptDownload = Predicates.alwaysFalse();

    /** The actual distribution. */
    private Path distribution;
    
    /** Where to get the keys from if not defaulted. */
    private String truststore;

    /** What to use to download things. */
    private HttpClient httpClient;

    /** Dumping space for renamed files. */
    @NonnullAfterInit private Path renamePath;

    /** DistDir. */
    @NonnullAfterInit private Path distPath;

    /** Pluginss webapp. */
    @NonnullAfterInit private Path pluginsWebapp;

    /** What was installed - this is setup by {@link #loadCopiedFiles()}. */
    @Nullable private List<String> installedContents;

    /** The version from the contents file, or null if it isn't loaded. */
    @Nullable private String installedVersionFromContents;

    /** The Module Context. */
    @NonnullAfterInit private ModuleContext moduleContext;

    /** The "plugins" classpath loader. AutoClosed. */
    private URLClassLoader installedPluginLoader;

    /** The "plugin under construction" classpath loader. AutoClosed. */
    private URLClassLoader installingPluginLoader;

    /** The securiotyParams for the module context. */
    private HttpClientSecurityParameters securityParams;

    /** Set IdP Home.
     * @param home Where we are working from
     */
    public void setIdpHome(@Nonnull final Path home) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        idpHome = Constraint.isNotNull(home, "IdPHome should be non-null");
    }

    /** Set the plugin id.
     * @param id what to set.
     */
    public void setPluginId(@Nonnull @NotEmpty final String id) {
        pluginId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Plugin id should be be non-null");
    }

    /** Set the truststore.
     * @param loc what set.
     */
    public void setTrustore(@Nullable final String loc) {
        truststore = StringSupport.trimOrNull(loc);
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
    public void setAcceptDownload(@Nonnull final Predicate<String> what) {
        acceptDownload  = Constraint.isNotNull(what, "Accept Download Predicate should be non-null");
    }

    /** Set the httpClient.
     * @param what what to set.
     */
    public void setHttpClient(@Nonnull final HttpClient what) {
        httpClient = Constraint.isNotNull(what, "HttpClient should be non-null");
    }

    /** Set the Module Context security parameters.
     * @param params what to set.
     */
    public void setModuleContextSecurityParams(@Nullable final HttpClientSecurityParameters params) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        securityParams =  params;
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
            LOG.error("Could not find distribution {}", base.resolve(fileName));
            throw new BuildException("Could not find distribution");
        }
        if (!Files.exists(base.resolve(fileName + ".asc"))) {
            LOG.error("Could not find distribution {}", base.resolve(fileName + ".asc"));
            throw new BuildException("Could not find signature for distribution");
        }

        unpack(base, fileName);
        setupPluginId();
        checkSignature(base, fileName);
        setupDescriptionFromDistribution();
        LOG.info("Installing Plugin {} version {}.{}.{}", pluginId,
                description.getMajorVersion(),description.getMinorVersion(), description.getPatchVersion());

        try (final RollbackPluginInstall rollBack = new RollbackPluginInstall(moduleContext)) {
            uninstallOld(rollBack);

            checkRequiredModules();
            installNew(rollBack);

            saveCopiedFiles(rollBack.getFilesCopied());
            rollBack.completed();
        }

        final BuildWar builder = new BuildWar(idpHome);
        try {
            builder.initialize();
        } catch (final ComponentInitializationException e) {
            throw new BuildException(e);
        }
        builder.execute();
    }

    /** Remove the jars for this plugin and rebuild the war.
     * @throws BuildException if badness occurs. */
    public void uninstall() throws BuildException {

        String moduleId = null;
        description = getInstalledPlugin(pluginId);
        if (description == null) {
            LOG.warn("Description for {} not found", pluginId);
        } else {
            try (final RollbackPluginInstall rollback = new RollbackPluginInstall(moduleContext)){
                for (final IdPModule module: description.getDisableOnRemoval()) {
                    moduleId = module.getId();
                    module.disable(moduleContext, true);
                    rollback.getModulesDisabled().add(module);
                }
                rollback.completed();
            } catch (final ModuleException e) {
                LOG.error("Uninstalling {}. Could not disable {}", pluginId, moduleId, e);
                LOG.error("Fix this and rerun");
                throw new BuildException(e);
            }
        }
        if (getVersionFromContents() == null) {
            LOG.warn("Installed contents for {} not found", pluginId);
        } else {
            for (final String content: getInstalledContents()) {
                final Path p = Path.of(content);
                if (!Files.exists(p)) {
                    continue;
                }
                try {
                    InstallerSupport.setReadOnly(p, false);
                    Files.deleteIfExists(p);
                } catch (final IOException e) {
                    LOG.warn("Could not delete {}, deferring the delete", content, e);
                    p.toFile().deleteOnExit();
                }
            }

            final BuildWar builder = new BuildWar(idpHome);
            try {
                builder.initialize();
            } catch (final ComponentInitializationException e) {
                throw new BuildException(e);
            }
            builder.execute();
            LOG.info("Removed resources for {} from the war", pluginId);
        }
    }

    /** Get hold of the {@link IdPPlugin} for this plugin.
     * @throws BuildException if badness is happens.
     */
    private void setupDescriptionFromDistribution() throws BuildException {
       final ServiceLoader<IdPPlugin> plugins;
       try {
           plugins = ServiceLoader.load(IdPPlugin.class, getDistributionLoader());
       } catch (final IOException e) {
           LOG.error("Error loading descritpion");
           throw new BuildException(e);
       }
       final Optional<IdPPlugin> first = plugins.findFirst();
       if (first.isEmpty()) {
           LOG.error("No Plugin services found in plugin distribution");
           throw new BuildException("No Plugin services found in plugin distribution");
       }
       for (final IdPPlugin plugin:plugins) {
           LOG.debug("Found Service announcing itself as {}", plugin.getPluginId() );
           if (pluginId.equals(plugin.getPluginId())) {
               description = plugin;
               return;
           }
           LOG.trace("Did not match {}", pluginId);
       }
       LOG.error("Looking in plugin distibution for a plugin called {}, but found a plugin called {}.",
              pluginId, first.get().getPluginId());
       throw new BuildException("Could not locate PluginDescription");
    }

    /** What files were installed to webapp for this plugin?
     * @return a list of the installed contents, may be empty if
     * nothing is installed or the plugin didn't install anything.
     */
    @Nonnull public List<String> getInstalledContents() {
        loadCopiedFiles();
        return installedContents;
    }

    /** return the version that the contents page thinks is installed.
     * @return the version, or null if it is not found.
     */
    @Nullable public String getVersionFromContents() {
        loadCopiedFiles();
        return installedVersionFromContents;
    }

    /**
     * Police that required modules for plugin installation are enabled.
     * 
     * @throws BuildException if any required modules are missing or disabled or loading the module fails
     */
    private void checkRequiredModules() throws BuildException {
        final Set<String> requiredModules = new HashSet<>(description.getRequiredModules());
        final Iterator<IdPModule> modules = ServiceLoader.load(IdPModule.class, getInstalledPluginLoader()).iterator();
        while (modules.hasNext() && !requiredModules.isEmpty()) {
            try {
                final IdPModule module = modules.next();
                if (requiredModules.contains(module.getId())) {
                    if (module.isEnabled(moduleContext)) {
                        requiredModules.remove(module.getId());
                    }
                }
            } catch (final ServiceConfigurationError e) {
                LOG.error("Unable to instantiate IdPModule", e);
            }
        }
        
        if (!requiredModules.isEmpty()) {
            LOG.warn("Required modules are missing or disabled: {}", requiredModules);
            throw new BuildException("One or more required modules are not enabled");
        }
    }

    /** Copy the webapp folder from the distribution to the per plugin
     * location inside dist.
     * @param rollBack Roll Back Context
     * @throws BuildException if badness is detected.
     */
    private void installNew(final RollbackPluginInstall rollBack) throws BuildException {
        final Path from = distribution.resolve("webapp");
        if (PluginInstallerSupport.detectDuplicates(from, pluginsWebapp)) {
            throw new BuildException("Install would overwrite filess");
        }
        PluginInstallerSupport.copyWithLogging(from, pluginsWebapp, rollBack.getFilesCopied());

        String moduleId = null;
        try {
            for (final IdPModule module: description.getDisableOnRemoval()) {
                moduleId = module.getId();
                if (!module.isEnabled(moduleContext)) {
                    module.enable(moduleContext);
                    rollBack.getModulesEnabled().add(module);
                }
            }
        } catch (final ModuleException e) {
            LOG.error("Error enabling {}", moduleId);
            throw new BuildException(e);
        }
    }

    /** Uninstall the old version of the plugin.
     * @param rollback Rollback Context
     * @throws BuildException on IO or module errors */
    private void uninstallOld(final RollbackPluginInstall rollback) throws BuildException {
        final IdPPlugin oldPlugin = getInstalledPlugin(pluginId);
        if (oldPlugin == null) {
            LOG.debug("{} not installed. No modules disabled", pluginId);
        } else {
            String moduleId = null;
            try {
                for (final IdPModule module: oldPlugin.getDisableOnRemoval()) {
                    moduleId = module.getId();
                    if (module.isEnabled(moduleContext)) {
                        module.disable(moduleContext, true);
                        rollback.getModulesDisabled().add(module);
                    }
                }
            } catch (final ModuleException e) {
                LOG.error("Error disabling {}", moduleId);
                throw new BuildException(e);
            }
        }

        if (getVersionFromContents() == null) {
            LOG.debug("{} not installed. files renamed", pluginId);
        } else {
            try {
                PluginInstallerSupport.renameToTree(pluginsWebapp, renamePath,
                        getInstalledContents(),
                        rollback.getFilesRenamedAway());
            } catch (final IOException e) {
                LOG.error("Error uninstalling plugin");
                throw new BuildException(e);
            }
        }
    }

    /** Stream the copy list to a property file and empty it.
     * @param copiedFiles The copied files
     * @throws BuildException If we hit an IO exception
     */
    private void saveCopiedFiles(final List<Path> copiedFiles) throws BuildException {
        try {
            final Path parent = distPath.resolve("plugin-contents");
            Files.createDirectories(parent);
            final Properties props = new Properties(1+copiedFiles.size());
            props.setProperty("idp.plugin.version",
                    new PluginVersion(description).toString());
            int count = 1;
            for (final Path p: copiedFiles) {
                props.setProperty("idp.plugin.file."+Integer.toString(count++),
                        PluginInstallerSupport.canonicalPath(p).toString());
            }
            final File outFile = parent.resolve(pluginId).toFile();
            try (final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
                props.store(out, "Files Copied "  + Instant.now());
            }
        } catch (final IOException e) {
            LOG.error("Error saving list of copied files.", e);
            throw new BuildException(e);
        }
    }

    /** Load the contents for this plugin from the properties file used during
     * installation.
     * @throws BuildException if the load fails
     */
    private void loadCopiedFiles() throws BuildException {
        if (installedContents != null) {
            return;
        }
        final Path parent = distPath.resolve("plugin-contents");
        final Properties props = new Properties();
        final File inFile = parent.resolve(pluginId).toFile();
        if (!inFile.exists()) {
            LOG.debug("Contents file for plugin {} ({}) does not exist", pluginId, inFile.getAbsolutePath());
            installedContents = Collections.emptyList();
            return;
        }
        try (final BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(inFile))) {
            props.load(inStream);
        } catch (final IOException e) {
            LOG.error("Error loading list of copied files from {}.", inFile, e);
            throw new BuildException(e);
        }
        installedContents = new ArrayList<>(props.size());
        installedVersionFromContents = StringSupport.trimOrNull(props.getProperty("idp.plugin.version"));
        int count = 1;
        String val = props.getProperty("idp.plugin.file."+Integer.toString(count++));
        while (val != null) {
            installedContents.add(val);
            val = props.getProperty("idp.plugin.file."+Integer.toString(count++));
        }
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
            LOG.error("Error in download", e);
            throw new BuildException(e);
        }
    }

    /** Build the Http Client if it doesn't exist. */
    private void buildHttpClient() {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        if (httpClient == null) {
            LOG.debug("No HttpClient built, creating default");
            try {
                httpClient = new HttpClientBuilder().buildClient();
                moduleContext.setHttpClient(httpClient);
            } catch (final Exception e) {
                LOG.error("Could not create HttpClient", e);
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
        LOG.info("Downloading from {}", fileResource.getDescription());
        LOG.debug("Downloading to {}", filePath);
        try (final OutputStream fileOut = new ProgressReportingOutputStream(new FileOutputStream(filePath.toFile()))) {
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
                        LOG.warn("Could not read next entry from {}", inStream);
                        continue;
                    }
                    final File output = unpackDirectory.resolve(entry.getName()).toFile();
                    LOG.trace("Unpacking {} to {}", entry.getName(), output);
                    if (entry.isDirectory()) {
                        if (!output.isDirectory() && !output.mkdirs()) {
                            LOG.error("Failed to create directory {}", output);
                            throw new BuildException("failed to create unpacked directory");
                        }
                    } else {
                        final File parent = output.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs()) {
                            LOG.error("Failed to create parent directory {}", parent);
                            throw new BuildException("failed to create unpacked directory");
                        }
                        try (OutputStream outStream = Files.newOutputStream(output.toPath())) {
                            IOUtils.copy(inStream, outStream);
                        }
                    }
                }
            }
            try (final DirectoryStream<Path> unpackDirStream = Files.newDirectoryStream(unpackDirectory)) {
                final Iterator<Path> contents = unpackDirStream.iterator();
                if (!contents.hasNext()) {
                    LOG.error("No contents unpacked from {}", fullName);
                    throw new BuildException("Distro was empty");
                }
                distribution = PluginInstallerSupport.canonicalPath(contents.next());
                if (contents.hasNext()) {
                    LOG.error("Too many packages in distributions {}", fullName);
                    throw new BuildException("Too many packages in distributions");
                }
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
            LOG.error("Improbably small file name: {}", fileName);
            throw new BuildException("Improbably small file name");
        }
        if (".zip".equalsIgnoreCase(fileName.substring(fileName.length()-4))) {
            return true;
        }
        if (!".tar.gz".equalsIgnoreCase(fileName.substring(fileName.length()-7))) {
            LOG.warn("FileName {} did not end with .zip or .tar.gz, assuming tar-gz", fileName);
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
        final File propertyFile = distribution.resolve("bootstrap").resolve("plugin.properties").toFile();
        if (!propertyFile.exists()) {
            LOG.error("Could not locate identity of plugin. "
                    + "Identity file 'bootstrap/plugin.properties' not present in plugin distribution.");
            throw new BuildException("Could not locate identity of plugin");
        }
        try (final InputStream inStream = new BufferedInputStream(new FileInputStream(propertyFile))) {
            final Properties idProperties = new Properties();
            idProperties.load(inStream);
            final String id = StringSupport.trimOrNull(idProperties.getProperty("plugin.id"));
            if (id == null) {
                LOG.error("Identity property file 'bootstrap/id.property' did not contain 'pluginid' property");
                throw new BuildException("No property in ID file");
            }
            if (pluginId != null && !pluginId.equals(id)) {
                LOG.error("Downloaded plugin id {} overriden by provided id {}", id, pluginId);
            } else {
                setPluginId(id);
            }
        } catch (final IOException e) {
            LOG.error("Could not load plugin identity file 'bootstrap/id.property'", e);
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
            trust.setTrustStore(truststore);
            trust.setPluginId(pluginId);
            trust.initialize();
            final Signature sig = TrustStore.signatureOf(sigStream);
            if (!trust.contains(sig)) {
                LOG.info("TrustStore does not contain signature {}", sig);
                final File certs = distribution.resolve("bootstrap").resolve("keys.txt").toFile();
                if (!certs.exists()) {
                    LOG.info("No embedded keys file, signature check fails");
                    throw new BuildException("No Certificate found to check signiture o distribution");
                }
                try (final InputStream keysStream = new BufferedInputStream(
                        new FileInputStream(certs))) {
                    trust.importCertificateFromStream(sig, keysStream, acceptCert);
                }
                if (!trust.contains(sig)) {
                    LOG.info("Certificate not added to Trust Store");
                    throw new BuildException("Could not check signature of distribution");
                }
            }

            try (final InputStream distroStream = new BufferedInputStream(
                new FileInputStream(base.resolve(fileName).toFile()))) {
                if (!trust.checkSignature(distroStream, sig)) {
                    LOG.info("Signature checked for {} failed", fileName);
                    throw new BuildException("Signature check failed");
                }
            }

        } catch (final ComponentInitializationException | IOException e) {
            LOG.error("Could not manage truststore for [{}, {}] ", idpHome, pluginId, e);
            throw new BuildException(e);
        }
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        if (idpHome == null) {
            throw new ComponentInitializationException("Idp Home should be set");
        }
        try {
            idpHome = PluginInstallerSupport.canonicalPath(idpHome);
        } catch (final IOException e) {
            LOG.error("Could not canonicalize idp home", e);
            throw new ComponentInitializationException(e);
        }
        moduleContext = new ModuleContext(idpHome);
        moduleContext.setHttpClientSecurityParameters(securityParams);
        moduleContext.setHttpClient(httpClient);
        distPath = idpHome.resolve("dist");
        renamePath = distPath.resolve("plugin-rollback");
        pluginsWebapp = distPath.resolve("plugin-webapp");
        InstallerSupport.setReadOnly(distPath, false);
    }

    /** Generate a {@link URLClassLoader} which looks at the
     * installed WEB-INF/lib in addition to the dist webapp and bin/lib directories.
     * @return an appropriate loader
     * @throws BuildException if a directory traversal fails.
     */
    private synchronized URLClassLoader getInstalledPluginLoader() throws BuildException {
        if (installedPluginLoader != null) {
            return installedPluginLoader;
        }
        final List<URL> urls = new ArrayList<>();
        final Path libs = pluginsWebapp.resolve("WEB-INF").resolve("lib");
        if (Files.exists(libs)) {
            try (final DirectoryStream<Path> webInfLibs = Files.newDirectoryStream(libs)) {
                for (final Path jar : webInfLibs) {
                    urls.add(jar.toUri().toURL());
                }
            } catch (final IOException e) {
                LOG.error("Error finding Plugins' classpath");
                throw new BuildException(e);
            }
        }
        installedPluginLoader = new URLClassLoader(urls.toArray(URL[]::new));
        return installedPluginLoader;        
    }

    
    /** Generate a {@link URLClassLoader} which looks at the
     * installing WEB-INF.
     * @return an appropriate loader
     * @throws IOException if a directory traversal fails.
     */
    private synchronized URLClassLoader getDistributionLoader() throws IOException {
        if (installingPluginLoader!= null) {
            return installingPluginLoader;
        }
        final List<URL> urls = new ArrayList<>();
        final Path libDir = distribution.resolve("webapp").resolve("WEB-INF").resolve("lib");
        try (final DirectoryStream<Path> libDirPaths = Files.newDirectoryStream(libDir)){
            for (final Path jar : libDirPaths) {
                urls.add(jar.toUri().toURL());
            }
            installingPluginLoader  = new URLClassLoader(urls.toArray(URL[]::new));
            return installingPluginLoader;
        }
    }

    /**
     * Return a list of the installed plugins.
     * @return All the plugins.
     * @throws BuildException if loafing the classpath fails.
     */
    public List<IdPPlugin> getInstalledPlugins() throws BuildException {
       final Stream<Provider<IdPPlugin>> loaderStream =
               ServiceLoader.load(IdPPlugin.class, getInstalledPluginLoader()).stream();
       return loaderStream.map(ServiceLoader.Provider::get).collect(Collectors.toList());
    }

    /** Find the {@link IdPPlugin} with the provided Id.
     * @param name what to find
     * @return the {@link IdPPlugin} or null if not found.
     */
    @Nullable public IdPPlugin getInstalledPlugin(@Nonnull final String name) {
        Constraint.isNotNull(name, "Plugin Name must not be null");
        final List<IdPPlugin> plugins = getInstalledPlugins();
        for (final IdPPlugin plugin: plugins) {
            if (name.equals(plugin.getPluginId())) {
                return plugin;
            }
        }
        return null;
    }

    /** close, ignoring errors.
     * @param what what to close
     */
    private void closeSilently(final AutoCloseable what) {
        if (what == null) {
            return;
        }
        try {
            what.close();
        } catch (final Exception e) {
            LOG.error("Autoclose of {} failed", what, e);
        }
    }

    /** {@inheritDoc} */
    public void close() {
        closeSilently(installedPluginLoader);
        closeSilently(installingPluginLoader);
        PluginInstallerSupport.deleteTree(downloadDirectory);
        PluginInstallerSupport.deleteTree(unpackDirectory);
        PluginInstallerSupport.deleteTree(renamePath);
        InstallerSupport.setReadOnly(distPath, true);
    }
    
}


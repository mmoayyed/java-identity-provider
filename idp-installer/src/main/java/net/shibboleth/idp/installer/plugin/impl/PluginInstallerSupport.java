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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.tools.ant.BuildException;
import org.opensaml.security.httpclient.HttpClientSecurityContextHandler;
import org.slf4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLine;
import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.idp.installer.ProgressReportingOutputStream;
import net.shibboleth.idp.installer.plugin.impl.PluginState.VersionInfo;
import net.shibboleth.idp.plugin.PluginSupport.SupportLevel;
import net.shibboleth.idp.plugin.PluginVersion;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.spring.httpclient.resource.HTTPResource;

/**
 * Support for copying files during plugin manipulation.
 */
public final class PluginInstallerSupport {
    
    /** Class logger. */
    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(PluginInstallerSupport.class);

    /** Constructor. */
    private PluginInstallerSupport() {
    }

    /** Return the canonical path.
     * @param from the path we get given
     * @return the canonicalized one
     * @throws IOException  as from {@link File#getCanonicalFile()}
     */
    @SuppressWarnings("null")
    @Nonnull static Path canonicalPath(@Nonnull final Path from) throws IOException {
        return from.toFile().getCanonicalFile().toPath();
    }

    /** Delete a directory tree. 
     * @param directory what to delete
     */
    public static void deleteTree(@Nullable final Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        LOG.debug("Deleting directory {}", directory);
        InstallerSupport.setReadOnly(directory, false);
        try {
            Files.walkFileTree(directory, new DeletingVisitor());
        } catch (final IOException e) {
            LOG.error("Couldn't delete {}", directory, e);
        }
    }
    
    /** Traverse "from" looking to see if any of the files are already in "to".
     * @param from source directory
     * @param to target directory
     * @return true if there was a match 
     * @throws BuildException if anything threw and {@link IOException}
     */
    public static boolean detectDuplicates(final Path from, final Path to) throws BuildException {
        
        if (to == null || !Files.exists(to)) {
            return false;
        }
        final NameClashVisitor detector = new NameClashVisitor(from, to);
        LOG.debug("Walking {}, looking for a name clash in {}", from, to);
        try {
            Files.walkFileTree(from, detector);
        } catch (final IOException e) {
            LOG.error("Failed during duplicate detection:", e);
            throw new BuildException(e);
        }        
        return detector.wasNameClash();
    }
    
    /** Copy a directory tree and keep a log of what has changed.
     * @param from source directory
     * @param to target directory
     * @param pathsCopied the list of files copied up (including if there was a failure)
     * @throws BuildException from the copy
     */
    public static void copyWithLogging(final Path from, 
            final Path to, @Live final List<Path> pathsCopied) throws BuildException {
        if (from == null || !Files.exists(from)) {
            return;
        }
        LOG.debug("Copying from {} to {}", from, to);
        final LoggingVisitor visitor = new LoggingVisitor(from, to);
        try {
            Files.walkFileTree(from, visitor);
        } catch (final IOException e) {
            pathsCopied.addAll(visitor.getCopiedList());
            LOG.error("Error copying files from {} to {}", from, to, e);
            throw new BuildException(e);
        }
        pathsCopied.addAll(visitor.getCopiedList());
    }
    
    /** Rename Files into the provided tree.
     * @param fromBase The root directory of the from files
     * @param toBase The root directory to rename to
     * @param fromFiles The list of files (inside fromBase) to rename
     * @param renames All the work as it is done
     * @throws IOException If any of the file operations fail
     */
    public static void renameToTree(@Nonnull final Path fromBase,
            @Nonnull final Path toBase,
            @Nonnull final List<Path> fromFiles,
            @Nonnull @Live final List<Pair<Path, Path>> renames) throws IOException {
        if (!Files.exists(toBase)) {
            Files.createDirectories(toBase);
        }
        for (final Path path : fromFiles) {
            if (!Files.exists(path)) {
                LOG.info("File {} was not renamed away because it does not exist", path);
                continue;
            }
            final Path relName = fromBase.relativize(path);
            LOG.trace("Relative name {}", relName);
            final Path to = toBase.resolve(relName);
            Files.createDirectories(to.getParent());
            Files.move(path,to);
            renames.add(new Pair<>(path, to));
        }
    }

    /**
     * A @{link {@link FileVisitor} which detects (and logs) whether a copy would overwrite.
     */
    private static final class NameClashVisitor extends SimpleFileVisitor<Path> {
        /** did we find a duplicate. */
        private boolean nameClash;

        /** Path we are traversing. */
        private final Path from;
        
        /** Path where we check for Duplicates. */
        private final Path to;
        /**
         * Constructor.
         *
         * @param fromDir Path we are traversing
         * @param toDir Path where we check for Duplicates
         */
        public NameClashVisitor(final Path fromDir, final Path toDir) {
            from = fromDir;
            to = toDir;
        }
        
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            final Path relFile = from.relativize(file);
            final Path toFile = to.resolve(relFile);
            if (Files.exists(toFile)) {
                nameClash = true;
                LOG.warn("{} already exists", toFile);
            }
            return FileVisitResult.CONTINUE;
        }
        
        /** did we find a name clash?
         * @return whether we found a name clash.
         */
        public boolean wasNameClash() {
            return nameClash;
        }
    }
    
    /**
     * A @{link {@link FileVisitor} which deletes files.
     */
    private static final class DeletingVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            try {
                Files.delete(file);
            } catch (final IOException e) {
                LOG.error("Could not delete {}", file.toAbsolutePath(), e);
                file.toFile().deleteOnExit();
                // and carry on
            }
            return FileVisitResult.CONTINUE;
        }
        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            if (exc != null) {
                throw exc;
            }
            try {
                Files.delete(dir);
            } catch (final IOException e) {
                LOG.error("Could not delete {}", dir.toAbsolutePath(), e);
                dir.toFile().deleteOnExit();
                // and carry on
            }
            return FileVisitResult.CONTINUE;
        }
    }

    /** Find the best update version (plugin or IdP).
     * @param pluginVersion The Plugin version
     * @param pluginInfo all about the plugin
     * @return the best version (or null)
     */
    @Nullable static public PluginVersion getBestVersion(@Nonnull final PluginVersion pluginVersion, @Nonnull final PluginInfo pluginInfo) {
        return getBestVersion(PluginInstaller.getIdPVersion(), pluginVersion, pluginInfo);
    }

   /** Find the best update version  (plugin or IdP).
     * @param idPVersion The IdP version to check.
     * @param pluginVersion The Plugin version
     * @param pluginInfo all about the plugin
     * @return the best version (or null)
     */
    @Nullable static public PluginVersion getBestVersion(@Nonnull final PluginVersion idPVersion,
            @Nonnull final PluginVersion pluginVersion, @Nonnull final PluginInfo pluginInfo) {

        final List<PluginVersion> availableVersions = new ArrayList<>(pluginInfo.getAvailableVersions().keySet());
        availableVersions.sort(null);
        LOG.debug("Considering versions: {}", availableVersions);

        for (int i = availableVersions.size()-1; i >= 0; i--) {
            final PluginVersion version = availableVersions.get(i);
            if (version.compareTo(pluginVersion) <= 0) {
                LOG.debug("Version {} is less than or the same as {}. All done", version, pluginVersion);
                return null;
            }
            final VersionInfo versionInfo = pluginInfo.getAvailableVersions().get(version);
            if (versionInfo.getSupportLevel() != SupportLevel.Current) {
                LOG.debug("Version {} has support level {}, ignoring", version, versionInfo.getSupportLevel());
                continue;
            }
            if (!pluginInfo.isSupportedWithIdPVersion(version, idPVersion)) {
                LOG.debug("Version {} is not supported with idpVersion {}", version, idPVersion);
                continue;
            }
            LOG.debug("Version {} is supported with idpVersion {}", version, idPVersion);
            if (pluginInfo.getUpdateURL(version) == null || pluginInfo.getUpdateBaseName(version) == null) {
                LOG.debug("Version {} is does not have update information", version);
                continue;
            }
            return version;
        }
        return null;
    }

    /** Load the property file describing all the plugin we know about from a known location.
     * @param updateURLs where to look
     * @param commandLine the programming calling us.
     * @return the property files plugins.
     */
    @Nullable public static Properties loadPluginInfo(@Nonnull final List<URL> updateURLs,
            @Nonnull final AbstractIdPHomeAwareCommandLine<?> commandLine) {
        final List<URL> urls;
        final Properties props = new Properties();
        try {
            if (updateURLs.isEmpty()) {
                urls = List.of(
                        new URL("https://shibboleth.net/downloads/identity-provider/plugins/plugins.properties"),
                        new URL("http://plugins.shibboleth.net/plugins.properties"));
            } else {
                urls = updateURLs;
            }
        } catch (final IOException e) {
            LOG.error("Could not load update URLs", e);
            return null;
        }
        for (final URL url: urls) {
            final Resource propertyResource;
            try {
                if ("file".equals(url.getProtocol())) {
                    final String path =url.getPath();
                    assert path != null;
                    propertyResource = new FileSystemResource(path);
                } else if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
                    final HttpClient client = commandLine.getHttpClient();
                    assert client != null;
                    final HTTPResource httpResource;
                    propertyResource = httpResource = new HTTPResource(client , url);
                    final HttpClientSecurityContextHandler handler = new HttpClientSecurityContextHandler();
                    handler.setHttpClientSecurityParameters(commandLine.getHttpClientSecurityParameters());
                    handler.initialize();
                    httpResource.setHttpClientContextHandler(handler);
                } else {
                    LOG.error("Only file and http[s] URLs are allowed");
                    continue;
                }
                LOG.debug("Plugin Listing: Looking for update at {}", propertyResource.getDescription());
                if (!propertyResource.exists()) {
                    LOG.info("{} could not be located", propertyResource.getDescription());
                    continue;
                }
                props.load(propertyResource.getInputStream());
                return props;
            } catch (final IOException | ComponentInitializationException e) {
                LOG.error("Could not open Update URL {} :", url, e);
                continue;
            }
        }
        LOG.error("Could not locate any active update servers");
        return null;
    }

    /** Download helper method.
     * @param baseResource where to go for the file
     * @param handler HttpClientSecurityContextHandler to use
     * @param downloadDirectory where to download to
     * @param fileName the file name
     * @throws IOException as required
     */
    public static void download(@Nonnull final HTTPResource baseResource,
            @Nonnull final HttpClientSecurityContextHandler handler,
            @Nonnull final Path downloadDirectory,
            @Nonnull final String fileName) throws IOException {
        final HTTPResource httpResource = baseResource.createRelative(fileName, handler);
        final Path filePath = downloadDirectory.resolve(fileName);
        LOG.info("Downloading from {}", httpResource.getDescription());
        LOG.debug("Downloading to {}", filePath);
        try (final OutputStream fileOut = new ProgressReportingOutputStream(new FileOutputStream(filePath.toFile()))) {
            httpResource.getInputStream().transferTo(fileOut);
        }
    }

}

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
import java.io.File;
import java.io.FileInputStream;
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
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.tools.ant.BuildException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.plugin.PluginDescription;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

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
    
    /** Our TrustStore. */
    private TrustStore trustStore;
      
    /** set IdP Home.
     * @param home Where we are working from
     */
    public void setIdpHome(@Nonnull final Path home) {
        idpHome = Constraint.isNotNull(home, "IdPHome should be non-null");
    }

    /** Set the plugin in.
     * @param id The pluginId to set.
     */
    public void setPluginId( @Nonnull @NotEmpty final String id) {
        pluginId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Plugin id should be be non-null");
    }    

    /** Install the plugin from the provided URL.  Involves downloading
     *  the file and then doing a {@link #installPlugin(Path, String)}.
     * @param baseURL where we get the files from
     * @param fileName the name
     */
    public void installPlugin(@Nonnull final URL baseURL,
                              @Nonnull @NotEmpty final String fileName) {
        //download(baseURL, fileName);
        installPlugin(downloadDirectory, fileName);
    }
    
    /** Install the plugin from a local path.
     * <ul><li> Check signature</li>
     * <li>Unpack to temp folder</li>
     * <li>Install from the folder</li></ul>
     * @param base the directory where the files are
     * @param fileName the name
     */
    public  void installPlugin(@Nonnull final Path base, 
                               @Nonnull @NotEmpty final String fileName) {
        unpack(base, fileName);

    }
    
    /** Method to unpack a zip or tgz file into out {{@link #unpackDirectory}. 
     * @param base Where the zip/tgz file is
     * @param fileName the name.
     * @throws BuildException if badness is detected.
     */
    private void unpack(final Path base, final String fileName) throws BuildException {
        Constraint.isNull(unpackDirectory, "cannot unpack multiple times");
        try {
            unpackDirectory = Files.createTempDirectory("plugin-installer");
            
            final Path fullName = base.resolve(fileName);
            try (final ArchiveInputStream inStream = getStreamFor(fullName, isZip(fileName))) {
                
                ArchiveEntry entry = null;
                while ((entry = inStream.getNextEntry()) != null) {
                    if (!inStream.canReadEntryData(entry)) {
                        log.warn("Could not read next entry from {}", inStream);
                        continue;
                    }
                    final File output = unpackDirectory.resolve(entry.getName()).toFile();
                    log.debug("Unpacking {} to {}", entry.getName(), output);
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
        } catch (final IOException e) {
            throw new BuildException(e);
        }
    }
    
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
        if (directory == null) {
            return;
        }
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


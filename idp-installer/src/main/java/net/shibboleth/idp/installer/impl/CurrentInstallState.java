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

package net.shibboleth.idp.installer.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;
import org.slf4j.Logger;

import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.module.ModuleContext;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.LoggerFactory;

/** Tells the installers about the current install state. */
public final class CurrentInstallState extends AbstractInitializableComponent {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CurrentInstallState.class);

    /** Where we are installing to. */
    @Nonnull private final Path targetDir;
    
    /** The files we will delete if they created on upgrade. */
    @Nonnull private final String[][] deleteAfterUpgrades = { { "credentials", "secrets.properties", }, };

    /** The module IDs which are enabled. */
    @Nonnull private Set<String> enabledModules;

    /** Whether the IdP properties file exists.*/
    private boolean idpPropertiesPresent;

    /** Whether the LDAP properties file exists.*/
    private boolean ldapPropertiesPresent;

    /** Old Version. */
    private String oldVersion;
    
    /** The files to delete after an upgrade. */
    @NonnullAfterInit private List<Path> pathsToDelete;

    /** The classloader for "us plus the plugins in the target". */
    @Nullable private ClassLoader installedPluginsLoader;

    /** Constructor.
     * @param installerProps the installer situation.
     */
    public CurrentInstallState(final InstallerProperties installerProps) {
        targetDir = installerProps.getTargetDir();
        enabledModules = CollectionSupport.emptySet();
    }

    /** Work out what the "current" install state is (before we do any more work).
     * @throws ComponentInitializationException if we find a strange state
     */
    private void findPreviousVersion() throws ComponentInitializationException {
        final Path conf = targetDir.resolve("conf");
        final Path currentInstall = targetDir.resolve("dist").resolve(InstallerSupport.VERSION_NAME);
        final Path ip = conf.resolve("idp.properties");
        if (!Files.exists(ip)) {
            // No idp.properties, no install
            log.debug("No idp.properties file detetected at {} .  Inferring a clean install", ip);
            oldVersion = null;
        } else if (!Files.exists(currentInstall)) {
            throw new ComponentInitializationException("V3 Installation detected");
        } else {
            final Properties vers = new Properties(1);
            try {
                vers.load(new FileInputStream(currentInstall.toFile()));
            } catch (final IOException e) {
                LoggerFactory.getLogger(CurrentInstallState.class).
                    error("Could not load {}", currentInstall.toAbsolutePath(), e);
                throw new ComponentInitializationException(e);
            }
            oldVersion = vers.getProperty(InstallerSupport.VERSION_NAME);
            if (null == oldVersion) {
                LoggerFactory.getLogger(CurrentInstallState.class).
                error("Failed loading {}", currentInstall.toAbsolutePath());
                throw new ComponentInitializationException("File " + InstallerSupport.VERSION_NAME +
                        " did not contain property " + InstallerSupport.VERSION_NAME);
            }
            log.debug("Previous version {}", oldVersion);
        }
    }

    /**
     * Populate {{@link #enabledModules} from the current classpath and the new IdP home.
     */
    private void findEnabledModules() {
        if (getInstalledVersion() == null) {
            return;
        }
        final String td = targetDir.toString();
        assert td != null;
        final ModuleContext moduleContext = new ModuleContext(td);
        enabledModules = new HashSet<>();
        final Iterator<IdPModule> modules = ServiceLoader.load(IdPModule.class).iterator();

        while (modules.hasNext()) {
            try {
                final IdPModule module = modules.next();
                if (module.isEnabled(moduleContext)) {
                    log.debug("Detected enabled Module {}", module.getId());
                    enabledModules.add(module.getId());
                } else {
                    log.debug("Detected disabled Module {}", module.getId());
                }
            } catch (final ServiceConfigurationError e) {
                log.error("Error loading modules", e);
            }
        }
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        idpPropertiesPresent = Files.exists(targetDir.resolve("conf").resolve("idp.properties"));
        ldapPropertiesPresent = Files.exists(targetDir.resolve("conf").resolve("ldap.properties"));
        if (Files.exists(targetDir.resolve("system"))) {
            log.error("'system' folder exists emove this and make appropriate changes to web.xml before proceeding");
            log.error("see https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3199500925/Upgrading");
            throw new ComponentInitializationException("'systems folder exists");
        }
        findPreviousVersion();
        findEnabledModules();

        if (null == getInstalledVersion()) {
            // New install.  We need all files
            pathsToDelete = CollectionSupport.emptyList();
        } else {
            pathsToDelete = new ArrayList<>();
            for (int i = 0; i < deleteAfterUpgrades.length; i++) {
                Path p = targetDir;
                final String[] paths = deleteAfterUpgrades[i];
                for (int j = 0; j < paths.length; j++) {
                    p = p.resolve(paths[j]);
                }
                if (!Files.exists(p)) {
                    // doesn't exist,  Candidate for deletion
                    pathsToDelete.add(p);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Nullable public String getInstalledVersion() {
        return oldVersion;
    }
    
    /** {@inheritDoc} */
    public boolean isIdPPropertiesPresent() {
        return idpPropertiesPresent;
    }

    /** {@inheritDoc} */
    public boolean isLDAPPropertiesPresent() {
        return ldapPropertiesPresent;
    }

    /** {@inheritDoc} */
    public @Nonnull List<Path> getPathsToBeDeleted() {
        assert pathsToDelete != null;
        return pathsToDelete;
    }

    /** {@inheritDoc} */
    @Nonnull public Collection<String> getEnabledModules() {
        return enabledModules;
    }

    /** {@inheritDoc} */
    @Nullable public synchronized ClassLoader getInstalledPluginsLoader() {

        if (installedPluginsLoader != null) {
            return installedPluginsLoader;
        }
        
        final Path libs = targetDir.resolve("dist").resolve("plugin-webapp").resolve("WEB-INF").resolve("lib");
        final URL[] urls;
        if (Files.exists(libs)) {
            try {
                final List<Path> copiedFiles = new ArrayList<>();
                final FileVisitor<Path> visitor = new SimpleFileVisitor<>() {
                    public FileVisitResult visitFile(final Path file,
                            final BasicFileAttributes attrs) throws IOException {
                        copiedFiles.add(file);
                        return FileVisitResult.CONTINUE;
                    }
                };
                try (final DirectoryStream<Path> webInfLibs = Files.newDirectoryStream(libs)) {
                    for (final Path jar : webInfLibs) {
                        visitor.visitFile(jar, null);
                    }
                }
                urls = copiedFiles.
                        stream().
                        map( path -> {
                            try {
                                return path.toUri().toURL();
                            } catch (final MalformedURLException e1) {
                                throw new BuildException(e1);
                            }
                        }).
                        toArray(URL[]::new);
            } catch (final IOException e) {
                log.error("Error finding Plugins' classpath", e);
                installedPluginsLoader = getClass().getClassLoader();
                return installedPluginsLoader;
            }
        } else {
            urls = new URL[0];
        }
        installedPluginsLoader = new URLClassLoader(urls);
        return installedPluginsLoader;
    }
}

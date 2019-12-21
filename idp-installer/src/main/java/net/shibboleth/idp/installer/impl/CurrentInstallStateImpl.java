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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.installer.CurrentInstallState;
import net.shibboleth.idp.installer.InstallerProperties;
import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.idp.spring.IdPPropertiesApplicationContextInitializer;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** Tells the installers about the current install state. */
public final class CurrentInstallStateImpl extends AbstractInitializableComponent implements CurrentInstallState {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CurrentInstallStateImpl.class);

    /** Where we are installing to. */
    private final Path targetDir;
    
    /** The files we will delete if they created on upgrade. */
    private final String[][] deleteAfterUpgrades = { { "credentials", "secrets.properties", },
                                                   }; 

    /** Whether the IdP properties file exists.*/
    private boolean idpPropertiesPresent;

    /** Whether the LDAP properties file exists.*/
    private boolean ldapPropertiesPresent;

    /** Whether the secrets properties file exists.*/
    private boolean secretsPropertiesPresent;

    /** Old Version. */
    private String oldVersion;
    
    /** Previous props. */
    private Properties props;
    
    /** The files to delete after an upgrade. */
    @NonnullAfterInit private List<Path> pathsToDelete;

    /** Constructor.
     * @param installerProps the installer situation.
     */
    public CurrentInstallStateImpl(final InstallerProperties installerProps) {
        targetDir = installerProps.getTargetDir();
    }

    /** Work out what the "current" install state is (before we do any more work).
     * @throws ComponentInitializationException if we find a strange state
     */
    private void findPreviousVersion() throws ComponentInitializationException {
        final Path conf = targetDir.resolve("conf");
        final Path currentInstall = targetDir.resolve("dist").resolve(InstallerSupport.VERSION_NAME);
        if (!Files.exists(conf.resolve("relying-party.xml"))) {
            // No relying party, no install
            log.debug("No relying-party.xml file detetected.  Inferring a clean install");
            oldVersion = null;
        } else if (!Files.exists(conf.resolve("idp.properties"))) {
            throw new ComponentInitializationException("V2 Installation detected");
        } else if (!Files.exists(currentInstall)) {
            log.debug("No {} file detetected.  Inferring a V3 install", currentInstall);
            oldVersion= V3_VERSION;
        } else {
            final Properties vers = new Properties(1);
            try {
                vers.load(new FileInputStream(currentInstall.toFile()));
            } catch (final IOException e) {
                LoggerFactory.getLogger(CurrentInstallStateImpl.class).
                    error("Could not load {}", currentInstall.toAbsolutePath(), e);
                throw new ComponentInitializationException(e);
            }
            oldVersion = vers.getProperty(InstallerSupport.VERSION_NAME);
            if (null == oldVersion) {
                LoggerFactory.getLogger(CurrentInstallStateImpl.class).
                error("Failed loading {}", currentInstall.toAbsolutePath());
                throw new ComponentInitializationException("File " + InstallerSupport.VERSION_NAME +
                        " did not contain property " + InstallerSupport.VERSION_NAME);
            }
            log.debug("Previous version {}", oldVersion);
        }
    }

    /** Populate {{@link #props} from idp.properties and other files pointed to by
     * {@value IdPPropertiesApplicationContextInitializer#IDP_ADDITIONAL_PROPERTY}.
     */
    private void setupPreviousProps() {
        if (!isIdPPropertiesPresent()) {
            return ;
        }
        props = new Properties();
        try {
            final File idpPropsFile = targetDir.resolve("conf").resolve("idp.properties").toFile();
            final InputStream idpPropsStream = new FileInputStream(idpPropsFile);
            props.load(idpPropsStream);
        } catch (final IOException e) {
            log.error("Error loading idp.properties", e);
            return;
        }
        final String additionalSources =
                props.getProperty(IdPPropertiesApplicationContextInitializer.IDP_ADDITIONAL_PROPERTY);
        if (additionalSources != null) {
            final String[] sources = additionalSources.split(",");
            for (final String source : sources) {
                final String trimmedSource = StringSupport.trimOrNull(source);
                if (trimmedSource == null) {
                    continue;
                }
                final Path path = Path.of(targetDir + trimmedSource);
                try {
                    final InputStream stream = new FileInputStream(path.toFile());
                    props.load(stream);
                } catch (final IOException e) {
                    log.error("Error loading {}", path, e);
                }
            }
        }
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        idpPropertiesPresent = Files.exists(targetDir.resolve("conf").resolve("idp.properties"));
        ldapPropertiesPresent = Files.exists(targetDir.resolve("conf").resolve("ldap.properties"));
        secretsPropertiesPresent = Files.exists(targetDir.resolve("conf").resolve("secrets.properties"));
        findPreviousVersion();
        setupPreviousProps();

        if (null == getInstalledVersion()) {
            // New install.  We need all files
            pathsToDelete = Collections.emptyList();
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
    public boolean isSecretsPropertiesPresent() {
        return secretsPropertiesPresent;
    }

    /** {@inheritDoc} */
    @Nullable public Properties getCurrentlyInstalledProperties() {
        return props;
    }

    /** {@inheritDoc} */
    public List<Path> getPathsToBeDeleted() {
        return pathsToDelete;
    }
}

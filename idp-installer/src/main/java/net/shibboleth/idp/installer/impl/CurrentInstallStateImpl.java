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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.installer.CurrentInstallState;
import net.shibboleth.idp.installer.InstallerProperties;
import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/** Tells the installers about the current install state. */
public final class CurrentInstallStateImpl extends AbstractInitializableComponent implements CurrentInstallState {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CurrentInstallStateImpl.class);

    /** Where we are installing to. */
    private final Path targetDir;

    /** Whether the IdP properties file exists.*/
    private boolean idpPropertiesPresent;

    /** Whether the LDAP properties file exists.*/
    private boolean ldapPropertiesPresent;

    /** Old Version. */
    private String oldVersion;

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

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        idpPropertiesPresent = Files.exists(targetDir.resolve("conf").resolve("idp.properties"));
        ldapPropertiesPresent = Files.exists(targetDir.resolve("conf").resolve("ldap.properties"));
        findPreviousVersion();
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
}

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

package net.shibboleth.idp.installer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/** Tells the installers about the current install state. */
final class CurrentInstallState extends AbstractInitializableComponent {

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
    protected CurrentInstallState(final InstallerProperties installerProps) {
        targetDir = installerProps.getTargetDir();
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        idpPropertiesPresent = Files.exists(targetDir.resolve("conf").resolve("idp.properties"));
        ldapPropertiesPresent = Files.exists(targetDir.resolve("conf").resolve("ldap.properties"));
        final Path conf = targetDir.resolve("conf");
        if (!Files.exists(conf.resolve("relying-party.xml"))) {
            // No relying party, no install
            oldVersion = null;
            return;
        }
        
        if (!Files.exists(conf.resolve("idp.properties"))) {
            throw new ComponentInitializationException("V2 Installation detected");
        }

        final Path currentInstall = targetDir.resolve("dist").resolve(InstallerSupport.VERSION_NAME);
        if (!Files.exists(currentInstall)) {
            oldVersion= "3";
            return;
        }
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
    }

    /** What is the installer version.
     * @return "3" for a V3 install, null for a new install or the value we write during last install.
     * @throws BuildException if we find an inconsistency
     */
    @Nullable protected String getInstalledVersion() {
        return oldVersion;
    }
    
    /** Was idp.properties present in the target file when we started the install?
     * @return if it was.
     */
    protected boolean isIdPPropertiesPresent() {
        return idpPropertiesPresent;
    }

    /** Was ldapp.properties present in the target file when we started the install?
     * @return if it was.
     */
    protected boolean isLDAPPropertiesPresent() {
        return ldapPropertiesPresent;
    }
}

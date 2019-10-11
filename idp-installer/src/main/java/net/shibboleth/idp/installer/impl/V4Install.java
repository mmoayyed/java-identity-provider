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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Copy;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.Version;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

/** Code to do most of the V4 Install.
 */
public class V4Install extends AbstractInitializableComponent {

    /** Log. */
    private final Logger log = LoggerFactory.getLogger(V4Install.class);

    /** Installer Properties. */
    @Nonnull private final InstallerProperties installerProps;

    /** Current Install. */
    @Nonnull private final CurrentInstallState currentState;

    /** Constructor.
     * @param props The properties to drive the installs.
     * @param installState The current install.
     */
    public V4Install(@Nonnull final InstallerProperties props, @Nonnull final CurrentInstallState installState) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(props);
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(installState);
        installerProps = props;
        currentState = installState;
    }

    /** Method to do the work. It assumes that the distribution has been copied.
     * @throws BuildException if unexpected badness occurs.
     */
    public void execute() throws BuildException {
        handleVersioning();
        // To keep the UI order the same as the V3 Installer
        //installerProps.getEntityID();
        //installerProps.getScope();
        createUserDirectories();
        final KeyManagement keys = new KeyManagement(installerProps, currentState);
        keys.execute();
        populatePropertyFiles(keys.isCreatedSealer());
        handleEditWebApp();
        populateUserDirectories();
        generateMetadata();
        reprotect();
    }

    /** Report the to be installed and (if there is one) current versions. 
     * Write to be installed version to the dist folder.
     * @throws BuildException if the write fails
     */
    protected void handleVersioning() throws BuildException {
        final String installedVersion = currentState.getInstalledVersion();
        String currentVersion = Version.getVersion();
        if (null == currentVersion) {
            currentVersion = "4Generic";
        }
        if (null == installedVersion) {
            log.info("New Install.  Version: {}", currentVersion);
        } else if (currentVersion == installedVersion) {
            log.info("Reinstall of version {}", currentVersion);
        } else {
            log.info("Update from version {} to version {}", installedVersion, currentVersion);
        }
        try {
            final Properties vers = new Properties();
            vers.setProperty(InstallerSupport.VERSION_NAME, currentVersion);
            vers.setProperty(InstallerSupport.PREVIOUS_VERSION_NAME, installedVersion==null?"":installedVersion);
            final OutputStream out = new FileOutputStream(
                    installerProps.getTargetDir().resolve("dist").resolve(InstallerSupport.VERSION_NAME).toFile());
            vers.store(out, "Version file written at " + Instant.now());
        } catch (final IOException e) {
            throw new BuildException("Couldn't write versiining information", e);
        }
    }

    /** Create (if they do not exist) the user editable folders, suitable for
     * later population during update or install.
     * @throws BuildException if badness occurs
     */
    protected void createUserDirectories() throws BuildException {
        final Path target = installerProps.getTargetDir();
        InstallerSupport.createDirectory(target.resolve("conf"));
        InstallerSupport.createDirectory(target.resolve("credentials"));
        InstallerSupport.createDirectory(target.resolve("flows"));
        InstallerSupport.createDirectory(target.resolve("logs"));
        InstallerSupport.createDirectory(target.resolve("messages"));
        InstallerSupport.createDirectory(target.resolve("metadata"));
        InstallerSupport.createDirectory(target.resolve("views"));
        InstallerSupport.createDirectory(target.resolve("war"));
    }
    
    /** Create the properties we need to replace when we merge idp.properties.
     * @param sealerCreated have we just created a sealer
     * @return what we need to replace
     */
    private Properties getIdPReplacements(final boolean sealerCreated) {
        final Properties result = new Properties();
        if (sealerCreated) {
            result.setProperty("idp.sealer.storePassword", installerProps.getSealerPassword());
            result.setProperty("idp.sealer.keyPassword", installerProps.getSealerPassword());
        }
        result.setProperty("idp.entityID", installerProps.getEntityID());
        result.setProperty("idp.scope", installerProps.getScope());
        return result;
    }

    /** Create (if they do not exist) propertyFiles. (idp.properties, ldap.properties).
     * This *MUST* happen before {@link #populateUserDirectories(InstallerProperties)} or it will not be effective.
     * Note that in V3 serice.properties and nameid.properties but we do not any more.
     * @param sealerCreated have we just created a sealer
     * @throws BuildException if badness occurs
     */
    // CheckStyle: CyclomaticComplexity OFF
    protected void populatePropertyFiles(final boolean sealerCreated) throws BuildException {
        final Path conf = installerProps.getTargetDir().resolve("conf");
        final Path dstConf = installerProps.getTargetDir().resolve("dist").resolve("conf");
        if (!currentState.isIdPPropertiesPresent()) {
            // We have to populate it
            try {
                final Path target = conf.resolve("idp.properties");
                if (Files.exists(target)) {
                    throw new BuildException("Internal error - idp.properties");
                }
                final File mergeFile = installerProps.getIdPMergePropertiesFile();
                final Path source = dstConf.resolve("idp.properties");
                if (!Files.exists(source)) {
                    throw new BuildException("missing idp.properties in dist");
                }
                final PropertiesWithComments propertiesToReWrite = new PropertiesWithComments();
                final Properties replacements;
                if (mergeFile != null) {
                    log.debug("Creating {} from {} and {}", target, source, mergeFile);
                    replacements = new Properties();
                    replacements.load(new FileInputStream(mergeFile));
                } else {
                    replacements = getIdPReplacements(sealerCreated);
                    log.debug("Creating {} from {} and {}", target, source, replacements);
                }
                propertiesToReWrite.load(new FileInputStream(source.toFile()));
                propertiesToReWrite.replaceProperties(replacements);
                propertiesToReWrite.store(new FileOutputStream(target.toFile()));
            } catch (final IOException e) {
                throw new BuildException("Failed to generate idp.properties", e);
            }
        }

        final File ldapMergeFile = installerProps.getLDAPMergePropertiesFile();
        if (ldapMergeFile != null && !currentState.isLDAPPropertiesPresent() ) {
            try {
                final Path target = conf.resolve("ldap.properties");
                if (Files.exists(target)) {
                    throw new BuildException("Internal error - ldap.properties");
                }
                final Path source = dstConf.resolve("ldap.properties");
                if (!Files.exists(source)) {
                    throw new BuildException("missing ldap.properties in dist");
                }
                log.debug("Creating {} from {} and {}", target, source, ldapMergeFile);
                final PropertiesWithComments propertiesToReWrite = new PropertiesWithComments();
                final Properties replacements = new Properties();
                replacements.load(new FileInputStream(ldapMergeFile));
                propertiesToReWrite.load(new FileInputStream(source.toFile()));
                propertiesToReWrite.replaceProperties(replacements);
                propertiesToReWrite.store(new FileOutputStream(target.toFile()));
            } catch (final IOException e) {
                throw new BuildException("Failed to generate ldap.properties", e);
            }
        }
    }
    // CheckStyle: CyclomaticComplexity ON

    /** Create and populate (if it does not exist) edit-webapp.
     * @throws BuildException if badness occurs
     */
    protected void handleEditWebApp() throws BuildException {
        final Path editWebApp = installerProps.getTargetDir().resolve("edit-webapp");
        if (Files.exists(editWebApp)) {
            return;
        }
        InstallerSupport.createDirectory(editWebApp);
        final Path css = editWebApp.resolve("css");
        InstallerSupport.createDirectory(css);
        final Path images = editWebApp.resolve("images");
        InstallerSupport.createDirectory(images);
        InstallerSupport.createDirectory(editWebApp.resolve("WEB-INF"));
        InstallerSupport.createDirectory(editWebApp.resolve("WEB-INF").resolve("lib"));
        InstallerSupport.createDirectory(editWebApp.resolve("WEB-INF").resolve("classes"));
        final Path distEditWebApp =  installerProps.getTargetDir().resolve("dist").resolve("webapp");
        final Copy cssCopy = InstallerSupport.getCopyTask(distEditWebApp.resolve("css"), css);
        cssCopy.setFailOnError(false);
        cssCopy.execute();
        final Copy imagesCopy = InstallerSupport.getCopyTask(distEditWebApp.resolve("images"), images);
        imagesCopy.setFailOnError(false);
        imagesCopy.execute();       
    }

    /** Create and populate (if they not exist) the "user visible" folders.
     * (conf, flows, messages, views, logs)
     * @throws BuildException if badness occurs
     */
    protected void populateUserDirectories() throws BuildException {
        final Path targetBase = installerProps.getTargetDir();
        final Path distBase = targetBase.resolve("dist");
        InstallerSupport.copyDirIfNotPresent(distBase.resolve("conf"), targetBase.resolve("conf"));
        InstallerSupport.copyDirIfNotPresent(distBase.resolve("flows"), targetBase.resolve("flows"));
        InstallerSupport.copyDirIfNotPresent(distBase.resolve("views"), targetBase.resolve("views"));
        InstallerSupport.copyDirIfNotPresent(distBase.resolve("messages"), targetBase.resolve("messages"));
        InstallerSupport.createDirectory(targetBase.resolve("logs"));
    }
    
    /** Create and populate (if it does not exist) the "metadata/idp-metadata.xml" file.
     * @throws BuildException if badness occurs
     */
    protected void generateMetadata() throws BuildException {
        final Path parentDir = installerProps.getTargetDir().resolve("metadata");
        final Path metadataFile = parentDir.resolve("idp-metadata");
        if (Files.exists(metadataFile)) {
            return;
        }
        log.warn("Metadata Implementation still pending");        
    }

    /** Set the protection on the files.
     * @throws BuildException if badness occurs
     */
    protected void reprotect() throws BuildException {
        log.warn("Reprotect Implementation still pending");
    }

}

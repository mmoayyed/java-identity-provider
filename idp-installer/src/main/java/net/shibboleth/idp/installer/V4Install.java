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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Copy;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.idp.Version;
import net.shibboleth.idp.spring.IdPPropertiesApplicationContextInitializer;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.security.BasicKeystoreKeyStrategyTool;
import net.shibboleth.utilities.java.support.security.SelfSignedCertificateGenerator;

/** Code to do most of the V4 Install.
 */
public class V4Install extends AbstractInitializableComponent {

    /** Log. */
    private final Logger log = LoggerFactory.getLogger(V4Install.class);

    /** Installer Properties. */
    @Nonnull private final InstallerProperties installerProps;

    /** Current Install. */
    @Nonnull private final CurrentInstallState currentState;

    /** Key Manager. */
    @Nonnull private final KeyManagement keyManager;

    /** What will generate metadata? */
    private MetadataGenerator metadataGenerator;

    /** Constructor.
     * @param props The properties to drive the installs.
     * @param installState The current install.
     */
    public V4Install(@Nonnull final InstallerProperties props, @Nonnull final CurrentInstallState installState) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(props);
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(installState);
        installerProps = props;
        currentState = installState;
        keyManager = new KeyManagement(installerProps, currentState);
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        keyManager.initialize();
        if (metadataGenerator == null) {
            log.warn("No MetadataGenerator configured.");
        }
    }

    /** Method to do the work. It assumes that the distribution has been copied.
     * @throws BuildException if unexpected badness occurs.
     */
    public void execute() throws BuildException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        handleVersioning();
        checkPreConditions();

        createUserDirectories();
        keyManager.execute();
        populatePropertyFiles(keyManager.isCreatedSealer());
        handleEditWebApp();
        populateUserDirectories();
        deleteSpuriousFiles();
        generateMetadata();
        reprotect();
    }

    /** Set the {@link MetadataGenerator}.
     * @param what what to set.  This need not have been initialized yet
     * {@link MetadataGenerator#setOutput(File)} and
     * {@link MetadataGenerator#setParameters(MetadataGeneratorParameters)} are called
     * prior to initialization.
     */
    public void setMetadataGenerator(final MetadataGenerator what) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        metadataGenerator = what;
    }
    
    /** Check for any preconditions to the install. 
     * @throws BuildException if one is broken.
     */
    protected void checkPreConditions() throws BuildException {
        final Properties props = currentState.getCurrentlyInstalledProperties();
        if (props != null) {
            final String value = StringSupport.trimOrNull(props.getProperty("idp.service.relyingparty.resources"));
            if ("shibboleth.LegacyRelyingPartyResolverResources".equals(value)) {
                log.error("Install failed: system will not work after V4 upgrade");
                log.error("idp.service.relyingparty.resources is set to shibboleth.RelyingPartyResolverResources");
                throw new BuildException("Install failed: system will not work after V4 upgrade");
            }
        }
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
            final Path versFile = installerProps.getTargetDir().resolve("dist").resolve(InstallerSupport.VERSION_NAME);
            if (Files.exists(versFile) ) {
                InstallerSupport.setReadOnly(versFile, false);
            }
            final Properties vers = new Properties();
            vers.setProperty(InstallerSupport.VERSION_NAME, currentVersion);
            vers.setProperty(InstallerSupport.PREVIOUS_VERSION_NAME, installedVersion==null?"":installedVersion);
            try(final OutputStream out = new FileOutputStream(versFile.toFile())) {
                vers.store(out, "Version file written at " + Instant.now());
            }
        } catch (final IOException e) {
            log.error("Couldn't write version file: {}", e.getMessage());
            throw new BuildException("Couldn't write versioning information", e);
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
        result.setProperty("idp.entityID", installerProps.getEntityID());
        result.setProperty("idp.scope", installerProps.getScope());
        return result;
    }

    /** Create (if they do not exist) propertyFiles. (idp.properties, ldap.properties).
     * This *MUST* happen before {@link #populateUserDirectories()} or it will not be effective.
     * Note that in V3 serice.properties and nameid.properties but we do not any more.
     * @param sealerCreated have we just created a sealer
     * @throws BuildException if badness occurs
     */
    // CheckStyle: CyclomaticComplexity|MethodLength OFF
    protected void populatePropertyFiles(final boolean sealerCreated) throws BuildException {
        final Set<String> blackList = Set.of(
                "idp.sealer.storePassword",
                "idp.sealer.keyPassword",
                "idp.authn.LDAP.bindDNCredential",
                "idp.attribute.resolver.LDAP.bindDNCredential",
                "idp.persistentId.salt");

        final Path conf = installerProps.getTargetDir().resolve("conf");
        final Path dstConf = installerProps.getTargetDir().resolve("dist").resolve("conf");
        if (!currentState.isIdPPropertiesPresent()) {
            // We have to populate it
            try {
                final Path target = conf.resolve("idp.properties");
                if (Files.exists(target)) {
                    throw new BuildException("Internal error - idp.properties");
                }
                final Path mergePath = installerProps.getIdPMergeProperties();
                final Path source = dstConf.resolve("idp.properties");
                if (!Files.exists(source)) {
                    throw new BuildException("missing idp.properties in dist");
                }
                final PropertiesWithComments propertiesToReWrite = new PropertiesWithComments(blackList);
                final Properties replacements;
                if (mergePath != null) {
                    log.debug("Creating {} from {} and {}", target, source, mergePath);
                    replacements = new Properties();
                    final File mergeFile = mergePath.toFile();
                    if (!installerProps.isNoTidy()) {
                        mergeFile.deleteOnExit();
                    }
                    try (final FileInputStream stream = new FileInputStream(mergeFile)) {
                        replacements.load(stream);
                    }
                } else {
                    replacements = getIdPReplacements(sealerCreated);
                    log.debug("Creating {} from {} and {}", target, source, replacements.keySet());
                }
                try (final FileInputStream stream = new FileInputStream(source.toFile())) {
                    propertiesToReWrite.load(stream);
                }
                propertiesToReWrite.replaceProperties(replacements);
                try (final FileOutputStream stream = new FileOutputStream(target.toFile())) {
                    propertiesToReWrite.store(stream);
                }
            } catch (final IOException e) {
                throw new BuildException("Failed to generate idp.properties", e);
            }
        }

        final Path ldapMergePath = installerProps.getLDAPMergeProperties();
        if (ldapMergePath != null && !currentState.isLDAPPropertiesPresent() ) {
            log.debug("Merging {} with ldap.properties", ldapMergePath);
            try {
                final Path target = conf.resolve("ldap.properties");
                if (Files.exists(target)) {
                    throw new BuildException("Internal error - ldap.properties");
                }
                final Path source = dstConf.resolve("ldap.properties");
                if (!Files.exists(source)) {
                    throw new BuildException("missing ldap.properties in dist");
                }
                log.debug("Creating {} from {} and {}", target, source, ldapMergePath);
                final PropertiesWithComments propertiesToReWrite = new PropertiesWithComments(blackList);
                final Properties replacements = new Properties();
                final File mergeFile = ldapMergePath.toFile();
                if (!installerProps.isNoTidy()) {
                    mergeFile.deleteOnExit();
                }
                try (final FileInputStream stream = new FileInputStream(mergeFile)) {
                    replacements.load(stream);
                }
                try (final FileInputStream stream = new FileInputStream(source.toFile())) {
                    propertiesToReWrite.load(stream);
                }
                propertiesToReWrite.replaceProperties(replacements);
                try (final FileOutputStream stream = new FileOutputStream(target.toFile())) {
                    propertiesToReWrite.store(stream);
                }
            } catch (final IOException e) {
                throw new BuildException("Failed to generate ldap.properties", e);
            }
        }

        if (null == currentState.getInstalledVersion()) {
            log.debug("Detected a new Install.  Creating secrets.properties.");
            final Path secrets = installerProps.getTargetDir().resolve("credentials").resolve("secrets.properties");
            try (final FileWriter fileWriter = new FileWriter(secrets.toFile());
                 final BufferedWriter out = new BufferedWriter(fileWriter)) {
                
                out.write("# This is a reserved spot for most properties containing passwords or other secrets.");
                out.newLine();
                out.write("# Created by install at " + Instant.now());
                out.newLine();
                out.newLine();
                out.write("# Access to internal AES encryption key");
                out.newLine();
                final String password;
                if (sealerCreated) {
                    password = installerProps.getSealerPassword();
                } else {
                    password = "password";
                }
                out.write("idp.sealer.storePassword = " + password);
                out.newLine();
                out.write("idp.sealer.keyPassword = " + password);
                out.newLine();
                out.newLine();
                String ldapPassword = installerProps.getLDAPPassword();
                if (null == ldapPassword) {
                    ldapPassword = "myServicePassword";
                }
                out.write("# Default access to LDAP authn and attribute stores. ");
                out.newLine();
                out.write("idp.authn.LDAP.bindDNCredential              = " + ldapPassword);
                out.newLine();
                out.write("idp.attribute.resolver.LDAP.bindDNCredential " +
                          "= %{idp.authn.LDAP.bindDNCredential:undefined}");
                out.newLine();
                out.newLine();
                out.write("# Salt used to generate persistent/pairwise IDs, must be kept secret");
                out.newLine();
                out.write("#idp.persistentId.salt = changethistosomethingrandom");
                out.newLine();
            } catch (final IOException e) {
                throw new BuildException("Failed to generate secrets.properties", e);
            }
        } else if (CurrentInstallState.V3_VERSION.equals(currentState.getInstalledVersion())) {
            log.debug("Detected a V3 to V4 update.");
        }
    }
    // CheckStyle: CyclomaticComplexity|MethodLength ON

    /** Create and populate (if it does not exist) edit-webapp.
     * @throws BuildException if badness occurs
     */
    protected void handleEditWebApp() throws BuildException {
        final Path editWebApp = installerProps.getTargetDir().resolve("edit-webapp");
        if (Files.exists(editWebApp)) {
            return;
        }
        final Path suppliedInput = installerProps.getInitialEditWeb();
        if (suppliedInput != null) {
            final Copy copy = InstallerSupport.getCopyTask(suppliedInput, editWebApp);
            copy.setFailOnError(false);
            copy.execute();
        } else {
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
    }

    /** Create and populate (if they not exist) the "user visible" folders.
     * (conf, flows, messages, views, logs)
     * @throws BuildException if badness occurs
     */
    protected void populateUserDirectories() throws BuildException {
        final Path targetBase = installerProps.getTargetDir();
        final Path distBase = targetBase.resolve("dist");
        final Path preConfPath = installerProps.getConfPreOverlay();
        if (preConfPath != null) {
            InstallerSupport.copyDirIfNotPresent(preConfPath, targetBase.resolve("conf"));
        }
        InstallerSupport.copyDirIfNotPresent(distBase.resolve("conf"), targetBase.resolve("conf"));
        InstallerSupport.copyDirIfNotPresent(distBase.resolve("flows"), targetBase.resolve("flows"));
        InstallerSupport.copyDirIfNotPresent(distBase.resolve("views"), targetBase.resolve("views"));
        InstallerSupport.copyDirIfNotPresent(distBase.resolve("messages"), targetBase.resolve("messages"));
        InstallerSupport.createDirectory(targetBase.resolve("logs"));
    }
    
    /** Delete those files which were created but not needed.
     * @throws BuildException if badness occurs
     */
    protected void deleteSpuriousFiles() throws BuildException {
        for (final Path p : currentState.getPathsToBeDeleted()) {
            if (!Files.exists(p)) {
                log.trace("File to be deleted {} was not created", p.getFileName());
            } else {
                try {
                    Files.delete(p);
                } catch (final IOException e) {
                    log.debug("Delete failed", e);
                }
            }
        }
    }

    /** Create and populate (if it does not exist) the "metadata/idp-metadata.xml" file.
     * @throws BuildException if badness occurs
     */
    protected void generateMetadata() throws BuildException {
        if (metadataGenerator == null) {
            log.debug("No Metadata generator specified.");
            return;
        }

        final Path parentDir = installerProps.getTargetDir().resolve("metadata");
        final Path metadataFile = parentDir.resolve("idp-metadata.xml");
        if (Files.exists(metadataFile)) {
            log.debug("Metadata file {} exists", metadataFile.toString());
            return;
        }
        final Resource resource = new ClassPathResource("net/shibboleth/idp/installer/metadata-generator.xml");
        final GenericApplicationContext context = new ApplicationContextBuilder()
                .setName(MetadataGenerator.class.getName())
                .setServiceConfigurations(Collections.singletonList(resource))
                .setContextInitializer(new Initializer())
                .build();

        final MetadataGeneratorParameters parameters = context.getBean("IdPConfiguration",
                MetadataGeneratorParameters.class);

        log.info("Creating Metadata to {}", metadataFile);
        log.debug("Parameters {}", parameters);
        metadataGenerator.setOutput(metadataFile.toFile());
        metadataGenerator.setParameters(parameters);
        try {
            metadataGenerator.initialize();
        } catch (final ComponentInitializationException e) {
            throw new BuildException(e);
        }
        metadataGenerator.generate();
    }

    /** Set the protection on the files.
     * @throws BuildException if badness occurs
     */
    protected void reprotect() throws BuildException {
        InstallerSupport.setReadOnly(installerProps.getTargetDir().resolve("dist"), true);
        InstallerSupport.setReadOnly(installerProps.getTargetDir().resolve("system"), true);

        if (installerProps.isSetGroupAndMode()) {
            InstallerSupport.setMode(installerProps.getTargetDir().resolve("bin"), "755", "**/*.sh");
            InstallerSupport.setMode(installerProps.getTargetDir().resolve("system"), "444", "**/*");
            InstallerSupport.setMode(installerProps.getTargetDir().resolve("dist"), "444", "**/*");
            if (currentState.getInstalledVersion() == null) {
                InstallerSupport.setMode(installerProps.getTargetDir().resolve("credentials"),
                        installerProps.getCredentialsKeyFileMode(), "**/*");
                final String group = installerProps.getCredentialsGroup();
                if (group != null) {
                    InstallerSupport.setGroup(installerProps.getTargetDir().resolve("credentials"), group, "**/*");
                }
            }
        }
    }

    /**
     * Create (if needs be) all the keys needed by an install.
     */
    private class KeyManagement extends AbstractInitializableComponent {

        /** Properties for the job. */
        @Nonnull private final InstallerProperties installerProps;

        /** Current Install. */
        @Nonnull private final CurrentInstallState currentState;
        
        /** Did we create idp-signing.*?*/
        private boolean createdSigning;

        /** Did we create idp-encryption.*?*/
        private boolean createdEncryption;

        /** Did we create idp-backchannel.*?*/
        private boolean createdBackchannel;

        /** Did we create sealer.*?*/
        private boolean createdSealer;

        /** Constructor.
         * @param props The properties to drive the installs. 
         * @param installState - about where we installing into.
         */
        protected KeyManagement(@Nonnull final InstallerProperties props,
                @Nonnull final CurrentInstallState installState) {
            ComponentSupport.ifNotInitializedThrowUninitializedComponentException(props);
            ComponentSupport.ifNotInitializedThrowUninitializedComponentException(installState);
            installerProps = props;
            currentState = installState;
        }

        /** Create any keys that are needed.
         * @throws BuildException if badness occurs
         */
        protected void execute() throws BuildException {
            if (currentState.getInstalledVersion() != null) {
                log.debug("Skipping key generation");
                return;
            }
            ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
            createdSigning = generateKey("idp-signing");
            createdEncryption = generateKey("idp-encryption");
            generateKeyStore();
            generateSealer();
        }

        /**
         * Helper method for {@link #execute()} to generate a crt and key file.
         * 
         * @param fileBase the partial file name
         * @return true iff the file pair was created
         * @throws BuildException if badness occurrs.
         */
        private boolean generateKey(final String fileBase) throws BuildException {
            final Path credentials = installerProps.getTargetDir().resolve("credentials");
            final Path key = credentials.resolve(fileBase+".key");
            final Path crt = credentials.resolve(fileBase+".crt");

            if (Files.exists(key) && Files.exists(crt)) {
                if (!currentState.isIdPPropertiesPresent()) {
                    log.error("key files {} and {} exist but idp.properties does not", key, crt);
                    throw new BuildException("Invalid key file configuration");
                }
                log.debug("keys files {} and {} exist.  Not generating", key, crt);
                return false;
            } else if (currentState.isIdPPropertiesPresent()) {
                log.error("idp.properties exists but key files {} and/or {} do not", key, crt);
                throw new BuildException("Invalid key file configuration");
            } else if (Files.exists(key) || Files.exists(crt)) {
                log.error("One of two expected key files {} and {} exist", key, crt);
                throw new BuildException("Invalid key file configuration");
            } else {
              final SelfSignedCertificateGenerator generator = new SelfSignedCertificateGenerator();
              generator.setCertificateFile(crt.toFile());
              generator.setPrivateKeyFile(key.toFile());
              generator.setKeySize(installerProps.getKeySize());
              generator.setHostName(installerProps.getHostName());
              generator.setURISubjectAltNames(Collections.singletonList(installerProps.getSubjectAltName()));
              log.info("Creating {}, CN = {} URI = {}, keySize={}", fileBase,
                      installerProps.getHostName(), installerProps.getSubjectAltName(), installerProps.getKeySize());
              try {
                generator.generate();
                } catch (final Exception e) {
                    log.error("Error building {} files", fileBase, e);
                    throw new BuildException("Error Building Self Signed Cert", e);
                }
            }
            log.debug("... Done");
            return true;
        }

        /**
         * Helper method for {@link #execute()} to generate the backchannel keystore.
         * 
         * @throws BuildException if badness occurs.
         */
        private void generateKeyStore() {
            final Path credentials = installerProps.getTargetDir().resolve("credentials");
            final Path keyStore = credentials.resolve("idp-backchannel.p12");
            final Path crt = credentials.resolve("idp-backchannel.crt");

            if (Files.exists(keyStore) && Files.exists(crt)) {
                if (!currentState.isIdPPropertiesPresent()) {
                    log.error("Key store files {} and {} exist but idp.properties does not", keyStore, crt);
                    throw new BuildException("Invalid key file configuration");
                }
                log.debug("Keys store files {} and {} exist.  Not generating", keyStore, crt);
            } else if (currentState.isIdPPropertiesPresent()) {
                log.error("idp.properties exists but key store files {} and/or {} do not", keyStore, crt);
                throw new BuildException("Invalid key file configuration");
            } else if (Files.exists(keyStore) || Files.exists(crt)) {
                log.error("One of two expected key files {} and {} exist", keyStore, crt);
                throw new BuildException("Invalid key file configuration");
            } else {
                final SelfSignedCertificateGenerator generator = new SelfSignedCertificateGenerator();
                generator.setCertificateFile(crt.toFile());
                generator.setKeystoreFile(keyStore.toFile());
                generator.setKeySize(installerProps.getKeySize());
                generator.setHostName(installerProps.getHostName());
                generator.setURISubjectAltNames(Collections.singletonList(installerProps.getSubjectAltName()));
                generator.setKeystorePassword(installerProps.getKeyStorePassword());
                log.info("Creating backchannel keystore, CN = {} URI = {}, keySize={}",
                        installerProps.getHostName(), installerProps.getSubjectAltName(), installerProps.getKeySize());
                try {
                  generator.generate();
                  } catch (final Exception e) {
                      log.error("Error building backchannel ketsyore files", e);
                      throw new BuildException("Error Building Backchannel Key Store", e);
                  }
                createdBackchannel = true;
              }
        }

        /**
         * Helper method for {@link #execute()} to generate the Sealer.
         * 
         * @throws BuildException if badness occurs.
         */
        private void generateSealer() {
            final Path credentials = installerProps.getTargetDir().resolve("credentials");
            final Path sealer = credentials.resolve("sealer.jks");
            final Path versionFile = credentials.resolve("sealer.kver");

            if (Files.exists(sealer)  && Files.exists(versionFile)) {
                if (!currentState.isIdPPropertiesPresent()) {
                    log.error("Cookie encryption files {} and {} exist, but idp.properties does not",
                            sealer, versionFile);
                    throw new BuildException("Invalid Cookie encryption  file configuration");
                }
                log.debug("Cookie encryption files {} and {} exists.  Not generating.", sealer, versionFile);
            } else if (currentState.isIdPPropertiesPresent()) {
                log.error("idp.properties exists but cookie encryption files {} do not", sealer, versionFile);
                throw new BuildException("Invalid key file configuration");
            } else if (Files.exists(sealer) || Files.exists(versionFile)) {
                log.error("One of two expected cookie encryption file {} and {} exist", sealer, versionFile);
                throw new BuildException("Invalid cookie encryption file configuration");
            } else {
                final BasicKeystoreKeyStrategyTool generator = new BasicKeystoreKeyStrategyTool();
                generator.setKeystoreFile(sealer.toFile());
                generator.setVersionFile(versionFile.toFile());
                generator.setKeyAlias(installerProps.getSealerAlias());
                generator.setKeystorePassword(installerProps.getSealerPassword());
                log.info("Creating backchannel keystore, CN = {} URI = {}, keySize={}",
                        installerProps.getHostName(), installerProps.getSubjectAltName(),installerProps.getKeySize());
                try {
                    generator.changeKey();
                } catch (final Exception e) {
                    log.error("Error building cookie encryption files", e);
                    throw new BuildException("Error Building Cookie Encryption", e);
                }
                createdSealer = true;
            }
        }

        /** Did we create idp-signing.*?
         * @return whether we did
         */
        @SuppressWarnings("unused")
        public boolean isCreatedSigning() {
            return createdSigning;
        }

        /** Did we create idp-encryption.*?
         * @return whether we did
         */
        @SuppressWarnings("unused")
        public boolean isCreatedEncryption() {
            return createdEncryption;
        }

        /** Did we create idp-backchannel.*?
         * @return whether we did
         */
        @SuppressWarnings("unused")
        public boolean isCreatedBackchannel() {
            return createdBackchannel;
        }

        /** Did we create sealer.*?
         * @return whether we did
         */
        public boolean isCreatedSealer() {
            return createdSealer;
        }
    }

    /**
     * An {@link ApplicationContextInitializer} which knows about our idp.home and
     * also injects properties for the backchannel certificate and hostname.
     */
    private class Initializer extends IdPPropertiesApplicationContextInitializer {

        /** {@inheritDoc} */
        @Override @Nonnull public String selectSearchLocation(
                @Nonnull final ConfigurableApplicationContext applicationContext) {
            return installerProps.getTargetDir().toString();
        }

        /** {@inheritDoc} */
        @Override @Nonnull public String getSearchLocation() {
            return installerProps.getTargetDir().toString();
        }

        /** {@inheritDoc} */
        public void initialize(final ConfigurableApplicationContext applicationContext) {
            final Properties props = new Properties(2);
            props.setProperty("idp.backchannel.cert",
                    installerProps.getTargetDir().resolve("credentials").resolve("idp-backchannel.crt").toString());
            props.setProperty("idp.dnsname", installerProps.getHostName());
            appendPropertySource(applicationContext, "internal", props);
            super.initialize(applicationContext);
        }
    }
}

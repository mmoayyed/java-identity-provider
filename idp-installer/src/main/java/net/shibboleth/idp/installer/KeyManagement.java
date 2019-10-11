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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import javax.annotation.Nonnull;

import org.apache.tools.ant.BuildException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.security.BasicKeystoreKeyStrategyTool;
import net.shibboleth.utilities.java.support.security.SelfSignedCertificateGenerator;

/**
 * Create (if needs be) all the keys needed by an install.
 */
final class KeyManagement extends AbstractInitializableComponent {

    /** Log. */
    private final Logger log = LoggerFactory.getLogger(KeyManagement.class);

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
    protected KeyManagement(@Nonnull final InstallerProperties props, @Nonnull final CurrentInstallState installState) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(props);
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(installState);
        installerProps = props;
        currentState = installState;
    }

    /** Create any keys that are needed.
     * @throws BuildException if badness occurs
     */
    protected void execute() throws BuildException {
        createdSigning = generateKey("idp-signing");
        createdEncryption = generateKey("idp-encryption");
        generateKeyStore();
        generateSealer();
    }

    /** Helper method for {@link #manageKeys(InstallerProperties)} to generate a crt and key file.
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
          log.info("Creating {}, CN = {} URI = {}", fileBase,
                  installerProps.getHostName(), installerProps.getSubjectAltName());
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

    /** Helper method for {@link #manageKeys(InstallerProperties)} to generate the backchannel keystore.
     * @throws BuildException if badness occurrs.
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
            log.info("Creating backchannel keystore, CN = {} URI = {}",
                    installerProps.getHostName(), installerProps.getSubjectAltName());
            try {
              generator.generate();
              } catch (final Exception e) {
                  log.error("Error building backchannel ketsyore files", e);
                  throw new BuildException("Error Building Backchannel Key Store", e);
              }
            createdBackchannel = true;
          }
    }

    /** Helper method for {@link #manageKeys(InstallerProperties)} to generate the Sealer.
     * @throws BuildException if badness occurrs.
     */
    private void generateSealer() {
        final Path credentials = installerProps.getTargetDir().resolve("credentials");
        final Path sealer = credentials.resolve("sealer.jks");
        final Path versionFile = credentials.resolve("sealer.kver");

        if (Files.exists(sealer)  && Files.exists(versionFile)) {
            if (!currentState.isIdPPropertiesPresent()) {
                log.error("Cookie encryption files {} and {} exist but idp.properties does not", sealer, versionFile);
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
            log.info("Creating backchannel keystore, CN = {} URI = {}",
                    installerProps.getHostName(), installerProps.getSubjectAltName());
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
    public boolean isCreatedSigning() {
        return createdSigning;
    }

    /** Did we create idp-encryption.*?
     * @return whether we did
     */
    public boolean isCreatedEncryption() {
        return createdEncryption;
    }

    /** Did we create idp-backchannel.*?
     * @return whether we did
     */
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

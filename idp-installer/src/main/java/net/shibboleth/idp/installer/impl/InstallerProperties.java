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
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.installer.ant.impl.PasswordHandler;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

/** Class to describe simply parameterization and status of the installation.
 *
 * Mostly it interrogates the system environment, but will load a file if one
 * is supplied (via a property name).<p/>Property names :
 * <ul>
 * <li> idp.src.dir (update only):  Where to install from.  Default is basedir. Not used for build-war</li>
 * <li> idp.target.dir (all): where to install to.  Default is basedir for build war /opt/shibboleth-idp otherwise.</li>
 * <li> idp.host.name: If we are creating certificates</li>
 * <li> idp.uri.subject.alt.name: If we are creating certificates.  Defaulted</li>
 * <li> idp.sealer.password:</li>
 * <li> idp.sealer.alias:</li>
 * <li> idp.keystore.password:</li>
 * <li> idp.keysize: to change from the the default of 3072</li>
 * <li> idp.scope: The scope to assert.  If present this should also be present in idp.merge.properties</li>
 * <li> idp.merge.properties: The name of a property file to merge with idp.properties.  This file is only
 *    used when doing the initial create of idp.properties, and is deleted after processing</li>
 * <ul><li> if idp.noprompt is set, then this file should contain a line setting idp.entityID. </li>
 *       <li> if idp.sealer.password is set, then this file should contain a line setting
 *            idp.sealer.storePassword and idp.sealer.keyPassword</li>
 *       <li> if idp.scope is present, then this file should contain a line setting idp.scope
 *            services.merge.properties: The name of a property file to merge with services.properties</li>
 *       <li> if idp.is.V2 is set, then this file should contain a line setting
 *            idp.service.relyingparty.resources=shibboleth.LegacyRelyingPartyResolverResources</li></ul>
 * <li> nameid.merge.properties: The name of a property file to merge with saml-nameid.properties.
 *             If idp.is.V2 is set, then this file should contain lines enabling legacy nameid generation
 * <li> idp.property.file: The name of a property file to fill in some or all of the above.
              This file is deleted after processing.</li>
 * <li> idp.no.tidy: Do not delete the two above files (debug only)</li>
 * <li> idp.jetty.config: Copy jetty configuration from distribution (Unsupported)</li>
 * <li> ldap.merge.properties:  The name of a property file to merge with ldap.properties</li>
 * <li> idp.conf.filemode (default "600"): The permissions to mark the files in conf with (UNIX only).</li>
 * <li> idp.conf.credentials.filemode (default "600"): The permissions to mark the files in conf with (UNIX only).</li>
 * <li> idp.noprompt will cause a failure rather than a prompt.</li>
 * </ul>
 */
public class InstallerProperties extends AbstractInitializableComponent {

    /** The base directory, inherited and shared with ant. */
    public static final String ANT_BASE_DIR = "ant.home";

    /** The name of a property file to fill in some or all of the above. This file is deleted after processing. */
    public static final String PROPERTY_SOURCE_FILE = "idp.property.file";

    /** Where to install to.  Default is basedir */
    public static final String TARGET_DIR = "idp.target.dir";

    /** Where to install from (installs only). */
    public static final String SOURCE_DIR = "idp.src.dir";

    /** The entity ID. */
    public static final String ENTITY_ID = "idp.entityID";

    /** Do we  cause a failure rather than a prompt. */
    public static final String NO_PROMPT = "idp.noprompt";

    /** What is the installer host name?  */
    public static final String HOST_NAME = "idp.host.name";

    /** The scope to assert.  */
    public static final String SCOPE = "idp.scope";

    /** The keystore password to use.  */
    public static final String KEY_STORE_PASSWORD = "idp.keystore.password";

    /** The sealer password to use.  */
    public static final String SEALER_PASSWORD = "idp.sealer.password";

    /** The sealer alias to use.  */
    public static final String SEALER_ALIAS = "idp.sealer.alias";

    /** The sealer alias to use.  */
    public static final String KEY_SIZE = "idp.keysize";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InstallerProperties.class);

    /** The base directory. */
    @NonnullAfterInit private Path baseDir;

    /** The properties driving the install. */
    @NonnullAfterInit private Properties installerProperties;

    /** The file specified in the system file idp.property.file (if present). */
    @Nullable private File idpPropertyFile;

    /** The target Directory. */
    private Path targetDir;

    /** The sourceDirectory. */
    private Path srcDir;

    /** Do we allow prompting?*/
    private boolean noPrompt;

    /** Is a source Dir needed? */
    private final boolean needSourceDir;

    /** The entity ID. */
    private String entityID;

    /** Old Version. */
    private String oldVersion;
    /** Have done the work aroun the old version ?*/
    private boolean oldVersionDefinitive;

    /** Hostname. */
    private String hostname;

    /** scope. */
    private String scope;

    /** Keystore Password. */
    private String keyStorePassword;

    /** Sealer Password. */
    private String sealerPassword;

    /** Sealer Alias. */
    private String sealerAlias;

    /** Key Size. (for signing, encryption and backchannel). */
    private int keySize;

    /** Whether the properties file exists.*/
    private boolean idpPropertiesPresent;

    /**
     * Constructor.
     * @param populateDone is this a war build (or a windows installer)? If no we don't need the source dir.
     */
    public InstallerProperties(final boolean populateDone) {
        needSourceDir = !populateDone;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        installerProperties = new Properties(System.getProperties());
        final String antBase = installerProperties.getProperty(ANT_BASE_DIR);
        if (antBase == null) {
            throw new ComponentInitializationException(ANT_BASE_DIR + " must be specified");
        }
        try {
            baseDir =  Path.of(antBase).resolve("..").toRealPath();
        } catch (final IOException e) {
            throw new ComponentInitializationException(e);
        }
        if (!Files.exists(baseDir)) {
            log.error("Base dir {} did not exist", baseDir.toAbsolutePath());
            throw new ComponentInitializationException(ANT_BASE_DIR + " must exist");
        }
        log.debug("base dir {}", baseDir);

        if (installerProperties.containsKey(PROPERTY_SOURCE_FILE)) {
            final Path file = baseDir.resolve(installerProperties.getProperty(PROPERTY_SOURCE_FILE));
            if (!Files.exists(file)) {
                log.error("Property file {} did not exist", file.toAbsolutePath());
                throw new ComponentInitializationException(file + " must exist");
            }
            log.debug("Loading properties from {}", file.toAbsolutePath());

            idpPropertyFile = file.toFile();
            try {
                installerProperties.load(new FileInputStream(idpPropertyFile));
            } catch (final IOException e) {
                log.error("Could not load {}", file.toAbsolutePath(), e);
                throw new ComponentInitializationException(e);
            }
            idpPropertyFile.deleteOnExit();
        }

        String value = installerProperties.getProperty(NO_PROMPT);
        noPrompt = value != null;

        if (needSourceDir) {
            value = getValue(SOURCE_DIR, "Source (Distribution) Directory (press <enter> to accept default):",
                    () -> antBase);
            srcDir = Path.of(value);
            log.debug("Source directory {}", srcDir.toAbsolutePath());
        }

        if (!installerProperties.contains(KEY_SIZE)) {
            keySize = 3072;
        } else {
            keySize = Integer.parseInt(installerProperties.getProperty(KEY_SIZE));
        }
        idpPropertiesPresent = Files.exists(getTargetDir().resolve("conf").resolve("idp.properties"));
    }

    /** Lookup a property.  If it isn't defined then ask the user (if we are allowed)
     * @param propertyName the property to lookup.
     * @param prompt what to say to the user
     * @param defaultSupplier how to get the default value
     * @throws BuildException of anything goes wrong
     * @return the value
     */
    protected String getValue(final String propertyName,
            final String prompt, final Supplier<String> defaultSupplier) throws BuildException {
        String value = installerProperties.getProperty(propertyName);
        if (value != null) {
            return value;
        }
        if (noPrompt) {
            throw new BuildException("No value for " + propertyName + " specified");
        }

        final InputRequest request = new InputRequest(prompt);
        final String defaultValue = defaultSupplier.get();
        request.setDefaultValue(defaultValue);

        new DefaultInputHandler().handleInput(request);
        value = request.getInput();
        if (value == null || "".contentEquals(value)) {
            return defaultValue;
        }
        return value;
    }

    /** Lookup a property.  If it isn't defined then ask the user (if we are allowed)
     * @param propertyName the property to lookup.
     * @param prompt what to say to the user
     * @throws BuildException of anything goes wrong
     * @return the value.  this is not repeated to the screen
     */
    protected String getPassword(final String propertyName, final String prompt) throws BuildException {
        final String value = installerProperties.getProperty(propertyName);
        if (value != null) {
            return value;
        }
        if (noPrompt) {
            throw new BuildException("No value for " + propertyName + " specified");
        }

        final InputRequest request = new InputRequest(prompt);

        new PasswordHandler().handleInput(request);
        return request.getInput();
    }


    /** Get where we are installing/updating/building the war.
     * @return the target directory
     * @throws BuildException if something goes awry.
     */
    @Nonnull public Path getTargetDir() throws BuildException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        if (targetDir != null) {
            return targetDir;
        }
        final String defTarget;
        if (needSourceDir) {
            // Source is not "here"
            defTarget = "/opt/shibboleth-idp";
        } else {
            // build-war or Windows so "here" is also "where"
            defTarget = baseDir.toAbsolutePath().toString();
        }
        final String targetValue = getValue(TARGET_DIR, "Installation Directory:", () -> defTarget);
        targetDir = Path.of(targetValue);
        return targetDir;
    }

    /** Where is the install coming from?
     * @return the source directory
     */
    @Nullable public Path getSourceDir() {
        return srcDir;
    }

    /** What is the installer version.
     * @return "3" for a V3 install, null for a new install or the value we write during last install.
     */
    @Nullable public String getInstallerVersion() {
        if (oldVersionDefinitive) {
            return oldVersion;
        }
        oldVersionDefinitive = true;
        final Path conf = getTargetDir().resolve("conf");
        if (!Files.exists(conf.resolve("relying-party.xml"))) {
            // No relying party, no install
            oldVersion = null;
            return null;
        }
        if (!Files.exists(conf.resolve("idp.properties"))) {
            throw new BuildException("V2 Installation detected");
        }

        final Path currentInstall = getTargetDir().resolve("dist").resolve(InstallerSupport.VERSION_NAME);
        if (!Files.exists(currentInstall)) {
            oldVersion= "3";
            return oldVersion;
        }
        final Properties vers = new Properties(1);
        try {
            vers.load(new FileInputStream(currentInstall.toFile()));
        } catch (final IOException e) {
            log.error("Could not load {}", currentInstall.toAbsolutePath(), e);
            throw new BuildException(e);
        }
        oldVersion = vers.getProperty(InstallerSupport.VERSION_NAME);
        if (null == oldVersion) {
            throw new BuildException("File " + InstallerSupport.VERSION_NAME +
                    " did not contain property " + InstallerSupport.VERSION_NAME);
        }
        return oldVersion;
    }

    /** Get the host name for this install. Defaults to information pulled from the network.
     * @return the host name.*/
    @Nonnull public String getEntityID() {
        if (entityID == null) {
            entityID = getValue(ENTITY_ID, "SAML EntityID:", () -> "https://" + getHostName() + "/idp/shibboleth");
        }
        return entityID;
    }


    /** Is this address named? Helper method for {@link #bestHostName()}
     * @return true unless the name is the canonical name...
     * @param addr what to look at
     */
    private boolean hasHostName(final InetAddress addr) {
        return !addr.getHostAddress().equals(addr.getCanonicalHostName());
    }

    /** Find the most aposite network connector. Taken from Ant.
     * @throws SocketException
     * @return the best name we can work out
     */
    // CheckStyle: CyclomaticComplexity OFF
    private String bestHostName() {
        InetAddress bestSoFar = null;
        try {
            for (final NetworkInterface netInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (final InetAddress address : Collections.list(netInterface.getInetAddresses())) {
                    if (bestSoFar == null) {
                        // none selected so far, so this one is better.
                        bestSoFar = address;
                    } else if (address == null || address.isLoopbackAddress()) {
                        // definitely not better than the previously selected address.
                    } else if (address.isLinkLocalAddress()) {
                        // link local considered better than loopback
                        if (bestSoFar.isLoopbackAddress()) {
                            bestSoFar = address;
                        }
                    } else if (address.isSiteLocalAddress()) {
                        // site local considered better than link local (and loopback)
                        // address with hostname resolved considered better than
                        // address without hostname
                        if (bestSoFar.isLoopbackAddress()
                                || bestSoFar.isLinkLocalAddress()
                                || (bestSoFar.isSiteLocalAddress() && !hasHostName(bestSoFar))) {
                            bestSoFar = address;
                        }
                    } else {
                        // current is a "Global address", considered better than
                        // site local (and better than link local, loopback)
                        // address with hostname resolved considered better than
                        // address without hostname
                        if (bestSoFar.isLoopbackAddress()
                                || bestSoFar.isLinkLocalAddress()
                                || bestSoFar.isSiteLocalAddress()
                                || !hasHostName(bestSoFar)) {
                            bestSoFar = address;
                        }
                    }
                }
            }
        } catch (final SocketException e) {
            log.error("Could not get host information", e);
        }
        if (bestSoFar == null) {
            return "localhost.localdomain";
        }
        return bestSoFar.getCanonicalHostName();
    }
    // CheckStyle: CyclomaticComplexity ON

    /** Get the host name for this install. Defaults to information pulled from the network.
     * @return the host name.*/
    @Nonnull public String getHostName() {
        if (hostname == null) {
            hostname = getValue(HOST_NAME, "Host Name:", () -> bestHostName());
        }
        return hostname;
    }

    /** Evaluate the default scope value.
     * @return everything after the firsty '.' in the hostname
     */
    @Nonnull private String defaultScope() {
        final String host = getHostName();
        final int index = host.indexOf('.');
        if (index > 1) {
            return host.substring(index+1);
        }
        return "localdomain";
    }

    /** Get the scope for this installation.
     * @return The scope.
     */
    @Nonnull public String getScope() {
        if (scope == null) {
            scope = getValue(SCOPE, "Attribute Scope:", () -> defaultScope());
        }
        return scope;
    }

    /** Get the SubjectAltName for the certificates.
     * @return the  SubjectAltName
     */
    @Nonnull public String getSubjectAltName() {
        return "https://" + getHostName() + "/idp/shibboleth";
    }

    /** Get the password for the keystore for this installation.
     * @return the password.
     */
    @Nonnull public String getKeyStorePassword() {
        if (keyStorePassword == null) {
            keyStorePassword = getPassword(KEY_STORE_PASSWORD, "Backchannel PKCS12 Password:");
        }
        return keyStorePassword;
    }

    /** Get the password for the sealer for this installation.
     * @return the password.
     */
    @Nonnull public String getSealerPassword() {
        if (sealerPassword == null) {
            sealerPassword = getPassword(SEALER_PASSWORD, "Cookie Encryption Key Password:");
        }
        return sealerPassword;
    }

    /** Get the alias for the sealer key.
     * @return the alias
     */
    @Nonnull public String getSealerAlias() {
        if (sealerAlias == null) {
            sealerAlias = installerProperties.getProperty(SEALER_ALIAS);
        }
        if (sealerAlias == null) {
            sealerAlias = "secret";
        }
        return sealerAlias;
    }

    /** Get the key size for signing, encryption and backchannel.
     * @return the keysize, default is 3072 */
    public int getKeySize() {
        return keySize;
    }

    /** Was idp.properties present in the target file when we started the install?
     * @return if it was.
     */
    public boolean isIdPPropertiesPresent() {
        return idpPropertiesPresent;
    }
}

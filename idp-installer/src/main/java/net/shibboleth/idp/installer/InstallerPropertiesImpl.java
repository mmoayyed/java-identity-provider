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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputRequest;
import org.apache.tools.ant.launch.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.installer.ant.impl.PasswordHandler;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

/** Class implement {@link InstallerProperties} with properties/UI driven values. */
public class InstallerPropertiesImpl extends AbstractInitializableComponent implements InstallerProperties {

    /** The base directory, inherited and shared with ant. */
    public static final String ANT_BASE_DIR = Launcher.ANTHOME_PROPERTY;

    /** The name of a property file to fill in some or all of the above. This file is deleted after processing. */
    public static final String PROPERTY_SOURCE_FILE = "idp.property.file";

    /** The name of a property file to merge with idp.properties. */
    public static final String IDP_PROPERTIES_MERGE = "idp.merge.properties";

    /** The name of a property file to merge with ldap.properties. */
    public static final String LDAP_PROPERTIES_MERGE = "ldap.merge.properties";

    /** The name of a directory to overlay "under" the distribution conf. */
    public static final String CONF_PRE_OVERLAY = "idp.conf.preoverlay";

    /** The name of a directory to use to populate the initial webapp. */
    public static final String INITIAL_EDIT_WEBAPP = "idp.initial.edit-webapp";

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

    /** The the key size to generate.  */
    public static final String KEY_SIZE = "idp.keysize";

    /** Mode to set on conf files. */
    public static final String MODE_CONF = "idp.conf.filemode";

    /** Mode to set on credential *key files. */
    public static final String MODE_CREDENTIAL_KEYS = "idp.conf.credentials.filemode";

    /** Group to set on files in the credential and conf directories. */
    public static final String GROUP_CONF_CREDENTIALS = "idp.conf.group";

    /** Do we do any chgrp/chmod work? */
    public static final String PERFORM_SET_MODE = "idp.conf.setmode";

    /** Whether to tidy up after ourselves. */
    public static final String NO_TIDY = "idp.no.tidy";

    /** Whether to tidy up after ourselves. */
    public static final int DEFAULT_KEY_SIZE = 3072;

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InstallerProperties.class);

    /** The base directory. */
    @NonnullAfterInit private Path baseDir;

    /** The properties driving the install. */
    @NonnullAfterInit private Properties installerProperties;

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

    /** whether to tidy up. */
    private boolean tidy = true;

    /** whether to tidy up. */
    private boolean setGroupAndMode = true;

    /** conf all file mode. */
    private String confFileMode;

    /** credentials key file mode. */
    private String credentialsKeyFileMode;

    /** Local overload of properties (to deal with nested calling). */
    private Map<String, String> inheritedProperties = Collections.EMPTY_MAP;

    /**
     * Constructor.
     * @param copiedDistribution Has the distribution been copied? If no we don't need the source dir.
     */
    public InstallerPropertiesImpl(final boolean copiedDistribution) {
        needSourceDir = !copiedDistribution;
    }

    /** Set any properties inherited from the base environment.
     * @param props what to set
     */
    public void setInheritedProperties(final Map<String,String> props) {
        inheritedProperties = props;
    }

    /** {@inheritDoc} */
    // CheckStyle: CyclomaticComplexity OFF
    protected void doInitialize() throws ComponentInitializationException {
        installerProperties = new Properties(System.getProperties());

        for (final Map.Entry<String,String> entry:inheritedProperties.entrySet()) {
            installerProperties.setProperty(entry.getKey(), entry.getValue());
        }

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
        final String noTidy = installerProperties.getProperty(NO_TIDY);
        tidy = noTidy == null;
        final String setModeString = installerProperties.getProperty(PERFORM_SET_MODE);
        if (setModeString != null) {
            setGroupAndMode = Boolean.valueOf(setModeString);
        }

        final String propertyFile = installerProperties.getProperty(PROPERTY_SOURCE_FILE);
        if (propertyFile != null) {
            final Path file = baseDir.resolve(propertyFile);
            if (!Files.exists(file)) {
                log.error("Property file {} did not exist", file.toAbsolutePath());
                throw new ComponentInitializationException(file + " must exist");
            }
            log.debug("Loading properties from {}", file.toAbsolutePath());

            /* The file specified in the system file idp.property.file (if present). */
            final File idpPropertyFile = file.toFile();
            try {
                installerProperties.load(new FileInputStream(idpPropertyFile));
            } catch (final IOException e) {
                log.error("Could not load {}: {}", file.toAbsolutePath(), e.getMessage());
                throw new ComponentInitializationException(e);
            }
            if (!isNoTidy()) {
                idpPropertyFile.deleteOnExit();
            }
        }

        String value = installerProperties.getProperty(NO_PROMPT);
        noPrompt = value != null;

        if (needSourceDir) {
            value = getValue(SOURCE_DIR, "Source (Distribution) Directory (press <enter> to accept default):",
                    () -> baseDir.toString());
            srcDir = Path.of(value);
            log.debug("Source directory {}", srcDir.toAbsolutePath());
        }

        value = installerProperties.getProperty(KEY_SIZE);
        if (value == null) {
            keySize = DEFAULT_KEY_SIZE;
        } else {
            keySize = Integer.parseInt(value);
        }
    }
    // CheckStyle: CyclomaticComplexity ON

    /** Lookup a property.  If it isn't defined then ask the user (if we are allowed).
     * This is used by most (but all) getters that redirect through a property
     * @param propertyName the property to lookup.
     * @param prompt what to say to the user
     * @param defaultSupplier how to get the default value.  Using a Supplier allows this
     * to be a reasonably heavyweight operation.
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

    /** Lookup a property.  If it isn't defined then ask the user (if we are allowed) via
     * a no-echo interface.
     * Note that this does not work within a debugger.
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

    /** {@inheritDoc}
     * This is slightly complicated because the default depends on what we are doing.
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

    /**  {@inheritDoc} */
    @Nullable public Path getSourceDir() {
        return srcDir;
    }

    /**  {@inheritDoc}
     * Defaults to information pulled from {{@link #getHostName()}.
     */
    @Nonnull public String getEntityID() {
        if (entityID == null) {
            entityID = getValue(ENTITY_ID, "SAML EntityID:", () -> "https://" + getHostName() + "/idp/shibboleth");
        }
        return entityID;
    }

    /** {@inheritDoc} */
    public boolean isNoTidy() {
        return !tidy;
    }

    /** Is this address named? Helper method for {@link #bestHostName()}
     * @return true unless the name is the canonical name...
     * @param addr what to look at
     */
    private boolean hasHostName(final InetAddress addr) {
        return !addr.getHostAddress().equals(addr.getCanonicalHostName());
    }

    /**
     * Find the most apposite network connector. Taken from Ant.
     * 
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

    /**  {@inheritDoc}
     * Defaults to information pulled from the network.
     */
    @Nonnull public String getHostName() {
        if (hostname == null) {
            hostname = getValue(HOST_NAME, "Host Name:", () -> bestHostName());
        }
        return hostname;
    }

    /** {@inheritDoc} */
    @Override @Nonnull public String getConfFileMode() {
        if (confFileMode ==null) {
            confFileMode = installerProperties.getProperty(MODE_CONF, "600");
        }
        return confFileMode;
    }

    /** {@inheritDoc} */
    @Override @Nonnull public String getCredentialsKeyFileMode() {
        if (credentialsKeyFileMode == null) {
            credentialsKeyFileMode = installerProperties.getProperty(MODE_CREDENTIAL_KEYS, "600");
        }
        return credentialsKeyFileMode;
    }

    /** {@inheritDoc} */
    @Override @Nullable public String getConfCredentialsGroup() {
        return installerProperties.getProperty(GROUP_CONF_CREDENTIALS);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSetGroupAndMode() {
        return setGroupAndMode;
    }

    /** Evaluate the default scope value.
     * @return everything after the first '.' in {@link #getHostName()}
     */
    @Nonnull protected String defaultScope() {
        final String host = getHostName();
        final int index = host.indexOf('.');
        if (index > 1) {
            return host.substring(index+1);
        }
        return "localdomain";
    }

    /** {@inheritDoc}. */
    @Override @Nonnull public String getScope() {
        if (scope == null) {
            scope = getValue(SCOPE, "Attribute Scope:", () -> defaultScope());
        }
        return scope;
    }

    /** {@inheritDoc}. */
    @Override @Nonnull public String getSubjectAltName() {
        return "https://" + getHostName() + "/idp/shibboleth";
    }

    /** {@inheritDoc}. */
    @Override  @Nonnull public String getKeyStorePassword() {
        if (keyStorePassword == null) {
            keyStorePassword = getPassword(KEY_STORE_PASSWORD, "Backchannel PKCS12 Password:");
        }
        return keyStorePassword;
    }

    /** {@inheritDoc}. */
    @Override @Nonnull public String getSealerPassword() {
        if (sealerPassword == null) {
            sealerPassword = getPassword(SEALER_PASSWORD, "Cookie Encryption Key Password:");
        }
        return sealerPassword;
    }

    /** {@inheritDoc}. */
    @Nonnull public String getSealerAlias() {
        if (sealerAlias == null) {
            sealerAlias = installerProperties.getProperty(SEALER_ALIAS);
        }
        if (sealerAlias == null) {
            sealerAlias = "secret";
        }
        return sealerAlias;
    }

    /** {@inheritDoc}. default is {@value #DEFAULT_KEY_SIZE}. */
    @Override public int getKeySize() {
        return keySize;
    }

    /** Get the directory specified as the property as a File, or null if it doesn't exist.
     * @param propName the name to lookup;
     * @return null if the property is not provided a {@link File} otherwise
     * @throws BuildException if the property is supplied but the file doesn't exist.
     */
    protected Path getMergeDir(final String propName) throws BuildException {
        return getMergePath(propName, true);
    }

    /** Get the file specified as the property as a File, or null if it doesn't exist.
     * @param propName the name to lookup;
     * @return null if the property is not provided a {@link File} otherwise
     * @throws BuildException if the property is supplied but the file doesn't exist.
     */
    protected Path getMergeFile(final String propName) throws BuildException {
        return getMergePath(propName, false);
    }

    /** Get the {@link Path} specified as the property as a File, or null if it doesn't exist.
     * Police for type if required
     * @param propName the name to lookup;
     * @param mustBeDir if null do not policy.  Otherwise policy according to value
     * @return null if the property is not provided a {@link File} otherwise
     * @throws BuildException if the property is supplied but the file doesn't exist.
     */
    private Path getMergePath(final String propName, final Boolean mustBeDir) throws BuildException {
        final String propValue = installerProperties.getProperty(propName);
        if (propValue == null) {
            return null;
        }
        final Path result = baseDir.resolve(propValue);
        log.debug("Property '{}' had value '{}' returning path '{}'", propName, propValue, result);
        if (!Files.exists(result)) {
            log.error("Could not find specified file specified by property {} ({})", propName, result );
            throw new BuildException("Property file not found");
        }
        if (mustBeDir != null) {
            if (mustBeDir) {
                if (!Files.isDirectory(result)) {
                    log.error("Path '{}' supplied by property '{}' was not a directory", result, propName);
                    throw new BuildException("No a directory");
                }
            } else {
                if (!Files.isDirectory(result)) {
                    log.error("Path '{}' supplied by property '{}' was not a file", result, propName);
                    throw new BuildException("No a file");
                }                
            }
        }
        return result;
    }

    /** {@inheritDoc}. */
    @Override public Path getIdPMergeProperties() throws BuildException {
        return getMergeFile(IDP_PROPERTIES_MERGE);
    }

    /** {@inheritDoc}. */
    @Override public Path getLDAPMergeProperties() throws BuildException {
        return getMergeFile(LDAP_PROPERTIES_MERGE);
    }

    /** {@inheritDoc}. */
    @Override public Path getConfPreOverlay() throws BuildException {
        return getMergeFile(CONF_PRE_OVERLAY);
    }

    /** {@inheritDoc}. */
    @Override public Path getInitialEditWeb() throws BuildException {
        return getMergeFile(INITIAL_EDIT_WEBAPP);
    }
}

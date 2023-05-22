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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.input.InputRequest;
import org.slf4j.Logger;

import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.NonnullSupplier;
import net.shibboleth.shared.primitive.StringSupport;

/** Class which encapsulated all the properties/UI driven configuration of an install.

 NOTE Updated to this properties should be reflected in the "PropertyDriverInstallation" wiki page."/

*/
public class InstallerProperties  {

    /** The name of a property file to fill in some or all of the above. This file is deleted after processing. */
    public static final String PROPERTY_SOURCE_FILE = "idp.property.file";

    /** The name of a property file to merge with idp.properties. */
    public static final String IDP_PROPERTIES_MERGE = "idp.merge.properties";

    /** The name of a property file to merge with ldap.properties. */
    public static final String LDAP_PROPERTIES_MERGE = "ldap.merge.properties";

    /** The LDAP Password (usually associated with a username in ldap.properties). */
    public static final String LDAP_PASSWORD = "idp.LDAP.credential";

    /** Where to install to.  Default is basedir */
    public static final String TARGET_DIR = "idp.target.dir";

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

    /** Mode to set on credential *key files. */
    public static final String MODE_CREDENTIAL_KEYS = "idp.conf.credentials.filemode";

    /** Group to set on files in the credential and conf directories. */
    public static final String GROUP_CONF_CREDENTIALS = "idp.conf.credentials.group";

    /** Do we do any chgrp/chmod work? */
    public static final String PERFORM_SET_MODE = "idp.conf.setmode";

    /** Whether to tidy up after ourselves. */
    public static final String NO_TIDY = "idp.no.tidy";

    /** Which modules to enable on initial install.
     * @since 4.1.0 */
    public static final String INITIAL_INSTALL_MODULES = "idp.initial.modules";

    /** Whether to tidy up after ourselves. */
    public static final int DEFAULT_KEY_SIZE = 3072;

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InstallerProperties.class);

    /** The properties driving the install. */
    @NonnullAfterInit private Properties installerProperties;

    /** The target Directory. */
    private Path targetDir;

    /** The sourceDirectory. */
    @Nonnull private final Path srcDir;

    /** Do we allow prompting?*/
    private boolean noPrompt;

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

    /** credentials key file mode. */
    private String credentialsKeyFileMode;

    /** Input handler from the prompting. */
    private final InputHandler inputHandler;

    /** Those modules which are "core". */
    @Nonnull public static final Set<String> CORE_MODULES = CollectionSupport.setOf("idp.Core");

    /** Those modules enabled by default. */
    @Nonnull public static final Set<String> DEFAULT_MODULES = CollectionSupport.setOf("idp.EditWebApp",
            "idp.CommandLine" ,"idp.authn.Password", "idp.admin.Hello");

    /**
     * Constructor.
     * @param copiedDistribution Has the distribution been copied? If no we don't need the source dir.
     */
    public InstallerProperties(@Nonnull final Path sourceDir) {
        srcDir = sourceDir;
        inputHandler = getInputHandler();
    }

    /** Get an {@link InputHandler} for the prompting.
     * @return an input handler */
    protected InputHandler getInputHandler() {
        return new DefaultInputHandler() {
            // we want the prompts to be more obviously prompts
            protected String getPrompt(final InputRequest request) {
                return super.getPrompt(request) + " ? ";
            }
        };
    }

    /** {@inheritDoc} */
    // CheckStyle: CyclomaticComplexity OFF
    protected void doInitialize() throws ComponentInitializationException {
        installerProperties = new Properties(System.getProperties());

        if (!Files.exists(srcDir)) {
            log.error("Source dir {} did not exist", srcDir.toAbsolutePath());
            throw new ComponentInitializationException(srcDir.toString() + " must exist");
        }
        log.debug("Source dir {}", srcDir);

        final Path propertyFile = getMergeFile(PROPERTY_SOURCE_FILE);
        if (propertyFile != null) {
            /* The file specified in the system file idp.property.file (if present). */
            final File idpPropertyFile = propertyFile.toFile();
            try(final FileInputStream stream = new FileInputStream(idpPropertyFile)) {
                installerProperties.load(stream);
            } catch (final IOException e) {
                log.error("Could not load {}: {}", propertyFile.toAbsolutePath(), e.getMessage());
                throw new ComponentInitializationException(e);
            }
            if (!isNoTidy()) {
                idpPropertyFile.deleteOnExit();
            }
        }

        final String noTidy = installerProperties.getProperty(NO_TIDY);
        tidy = noTidy == null;
        final String setModeString = installerProperties.getProperty(PERFORM_SET_MODE);
        if (setModeString != null) {
            setGroupAndMode = Boolean.valueOf(setModeString);
        }

        String value = installerProperties.getProperty(NO_PROMPT);
        noPrompt = value != null;

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
    @Nonnull protected String getValue(final String propertyName,
            final String prompt, final NonnullSupplier<String> defaultSupplier) throws BuildException {
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

        inputHandler.handleInput(request);
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
    @Nonnull protected String getPassword(final String propertyName, final String prompt) throws BuildException {
        final String value = installerProperties.getProperty(propertyName);
        if (value != null) {
            return value;
        }
        if (noPrompt) {
            throw new BuildException("No value for " + propertyName + " specified");
        }

        final InputRequest request = new InputRequest(prompt);

        new PasswordHandler().handleInput(request);
        @Nullable final String result = request.getInput();
        if (result == null) {
            throw new BuildException("Null result from Ant PasswordHandler");
        }
        return result;
    }

    /** Get where we are installing/updating/building the war.
     * This is slightly complicated because the default depends on what we are doing.
     * @return the target directory
     * @throws BuildException if something goes awry.
     */
    @Nonnull public Path getTargetDir() throws BuildException {
        if (targetDir != null) {
            return targetDir;
        }
        final Path td = targetDir = Path.of(getValue(TARGET_DIR, "Installation Directory:", () -> "/opt/shibboleth-idp"));
        assert td != null;
        return td;
    }

    /** Where is the install coming from?
     * @return the source directory
     */
    @Nullable public Path getSourceDir() {
        return srcDir;
    }

    /** Get the EntityId for this install.
     * @return the  name.
     */
    @Nonnull public String getEntityID() {
        String result = entityID;
        if (result == null) {
            entityID = result = getValue(ENTITY_ID, "SAML EntityID:", () -> "https://" + getHostName() + "/idp/shibboleth");
        }
        return result;
    }

    /** Does the user want us to *not* tidy up.
     * @return do we not tidy up?*/
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
    @Nonnull private String bestHostName() {
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
        final String result = bestSoFar.getCanonicalHostName();
        assert result!=null;
        return result;
    }
    // CheckStyle: CyclomaticComplexity ON

    /** Get the host name for this install.
     * Defaults to information pulled from the network.
     * @return the host name.
     */
    @Nonnull public String getHostName() {
        String result = hostname;
        if (result == null) {
            result = hostname = getValue(HOST_NAME, "Host Name:", () -> bestHostName());
        }
        return result;
    }

    /** Mode to set on all files in credentials.
    * @return the mode
    */
    @Nonnull public String getCredentialsKeyFileMode() {
        String result = credentialsKeyFileMode;
        if (result != null) {
            return result;
        }
        result = credentialsKeyFileMode = installerProperties.getProperty(MODE_CREDENTIAL_KEYS, "600");
        assert result != null;
        return result;
    }

    /** Group to set on all files in credentials and conf.
    * @return the mode or null if none to be set
    */
    @Nullable public String getCredentialsGroup() {
        return installerProperties.getProperty(GROUP_CONF_CREDENTIALS);
    }

    /** Do we set the mode?
    * @return do we the mode
    */
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
            final String result =host.substring(index+1);
            assert result != null;
            return result;
        }
        return "localdomain";
    }

    /** Get the scope for this installation.
     * @return The scope.
     */
    @Nonnull public String getScope() {
        String result = scope;
        if (result  == null) {
            result = scope = getValue(SCOPE, "Attribute Scope:", () -> defaultScope());
        }
        return result;
    }

    /** Get the LDAP password iff one was provided.  DO NOT PROMPT
    *
    * @return the password if provided by a properties
    * @throws BuildException  if badness happens
    */
    @Nullable public String getLDAPPassword() throws BuildException {
        return installerProperties.getProperty(LDAP_PASSWORD);
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
        @SuppressWarnings("null") @Nonnull String result = keyStorePassword;
        if (keyStorePassword == null) {
            result = keyStorePassword = getPassword(KEY_STORE_PASSWORD, "Backchannel PKCS12 Password:");
        }
        return result;
    }

    /** Get the password for the sealer for this installation.
     * @return the password.
     */
    @Nonnull public String getSealerPassword() {
        String result = sealerPassword;
        if (result == null) {
            result = sealerPassword = getPassword(SEALER_PASSWORD, "Cookie Encryption Key Password:");
        }
        return result;
    }

    /** Get the modules to enable after first install.
     * @return the modules
     */
    @Nonnull  @NotLive @Unmodifiable public Set<String> getModulesToEnable() {
        String prop = StringSupport.trimOrNull(installerProperties.getProperty(INITIAL_INSTALL_MODULES));
        if (prop == null) {
            return InstallerProperties.DEFAULT_MODULES;
        }
        final boolean additive = prop.startsWith("+");
        if (additive) {
            prop = prop.substring(1);
        }
        final String[] modules = prop.split(",");
        assert modules != null;
        if (!additive) {
            final Set<String> result = CollectionSupport.copyToSet(CollectionSupport.arrayAsList(modules));
            return result;
        }
        final Set<String> result = new HashSet<>(modules.length + InstallerProperties.DEFAULT_MODULES.size());
        result.addAll(InstallerProperties.DEFAULT_MODULES);
        result.addAll(Arrays.asList(modules));
        return CollectionSupport.copyToSet(result);
    }
    
    /** Get the modules to enable before ant install.
     * @return the modules
     */
    @Nonnull  @NotLive @Unmodifiable public Set<String> getCoreModules() {
        return InstallerProperties.CORE_MODULES;
    }


    /** Get the alias for the sealer key.
     * @return the alias
     */
    @Nonnull public String getSealerAlias() {
        String result = sealerAlias;
        if (result == null) {
            result = sealerAlias = installerProperties.getProperty(SEALER_ALIAS);
        }
        if (result == null) {
            result = sealerAlias = "secret";
        }
        return result;
    }

    /** Get the key size for signing, encryption and backchannel.
     * @return the keysize
     *  default is {@value #DEFAULT_KEY_SIZE}. */
    public int getKeySize() {
        return keySize;
    }

    /** Get the file specified as the property as a File, or null if it doesn't exist.
     * @param propName the name to lookup;
     * @return null if the property is not provided a {@link Path} otherwise
     * @throws BuildException if the property is supplied but the file doesn't exist.
     */
    protected Path getMergeFile(final String propName) throws BuildException {
        final String propValue = installerProperties.getProperty(propName);
        if (propValue == null) {
            return null;
        }
        Path path = Path.of(propValue);
        if (Files.exists(path)) {
            log.debug("Property '{}' had value '{}' Path exists ", propName, propValue);
        } else {
            path = srcDir.resolve(path);
            if (!Files.exists(path)) {
                log.debug("Property '{}' had value '{}' neither '{}' nor '{}' exist", propName, propValue, path);
                log.error("Path '{}' supplied for '{}' does not exist", propValue, propName);
                throw new BuildException("Property file not found");
            }
            log.debug("Property '{}' had value '{}' Path {} exists ", propName, propValue, path);
        }
        if (Files.isDirectory(path)) {
            log.error("Path '{}' supplied by property '{}' was not a file", path, propName);
            throw new BuildException("No a file");
        }                
        return path;
    }

    /** Get the a file to merge with idp.properties or null.
    *
    * @return the file or null if it none required.
    * @throws BuildException if badness happens
    */
    public Path getIdPMergeProperties() throws BuildException {
        return getMergeFile(IDP_PROPERTIES_MERGE);
    }

    /** Get the a file to merge with ldap.properties or null.
    *
    * @return the path or null if it none required.
    * @throws BuildException  if badness happens
    */
    public Path getLDAPMergeProperties() throws BuildException {
        return getMergeFile(LDAP_PROPERTIES_MERGE);
    }

}

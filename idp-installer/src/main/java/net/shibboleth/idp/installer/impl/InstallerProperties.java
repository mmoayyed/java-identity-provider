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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**  The name of a property file to fill in some or all of the above. This file is deleted after processing. */
    public static final String PROPERTY_SOURCE_FILE = "idp.property.file";

    /** where to install to.  Default is basedir */
    public static final String TARGET_DIR = "idp.target.dir";

    /** where to install from (installs only). */
    public static final String SOURCE_DIR = "idp.src.dir";

    /** Do we  cause a failure rather than a prompt. */
    public static final String NO_PROMPT = "idp.noprompt";

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
            value = getValue(SOURCE_DIR, "Source (Distribution) Directory (press <enter> to accept default):", antBase);
            srcDir = Path.of(value);
            log.debug("Source directory {}", srcDir.toAbsolutePath());
        }
    }

    /** Lookup a property.  If it isn't defined then ask the user (if we are allowed)
     * @param propertyName the property to lookup.
     * @param prompt what to say to the user
     * @param defaultValue the default value
     * @throws BuildException of anything goes wrong
     * @return the value
     */
    protected String getValue(final String propertyName,
            final String prompt, final String defaultValue) throws BuildException {
        String value = installerProperties.getProperty(propertyName);
        if (value != null) {
            return value;
        }
        if (noPrompt) {
            throw new BuildException("No value for " + propertyName + " specified");
        }

        final InputRequest request = new InputRequest("Installation Directory:");
        request.setDefaultValue(defaultValue);

        new DefaultInputHandler().handleInput(request);
        value = request.getInput();
        if (value == null || "".contentEquals(value)) {
            return defaultValue;
        }
        return value;
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
        final String targetValue = getValue(TARGET_DIR, "Installation Directory", defTarget);
        targetDir = Path.of(targetValue);
        return targetDir;
    }

    /** Where is the install coming from?
     * @return the source directory
     */
    @Nullable public Path getSourceDir() {
        return srcDir;
    }
}

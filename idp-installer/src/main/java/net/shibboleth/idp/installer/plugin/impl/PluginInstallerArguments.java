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

package net.shibboleth.idp.installer.plugin.impl;

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.beust.jcommander.Parameter;

import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLineArguments;
import net.shibboleth.profile.installablecomponent.InstallableComponentVersion;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Arguments for Plugin Installer CLI.
 */
public class PluginInstallerArguments extends AbstractIdPHomeAwareCommandLineArguments {

    /** Logger. */
    @Nullable private Logger log;

    /** The PluginId - usually used to drive the update. */
    @Parameter(names= {"-p", "--pluginId"})
    @Nullable private String pluginId;

    /** Suppress Prompts. */
    @Parameter(names= {"--noPrompt"})
    private boolean noPrompt;

    /** Brief info about installed plugins. */
    @Parameter(names= {"-l", "--list"})
    private boolean list;

    /** Brief info about installed plugins. */
    @Parameter(names= {"-L", "--list-available"})
    private boolean listAvailable;

    /** Override version check. */
    @Parameter(names= {"--noCheck"})
    private boolean noCheck;

    /** Detailed info about installed plugins. */
    @Parameter(names= {"-fl", "--full-list"})
    private boolean fullList;

    /** List License. */
    @Parameter(names= {"--license"})
    @Nullable private String license;

    /** What to install. */
    @Parameter(names= {"-i", "--input"})
    @Nullable private String input;

    /** What to install. */
    @Parameter(names= {"-I", "--install-ID"})
    @Nullable private String installId;

    /** Truststore to use for signing. */
    @Parameter(names= {"--truststore"})
    @Nullable private String truststore;

    /** Update plugin Id. */
    @Parameter(names= {"-u", "--update"})
    @Nullable private String updatePluginId;

    /** Force update version. */
    @Parameter(names= {"-fu", "--force-update"})
    @Nullable private String forceUpdateVersion;
 
    /** Id to remove. */
    @Parameter(names= {"-r", "--uninstall", "--remove"})
    @Nullable private String uninstallId;

    /** Contents to list. */
    @Parameter(names= {"-cl", "--contents-list"})
    @Nullable private String contentsList;

    /** location to override the plugin supplied location. */
    @Parameter(names= {"--updateURL"})
    @Nullable private String updateURL;

    /** Override version check. */
    @Parameter(names= {"--noRebuild", "--no-rebuild"})
    private boolean noRebuild;

    /** The {@link #forceUpdateVersion} as a {@link InstallableComponentVersion}. */
    @Nullable private InstallableComponentVersion updateVersion;

    /** Decomposed input - name. */
    @Nullable private String inputName;

    /** Decomposed input - directory . */
    @Nullable private Path inputDirectory;

    /** Decomposed input - base URL. */
    @Nullable private URL inputURL;

    /** Operation enum. */
    public enum OperationType {
        /** Update a known install. */
        UPDATE,
        /** List all installs. */
        LIST,
        /** Install from a local copy. */
        INSTALLDIR,
        /** Install from the web. */
        INSTALLREMOTE,
        /** Uninstall plugin. */
        UNINSTALL,
        /** Print the license file to System.out. */
        OUTPUTLICENSE,
        /** List the contents for the plugin. */
        LISTCONTENTS,
        /** Unknown. */
        UNKNOWN
    };

    /** What to do. */
    @Nonnull private OperationType operation = OperationType.UNKNOWN;

    /** {@inheritDoc} */
    public @Nonnull Logger getLog() {
        if (log == null) {
            log = LoggerFactory.getLogger(PluginInstallerArguments.class);
        }
        assert log != null;
        return log;
    }

    /** Plugin Id (if specified).
     * 
     * @return plugin ID
     */
    @Nullable public String getPluginId() {
        return pluginId;
    }

    /** get TrustStore (if specified).
     * 
     * @return the trust store
     */
    @Nullable public String getTruststore() {
        return truststore;
    }

    /** Get the digested parent URL.
     * @return Returns the digested parent URL.
     *
     * Only valid for {@link OperationType#INSTALLREMOTE}.
     */
    @Nonnull public URL getInputURL() {
        final URL result = inputURL;
        Constraint.isTrue(operation == OperationType.INSTALLREMOTE, "Can only call getInputURL on remote operations");
        Constraint.isTrue(result != null, "Invalid Remote URL");
        assert result != null;
        return result;
    }

    /** Get the file Name.
     *
     * Only valid for {@link OperationType#INSTALLDIR}
     * and {@link OperationType#INSTALLREMOTE}.
     *
     * @return Returns the digested file Name.
     */
    @Nonnull public String getInputFileName() {
        final String result = inputName;
        Constraint.isTrue(operation == OperationType.INSTALLREMOTE || operation == OperationType.INSTALLDIR, 
                "Can only call getInputFileName on remote or local installs");
        Constraint.isTrue(result != null, "Invalid InputFileName");
        assert result != null;
        return result;
    }

    /** Get the digested input directory.
     *
     * Only valid for {@link OperationType#INSTALLDIR}.
     *
     * @return Returns the digested input directory.
     */
    @Nonnull public Path getInputDirectory() {
        final Path  result = inputDirectory;
        Constraint.isTrue(operation == OperationType.INSTALLDIR, 
                "Can only call getInputDirectory on local installs");
        Constraint.isTrue(result != null, "Invalid InputDirectory");
        assert result != null;
        return result;
    }

    /** Are we doing a full List?
     * 
     * @return whether we're doing a full list.
     */
    public boolean isFullList() {
        return fullList;
    }

    /** Are we going to list everything from the remote site?
     * @return listAvailable.
     */
    public boolean isListAvailable() {
        return listAvailable;
    }
    
    /** Are we going to install from the pluginId?
     * @return whether the user specified {@link #installId}
     */
    public boolean isInstallId() {
        return StringSupport.trimOrNull(installId) != null;
    }

    /** Are we doing a List?
     * 
     * @return whether we're doing a list
     */
    public boolean isList() {
        return list;
    }

    /** Are we checking the version or not?
     * @return noCheck.
     */
    public boolean isNoCheck() {
        return noCheck;
    }

    /** Are we doing an unattended install?
     * 
     * @return whether we're doing an unattended install
     */
    public boolean isUnattended() {
        return noPrompt;
    }

    /** Return the version to update to or null.
     * @return the version or null
     */
    @Nullable public InstallableComponentVersion getUpdateVersion() {
        return updateVersion;
    }

    /** return the update URL or null.
     * @return null or the value supplied
     */
    @Nullable public String getUpdateURL() {
        return StringSupport.trimOrNull(updateURL);
    }

    /**
     * Get operation to perform.
     * @return operation
     */
    @Nonnull public OperationType getOperation() {
        return operation;
    }

    /** Do we rebuild the war?
     * @return whether we do or not
     */
    public boolean isRebuild() {
        return !noRebuild;
    }

    /** {@inheritDoc} */
    // Checkstyle: CyclomaticComplexity OFF
    public void validate() throws IllegalArgumentException {
        super.validate();

        if (list || fullList || listAvailable) {
            operation = OperationType.LIST;
            if (input !=  null || uninstallId != null) {
                getLog().error("Cannot List and Install or Remove in the same operation.");
                throw new IllegalArgumentException("Cannot List and Install or Remove in the same operation.");
            }
            if (updatePluginId !=  null || uninstallId != null) {
                getLog().error("Cannot List and Update or Remove in the same operation.");
                throw new IllegalArgumentException("Cannot List and Update or Remove in the same operation.");
            }
        } else if (input != null) {
            if (updatePluginId !=  null || uninstallId != null) {
                getLog().error("Cannot Install and Update or Remove in the same operation.");
                throw new IllegalArgumentException("Cannot List and Update or Remove in the same operation.");
            }
            operation = decodeInput();
        } else if (installId != null) {
            operation = OperationType.INSTALLREMOTE;
            pluginId = installId;
        } else if (updatePluginId != null) {
            if (uninstallId != null) {
                getLog().error("Cannot Update and Remove in the same operation.");
                throw new IllegalArgumentException("Cannot Update and Remove in the same operation.");
            }
            pluginId = updatePluginId;
            operation = OperationType.UPDATE;
            if (forceUpdateVersion != null) {
                updateVersion = new InstallableComponentVersion(forceUpdateVersion);
            }
        } else if (uninstallId != null) {
            pluginId = uninstallId;
            operation = OperationType.UNINSTALL;
        } else if (license != null) {
            pluginId = license;
            operation = OperationType.OUTPUTLICENSE;
        } else if (contentsList != null){
            pluginId = contentsList;
            operation = OperationType.LISTCONTENTS;
        } else {
            throw new IllegalArgumentException("Missing qualifier. Try  --help");
        }
    }
    // Checkstyle: CyclomaticComplexity ON

    /** Given an input string, work out what the parts are.
     * @return Whether this is a remote install or a local one.
     */
    @Nonnull private OperationType decodeInput() {
        try {
            final String urlInput = input;
            assert urlInput != null;
            final URL inputAsURL = new URL(urlInput);
            if ("https".equals(inputAsURL.getProtocol()) || "http".equals(inputAsURL.getProtocol())) {
                final int i = urlInput.lastIndexOf('/')+1;
                inputURL = new URL(urlInput.substring(0, i));
                inputName = urlInput.substring(i);
                getLog().trace("Found URL: {}\t{}", inputDirectory, inputName);
                return OperationType.INSTALLREMOTE;
            }
        } catch (final MalformedURLException e) {
            // It's OK
        }
        // Must be a file
        final File inputAsFile = new File(input);
        if (!inputAsFile.exists()) {
            getLog().error("File {} does not exist", inputAsFile.getAbsolutePath());
            throw new IllegalArgumentException("Input File does not exist");
        }
        final Path inputAsPath = Path.of(inputAsFile.getAbsolutePath());
        inputDirectory = inputAsPath.getParent();
        inputName = inputAsPath.getFileName().toString();
        getLog().trace("Found File: {}\t{}", inputDirectory, inputName);
        return OperationType.INSTALLDIR;
    }

    /** {@inheritDoc} */
    public void printHelp(final @Nonnull PrintStream out) {
        out.println("Plugin");
        out.println("Provides a command line interface for plugin management operations.");
        out.println();
        out.println("   Plugin [options] springConfiguration [FullName]");
        out.println();
        out.println("      springConfiguration      name of Spring configuration resource to use");
        super.printHelp(out);
        out.println();
        out.println(String.format("  %-22s %s", "-l, --list", "Brief Information of all installed plugins"));
        out.println(String.format("  %-22s %s", "-fl, --full-list", "Full details of all installed plugins"));
        out.println(String.format("  %-22s %s", "-L, --list-available", "List plugins available to download."));
        out.println(String.format("  %-22s %s", "-cl, --contents-list", "Details of what was installed"));
        out.println(String.format("  %-22s %s", "-i, --input <what>", "Install (file name or web address)"));
        out.println(String.format("  %-22s %s", "-I, --install-ID <what>",
                "Install PluginID (must be available at download URL)"));
        out.println(String.format("  %-22s %s", "--noCheck", "Do not check the version"));
        out.println(String.format("  %-22s %s", "-u, --update <PluginId>", "update"));
        out.println(String.format("  %-22s %s", "--noRebuild", "do NOT rebuild war"));
        out.println(String.format("  %-22s %s", "-fu, --force-update <version>",
                "force version to update to (requires -u)"));
        out.println(String.format("  %-22s %s", "-r, --remove, --uninstall <PluginId>",
                "Uninstall plugin from the war file. \n" +
                "\t\t\tDOES NOT UNDO any other installation"));
        out.println(String.format("  %-22s %s", "--license <pluginid>",
                "Output all licenses for this plugin"));
        out.println(String.format("  %-22s %s", "--noPrompt", "Unattended Install"));
        out.println(String.format("  %-22s %s", "--truststore <path>",
                "Explicit location to look for keys (should exist but may be an empty file)"));
        out.println(String.format("  %-22s %s", "--updateURL <URL>",
                "Explicit location to look for update information (overrides the plugin's value)"));
        out.println();
    }

}

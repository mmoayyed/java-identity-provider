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
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.beust.jcommander.Parameter;

import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLineArguments;
import net.shibboleth.idp.installer.impl.InstallationLogger;
import net.shibboleth.idp.plugin.PluginVersion;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

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
    @Nullable private boolean noPrompt;

    /** Brief info about installed plugins. */
    @Parameter(names= {"-l", "--list"})
    @Nullable private boolean list;

    /** Override version check. */
    @Parameter(names= {"--noCheck"})
    @Nullable private boolean noCheck;

    /** Detailed info about installed plugins. */
    @Parameter(names= {"-fl", "--full-list"})
    @Nullable private boolean fullList;

    /** List License. */
    @Parameter(names= {"--license"})
    @Nullable private String license;

    /** What to install. */
    @Parameter(names= {"-i", "--input"})
    @Nullable private String input;

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
    @Nullable private boolean noRebuild;

    /** The {@link #forceUpdateVersion} as a {@link PluginVersion}. */
    @Nullable private PluginVersion updateVersion;

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
    public Logger getLog() {
        if (log == null) {
            log = InstallationLogger.getLogger(PluginInstallerArguments.class);
        }
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
    public URL getInputURL() {
        return inputURL;
    }

    /** Get the file Name.
     *
     * Only valid for {@link OperationType#INSTALLDIR}
     * and {@link OperationType#INSTALLREMOTE}.
     *
     * @return Returns the digested file Name.
     */
    public String getInputFileName() {
        return inputName;
    }

    /** Get the digested input directory.
     *
     * Only valid for {@link OperationType#INSTALLDIR}.
     *
     * @return Returns the digested input directory.
     */
    public Path getInputDirectory() {
        return inputDirectory;
    }

    /** Are we doing a full List?
     * 
     * @return whether we're doing a full list.
     */
    public boolean isFullList() {
        return fullList;
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
    @Nullable public PluginVersion getUpdateVersion() {
        return updateVersion;
    }

    /** return the update URL or null.
     * @return null or the calue supplied
     */
    @Nullable public String getUpdateURL() {
        return StringSupport.trimOrNull(updateURL);
    }

    /**
     * Get operation to perform.
     * @return operation
     */
    @Nullable public OperationType getOperation() {
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

        if (getHttpClientSecurityParameterstName() != null) {
            getLog().error("-hs / --http-security ignored");
        }

        final List<String> otherArgs = getOtherArgs();
        if (otherArgs.size() > 1) {
            final StringBuffer output = new StringBuffer().append('"');
            for (int i = 2; i <= otherArgs.size() ; i++ ) {
                output.append(otherArgs.get(i-1));
                if (i == otherArgs.size()) {
                    output.append('"');
                } else {
                    output.append(' ');
                }
            }
            getLog().error("Unexpected extra arguments {}", output);
            throw new IllegalArgumentException("Unexpected extra arguments");
        }
        if (list || fullList) {
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
            operation = decodeInput() ;
        } else if (updatePluginId != null) {
            if (uninstallId != null) {
                getLog().error("Cannot Update and Remove in the same operation.");
                throw new IllegalArgumentException("Cannot Update and Remove in the same operation.");
            }
            pluginId = updatePluginId;
            operation = OperationType.UPDATE;
            if (forceUpdateVersion != null) {
                updateVersion = new PluginVersion(forceUpdateVersion);
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
            getLog().error("Missing qualifier. Options are : -l, -fl, -cl, -i, -u, -r, --license");
            throw new IllegalArgumentException("Missing qualifier");
        }
    }
    // Checkstyle: CyclomaticComplexity ON

    /** Given an input string, work out what the parts are.
     * @return Whether this is a remote install or a local one.
     */
    private OperationType decodeInput() {
        try {
            final URL inputAsURL = new URL(input);
            if ("https".equals(inputAsURL.getProtocol()) || "http".equals(inputAsURL.getProtocol())) {
                final int i = input.lastIndexOf('/')+1;
                inputURL = new URL(input.substring(0, i));
                inputName = input.substring(i);
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
    public void printHelp(final PrintStream out) {
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
        out.println(String.format("  %-22s %s", "-cl, --contents-list", "Details of what was installed"));
        out.println(String.format("  %-22s %s", "-i, --input <what>", "Install (file name or web address)"));
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

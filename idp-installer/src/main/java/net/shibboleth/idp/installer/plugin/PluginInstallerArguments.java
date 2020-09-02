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

package net.shibboleth.idp.installer.plugin;

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

import net.shibboleth.ext.spring.cli.AbstractCommandLineArguments;
import net.shibboleth.idp.plugin.PluginVersion;

/**
 * Arguments for Plugin Installer CLI.
 */
public class PluginInstallerArguments extends AbstractCommandLineArguments {

    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(PluginInstallerArguments.class);

    /** The PluginId - usually used to drive the update. */
    @Parameter(names= {"-p", "--pluginId"})
    @Nullable private String pluginId;

    /** Brief info about installed plugins. */
    @Parameter(names= {"-l", "--list"})
    @Nullable private boolean list;

    /** Detailed info about installed plugins. */
    @Parameter(names= {"-fl", "--full-list"})
    @Nullable private boolean fullList;

    /** What to install. */
    @Parameter(names= {"-i", "--input"})
    @Nullable private String input;

    /** Update plugin Id. */
    @Parameter(names= {"-u", "--update"})
    @Nullable private String updatePluginId;

    /** Force update version. */
    @Parameter(names= {"-fu", "--force-update"})
    @Nullable private String forceUpdateVersion;

    /** Name for the {@link HttpClient} . */
    @Parameter(names= {"-h", "--http-client"})
    @Nullable private String httpClientName;

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
        /** Unknown. */
        UNKNOWN
    };

    /** What to do. */
    private OperationType operation = OperationType.UNKNOWN;

    /** Plugin Id (if specified).
     * @return {@link #pluginId}
     */
    @Nullable public String getPluginId() {
        return pluginId;
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
     * @return {@link #fullList}
     */
    public boolean getFullList() {
        return fullList;
    }

    /** Are we doing a List?
     * @return {@link #list}
     */
    public boolean getList() {
        return list;
    }

    /** Return the version to update to or null.
     * @return the version or null
     */
    @Nullable public PluginVersion getUpdateVersion() {
        return updateVersion;
    }

    /**
     * Get operation to perform.
     * @return operation
     */
    @Nullable public OperationType getOperation() {
        return operation;
    }

    /**
     * Get bean name for the httpClient (if specified).
     * @return the name or null
     */
    @Nullable public String getHttpClientName() {
        return httpClientName;
    }

    /** {@inheritDoc} */
    // Checkstyle: CyclomaticComplexity OFF
    public void validate() throws IllegalArgumentException {
        super.validate();

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
            log.error("Unexpected extra arguments {}", output);
            throw new IllegalArgumentException("Unexpected extra arguments");
        }
        if (list || fullList) {
            operation = OperationType.LIST;
            if (input !=  null) {
                log.error("Cannot List and Install in the same operation.");
                throw new IllegalArgumentException("Cannot List and Install in the same operation.");
            }
            if (updatePluginId !=  null) {
                log.error("Cannot List and Update in the same operation.");
                throw new IllegalArgumentException("Cannot List and Update in the same operation.");
            }
        } else if (input != null) {
            if (updatePluginId !=  null) {
                log.error("Cannot Install and Update in the same operation.");
                throw new IllegalArgumentException("Cannot List and Update in the same operation.");
            }
            operation = decodeInput() ;
        } else if (updatePluginId != null) {
            pluginId = updatePluginId;
            operation = OperationType.UPDATE;
            if (forceUpdateVersion != null) {
                updateVersion = new PluginVersion(forceUpdateVersion);
            }
        } else {
            log.error("Missing qualifier. Options are : -l, -fl, -i, -u");
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
                log.trace("Found URL: {}\t{}", inputDirectory, inputName);
                return OperationType.INSTALLREMOTE;
            }
        } catch (final MalformedURLException e) {
            log.trace("urg");
        }
        // Must be a file
        final File inputAsFile = new File(input);
        if (!inputAsFile.exists()) {
            log.error("File {} does not exist", inputAsFile.getAbsolutePath());
            throw new IllegalArgumentException("Input File does not exist");
        }
        final Path inputAsPath = Path.of(inputAsFile.getAbsolutePath());
        inputDirectory = inputAsPath.getParent();
        inputName = inputAsPath.getFileName().toString();
        log.trace("Found File: {}\t{}", inputDirectory, inputName);
        return OperationType.INSTALLDIR;
    }

    /** {@inheritDoc} */
    public void printHelp(final PrintStream out) {
        out.println("Plugin");
        out.println("Provides a command line interface for plugin management operations.");
        out.println();
        out.println("   Plugn [options] springConfiguration [FullName]");
        out.println();
        out.println("      springConfiguration      name of Spring configuration resource to use");
        super.printHelp(out);
        out.println();
        out.println(String.format("  %-22s %s", "-l, --list", "Brief Information of all installed plugins"));
        out.println(String.format("  %-22s %s", "-fl, --full-list", "Full details of all installed plugins"));
        out.println(String.format("  %-22s %s", "-i, --input <what>", "Install (file name or web address)"));
        out.println(String.format("  %-22s %s", "-u, --update <what>", "update (plugin id)"));
        out.println(String.format("  %-22s %s", "-fu, --force-update <version>",
                "force version to update to (requires -u)"));
        out.println(String.format("  %-22s %s", "-h, --http-client <bean ame>",
                "use the named bean for http operations"));
        out.println();
    }

}

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

import java.io.PrintStream;

import javax.annotation.Nullable;

import com.beust.jcommander.Parameter;

import net.shibboleth.ext.spring.cli.AbstractCommandLineArguments;

/**
 * Arguments for Plugin Installer CLI.
 */
public class PluginInstallerArguments extends AbstractCommandLineArguments {

    /** The PluginId - usually used to drive the update. */
    @Parameter(names= {"-p", "--pluginId"})
    @Nullable private String pluginId;

    /** Brief info about installed plugins. */
    @Parameter(names= {"-l", "--list"})
    @Nullable private boolean list;

    /** Detailed info about installed plugins. */
    @Parameter(names= {"-fl", "--full-list"})
    @Nullable private boolean fullList;

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

    /**
     * Get operation to perform.
     *
     * @return operation
     */
    @Nullable public OperationType getOperation() {
        return operation;
    }

    /** {@inheritDoc} */
    public void validate() throws IllegalArgumentException {
        super.validate();

        if (getOtherArgs().size() > 2) {
            throw new IllegalArgumentException("????");
        }
        if (list || fullList) {
            operation = OperationType.LIST;
        }
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
        out.println();
    }

}

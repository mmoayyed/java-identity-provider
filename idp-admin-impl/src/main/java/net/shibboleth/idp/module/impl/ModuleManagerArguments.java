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

package net.shibboleth.idp.module.impl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.client.HttpClient;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLineArguments;
import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

/**
 * Arguments for {@link IdPModule} management CLI.
 */
public class ModuleManagerArguments extends AbstractIdPHomeAwareCommandLineArguments {

    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ModuleManagerArguments.class);

    /** Brief info about installed modules. */
    @Parameter(names= {"-l", "--list"})
    @Nullable private boolean list;

    /** Detailed info about installed modules. */
    @Parameter(names= {"-al", "--full-list"})
    @Nullable private boolean fullList;

    /** ID of module to enable. */
    @Parameter(names= {"-e", "--enable"})
    @Nullable @NonnullElements private List<String> enableModuleIds = new ArrayList<>();

    /** ID of module to enable. */
    @Parameter(names= {"-d", "--disable"})
    @Nullable @NonnullElements private List<String> disableModuleIds = new ArrayList<>();

    /** Clean when disabling. */
    @Parameter(names= {"-f", "--clean"})
    @Nullable private boolean clean;

    /** Name for the {@link HttpClient} . */
    @Parameter(names= {"-h", "--http-client"})
    @Nullable @NotEmpty private String httpClientName;

    /** Name for the {@link HttpClientSecurityParameters} . */
    @Parameter(names= {"-s", "--http-security"})
    @Nullable @NotEmpty private String httpClientSecurityParametersName;

    /**
     * Are we doing a list?
     * 
     * @return {@link #list}
     */
    public boolean getList() {
        return list;
    }

    /**
     * Are we doing a full list?
     * 
     * @return {@link #fullList}
     */
    public boolean getFullList() {
        return fullList;
    }
    
    /**
     * Gets the module IDs to enable.
     * 
     * @return {@link #enableModuleIds}
     */
    @Nullable @NonnullElements @NotLive @Unmodifiable public Collection<String> getEnableModuleIds() {
        return List.copyOf(enableModuleIds);
    }
    
    /**
     * Gets the module IDs to disable.
     * 
     * @return {@link #disableModuleIds}
     */
    @Nullable @NonnullElements @NotLive @Unmodifiable public Collection<String> getDisableModuleIds() {
        return List.copyOf(disableModuleIds);
    }

    /**
     * Are we disabling with the clean option?
     * 
     * @return {@link #clean}
     */
    public boolean getClean() {
        return clean;
    }
    
    /**
     * Get bean name for the {@link HttpClient} (if specified).
     * 
     * @return the name or null
     */
    @Nullable @NotEmpty public String getHttpClientName() {
        return httpClientName;
    }

    /**
     * Get bean name for the {@link HttpClientSecurityParameters} (if specified).
     * 
     * @return the name or null
     */
    @Nullable @NotEmpty public String getHttpClientSecurityParameterstName() {
        return httpClientSecurityParametersName;
    }

    /** {@inheritDoc} */
    @Override
    public void validate() throws IllegalArgumentException {
        super.validate();

        if (enableModuleIds.isEmpty() && disableModuleIds.isEmpty()) {
            if (!list && !fullList) {
                list = true;
            }
        } else if (list || fullList) {
            log.error("Cannot list and enable/disable in the same operation");
            throw new IllegalArgumentException("Cannot list and enable/disable in the same operation.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void printHelp(final PrintStream out) {
        out.println("ModuleManager");
        out.println("Provides a command line interface for IdP Module management operations.");
        out.println();
        out.println("   module [options] [springConfiguration]");
        out.println();
        out.println("      springConfiguration      name of optional Spring configuration resource to use");
        super.printHelp(out);
        out.println();
        out.println(String.format("  %-22s %s", "-l, --list",
                "Brief Information on all installed modules"));
        out.println(String.format("  %-22s %s", "-al, --full-list",
                "Full details on all installed modules"));
        out.println(String.format("  %-22s %s", "-e, --enable <id>",
                "Enable module"));
        out.println(String.format("  %-22s %s", "-u, --disable <id>",
                "Disable module"));
        out.println(String.format("  %-22s %s", "-f, --clean",
                "Clean disabled files instead of preserving them"));
        out.println(String.format("  %-22s %s", "-h, --http-client <bean name>",
                "Use the named bean for HTTP operations"));
        out.println(String.format("  %-22s %s", "-s, --http-security <bean name>",
                "Use the named bean for HTTP security"));
        out.println();
    }

}
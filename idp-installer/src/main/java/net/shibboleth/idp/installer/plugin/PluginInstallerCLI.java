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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import net.shibboleth.ext.spring.cli.AbstractCommandLine;
import net.shibboleth.idp.Version;
import net.shibboleth.idp.installer.plugin.impl.PluginInstaller;
import net.shibboleth.idp.plugin.PluginDescription;
import net.shibboleth.idp.plugin.PluginVersion;
import net.shibboleth.idp.plugin.impl.PluginState;
import net.shibboleth.idp.plugin.impl.PluginState.VersionInfo;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Command line for Plugin Installation.
 */
public final class PluginInstallerCLI extends AbstractCommandLine<PluginInstallerArguments> {

    /** Class logger. */
    @Nullable private Logger log;
    
    /** Where the IdP is installed to. */
    @Nullable private Path idpHome;

    /** A Plugin Installer to use. */
    private PluginInstaller installer;
    
    /** The injected HttpClient. */
    private HttpClient httpClient;

    /**
     * Constrained Constructor.
     */
    private PluginInstallerCLI() {
    }

    /** Set where the IdP is installed to.
     * @param home where
     */
    protected void setIdpHome(@Nullable final String home) {
        if (home == null) {
            getLogger().error("net.shibboleth.idp.cli.idp.home propert not send.  Could not find IdP Home directory");
            return;
        }
        idpHome = Path.of(home);
        
        if (!Files.exists(idpHome) || !Files.isDirectory(idpHome)) {
            getLogger().error("IdP Home Directory {} did not exist or was not a directory", idpHome);
            idpHome = null;
        }
    }
    
    /** Return where the IdP is installed to.
     * @return the home directory.
     */
    @Nullable protected Path getIdpHome() {
        return idpHome;
    }


    /** {@inheritDoc} */
    @Override
    @Nonnull protected Logger getLogger() {
        if (log == null) {
            log = LoggerFactory.getLogger(PluginInstallerCLI.class);
        }
        return log;
    }

    /** {@inheritDoc} */
    @Override
    protected Class<PluginInstallerArguments> getArgumentClass() {
        return PluginInstallerArguments.class;
    }

    /** {@inheritDoc} */
    @Override
    protected String getVersion() {
        return Version.getVersion();
    }
    
    /** {@inheritDoc} */
    protected List<Resource> getAdditionalSpringResources() {
        return List.of(
                new ClassPathResource("net/shibboleth/idp/installer/plugin/patchResources.xml"),
                new FileSystemResource(idpHome.resolve("conf").resolve("global.xml")));
    }
    
    /** {@inheritDoc} */
    protected int doRun(final PluginInstallerArguments args) {
        final int ret = super.doRun(args);
        if (ret != RC_OK) {
            return ret;
        }
        final Set<Entry<String, HttpClient>> clients =
                getApplicationContext().getBeansOfType(HttpClient.class).entrySet();
        if (clients.isEmpty()) {
            log.debug("No HttpClient definitions found.");
        } else {
            final Entry<String, HttpClient> entry = clients.iterator().next();
            httpClient = entry.getValue();
            if (clients.size() > 1) {
                log.warn("Multiple HttpClient beans found; Taking {}", entry.getKey());
            } else {
                log.debug("Selecting HttpClient: {}", entry.getKey());
            }
        }

        try {
            constructPluginInstaller();
            switch (args.getOperation()) {
                case LIST:
                    doList(args.getFullList(), args.getPluginId());
                    break;

                default:
                    getLogger().error("Invalid operation");
                    return RC_IO;
            }

        } catch (final ComponentInitializationException | BeansException e) {
            getLogger().error("Plugin failed", e);
            return RC_IO;
        } 
        return ret;
    }

    /** Build the installer.
     * @throws ComponentInitializationException as required*/
    private void constructPluginInstaller() throws ComponentInitializationException {
        installer= new PluginInstaller();
        installer.setIdpHome(idpHome);
        if (httpClient!= null) {
            installer.setHttpClient(httpClient);
        }
        installer.initialize();
    }

    /** List all installed plugins (or just one if provided).
     * @param fullList whether to do full deatils
     * @param pluginId the pluginId or null.
     */
    private void doList(final boolean fullList, @Nullable final String pluginId) {
        boolean list = false;
        final List<PluginDescription> plugins = installer.getInstalledPlugins();
        for (final PluginDescription plugin: plugins) {
            if (pluginId == null || pluginId.equals(plugin.getPluginId())) {
                list = true;
                System.out.println(String.format("Plugin: %-22s\tCurrent Version: %d.%d.%d",
                       plugin.getPluginId(),
                       plugin.getMajorVersion(),plugin.getMinorVersion(), plugin.getPatchVersion()));
                if (fullList) {
                    printDetails(plugin);
                }
            }
        }
        if (!list) {
            if (pluginId == null) {
                System.out.println("No plugins installed");
            } else {
                System.out.println("Plugin " + pluginId + " not installed");
            }
        }
    }

    /** Print our more information about a plugin.
     * @param plugin what we are interested in.
     */
    private void printDetails(final PluginDescription plugin) {
        log.debug("Interrogating {} ", plugin.getPluginId());
        final PluginState state =  new PluginState(plugin);
        if (httpClient != null) {
            state.setHttpClient(httpClient);
        }
        try {
            state.initialize();
        } catch (final ComponentInitializationException e) {
            log.error("Could not interrogate plugin {}", plugin.getPluginId(), e);
            return;
        }
        final Map<PluginVersion, VersionInfo> versions = state.getAvailableVersions();
        System.out.println("\tVersions ");
        for (final Entry<PluginVersion, VersionInfo> entry  : versions.entrySet()) {
            System.out.println(String.format("\t%s:\tMin=%s\tMax=%s\tSupport level: %s",
                    entry.getKey(),
                    entry.getValue().getMinSupported(),
                    entry.getValue().getMaxSupported(),
                    entry.getValue().getSupportLevel()));
        }
    }

    /** Shim for CLI entry point: Allows the code to be run from a test.
     *
     * @return one of the predefines {@link AbstractCommandLine#RC_INIT},
     * {@link AbstractCommandLine#RC_IO}, {@link AbstractCommandLine#RC_OK}
     * or {@link AbstractCommandLine#RC_UNKNOWN}
     *
     * @param args arguments
     */
    public static int runMain(@Nonnull final String[] args) {
        final PluginInstallerCLI cli = new PluginInstallerCLI();
        cli.setIdpHome(StringSupport.trimOrNull(System.getProperty("net.shibboleth.idp.cli.idp.home")));
        if (cli.getIdpHome() == null) {
            return RC_INIT;
        } else {
            return cli.run(args);
        }
    }
    
    /**
     * CLI entry point.
     * @param args arguments
     */
    public static void main(@Nonnull final String[] args) {
        System.exit(runMain(args));
    }
}

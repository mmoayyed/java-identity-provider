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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.tools.ant.BuildException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import net.shibboleth.idp.Version;
import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLine;
import net.shibboleth.idp.installer.impl.InstallationLogger;
import net.shibboleth.idp.installer.plugin.impl.PluginState.VersionInfo;
import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.idp.plugin.PluginSupport.SupportLevel;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.cli.AbstractCommandLine;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.spring.httpclient.resource.HTTPResource;
import net.shibboleth.idp.plugin.PluginVersion;

/**
 * Command line for Plugin Installation.
 */
public final class PluginInstallerCLI extends AbstractIdPHomeAwareCommandLine<PluginInstallerArguments> {

    /** Class logger. */
    private Logger log;

    /** A Plugin Installer to use. */
    @Nullable private PluginInstaller installer;

    /** Update URLs. */
    private List<URL> updateURLs;

    /**
      * Constrained Constructor.
     */
    private PluginInstallerCLI() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Logger getLogger() {
        Logger theLog = log;
        if (theLog == null) {
            theLog = log = LoggerFactory.getLogger(PluginInstallerCLI.class);
        }
        return theLog;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Class<PluginInstallerArguments> getArgumentClass() {
        return PluginInstallerArguments.class;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected String getVersion() {
        final String result = Version.getVersion();
        assert result != null;
        return result;
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements protected List<Resource> getAdditionalSpringResources() {
        return CollectionSupport.singletonList(
               new ClassPathResource("net/shibboleth/idp/conf/http-client.xml"));
    }
    
    /** {@inheritDoc} */
    //CheckStyle: CyclomaticComplexity|MethodLength OFF
    protected int doRun(@Nonnull final PluginInstallerArguments args) {
        
        if (args.getHttpClientName() == null) {
            args.setHttpClientName("shibboleth.InternalHttpClient");
        }

        final int ret = super.doRun(args);
        if (ret != RC_OK) {
            return ret;
        }
        //
        // Sanity check - we rely on a non null HttpClient (see constructPluginInstaller)
        //
        Constraint.isTrue(getHttpClient()!=null, "no HttpClient supplied");
        
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (args.getUpdateURL() !=null) {
            try {
                updateURLs = CollectionSupport.singletonList(new URL(args.getUpdateURL()));
            } catch (final MalformedURLException e) {
                log.error("Could not convert update URL {}", args.getUpdateURL(), e);
                return RC_INIT;
            }
        } else {
            updateURLs = CollectionSupport.emptyList();
        }

        try (final PluginInstaller inst = new PluginInstaller(Constraint.isNotNull(getHttpClient(), "HJttpClient cannot be non null (by construction"))) {
            constructPluginInstaller(inst, args);
            assert inst == installer;
            final String pluginId = args.getPluginId();

            switch (args.getOperation()) {
                case LIST:
                    if (args.isListAvailable()) {
                        return doListAvailable();
                    }
                    doList(args.isFullList(), args.getPluginId());
                    break;

                case INSTALLDIR:
                    if (pluginId != null) {
                        inst.setPluginId(pluginId);
                    }
                    inst.installPlugin(args.getInputDirectory(), args.getInputFileName(), !args.isNoCheck());
                    break;

                case INSTALLREMOTE:
                    if (pluginId != null) {
                        inst.setPluginId(pluginId);
                    }
                    if (args.isInstallId()) {
                        assert(pluginId != null);
                        return autoPluginFromId(pluginId, !args.isNoCheck());
                    }
                    inst.installPlugin(args.getInputURL(), args.getInputFileName(), !args.isNoCheck());
                    break;

                case UPDATE:
                    assert pluginId != null;
                    doUpdate(pluginId, args.getUpdateVersion(), !args.isNoCheck());
                    break;

                case UNINSTALL:
                    assert pluginId != null;
                    inst.setPluginId(pluginId);
                    inst.uninstall();
                    break;

                case OUTPUTLICENSE:
                    assert pluginId != null;
                    outputLicense(pluginId);
                    break;

                case LISTCONTENTS:
                    assert pluginId != null;
                    inst.setPluginId(pluginId);
                    doContentList(pluginId);
                    break;

                default:
                    getLogger().error("Invalid operation");
                    return RC_INIT;
            }

        } catch (final BeansException e) {
            getLogger().error("Plugin Install failed", e);
            return RC_INIT;
        } catch (final ComponentInitializationException | BuildException e) {
            getLogger().error("Plugin Install failed:", e);
            return RC_IO;
        }
        return ret;
    }
    //CheckStyle: CyclomaticComplexity|MethodLength  ON

    /** Build the installer.
     * @param inst the newly created installed
     * @param args the arguments
     * @throws ComponentInitializationException as required
     */
    private void constructPluginInstaller(final PluginInstaller inst,
            final PluginInstallerArguments args) throws ComponentInitializationException {
        final Path idpHome = Path.of(getApplicationContext().getEnvironment().getProperty("idp.home"));
        assert idpHome != null;
        inst.setIdpHome(idpHome);
        if (!args.isUnattended()) {
            inst.setAcceptKey(new InstallerQuery("Accept this key"));
        }
        inst.setTrustore(args.getTruststore());
        final HttpClient client = getHttpClient();
        //
        // This is null because we set up the bean name before calling super.dorun
        //
        assert client != null;
        inst.setModuleContextSecurityParams(getHttpClientSecurityParameters());
        assert(updateURLs != null);
        inst.setUpdateOverrideURLs(updateURLs);
        inst.setRebuildWar(args.isRebuild());
        inst.initialize();
        installer = inst;
    }
    
    /** Emit the line to System.out or the log if not present.
     * @param message what to emit.
     */
    private void outOrLog(final String message) {
        if (System.out != null) {
            System.out.println(message);
        } else {
            log.info("{}", message);
        }
    }
    
    /** Print our more information about a plugin.
     * Helper method for {@link #doList(boolean, String)}
     * @param plugin what we are interested in.
     */
    private void printDetails(final IdPPlugin plugin) {
        log.debug("Interrogating {}", plugin.getPluginId());
        final PluginState state =  new PluginState(plugin, updateURLs);
        final HttpClient client = getHttpClient();
        if (client != null) {
            state.setHttpClient(client);
        }
        try {
            state.initialize();
        } catch (final ComponentInitializationException e) {
            log.error("Could not interrogate plugin {}", plugin.getPluginId(), e);
            return;
        }
        final Map<PluginVersion, VersionInfo> versionMap = state.getPluginInfo().getAvailableVersions();
        final List<PluginVersion> versionList = new ArrayList<>(versionMap.keySet());
        versionList.sort(null);
        outOrLog("\tVersions ");
        for (final PluginVersion version:versionList) {
            final String downLoadDetails;
            if (state.getPluginInfo().getUpdateBaseName(version) == null ||
                state.getPluginInfo().getUpdateURL(version)==null ) {
                downLoadDetails = " - No download available";
            } else {
                downLoadDetails = "";
            }
            final VersionInfo info = versionMap.get(version);
            outOrLog(String.format("\t%s:\tMin=%s\tMax=%s\tSupport level: %s%s",
                    version,
                    info.getMinSupported(),
                    info.getMaxSupported(),
                    info.getSupportLevel(),
                    downLoadDetails));
        }
    }

    /** Print the license file for the specified plugin.
     * @param pluginId what to list
     */
    private void outputLicense(@Nonnull final String pluginId) {
        assert installer != null;
        final IdPPlugin plugin = installer.getInstalledPlugin(pluginId);
        if (plugin == null) {
            log.error("Plugin {} not installed", pluginId);
            return;
        }
        final String location = plugin.getLicenseFileLocation();
        if (location == null) {
            log.info("Plugin {} has no license", pluginId);
            return;
        }
        try (final InputStream is = plugin.getClass().getResourceAsStream(location)) {
            if (is == null) {
                log.error("Plugin {} license could not be found at {}", pluginId, location);
                return;
            }
            outOrLog(String.format("License for %s", plugin));
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line = reader.readLine();
                while (line != null) {
                    outOrLog(line);
                    line = reader.readLine();
                }
            }
        } catch (final IOException e) {
            log.error("Failed to output license", e);
        }
    }

    /** List all installed plugins (or just one if provided).
     * @param fullList whether to do full deatils
     * @param pluginId the pluginId or null.
     */
    private void doList(final boolean fullList, @Nullable final String pluginId) {
        boolean list = false;
        assert installer != null;
        final List<IdPPlugin> plugins = installer.getInstalledPlugins();
        for (final IdPPlugin plugin: plugins) {
            if (pluginId == null || pluginId.equals(plugin.getPluginId())) {
                list = true;
                outOrLog(String.format("Plugin: %-22s\tCurrent Version: %d.%d.%d",
                       plugin.getPluginId(),
                       plugin.getMajorVersion(),plugin.getMinorVersion(), plugin.getPatchVersion()));
                if (fullList) {
                    printDetails(plugin);
                }
            }
        }
        if (!list) {
            if (pluginId == null) {
                outOrLog("No plugins installed");
            } else {
                outOrLog("Plugin " + pluginId + " not installed");
            }
        }
    }
    
    /** List the contents for the detailed plugin.
     * @param pluginId the pluginId
     */
    private void doContentList(@Nonnull final String pluginId) {
        final PluginInstaller inst = installer;
        assert inst != null;

        final IdPPlugin thePlugin = inst.getInstalledPlugin(pluginId);

        final String fromContentsVersion =  inst.getVersionFromContents();
        final List<Path> contents = inst.getInstalledContents();
        if (thePlugin == null) {
            log.warn("Plugin was not installed {}", pluginId);
            if (fromContentsVersion != null) {
                log.error("Plugin {} not installed, but contents found", pluginId);
                log.debug("{}", contents);
            }
            return;
        }
        if (fromContentsVersion == null) {
            log.error("Plugin {} found, but no contents listed", pluginId);
            return;
        }
        final String installedVersion = new PluginVersion(thePlugin).toString();
        if (!fromContentsVersion.equals(installedVersion)) {
            log.error("Installed version of Plugin {} ({}) does not match contents ({})", 
                    pluginId, installedVersion, fromContentsVersion);
        }
        if (contents.isEmpty()) {
            log.info("No contents");
        } else {
            for (final Path path: contents) {
                outOrLog(String.format("%s", path.toString()));
            }
        }
    }

    /** Go to the well known url (or the provided one) and list all
     * the available plugin ids.
     * @return whether it worked
     */
    private int doListAvailable() {
        final Properties props = loadPluginInfo();
        if (props == null) {
            return RC_IO;
        }

        final Map<String, PluginInfo> plugins = new HashMap<>();
        final Enumeration<Object> en = props.keys();
        while (en.hasMoreElements()) {
            final String key = (String)en.nextElement();
            if (key.endsWith(".versions")) {
                final String pluginId = key.substring(0, key.length()-9);
                final PluginInfo info = new PluginInfo(pluginId, props);
                if (info.isInfoComplete()) {
                    plugins.put(pluginId, info);
                }
            }
        }

        for (final Entry<String, PluginInfo> e: plugins.entrySet()) {
            final PluginVersion nullVersion = new PluginVersion(0, 0, 0);
            assert installer != null;
            final IdPPlugin existingPlugin = installer.getInstalledPlugin(e.getKey());
            if (existingPlugin == null) {
                final PluginVersion version = getBestVersion(nullVersion, e.getValue());
                if (version == null) {
                    log.debug("Plugin {} has no version available", e.getKey());
                } else {
                    outOrLog(String.format("Plugin %s: version %s available for install", e.getKey(), version));
                }
            } else {
                final PluginVersion existingVersion = new PluginVersion(existingPlugin);
                final PluginVersion version = getBestVersion(existingVersion, e.getValue());
                if (version == null) {
                    outOrLog(String.format("Plugin %s: Installed version %s: No update available",
                            e.getKey(),
                            existingVersion));
                } else {
                    outOrLog(String.format("Plugin %s: Installed version %s: Update to %s available",
                            e.getKey(),
                            existingVersion,
                            version));
                }
            }
        }
        return RC_OK;
    }

    /** Load the property file describing all the plugin we know about from a known location.
     * @return the property files plugins.
     */
    private Properties loadPluginInfo() {
        final List<URL> urls;
        final Properties props = new Properties();
        try {
            if (updateURLs == null || updateURLs.isEmpty()) {
                urls = List.of(
                        new URL("https://shibboleth.net/downloads/identity-provider/plugins/plugins.properties"),
                        new URL("http://plugins.shibboleth.net/plugins.properties"));
            } else {
                urls = updateURLs;
            }
        } catch (final IOException e) {
            log.error("Could not load update URLs", e);
            return null;
        }
        for (final URL url: urls) {
            final Resource propertyResource;
            try {
                if ("file".equals(url.getProtocol())) {
                    final String path =url.getPath();
                    assert path != null;
                    propertyResource = new FileSystemResource(path);
                } else if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
                    final HttpClient client = getHttpClient();
                    assert client != null;
                    propertyResource = new HTTPResource(client , url);
                } else {
                    log.error("Only file and http[s] URLs are allowed");
                    continue;
                }
                log.debug("Plugin Listing: Looking for update at {}", propertyResource.getDescription());
                if (!propertyResource.exists()) {
                    log.info("{} could not be located", propertyResource.getDescription());
                    continue;
                }
                props.load(propertyResource.getInputStream());
                return props;
            } catch (final IOException e) {
                log.error("Could not open Update URL {} :", url, e);
                continue;
            }
        }
        log.error("Could not locate any active update servers");
        return null;
    }

    /** Given the pluginId find the best version and install.
     * If already installed whine
     * @param pluginId what to install
     * @param checkVersion are we checking the version.
     * @return installation status
     */
    private int autoPluginFromId(@Nonnull final String pluginId, final boolean checkVersion) {
        final PluginInstaller inst = installer;
        assert inst != null;
        final IdPPlugin existing = inst.getInstalledPlugin(pluginId);
        if (existing != null) {
            log.error("Plugin {} is already installed", pluginId);
            return RC_INIT;
        }
        final Properties props = loadPluginInfo();
        if (props == null) {
            log.error("AutoInstall not possible");
            return RC_INIT;
        }
        final PluginInfo info = new PluginInfo(pluginId, props);
        if (!info.isInfoComplete()) {
            log.error("Plugin {}: Information not found", pluginId);
            return RC_INIT;
        }
        final PluginVersion versionToInstall = getBestVersion(new PluginVersion(0,0,0), info);
        if (versionToInstall == null) {
            log.error("Plugin {}: No version available to install", pluginId);
            return RC_INIT;
        }
        final URL updateURL = info.getUpdateURL(versionToInstall); 
        assert updateURL != null;
        inst.installPlugin(updateURL,
                info.getUpdateBaseName(versionToInstall) + ".tar.gz",
                checkVersion);
        return RC_OK;
    }



    /** Find the best update version.  Helper function for {@linkplain #doUpdate(String, PluginVersion, boolean)}.
     * @param pluginVersion The Plugin version
     * @param pluginInfo all about the plugin
     * @return the best version (or null)
     */
    @Nullable private PluginVersion getBestVersion(final PluginVersion pluginVersion , final PluginInfo pluginInfo) {

        final PluginVersion idPVersion = PluginInstaller.getIdPVersion();

        final List<PluginVersion> availableVersions = new ArrayList<>(pluginInfo.getAvailableVersions().keySet());
        availableVersions.sort(null);
        log.debug("Considering versions: {}", availableVersions);

        for (int i = availableVersions.size()-1; i >= 0; i--) {
            final PluginVersion version = availableVersions.get(i);
            if (version.compareTo(pluginVersion) <= 0) {
                log.debug("Version {} is less than or the same as {}. All done", version, pluginVersion);
                return null;
            }
            final VersionInfo versionInfo = pluginInfo.getAvailableVersions().get(version);
            if (versionInfo.getSupportLevel() != SupportLevel.Current) {
                log.debug("Version {} has support level {}, ignoring", version, versionInfo.getSupportLevel());
                continue;
            }
            if (!pluginInfo.isSupportedWithIdPVersion(version, idPVersion)) {
                log.debug("Version {} is not supported with idpVersion {}", version, idPVersion);
                continue;
            }
            log.debug("Version {} is supported with idpVersion {}", version, idPVersion);
            if (pluginInfo.getUpdateURL(version) == null || pluginInfo.getUpdateBaseName(version) == null) {
                log.debug("Version {} is does not have update information", version);
                continue;
            }
            return version;
        }
        return null;
    }

    /** Update the plugin.
     * @param pluginId the pluginId or null.
     * @param pluginVersion (optionally) the version to update to.
     * @param checkVersion are we checking the version. 
     */
    private void doUpdate(@Nonnull final String pluginId, 
            @Nullable final PluginVersion pluginVersion,
            final boolean checkVersion) {
        
        final PluginInstaller inst = installer;
        assert inst != null;
        final IdPPlugin plugin = inst.getInstalledPlugin(pluginId);
        if (plugin == null) {
            log.error("Plugin {} was not installed", pluginId);
            return;
        }
        log.debug("Interrogating {} ", plugin.getPluginId());
        final PluginState state =  new PluginState(plugin, updateURLs);
        final HttpClient client = getHttpClient();
        if (client != null) {
            state.setHttpClient(client);
        }
        try {
            state.initialize();
        } catch (final ComponentInitializationException e) {
            log.error("Could not interrogate plugin {}", plugin.getPluginId(), e);
            return;
        }
        final PluginVersion installVersion;
        if (pluginVersion == null) {
            installVersion = getBestVersion(new PluginVersion(plugin), state.getPluginInfo());
            if (installVersion == null) {
                log.info("No suitable update version available");
                return;
            }
        } else {
            installVersion = pluginVersion;
            final Map<PluginVersion, VersionInfo> versions = state.getPluginInfo().getAvailableVersions();
            if (!versions.containsKey(installVersion)) {
                log.error("Specified version {} could not be found. Available versions: {}",
                        installVersion, versions.keySet());
                return;
            }
        }
        // just use the tgz version - its an update so it should be jar files only
        final URL updateURL = state.getPluginInfo().getUpdateURL(installVersion); 
        assert updateURL != null;
        inst.installPlugin(updateURL,
                state.getPluginInfo().getUpdateBaseName(installVersion) + ".tar.gz",
                checkVersion);
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

        return cli.run(args);
    }
    
    /**
     * CLI entry point.
     * @param args arguments
     */
    public static void main(@Nonnull final String[] args) {
        System.exit(runMain(args));
    }

    /** Predicate to ask the user if they want to install the trust store provided. */
    private class InstallerQuery implements Predicate<String> {

        /** What to say. */
        @Nonnull
        private final String promptText;

        /**
         * Constructor.
         * @param text What to say before the prompt information
         */
        public InstallerQuery(@Nonnull final String text) {
            promptText = Constraint.isNotNull(text, "Text should not be null");
        }

        /** {@inheritDoc} */
        public boolean test(final String keyString) {
        	if (System.console() == null) {
        		log.error("No Console Attached to installer");
        		return false;
        	}
            System.console().printf("%s:\n%s [yN] ", promptText, keyString);
            System.console().flush();
            final String result  = StringSupport.trimOrNull(System.console().readLine());
            return result != null && "y".equalsIgnoreCase(result.substring(0, 1));
        }
    }
}

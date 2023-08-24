/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.tools.ant.BuildException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import net.shibboleth.idp.Version;
import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLine;
import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.idp.plugin.impl.PluginInfo;
import net.shibboleth.profile.installablecomponent.InstallableComponentInfo;
import net.shibboleth.profile.installablecomponent.InstallableComponentSupport;
import net.shibboleth.profile.installablecomponent.InstallableComponentVersion;
import net.shibboleth.shared.cli.AbstractCommandLine;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

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
    @Nonnull protected List<Resource> getAdditionalSpringResources() {
        return CollectionSupport.singletonList(
               new ClassPathResource("net/shibboleth/idp/conf/http-client.xml"));
    }
    
    /** Null-safe getter for {@linkplain #updateURLs}.
     * @return
     */
    @Nonnull private List<URL> ensureUpdateURLs() {
        List<URL> result = updateURLs;
        if (result == null) {
            updateURLs = result = CollectionSupport.emptyList();
        }
        return result;
    }

    /** {@inheritDoc} */
    //CheckStyle: CyclomaticComplexity|MethodLength OFF
    protected int doRun(@Nonnull final PluginInstallerArguments args) {

        if (args.getHttpClientName() == null) {
            args.setHttpClientName("shibboleth.InternalHttpClient");
        }

        int ret = super.doRun(args);
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
                getLogger().error("Could not convert update URL {}", args.getUpdateURL(), e);
                return RC_INIT;
            }
        }
        boolean doList = false;

        try (final PluginInstaller inst = new PluginInstaller(
                Constraint.isNotNull(getHttpClient(), "HJttpClient cannot be non null (by construction"))) {
            constructPluginInstaller(inst, args);
            assert inst == installer;
            final String pluginId = args.getPluginId();

            switch (args.getOperation()) {
                case LIST:
                    if (args.isListAvailable()) {
                        return doListAvailable();
                    }
                    ret = doList(args.isFullList(), args.getPluginId());
                    break;

                case INSTALLDIR:
                    if (pluginId != null) {
                        inst.setPluginId(pluginId);
                    }
                    inst.installPlugin(args.getInputDirectory(), args.getInputFileName(), !args.isNoCheck());
                    doList = true;
                    break;

                case INSTALLREMOTE:
                    if (pluginId != null) {
                        inst.setPluginId(pluginId);
                    }
                    if (args.isInstallId()) {
                        assert pluginId != null;
                        return autoPluginFromId(pluginId, !args.isNoCheck());
                    }
                    inst.installPlugin(args.getInputURL(), args.getInputFileName(), !args.isNoCheck());
                    doList = true;
                    break;

                case UPDATE:
                    assert pluginId != null;
                    doList = doUpdate(pluginId, args.getUpdateVersion(), !args.isNoCheck());
                    break;

                case UNINSTALL:
                    assert pluginId != null;
                    inst.setPluginId(pluginId);
                    inst.uninstall();
                    doList = true;
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
        if (doList) {
        	// we made a change to do a list operation
            try (final PluginInstaller inst = new PluginInstaller(
                Constraint.isNotNull(getHttpClient(), "HJttpClient cannot be non null (by construction"))) {
	            constructPluginInstaller(inst, args);
	            ret = doList(false, null);
            } catch (ComponentInitializationException e) {
            	getLogger().error("Post Install list failed:", e);
            	return RC_IO;
            }
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
            inst.setAcceptKey(new InstallerSupport.InstallerQuery("Accept this key"));
        }
        inst.setTrustore(args.getTruststore());
        final HttpClient client = getHttpClient();
        //
        // This is null because we set up the bean name before calling super.dorun
        //
        assert client != null;
        inst.setModuleContextSecurityParams(getHttpClientSecurityParameters());
        inst.setUpdateOverrideURLs(ensureUpdateURLs());
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
            getLogger().info("{}", message);
        }
    }
    
    /** Print our more information about a plugin.
     * Helper method for {@link #doList(boolean, String)}
     * @param plugin what we are interested in.
     */
    private void printDetails(final IdPPlugin plugin) {
        getLogger().debug("Interrogating {}", plugin.getPluginId());
        final PluginState state =  new PluginState(plugin, ensureUpdateURLs());
        final HttpClient client = getHttpClient();
        if (client != null) {
            state.setHttpClient(client);
            state.setHttpClientSecurityParameters(getHttpClientSecurityParameters());
        }
        try {
            state.initialize();
        } catch (final ComponentInitializationException e) {
            getLogger().error("Could not interrogate plugin {}", plugin.getPluginId(), e);
            return;
        }
        final var versionMap = state.getPluginInfo().getAvailableVersions();
        final List<InstallableComponentVersion> versionList = new ArrayList<>(versionMap.keySet());
        versionList.sort(null);
        outOrLog("\tVersions ");
        for (final InstallableComponentVersion version:versionList) {
            final String downLoadDetails;
            assert version != null;
            if (state.getPluginInfo().getUpdateBaseName(version) == null ||
                state.getPluginInfo().getUpdateURL(version)==null ) {
                downLoadDetails = " - No download available";
            } else {
                downLoadDetails = "";
            }
            final InstallableComponentInfo.VersionInfo info = versionMap.get(version);
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
            getLogger().error("Plugin {} not installed", pluginId);
            return;
        }
        final String location = plugin.getLicenseFileLocation();
        if (location == null) {
            getLogger().info("Plugin {} has no license", pluginId);
            return;
        }
        try (final InputStream is = plugin.getClass().getResourceAsStream(location)) {
            if (is == null) {
                getLogger().error("Plugin {} license could not be found at {}", pluginId, location);
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
            getLogger().error("Failed to output license", e);
        }
    }

    /** List all installed plugins (or just one if provided).
     * @param fullList whether to do full deatils
     * @param pluginId the pluginId or null.
     * @return {@link AbstractCommandLine#RC_IO} if we hit a module issue, otherwise {@link AbstractCommandLine#RC_OK}  
     */
    private int doList(final boolean fullList, @Nullable final String pluginId) {
        boolean list = false;
        final PluginInstaller inst = installer;
        assert inst != null;
        int result = RC_OK;
        final List<IdPPlugin> plugins = inst.getInstalledPlugins();
        final Set<String> modules = inst.getLoadedModules();
        for (final IdPPlugin plugin: plugins) {
            if (pluginId == null || pluginId.equals(plugin.getPluginId())) {
                list = true;
                outOrLog(String.format("Plugin: %-22s\tCurrent Version: %d.%d.%d",
                       plugin.getPluginId(),
                       plugin.getMajorVersion(),plugin.getMinorVersion(), plugin.getPatchVersion()));
                for (final String module:plugin.getRequiredModules()) {
					if (!modules.contains(module)) {
						getLogger().error("Plugin {} requires non-enabled module {}", plugin.getPluginId(), module);
						result = RC_IO;
					}
                }
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
        return result;
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
            getLogger().warn("Plugin was not installed {}", pluginId);
            if (fromContentsVersion != null) {
                getLogger().error("Plugin {} not installed, but contents found", pluginId);
                getLogger().debug("{}", contents);
            }
            return;
        }
        if (fromContentsVersion == null) {
            getLogger().error("Plugin {} found, but no contents listed", pluginId);
            return;
        }
        final String installedVersion = new InstallableComponentVersion(thePlugin).toString();
        if (!fromContentsVersion.equals(installedVersion)) {
            getLogger().error("Installed version of Plugin {} ({}) does not match contents ({})", 
                    pluginId, installedVersion, fromContentsVersion);
        }
        if (contents.isEmpty()) {
            getLogger().info("No contents");
        } else {
            for (final Path path: contents) {
                outOrLog(String.format("%s", path.toString()));
            }
        }
    }

    /** Find the best update version.
     * @param pluginVersion The Plugin version
     * @param pluginInfo all about the plugin
     * @return the best version (or null)
     */
    @Nullable public InstallableComponentVersion getBestVersion(
            @Nonnull final InstallableComponentVersion pluginVersion,
            @Nonnull final InstallableComponentInfo pluginInfo) {

        final InstallableComponentVersion idpVersion;
        final String idpVersionString = Version.getVersion();
        if (idpVersionString!=null) {
            idpVersion =  new InstallableComponentVersion(idpVersionString);
        } else {
            getLogger().error("Could not locate IdP Version, assuming 5.0.0");
            idpVersion = new InstallableComponentVersion(5,0,0);
        }
        return InstallableComponentSupport.getBestVersion(idpVersion, pluginVersion, pluginInfo);
    }

    /** Go to the well known url (or the provided one) and list all
     * the available plugin ids.
     * @return whether it worked
     */
    private int doListAvailable() {
        final HttpClient client = getHttpClient();
        assert client != null;
        final Properties props = loadAllPluginInfo();
        if (props == null) {
            return RC_IO;
        }

        final Map<String, InstallableComponentInfo> plugins = new HashMap<>();
        final Enumeration<Object> en = props.keys();
        while (en.hasMoreElements()) {
            final String key = (String)en.nextElement();
            if (key.endsWith(".versions")) {
                final String pluginId = key.substring(0, key.length()-9);
                assert pluginId != null;
                final InstallableComponentInfo info = new PluginInfo(pluginId, props);
                if (info.isInfoComplete()) {
                    plugins.put(pluginId, info);
                }
            }
        }

        for (final Entry<String, InstallableComponentInfo> entry: plugins.entrySet()) {
            final InstallableComponentVersion nullVersion = new InstallableComponentVersion(0, 0, 0);
            final String key = entry.getKey();
            final InstallableComponentInfo value = entry.getValue();
            assert key!=null && value !=null;
            assert installer != null;
            final IdPPlugin existingPlugin = installer.getInstalledPlugin(key);
            if (existingPlugin == null) {
                final InstallableComponentVersion version = getBestVersion(nullVersion, value);
                if (version == null) {
                    getLogger().debug("Plugin {} has no version available", entry.getKey());
                } else {
                    outOrLog(String.format("Plugin %s: version %s available for install", entry.getKey(), version));
                }
            } else {
                final InstallableComponentVersion existingVersion = new InstallableComponentVersion(existingPlugin);
                final InstallableComponentVersion version = getBestVersion(existingVersion, value);
                if (version == null) {
                    outOrLog(String.format("Plugin %s: Installed version %s: No update available",
                            entry.getKey(),
                            existingVersion));
                } else {
                    outOrLog(String.format("Plugin %s: Installed version %s: Update to %s available",
                            entry.getKey(),
                            existingVersion,
                            version));
                }
            }
        }
        return RC_OK;
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
            getLogger().error("Plugin {} is already installed", pluginId);
            return RC_INIT;
        }
        final Properties props = loadAllPluginInfo();
        if (props == null) {
            getLogger().error("AutoInstall not possible");
            return RC_INIT;
        }
        final InstallableComponentInfo info = new PluginInfo(pluginId, props);
        if (!info.isInfoComplete()) {
            getLogger().error("Plugin {}: Information not found", pluginId);
            return RC_INIT;
        }
        final InstallableComponentVersion versionToInstall =
                getBestVersion(new InstallableComponentVersion(0,0,0), info);
        if (versionToInstall == null) {
            getLogger().error("Plugin {}: No version available to install", pluginId);
            return RC_INIT;
        }
        final URL updateURL = info.getUpdateURL(versionToInstall); 
        assert updateURL != null;
        inst.installPlugin(updateURL,
                info.getUpdateBaseName(versionToInstall) + ".tar.gz",
                checkVersion);
        return RC_OK;
    }

    /** Download all plugin info from the provide or "known" location.
     * @return the properties.
     */
    private Properties loadAllPluginInfo() {
        final HttpClient client = getHttpClient();
        assert client != null;
        if (ensureUpdateURLs().isEmpty()) {
            try {
                return InstallableComponentSupport.loadInfo(CollectionSupport.listOf(
                        new URL("https://shibboleth.net/downloads/identity-provider/plugins/plugins.properties"),
                        new URL("http://plugins.shibboleth.net/plugins.properties")),
                        client,
                        getHttpClientSecurityParameters());
            } catch (final MalformedURLException e) {
                getLogger().error("Could not contruct URL list");
                return new Properties();
            }
        }
        return  InstallableComponentSupport.loadInfo(ensureUpdateURLs(), client, getHttpClientSecurityParameters());
    }

    /** Update the plugin.
     * @param pluginId the pluginId or null.
     * @param pluginVersion (optionally) the version to update to.
     * @param checkVersion are we checking the version.
     * @return if the installation did any work. 
     */
    private boolean doUpdate(@Nonnull final String pluginId, 
            @Nullable final InstallableComponentVersion pluginVersion,
            final boolean checkVersion) {
        
        final PluginInstaller inst = installer;
        assert inst != null;
        final IdPPlugin plugin = inst.getInstalledPlugin(pluginId);
        if (plugin == null) {
            getLogger().error("Plugin {} was not installed", pluginId);
            return false;
        }
        getLogger().debug("Interrogating {} ", plugin.getPluginId());
        final PluginState state =  new PluginState(plugin, ensureUpdateURLs());
        final HttpClient client = getHttpClient();
        if (client != null) {
            state.setHttpClient(client);
            state.setHttpClientSecurityParameters(getHttpClientSecurityParameters());
        }
        try {
            state.initialize();
        } catch (final ComponentInitializationException e) {
            getLogger().error("Could not interrogate plugin {}", plugin.getPluginId(), e);
            return false;
        }
        final InstallableComponentVersion installVersion;
        if (pluginVersion == null) {
            installVersion = getBestVersion(new InstallableComponentVersion(plugin), state.getPluginInfo());
            if (installVersion == null) {
                getLogger().info("No suitable update version available");
                return false;
            }
        } else {
            installVersion = pluginVersion;
            final var versions = state.getPluginInfo().getAvailableVersions();
            if (!versions.containsKey(installVersion)) {
                getLogger().error("Specified version {} could not be found. Available versions: {}",
                        installVersion, versions.keySet());
                return false;
            }
        }
        // just use the tgz version - its an update so it should be jar files only
        final URL updateURL = state.getPluginInfo().getUpdateURL(installVersion); 
        assert updateURL != null;
        inst.installPlugin(updateURL,
                state.getPluginInfo().getUpdateBaseName(installVersion) + ".tar.gz",
                checkVersion);
        return true;
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
}

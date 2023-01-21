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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import net.shibboleth.idp.installer.impl.InstallationLogger;
import net.shibboleth.idp.installer.plugin.impl.PluginState.VersionInfo;
import net.shibboleth.idp.plugin.PluginSupport;
import net.shibboleth.idp.plugin.PluginSupport.SupportLevel;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.idp.plugin.PluginVersion;

/**
 * Class which encapsulates the information about a given plugin as downloaded
 * fro  the plugin URL (or file).
 */
public class PluginInfo {

    /** regexp for spaces. */
    private static final Pattern SPACE_CONTAINING = Pattern.compile("\\s+");

    /** Class logger. */
    @Nonnull private final Logger log = InstallationLogger.getLogger(PluginInfo.class);

    /** The support information. */
    @Nonnull private final Map<PluginVersion, VersionInfo> versionInfo = new HashMap<>();

    /** The Download information. */
    @Nonnull private final Map<PluginVersion, Pair<URL,String>> downloadInfo = new HashMap<>();

    /** The pluginId. */
    @Nonnull private final String pluginId;

    /** Whether the information was sufficient. */
    private boolean allInfoPresent = true;

    /**
     * Constructor.
     *
     * @param id the id we care about
     * @param props all the properties.
     */
    public PluginInfo(final String id, @Nonnull final Properties props) {
        pluginId = Constraint.isNotNull(StringSupport.trimOrNull(id), "pluginID must be non-null");
        parse(props);
    }

    /** Did the property file have enough information?
     * @return true if something was missing.
     */
    public boolean isInfoComplete() {
        return allInfoPresent;
    }

    /** Get the base URL for this version.
     * @param version which version
     * @return the base URL
     */
    @Nullable public URL getUpdateURL(final PluginVersion version) {
        final Pair<URL, String> p = downloadInfo.get(version);
        if (p == null) {
            return null;
        }
        return p.getFirst();
    }

    /** Get the base Name for this version.
     * @param version which version
     * @return the base name
     */
    @Nullable public String getUpdateBaseName(final PluginVersion version) {
        final Pair<URL, String> p = downloadInfo.get(version);
        if (p == null) {
            return null;
        }
        return p.getSecond();
    }

    /** Get the raw version imfo for this plugin.
     * @return the info.
     */
    public Map<PluginVersion, VersionInfo> getAvailableVersions() {
        return versionInfo;
    }

    /** (try to) populate the information about this plugin.
     * @param props what to load.
     */
    private void  parse(@Nonnull final Properties props) {
        final String name = pluginId + PluginSupport.AVAILABLE_VERSIONS_PROPERTY_SUFFIX;
        final String availableVersions = StringSupport.trim(props.getProperty(name));
        if (availableVersions == null) {
            log.warn("Plugin {}: Could not find {} property.", pluginId, name);
            allInfoPresent = false;
        } else {
            handleAvailableVersions(props, availableVersions);
        }
    }

    /** Given a version find out more.
     * @param props the property files for this plugin we are looking at
     * @param version the version in question.
     */
    // Checkstyle: CyclomaticComplexity OFF
    private void handleAvailableVersion(final Properties props, final String version) {
        final PluginVersion theVersion = new PluginVersion(version);
        if (theVersion.getMajor() == 0 && theVersion.getMinor() == 0 && theVersion.getPatch() == 0) {
            log.warn("Plugin {}: Improbable version {}", pluginId, version);
        }
        if (versionInfo.containsKey(theVersion)) {
            log.warn("Plugin {}: Duplicate version {}", pluginId, version);
        }

        final String maxVersionInfo = StringSupport.trimOrNull(
                props.getProperty(pluginId + PluginSupport.MAX_IDP_VERSION_INTERFIX + version));
        if (maxVersionInfo == null) {
            log.warn("Plugin {}, Version {}: Could not find max idp version.", pluginId, version);
            allInfoPresent = false;
            return;
        }

        final String minVersionInfo = StringSupport.trimOrNull(
                props.getProperty(pluginId + PluginSupport.MIN_IDP_VERSION_INTERFIX + version));
        if (minVersionInfo == null) {
            log.warn("Plugin {}, Version {}: Could not find min idp version.", pluginId, version);
            allInfoPresent = false;
            return;
        }

        final String supportLevelString = StringSupport.trimOrNull(
                props.getProperty(pluginId + PluginSupport.SUPPORT_LEVEL_INTERFIX + version));
        PluginSupport.SupportLevel supportLevel;
        if (supportLevelString == null) {
            log.debug("Plugin {}, Version {}: Could not find support level for {}.", pluginId, version);
            supportLevel = SupportLevel.Unknown;
        } else {
            try {
                supportLevel = Enum.valueOf(SupportLevel.class, supportLevelString);
            } catch (final IllegalArgumentException e) {
                log.warn("Plugin {}, Version {}: Invalid support level {}.", pluginId, version, supportLevelString);
                supportLevel = SupportLevel.Unknown;
            }
        }

        log.debug("Plugin {}: MaxIdP {}, MinIdP {}, Support Level {}",
                pluginId, maxVersionInfo, minVersionInfo, supportLevel);
        final VersionInfo info;
        info = new VersionInfo(new PluginVersion(maxVersionInfo), new PluginVersion(minVersionInfo), supportLevel);
        versionInfo.put(theVersion, info);
        String downloadURL =  StringSupport.trimOrNull(
                getDefaultedValue(props, PluginSupport.DOWNLOAD_URL_INTERFIX, version));
        final String baseName =  StringSupport.trimOrNull(
                getDefaultedValue(props, PluginSupport.BASE_NAME_INTERFIX, version));
        if (baseName != null && downloadURL != null) {
            try {
                if (!downloadURL.endsWith("/")) {
                    downloadURL += "/";
                }
                final URL url = new URL(downloadURL);
                downloadInfo.put(theVersion, new Pair<>(url, baseName));
                log.trace("Plugin {}, version {}: Added download URL {}  baseName {} for {}",
                        pluginId, theVersion, url, baseName);
            } catch (final MalformedURLException e) {
               log.warn("Plugin {}, version {}: Download URL '{}' could not be constructed",
                       pluginId, theVersion, downloadURL, e);
            }
        } else {
            log.info("Plugin {}, version {}: no download information present", pluginId, theVersion);
        }
    }
    // Checkstyle: CyclomaticComplexity ON

    /** Given a list of versions find out more.
     * @param props the property files for this plugin we are looking at
     * @param availableVersions a space delimited array of versions
     */
    private void handleAvailableVersions(final Properties props, final String availableVersions) {
        final String[] versions = SPACE_CONTAINING.split(availableVersions, 0);

        log.debug("Plugin {}: Available versions : {} ", pluginId, availableVersions);
        for (final String version:versions) {
            log.debug("Plugin {}: Considering {}", pluginId, version);
            handleAvailableVersion(props, version);
        }
    }

    /** Look up the key derived from the pluginId, the interfix and the version, but if that
     * fails look for a templated definition.
     * @param props what to look in
     * @param interfix the interface (between the ID and the version)
     * @param version the version.
     * @return the suitable value
     */
    @Nullable private String getDefaultedValue(final Properties props, final String interfix, final String version) {
        String result = props.getProperty(pluginId + interfix + version);
        if (result != null) {
            return result;
        }
        result = props.getProperty(pluginId + interfix + PluginSupport.VERSION_PATTERN);
        if (result == null) {
            return result;
        }
        return result.replaceAll(PluginSupport.VERSION_PATTERN_REGEX, version);
    }

    /** Is the specified plugin supported with this IdP version.
     * @param pluginVersion the version if the plugin as a {@link PluginVersion}
     * @param idPVersion the version if the IDP as a {@link PluginVersion}
     * @return whether it is supported.
     */
    public boolean isSupportedWithIdPVersion(final PluginVersion pluginVersion, final PluginVersion idPVersion) {
        final VersionInfo info = versionInfo.get(pluginVersion);
        if (info == null) {
            log.error("Plugin {}: Unknown version {} supplied.", pluginId, pluginVersion);
            log.debug("Plugin {}: Available {}", pluginId, versionInfo.keySet());
            return false;
        }
        return PluginInfo.isSupportedWithIdPVersion(info, idPVersion);
    }

    /** Is the specified plugin supported with this IdP version.
     * Worker method for all 'isSupportedWith' classes.
     * @param pluginVersionInfo the version info to consider
     * @param idPVersion the version as a {@link PluginVersion}
     * @return whether it is supported.
     */
    public static boolean isSupportedWithIdPVersion(final PluginState.VersionInfo pluginVersionInfo,
            final PluginVersion idPVersion) {
        final int maxCompare = idPVersion.compareTo(pluginVersionInfo.getMaxSupported()); 
        if (maxCompare >= 0) {
            // Exclusive:
            // IdP (test against) Version is GREATER THAN OR EQUAL to our Max
            return false;
        }
        final int minCompare = idPVersion.compareTo(pluginVersionInfo.getMinSupported());
        if (minCompare >= 0) {
            // Inclusive:
            // IdP (test against) version is GREATER THAN OR EQUAL to our Min
            return true;
        }
        return false;
    }
}

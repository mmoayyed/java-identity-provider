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

package net.shibboleth.idp.plugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import net.shibboleth.idp.plugin.InstallableComponentSupport.SupportLevel;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Class which encapsulates the information about a given Installable Component as downloaded
 * from  the appropriate location (as a Properties file).
 * Different variants deal with the max and min supported versions (Plugins get them from a file
 * Non plugins derive them from the version under consideration.
 */
public abstract class InstallableComponentInfo {

    /** regexp for spaces. */
    private static final Pattern SPACE_CONTAINING = Pattern.compile("\\s+");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InstallableComponentInfo.class);

    /** The support information. */
    @Nonnull private final Map<InstallableComponentVersion, InstallableComponentInfo.VersionInfo> versionInfo = new HashMap<>();

    /** The Download information. */
    @Nonnull private final Map<InstallableComponentVersion, Pair<URL,String>> downloadInfo = new HashMap<>();

    /** The Id. */
    @Nonnull private final String componentId;

    /** Whether the information was sufficient. */
    private boolean allInfoPresent = true;

    /**
     * Constructor.
     *
     * @param id the id we care about
     * @param props all the properties.
     */
    public InstallableComponentInfo(@Nonnull final String id, @Nonnull final Properties props) {
        componentId = id;
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
    @Nullable public URL getUpdateURL(@Nonnull final InstallableComponentVersion version) {
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
    @Nullable public String getUpdateBaseName(@Nonnull final InstallableComponentVersion version) {
        final Pair<URL, String> p = downloadInfo.get(version);
        if (p == null) {
            return null;
        }
        return p.getSecond();
    }

    /** Get the raw version info for this component.
     * @return the info.
     */
    @Nonnull public Map<InstallableComponentVersion, InstallableComponentInfo.VersionInfo> getAvailableVersions() {
        return versionInfo;
    }
    
    /** Get the Installable Component Id under consideration
     * @return Returns the componentId.
     */
    @Nonnull protected String getComponentId() {
        return componentId;
    }

    /** (try to) populate the information about this component.
     * @param props what to load.
     */
    private void  parse(@Nonnull final Properties props) {
        final String name = componentId + InstallableComponentSupport.AVAILABLE_VERSIONS_PROPERTY_SUFFIX;
        final String availableVersions = StringSupport.trim(props.getProperty(name));
        if (availableVersions == null) {
            log.warn("Component {}: Could not find {} property.", componentId, name);
            allInfoPresent = false;
        } else {
            handleAvailableVersions(props, availableVersions);
        }
    }

    /** Given a version find out more.
     * @param props the property files for this component we are looking at
     * @param version the version in question.
     */
    private void handleAvailableVersion(@Nonnull final Properties props, @Nonnull final String version) {
        final InstallableComponentVersion theVersion = new InstallableComponentVersion(version);
        if (theVersion.getMajor() == 0 && theVersion.getMinor() == 0 && theVersion.getPatch() == 0) {
            log.warn("Component {}: Improbable version {}", componentId, version);
        }
        if (versionInfo.containsKey(theVersion)) {
            log.warn("Component {}: Duplicate version {}", componentId, version);
        }

        final InstallableComponentVersion maxVersionInfo = getMaxVersion(props, version);
        if (maxVersionInfo == null) {
            log.warn("Component {}, Version {}: Could not find max idp version.", componentId, version);
            allInfoPresent = false;
            return;
        }

        final InstallableComponentVersion minVersionInfo = getMinVersion(props, version);
        if (minVersionInfo == null) {
            log.warn("Component {}, Version {}: Could not find min idp version.", componentId, version);
            allInfoPresent = false;
            return;
        }

        final String supportLevelString = StringSupport.trimOrNull(
                props.getProperty(componentId + InstallableComponentSupport.SUPPORT_LEVEL_INTERFIX + version));
        InstallableComponentSupport.SupportLevel supportLevel;
        if (supportLevelString == null) {
            log.debug("Component {}, Version {}: Could not find support level for {}.", componentId, version);
            supportLevel = SupportLevel.Unknown;
        } else {
            try {
                supportLevel = Enum.valueOf(SupportLevel.class, supportLevelString);
            } catch (final IllegalArgumentException e) {
                log.warn("Component {}, Version {}: Invalid support level {}.", componentId, version, supportLevelString);
                supportLevel = SupportLevel.Unknown;
            }
        }

        log.debug("Component {}: MaxIdP {}, MinIdP {}, Support Level {}",
                componentId, maxVersionInfo, minVersionInfo, supportLevel);
        final InstallableComponentInfo.VersionInfo info;
        info = new InstallableComponentInfo.VersionInfo(maxVersionInfo, minVersionInfo, supportLevel);
        versionInfo.put(theVersion, info);
        String downloadURL =  StringSupport.trimOrNull(
                getDefaultedValue(props, InstallableComponentSupport.DOWNLOAD_URL_INTERFIX, version));
        final String baseName =  StringSupport.trimOrNull(
                getDefaultedValue(props, InstallableComponentSupport.BASE_NAME_INTERFIX, version));
        if (baseName != null && downloadURL != null) {
            try {
                if (!downloadURL.endsWith("/")) {
                    downloadURL += "/";
                }
                final URL url = new URL(downloadURL);
                downloadInfo.put(theVersion, new Pair<>(url, baseName));
                log.trace("Component {}, version {}: Added download URL {}  baseName {} for {}",
                        componentId, theVersion, url, baseName);
            } catch (final MalformedURLException e) {
               log.warn("Component {}, version {}: Download URL '{}' could not be constructed",
                       componentId, theVersion, downloadURL, e);
            }
        } else {
            log.info("Component {}, version {}: no download information present", componentId, theVersion);
        }
    }
    
    /** Find the max supported version version we can be installed into.
     * @param props the properties to look at
     * @param version the component version we are enquiring about
     * @return the version null if not found.
     */
    @Nullable abstract protected InstallableComponentVersion getMaxVersion(@Nonnull final Properties props, @Nonnull final String version);

    /** Find the min supported version we can be installed into.
     * @param props the properties to look at
     * @param version the component version we are enquiring about
     * @return the version null if not found.
     */
    @Nullable abstract protected InstallableComponentVersion getMinVersion(@Nonnull final Properties props, @Nonnull final String version);

    /** Given a list of versions find out more.
     * @param props the property files for the component we are looking at
     * @param availableVersions a space delimited array of versions
     */
    private void handleAvailableVersions(@Nonnull final Properties props, @Nonnull final String availableVersions) {
        final String[] versions = SPACE_CONTAINING.split(availableVersions, 0);

        log.debug("Component {}: Available versions : {} ", componentId, availableVersions);
        for (final String version:versions) {
            assert version  != null;
            log.debug("Component {}: Considering {}", componentId, version);
            handleAvailableVersion(props, version);
        }
    }

    /** Look up the key derived from the component, the interfix and the version, but if that
     * fails look for a templated definition.
     * @param props what to look in
     * @param interfix the interface (between the ID and the version)
     * @param version the version.
     * @return the suitable value
     */
    @Nullable private String getDefaultedValue(@Nonnull final Properties props, @Nonnull final String interfix, @Nonnull final String version) {
        String result = props.getProperty(componentId + interfix + version);
        if (result != null) {
            return result;
        }
        result = props.getProperty(componentId + interfix + InstallableComponentSupport.VERSION_PATTERN);
        if (result == null) {
            return result;
        }
        return result.replaceAll(InstallableComponentSupport.VERSION_PATTERN_REGEX, version);
    }

    /** Can the specified component be installed into this version?
     * @param componentVersion the version if the component as a {@link InstallableComponentVersion}
     * @param intsallIntoVersion the version if the IDP as a {@link InstallableComponentVersion}
     * @return whether it is supported.
     */
    public boolean isSupportedWithIdPVersion(@Nonnull final InstallableComponentVersion componentVersion, @Nonnull final InstallableComponentVersion intsallIntoVersion) {
        final InstallableComponentInfo.VersionInfo info = versionInfo.get(componentVersion);
        if (info == null) {
            log.error("Component {}: Unknown version {} supplied.", componentId, componentVersion);
            log.debug("Component {}: Available {}", componentId, versionInfo.keySet());
            return false;
        }
        return InstallableComponentInfo.isSupportedWithIdPVersion(info, intsallIntoVersion);
    }

    /** Can the specified component be installed into this version?
     * Worker method for all 'isSupportedWith' classes.
     * @param componentVersionInfo the version info to consider
     * @param installIntoVersion the version as a {@link InstallableComponentVersion}
     * @return whether it is supported.
     */
    public static boolean isSupportedWithIdPVersion(@Nonnull final VersionInfo componentVersionInfo,
            @Nonnull final InstallableComponentVersion installIntoVersion) {
        final int maxCompare = installIntoVersion.compareTo(componentVersionInfo.getMaxSupported()); 
        if (maxCompare >= 0) {
            // Exclusive:
            // IdP (test against) Version is GREATER THAN OR EQUAL to our Max
            return false;
        }
        final int minCompare = installIntoVersion.compareTo(componentVersionInfo.getMinSupported());
        if (minCompare >= 0) {
            // Inclusive:
            // IdP (test against) version is GREATER THAN OR EQUAL to our Min
            return true;
        }
        return false;
    }

    /** Encapsulation of the information about a given IdP version. */
    public static class VersionInfo {

        /** Maximum version - this version is NOT SUPPORTED. */
        private final InstallableComponentVersion maxSupported;

        /** Minimum version - this version IS supported. */ 
        private final InstallableComponentVersion minSupported;

        /** support level. */
        private final SupportLevel supportLevel;

        /**
         * Constructor.
         *
         * @param max support level
         * @param min support level
         * @param support support level
         */
        public VersionInfo(final InstallableComponentVersion max, final InstallableComponentVersion min, final SupportLevel support) {
            maxSupported = max;
            minSupported = min;
            supportLevel = support;
        }

        /** get Maximum version - this version is NOT SUPPORTED.
         * @return Returns the maxSupported.
         */
        public InstallableComponentVersion getMaxSupported() {
            return maxSupported;
        }

        /** get Minimum (IdP) version - this version IS supported.
         * @return Returns the minSupported.
         */
        public InstallableComponentVersion getMinSupported() {
            return minSupported;
        }

        /** get support level.
         * @return Returns the supportLevel.
         */
        public SupportLevel getSupportLevel() {
            return supportLevel;
        }
    }
}

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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import net.shibboleth.ext.spring.resource.HTTPResource;
import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.idp.plugin.PluginSupport;
import net.shibboleth.idp.plugin.PluginSupport.SupportLevel;
import net.shibboleth.idp.plugin.PluginVersion;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A class which will answer questions about a plugin state as of now
 * (by querying the information Resources for the current published state). 
 */
public class PluginState extends AbstractInitializableComponent {

    /** regexp for spaces. */
    private static final Pattern SPACE_CONTAINING = Pattern.compile("\\s+");

    /** The plug in in question. */
    @Nonnull private final IdPPlugin plugin;

    /** The version of this plugin. */
    @Nonnull private final PluginVersion myPluginVersion;

    /** The support information. */
    @Nonnull private final Map<PluginVersion, VersionInfo> versionInfo = new HashMap<>();

    /** The Download information. */
    @Nonnull private final Map<PluginVersion, Pair<URL,String>> downloadInfo = new HashMap<>();

    /** My support information. */
    @NonnullAfterInit private VersionInfo myVersionInfo;
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PluginState.class);

    /** The HttpClient to use.*/
    @NonnullAfterInit private HttpClient httpClient;

    /** If overridden these are the urls to us for update (rather than what the plguin asks for. */
    @Nonnull private final List<URL> updateOverrideURLs;

    /**
     * Constructor.
     *
     * @param description what we are talking about.
     * @param updateOverrides override for update locations.  An empty list signifies no overrride.
     */
    public PluginState(@Nonnull final IdPPlugin description, final List<URL> updateOverrides) {
        updateOverrideURLs = Constraint.isNotNull(updateOverrides, "updated Locations must not be null");
        plugin = Constraint.isNotNull(description, "Plugin must not be null");
        myPluginVersion = new PluginVersion(plugin);
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

    /** Set the client.
     * @param what what to set.
     */
    public void setHttpClient(@Nonnull final HttpClient what) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        httpClient = Constraint.isNotNull(what, "HttpClient cannot be null");
    }

    /** Look up the key derived from the pluginId, the interfix and the version, but if that
     * fails look for a templated definition.
     * @param props what to look in
     * @param interfix the interface (between the ID and the version)
     * @param version the version.
     * @return the suitable value
     */
    @Nullable private String getDefaultedValue(final Properties props, final String interfix, final String version) {
        String result = props.getProperty(plugin.getPluginId() + interfix + version);
        if (result != null) {
            return result;
        }
        result = props.getProperty(plugin.getPluginId() + interfix + PluginSupport.VERSION_PATTERN);
        if (result == null) {
            return result;
        }
        return result.replaceAll(PluginSupport.VERSION_PATTERN_REGEX, version);
    }

    /** Given a version find out more.
     * @param props the property files for this plugin we are looking at
     * @param version the version in question.
     * @return true if we processed everything OK.
     */
    // Checkstyle: CyclomaticComplexity OFF
    private boolean handleAvailableVersion(final Properties props, final String version) {
        final PluginVersion theVersion = new PluginVersion(version);
        if (theVersion.getMajor() == 0 && theVersion.getMinor() == 0 && theVersion.getPatch() == 0) {
            log.warn("Plugin {}: Improbable version {}", plugin.getPluginId(), version);
        }
        if (versionInfo.containsKey(theVersion)) {
            log.warn("Plugin {}: Duplicate version {}", plugin.getPluginId(), version);
        }

        final String maxVersionInfo = StringSupport.trimOrNull(
                props.getProperty(plugin.getPluginId() + PluginSupport.MAX_IDP_VERSION_INTERFIX + version));
        if (maxVersionInfo == null) {
            log.warn("Plugin {}, Version {}: Could not find max idp version.", plugin.getPluginId(), version);
            return false;
        }

        final String minVersionInfo = StringSupport.trimOrNull(
                props.getProperty(plugin.getPluginId() + PluginSupport.MIN_IDP_VERSION_INTERFIX + version));
        if (minVersionInfo == null) {
            log.warn("Plugin {}, Version {}: Could not find min idp version.", plugin.getPluginId(), version);
            return false;
        }

        final String supportLevelString = StringSupport.trimOrNull(
                props.getProperty(plugin.getPluginId()+ PluginSupport.SUPPORT_LEVEL_INTERFIX + version));
        PluginSupport.SupportLevel supportLevel;
        if (supportLevelString == null) {
            log.debug("Plugin {}, Version {}: Could not find support level for {}.", plugin.getPluginId(), version);
            supportLevel = SupportLevel.Unknown;
        } else {
            try {
                supportLevel = Enum.valueOf(SupportLevel.class, supportLevelString);
            } catch (final IllegalArgumentException e) {
                log.warn("Plugin {}, Version {}: Invalid support level {}.",
                        plugin.getPluginId(), version, supportLevelString);
                supportLevel = SupportLevel.Unknown;
            }
        }

        log.debug("Plugin {}: MaxIdP {}, MinIdP {}, Support Level {}", 
                plugin.getPluginId(), maxVersionInfo, minVersionInfo, supportLevel);
        final VersionInfo info; 
        info = new VersionInfo(new PluginVersion(maxVersionInfo), new PluginVersion(minVersionInfo), supportLevel);
        versionInfo.put(theVersion, info);
        if (myPluginVersion.equals(theVersion)) {
            myVersionInfo = info;
        }
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
                        plugin.getPluginId(), theVersion, url, baseName);
            } catch (final MalformedURLException e) {
               log.warn("Plugin {}, version {}: Download URL '{}' could not be constructed",
                       plugin.getPluginId(), theVersion, downloadURL, e);
            }
        } else {
            log.info("Plugin {}, version {}: no download information present", plugin.getPluginId(), theVersion);
        }
        return true;
    }
    // Checkstyle: CyclomaticComplexity ON

    /** Given a list of versions find out more.
     * @param props the property files for this plugin we are looking at
     * @param availableVersions a space delimited array of versions
     * @return true if we processed everything OK.
     */
    private boolean handleAvailableVersions(final Properties props, final String availableVersions) {
        final String[] versions = SPACE_CONTAINING.split(availableVersions, 0);

        log.debug("Plugin {}: Available versions : {} ", plugin.getPluginId(), availableVersions);
        for (final String version:versions) {
            log.debug("Plugin {}: Considering {}", plugin.getPluginId(), version);
            if (!handleAvailableVersion(props, version)) {
                return false;
           }
        }
        return true;
    }
    
    /** (try to) populate the information about this plugin.
     * @param propertyResource where to start looking
     * @return whether it worked
     */
    protected boolean populate(@Nonnull final Resource propertyResource) {
        
        try {
            final Properties props = new Properties();
            log.debug("Loading properties from {}", propertyResource.getDescription());
            props.load(propertyResource.getInputStream());
            final String name = plugin.getPluginId() + PluginSupport.AVAILABLE_VERSIONS_PROPERTY_SUFFIX;
            final String availableVersions = StringSupport.trim(props.getProperty(name));
            if (availableVersions == null) {
                log.warn("Plugin {}: Could not find {} property in {}", 
                        plugin.getPluginId(), name, propertyResource.getDescription());
                return false;
            }
            return handleAvailableVersions(props, availableVersions);
        } catch (final IOException e) {
            // INFO - not being there is not a failure
            log.info("Plugin {}: Could not find description {}", 
                    plugin.getPluginId(), propertyResource.getDescription(), e);
            return false;
        }
    }
        
    /** {@inheritDoc} */
    // CheckStyle: CyclomaticComplexity OFF
    protected void doInitialize() throws ComponentInitializationException {
        
        try {
            if (httpClient == null) {
                httpClient = new HttpClientBuilder().buildClient();
            }
            final List<URL> urls;
            if (updateOverrideURLs.isEmpty()) {
                urls = plugin.getUpdateURLs();
                if (urls == null) {
                    log.error("Plugin {} was malformed", plugin.getPluginId());
                    throw new ComponentInitializationException("Could not locate information plugin");
                }
            } else {
                urls = updateOverrideURLs;
            }
            for (final URL url: urls) {
                final Resource propertyResource;
                if ("file".equals(url.getProtocol())) {
                    propertyResource = new FileSystemResource(url.getPath());
                } else if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
                    propertyResource = new HTTPResource(httpClient, url);
                } else {
                    throw new ComponentInitializationException("Only file and http[s] URLs are allowed");
                }

                log.debug("Plugin {}: Looking for update at {}", plugin.getPluginId(),
                        propertyResource.getDescription());
                if (!propertyResource.exists()) {
                    log.info("Plugin {}: {} could not be located", plugin.getPluginId(),
                            propertyResource.getDescription());
                    continue;
                }
                
                if (populate(propertyResource)) {
                    log.debug("Plugin {}: PluginState populated from {}",
                            plugin.getPluginId(), propertyResource.getDescription());
                    if (myVersionInfo == null) {
                        log.error("Plugin {} : Could not find version {} in descriptions at {}",
                                plugin.getPluginId(), myPluginVersion, propertyResource.getDescription());
                    }
                    return;
                }
            }
            log.error("Plugin {}: No available servers found.", plugin.getPluginId());
            throw new ComponentInitializationException("Could not locate information for " + plugin.getPluginId());

        } catch (final IOException e) {
            throw new ComponentInitializationException("Could not locate Update Resource for "
                        + plugin.getPluginId(), e);
        } catch (final ComponentInitializationException e) {
            throw e;
        } catch (final Exception e) {
            throw new ComponentInitializationException("Could not initialize http client for "
                    + plugin.getPluginId(), e);
        } finally {
            super.doInitialize();
        }
    }
    // CheckStyle: CyclomaticComplexity ON
    
    /** Is the specified plugin supported with this IdP version.
     * @param pluginVersion the version if the plugin as a {@link PluginVersion}
     * @param idPVersion the version if the IDP as a {@link PluginVersion}
     * @return whether it is supported.
     */
    public boolean isSupportedWithIdPVersion(final PluginVersion pluginVersion, final PluginVersion idPVersion) {
        final VersionInfo info = versionInfo.get(pluginVersion);
        
        if (info == null) {
            log.error("Plugin {}: Unknown version {} supplied.", plugin.getPluginId(), pluginVersion);
            log.debug("Plugin {}: Available {}", plugin.getPluginId(), versionInfo.keySet());
            return false;
        }
        
        return isSupportedWithIdPVersion(info, idPVersion);
    }
    
    /** Is the specified plugin supported with this IdP version.
     * Worker method for all 'isSupportedWith' classes.
     * @param pluginVersionInfo the version info to consider
     * @param idPVersion the version as a {@link PluginVersion}
     * @return whether it is supported.
     */
    public static boolean isSupportedWithIdPVersion(final VersionInfo pluginVersionInfo,
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
    
    /** Return the current state (from provided plugin).
     * @return Returns the Current Info.
     */
    public VersionInfo getCurrentInfo() {
        return myVersionInfo;
    }
    
    /** Return all announced versions.
     * @return the versions.
     */
    @Nonnull @NotEmpty public Map<PluginVersion, VersionInfo> getAvailableVersions() {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        return versionInfo;
    }
    
    /** Encapsulation of the information about a given IdP version. */
    public static class VersionInfo {
        
        /** Maximum version - this version is NOT SUPPORTED. */ 
        private final PluginVersion maxSupported; 

        /** Minimum version - this version IS supported. */ 
        private final PluginVersion minSupported; 
        
        /** support level. */
        private final SupportLevel supportLevel;

        /**
         * Constructor.
         *
         * @param max support level
         * @param min support level
         * @param support support level
         */
        VersionInfo(final PluginVersion max, final PluginVersion min, final SupportLevel support) {
            maxSupported = max;
            minSupported = min;
            supportLevel = support;
        }

        /** get Maximum version - this version is NOT SUPPORTED.
         * @return Returns the maxSupported.
         */
        public PluginVersion getMaxSupported() {
            return maxSupported;
        }

        /** get Minimum (IdP) version - this version IS supported.
         * @return Returns the minSupported.
         */
        public PluginVersion getMinSupported() {
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

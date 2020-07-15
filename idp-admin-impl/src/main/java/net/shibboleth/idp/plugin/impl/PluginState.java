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

package net.shibboleth.idp.plugin.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import net.shibboleth.ext.spring.resource.HTTPResource;
import net.shibboleth.idp.plugin.PluginDescription;
import net.shibboleth.idp.plugin.PluginSupport;
import net.shibboleth.idp.plugin.PluginVersion;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
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
    @Nonnull private final PluginDescription plugin;
    
    /** The version of this plugin. */
    @Nonnull private final PluginVersion myPluginVersion;
    
    /** The support information. */
    @Nonnull private final Map<PluginVersion, VersionInfo> versionInfo = new HashMap<>();

    /** My support information. */
    @NonnullAfterInit private VersionInfo myVersionInfo;
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PluginState.class);
    
    /** The HttpClient to use.*/
    @NonnullAfterInit private HttpClient httpClient;

    /**
     * Constructor.
     *
     * @param description what we are talking about.
     */
    public PluginState(@Nonnull final PluginDescription description) {
        plugin = Constraint.isNotNull(description, "Plugin must not be null");
        myPluginVersion = new PluginVersion(plugin.getMajorVersion(), 
                plugin.getMinorVersion(), plugin.getPatchVersion());
    }
    
    
    /** Set the client.
     * @param what what to set.
     */
    public void setHttpClient(@Nonnull final HttpClient what) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        httpClient = Constraint.isNotNull(what, "HttpClient must be non null");
    }

    /** Given a version find out more.
     * @param parentResource the root we are looking at
     * @param version the version in question.
     * @return true if we processed everything OK.
     */
    // Checkstyle: CyclomaticComplexity OFF
    private boolean handleAvailableVersion(final Resource parentResource, final String version) {
        final Properties detailsProps = new Properties(3);
        final PluginVersion theVersion = new PluginVersion(version);
        if (theVersion.getMajor() == 0 && theVersion.getMinor() == 0 && theVersion.getPatch() == 0) {
            log.warn("Plugin {}: improbable version {}", plugin.getPluginId(), version);
        }
        if (versionInfo.containsKey(theVersion)) {
            log.warn("Plugin {}: Duplicate version {}", plugin.getPluginId(), version);
        }
        
        try {
            final Resource details = parentResource.createRelative(version + "/")
                    .createRelative(PluginSupport.VERSION_INFO_PATH);
            detailsProps.load(details.getInputStream());
        } catch (final IOException e) {
            log.warn("Could not find details description {}, version {} ", plugin.getPluginId(), version, e);
            return false;
        }
        log.debug("Plugin {}: Version details : {}", plugin.getPluginId(), detailsProps);
        final String maxVersionInfo = StringSupport.trimOrNull(detailsProps.getProperty(PluginSupport.MAX_IDP_VERSION));
        if (maxVersionInfo == null) {
            log.warn("Plugin {}: Could not find max idp version for version {} ", plugin.getPluginId(), version);
            return false;
        }
        final String minVersionInfo = StringSupport.trimOrNull(detailsProps.getProperty(PluginSupport.MIN_IDP_VERSION));
        if (minVersionInfo == null) {
            log.warn("Plugin {}: Could not find min idp version for version {} ", plugin.getPluginId(), version);
            return false;
        }
        final String supportLevel = StringSupport.trimOrNull(detailsProps.getProperty(PluginSupport.SUPPORT_LEVEL));
        if (supportLevel == null) {
            log.warn("Plugin {}: Could not find support level for {}, version {} ", plugin.getPluginId(), version);
            return false;
        }
        log.debug("Plugin {}: MaxIdP {}, MinIdP {}, Support Level {}", 
                plugin.getPluginId(), maxVersionInfo, minVersionInfo, supportLevel);
        final VersionInfo info; 
        try {
            info = new VersionInfo(
                new PluginVersion(maxVersionInfo),
                new PluginVersion(minVersionInfo),
                Integer.parseInt(supportLevel));
        } catch (final NumberFormatException e) {
            log.warn("Plugin {}: version {}: Could not parse version info",
                    plugin.getPluginId(), version, e);
            return false;
        }
        versionInfo.put(theVersion, info);
        if (myPluginVersion.equals(theVersion)) {
            myVersionInfo = info;
        }
        return true;
    }
    // Checkstyle: CyclomaticComplexity ON

    /** Given a list of versions find out more.
     * @param parentResource the root we are looking at
     * @param availableVersions a space delimited array of versions
     * @return true if we processed everything OK.
     */
    private boolean handleAvailableVersions(final Resource parentResource, final String availableVersions) {
        final String[] versions = SPACE_CONTAINING.split(availableVersions, 0);

        log.debug("Plugin {}: available versions : {} ", plugin.getPluginId(), availableVersions);
        for (final String version:versions) {
            log.debug("Plugin {} : considering {}", plugin.getPluginId(), version);
            if (!handleAvailableVersion(parentResource, version)) {
                return false;
           }
        }
        return true;
    }
    
    /** (try to) populate the information about this plugin.
     * @param parentResource where to start looking
     * @return whether it worked
     */
    protected boolean populate(@Nonnull final Resource parentResource) {
        
        final Resource pluginIdResource;
        try {
            pluginIdResource = 
                    parentResource.createRelative(plugin.getPluginId() + "/");
            if (!pluginIdResource.exists()) {
                log.debug("Plugin {}: directory at {} could not be found",
                        plugin.getPluginId(), parentResource.getDescription());
                return false;
            }
        } catch (final IOException e) {
            log.info("Plugin{}: problems open directory at {}",
                        plugin.getPluginId(), parentResource.getDescription(), e);
            return false;
        }
        try {
            final Resource versionsResource =
                    pluginIdResource.createRelative(PluginSupport.AVAILABLE_VERSIONS_PATH);
            final Properties versionsProps = new Properties(1);
            versionsProps.load(versionsResource.getInputStream());
            final String name = plugin.getPluginId() + PluginSupport.AVAILABLE_VERSIONS_PROPERTY_SUFFIX;
            final String availableVersions = StringSupport.trim(versionsProps.getProperty(name));
            if (availableVersions.length() == 0) {
                log.warn("Plugin {}: Could not find {} property in {}", 
                        plugin.getPluginId(), name, parentResource.getDescription());
                return false;
            }
            return handleAvailableVersions(pluginIdResource, availableVersions);
        } catch (final IOException e) {
            // INFO - not being there is not a failure
            log.info("Plugin {}: Could not find description {}", 
                    plugin.getPluginId(), parentResource.getDescription(), e);
            return false;
        }
    }
        
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        
        try {
            if (httpClient == null) {
                httpClient = new HttpClientBuilder().buildClient();
            }
            for (final URL url: plugin.getUpdateURLs()) {
                final Resource parentResource;
                if ("file".equals(url.getProtocol())) {
                    // Kludge to allow classpath backed files
                    parentResource = new FileSystemResource(url.getPath());
                } else {
                    parentResource = new HTTPResource(httpClient, url);
                }

                log.debug("Plugin {}: Looking for update at {}", plugin.getPluginId(),
                        parentResource.getDescription());
                if (!parentResource.exists()) {
                    log.info("Plugin {}: {} could not be located", plugin.getPluginId(),
                            parentResource.getDescription());
                    continue;
                }
                
                if (populate(parentResource)) {
                    log.debug("Plugin {}: PluginState populated from {}",
                            plugin.getPluginId(), parentResource.getDescription());
                    if (myVersionInfo == null) {
                        log.error("Plugin {} : Could not find version {} in descriptions at {}",
                                plugin.getPluginId(), myPluginVersion, parentResource.getDescription());
                    }
                    return;
                }
            }
            log.error("Plugin {}: No available servers found.");
            throw new ComponentInitializationException("Could not locate information for " + plugin.getPluginId());

        } catch (final IOException e) {
            throw new ComponentInitializationException("Could not locate Update Resource for "
                        + plugin.getPluginId(), e);
        } catch (final Exception e) {
            throw new ComponentInitializationException("Could not initialize http client for "
                    + plugin.getPluginId(), e);
        } finally {
            super.doInitialize();
        }
    }
    
    /** Get the current support level for this version.
     * @return the level
     */
    public int getSupportLevel() {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        return myVersionInfo.getSupportLevel();
    }

    /** Is this plugin supported with the current IdP?
     * @return whether it is supported.
     */
    public boolean isSupportedWithIdPVersion() {
        return isSupportedWithIdPVersion(PluginSupport.getIdPVersion());
    }
    
    /** Is this plugin supported with this IdP version.
     * @param idPVersion the version as a {@link String}
     * @return whether it is supported.
     */
    public boolean isSupportedWithIdPVersion(final String idPVersion) {
        return isSupportedWithIdPVersion(new PluginVersion(idPVersion));
    }
    
    /** Is this plugin supported with this IdP version.
     * @param idPVersion the version as a {@link PluginVersion}
     * @return whether it is supported.
     */
    public boolean isSupportedWithIdPVersion(final PluginVersion idPVersion) {
        return isSupportedWithIdPVersion(myVersionInfo, idPVersion);
    }
    
    /** Is the specified plugin supported with this IdP version.
     * @param pluginVersion the version if the plugin as a {@link PluginVersion}
     * @param idPVersion the version if the IDP as a {@link PluginVersion}
     * @return whether it is supported.
     */
    public boolean isSupportedWithIdPVersion(final PluginVersion pluginVersion, final PluginVersion idPVersion) {
        final VersionInfo info = versionInfo.get(pluginVersion);
        
        if (info == null) {
            log.error("Plugin: {} Non existant version {} supplied.", plugin.getPluginId(), pluginVersion);
            log.debug("Plugin: {} available {}", plugin.getPluginId(), versionInfo.keySet());
            return false;
        }
        
        return isSupportedWithIdPVersion(info, idPVersion);
    }
    
    /** Is the specified plugin supported with this IdP version.
     * @param pluginVersion the version if the plugin as a {@link PluginVersion}
     * @param idPVersion the version if the IDP as a {@link String}
     * @return whether it is supported.
     */
    public boolean isSupportedWithIdPVersion(final PluginVersion pluginVersion, final String idPVersion) {
        return isSupportedWithIdPVersion(pluginVersion, new PluginVersion(idPVersion));
    }
        
    /** Is the specified plugin supported with this IdP version.
     * Worker method for all 'isSupportedWith' classes.
     * @param pluginVersionInfo the version info to consider
     * @param idPVersion the version as a {@link PluginVersion}
     * @return whether it is supported.
     */
    protected static boolean isSupportedWithIdPVersion(final VersionInfo pluginVersionInfo,
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
    
    /** Return all announced versions.
     * @return the versions.
     */
    @Nonnull @NotEmpty public Collection<PluginVersion> getAvailableVersions() {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        return versionInfo.keySet();
    }
    
    /** Encapsulation of the information about a given IdP version. */
    protected static class VersionInfo {
        
        /** Maximum version - this version is NOT SUPPORTED. */ 
        private final PluginVersion maxSupported; 

        /** Minimum version - this version IS supported. */ 
        private final PluginVersion minSupported; 
        
        /** support level. */
        private final int supportLevel;

        /**
         * Constructor.
         *
         * @param max support level
         * @param min support level
         * @param support support level
         */
        VersionInfo(final PluginVersion max, final PluginVersion min, final int support) {
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
        public int getSupportLevel() {
            return supportLevel;
        }
    }
}

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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.HttpClient;
import org.opensaml.security.httpclient.HttpClientSecurityContextHandler;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.slf4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import net.shibboleth.idp.Version;
import net.shibboleth.idp.plugin.InstallableComponentInfo.VersionInfo;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.spring.httpclient.resource.HTTPResource;

/** Useful methods for supporting plugins. 
 *
 */
public final class PluginSupport {
    
    /** Property Name suffix for available versions inside {@link IdPPlugin#getUpdateURLs()}. */
    @Nonnull public static final String AVAILABLE_VERSIONS_PROPERTY_SUFFIX = ".versions";

    /** Property Name for Download directory {@link IdPPlugin#getUpdateURLs()}. */
    @Nonnull public static final String DOWNLOAD_URL_INTERFIX = ".downloadURL.";

    /** Property Name for download name {@link IdPPlugin#getUpdateURLs()}. */
    @Nonnull public static final String BASE_NAME_INTERFIX = ".baseName.";

    /** Property Name for max supported IdP version inside inside {@link IdPPlugin#getUpdateURLs()}. */
    @Nonnull public static final String MAX_IDP_VERSION_INTERFIX = ".idpVersionMax.";

    /** Property Name for minimum supported IdP version inside inside {@link IdPPlugin#getUpdateURLs()}. */
    @Nonnull public static final String MIN_IDP_VERSION_INTERFIX = ".idpVersionMin.";

    /** Property Name for support level inside inside {@link IdPPlugin#getUpdateURLs()}. */
    @Nonnull public static final String SUPPORT_LEVEL_INTERFIX = ".supportLevel.";

    /** Used for specifying templated keynames. */
    @Nonnull public static final String VERSION_PATTERN = "%{version}";

    /** Used for specifying templated results. */
    @Nonnull public static final String VERSION_PATTERN_REGEX = "\\%\\{version\\}";

    /** Value for support level pointed to by {@link #SUPPORT_LEVEL_INTERFIX}.*/
    public static enum SupportLevel {
        /** The current release. */
        Current,
        /** Still working but a new version is available. */
        OutOfDate,
        /** Out of Support. */
        Unsupported,
        /** Security alerts against this plugin. */
        Secadv,
        /** Withdrawn. */
        Withdrawn,
        /** Nothing published. */
        Unknown
    }

    /** Class logger. */
    @Nonnull private static Logger log = LoggerFactory.getLogger(PluginSupport.class);
    
    /** Constructor. */
    private PluginSupport() {
    }

    /** Get parse IdP Version (with fallback for testing).
     * @return a {@link InstallableComponentVersion} of the version.
     */
    public static InstallableComponentVersion getIdPVersion() {
        final String idpVersion = Version.getVersion();
        if (idpVersion!=null) {
            return new InstallableComponentVersion(idpVersion);
        }
        log.error("Could not locate IdP Version, assuming 5.0.0");
        return new InstallableComponentVersion(5,0,0);
    }

    /** Find the best update version  (plugin or IdP).
     * @param installIntoVersion The IdP version to check.
     * @param pluginVersion The Plugin version
     * @param pluginInfo all about the plugin
     * @return the best version (or null)
     */
    @Nullable static public InstallableComponentVersion getBestVersion(
            @Nonnull final InstallableComponentVersion installIntoVersion,
            @Nonnull final InstallableComponentVersion pluginVersion,
            @Nonnull final InstallableComponentInfo pluginInfo) {
        final List<InstallableComponentVersion> availableVersions = new ArrayList<>(pluginInfo.getAvailableVersions().keySet());
        availableVersions.sort(null);
        log.debug("Considering versions: {}", availableVersions);
    
        for (int i = availableVersions.size()-1; i >= 0; i--) {
            final InstallableComponentVersion version = availableVersions.get(i);
            if (version.compareTo(pluginVersion) <= 0) {
                log.debug("Version {} is less than or the same as {}. All done", version, pluginVersion);
                return null;
            }
            final VersionInfo versionInfo = pluginInfo.getAvailableVersions().get(version);
            if (versionInfo.getSupportLevel() != SupportLevel.Current) {
                log.debug("Version {} has support level {}, ignoring", version, versionInfo.getSupportLevel());
                continue;
            }
            if (!pluginInfo.isSupportedWithIdPVersion(version, installIntoVersion)) {
                log.debug("Version {} is not supported with idpVersion {}", version, installIntoVersion);
                continue;
            }
            log.debug("Version {} is supported with idpVersion {}", version, installIntoVersion);
            if (pluginInfo.getUpdateURL(version) == null || pluginInfo.getUpdateBaseName(version) == null) {
                log.debug("Version {} is does not have update information", version);
                continue;
            }
            return version;
        }
        return null;
    }

    /** Find the best update version (plugin or IdP).
     * @param pluginVersion The Plugin version
     * @param pluginInfo all about the plugin
     * @return the best version (or null)
     */
    @Nullable static public InstallableComponentVersion getBestVersion(
            @Nonnull final InstallableComponentVersion pluginVersion, @Nonnull final InstallableComponentInfo pluginInfo) {
        return getBestVersion(getIdPVersion(), pluginVersion, pluginInfo);
    }

    /** Load the property file describing all the plugin we know about from a known location.
     * @param updateURLs where to look
     * @param client the http client to use
     * @param securityParameters the HttpClientSecurityParameters, if any
     * @return the property files for the component.
     */
    @Nullable public static Properties loadPluginInfo(@Nonnull final List<URL> updateURLs, @Nonnull final HttpClient client,
            @Nullable final HttpClientSecurityParameters securityParameters) {
        final List<URL> urls;
        final Properties props = new Properties();
        try {
            if (updateURLs.isEmpty()) {
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
                    final HTTPResource httpResource;
                    propertyResource = httpResource = new HTTPResource(client , url);
                    final HttpClientSecurityContextHandler handler = new HttpClientSecurityContextHandler();
                    handler.setHttpClientSecurityParameters(securityParameters);
                    handler.initialize();
                    httpResource.setHttpClientContextHandler(handler);
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
            } catch (final IOException | ComponentInitializationException e) {
                log.error("Could not open Update URL {} :", url, e);
                continue;
            }
        }
        log.error("Could not locate any active update servers");
        return null;
    }
}

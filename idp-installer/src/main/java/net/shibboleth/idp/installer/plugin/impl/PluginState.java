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
import java.net.URL;
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

import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.idp.plugin.PluginSupport.SupportLevel;
import net.shibboleth.idp.plugin.PluginVersion;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.httpclient.HttpClientBuilder;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.spring.httpclient.resource.HTTPResource;

/**
 * A class which will answer questions about a plugin state as of now
 * (by querying the information Resources for the current published state). 
 */
public class PluginState extends AbstractInitializableComponent {

    /** The plug in in question. */
    @Nonnull private final IdPPlugin plugin;

    /** My Plugin Info. */
    @NonnullAfterInit private PluginInfo myPluginInfo;

    /** The version of this plugin. */
    @Nonnull private final PluginVersion myPluginVersion;

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PluginState.class);

    /** The HttpClient to use.*/
    @NonnullAfterInit private HttpClient httpClient;

    /** The Injected security parameters. */
    @Nullable private HttpClientSecurityParameters httpClientSecurityParameters;

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
    
    /** get our PluginInfo.
     * @return our PluginInfo.
     */
    @Nonnull public PluginInfo getPluginInfo() {
        checkComponentActive();
        assert myPluginInfo!=null;
        return myPluginInfo;
    }
    
    /** (try to) populate the information about this plugin.
     * @param propertyResource where to start looking
     * @return whether it worked
     */
    private boolean populate(@Nonnull final Resource propertyResource) {
        
        try {
            final Properties props = new Properties();
            log.debug("Loading properties from {}", propertyResource.getDescription());
            props.load(propertyResource.getInputStream());
            myPluginInfo = new PluginInfo(plugin.getPluginId(), props);
            return myPluginInfo.isInfoComplete();
        } catch (final IOException e) {
            // INFO - not being there is not a failure
            log.info("Plugin {}: Could not find description {}", 
                    plugin.getPluginId(), propertyResource.getDescription(), e);
            return false;
        }
    }

    /** Set the client.
     *
     * @param what what to set.
     */
    public void setHttpClient(@Nonnull final HttpClient what) {
        checkSetterPreconditions();
        httpClient = Constraint.isNotNull(what, "HttpClient cannot be null");
    }

    /** Gets {@link HttpClient} security parameters, if any.
     *
     * @return HTTP client security parameters to use
     */
    @Nullable public HttpClientSecurityParameters getHttpClientSecurityParameters() {
        return httpClientSecurityParameters;
    }

    /**
     * Sets {@link HttpClient} security parameters to use.
     *
     * @param params security parameters
     */
    public void setHttpClientSecurityParameters(@Nullable final HttpClientSecurityParameters params) {
        httpClientSecurityParameters = params;
    }

    /**

    /** {@inheritDoc} */
    // CheckStyle: CyclomaticComplexity OFF
    @SuppressWarnings("unused")
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
                try {
                    if ("file".equals(url.getProtocol())) {
                        final String path = url.getPath();
                        assert path != null;
                        propertyResource = new FileSystemResource(path);
                    } else if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
                            final HTTPResource httpResource;
                            assert(httpClient != null);
                            propertyResource = httpResource = new HTTPResource(httpClient, url);
                            final HttpClientSecurityContextHandler handler = new HttpClientSecurityContextHandler();
                            handler.setHttpClientSecurityParameters(httpClientSecurityParameters);
                            handler.initialize();
                            httpResource.setHttpClientContextHandler(handler);
                    } else {
                        log.error("Plugin {}: Only file and http[s] URLs are allowed: '{}'", plugin.getPluginId(), url);
                        continue;
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
                        if (myPluginInfo.getAvailableVersions().get(myPluginVersion) == null) {
                            log.error("Plugin {} : Could not find version {} in descriptions at {}",
                                    plugin.getPluginId(), myPluginVersion, propertyResource.getDescription());
                        }
                        return;
                    }
                }
                catch (final IOException e) {
                    log.error("Could not open Update Resource for {} :", plugin.getPluginId(), e);
                    continue;
                }
            }
            log.error("Plugin {}: No available servers found.", plugin.getPluginId());
            throw new ComponentInitializationException("Could not locate information for " + plugin.getPluginId());

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

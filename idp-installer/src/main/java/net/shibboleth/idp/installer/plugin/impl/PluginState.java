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

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import net.shibboleth.idp.installer.impl.InstallationLogger;
import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.idp.plugin.PluginSupport.SupportLevel;
import net.shibboleth.shared.spring.httpclient.resource.HTTPResource;
import net.shibboleth.idp.plugin.PluginVersion;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.logic.Constraint;

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
    @Nonnull private final Logger log = InstallationLogger.getLogger(PluginState.class);

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
    
    /** get our PluginInfo.
     * @return our PluginInfo.
     */
    public PluginInfo getPluginInfo() {
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
     * @param what what to set.
     */
    public void setHttpClient(@Nonnull final HttpClient what) {
        checkSetterPreconditions();
        httpClient = Constraint.isNotNull(what, "HttpClient cannot be null");
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
                    if (myPluginInfo.getAvailableVersions().get(myPluginVersion) == null) {
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

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

package net.shibboleth.idp.admin.impl;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.HttpClient;
import org.jetbrains.annotations.NotNull;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.slf4j.Logger;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;

import net.shibboleth.idp.Version;
import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.idp.plugin.impl.PluginInfo;
import net.shibboleth.profile.installablecomponent.InstallableComponentInfo;
import net.shibboleth.profile.installablecomponent.InstallableComponentInfo.VersionInfo;
import net.shibboleth.profile.installablecomponent.InstallableComponentSupport;
import net.shibboleth.profile.installablecomponent.InstallableComponentSupport.SupportLevel;
import net.shibboleth.profile.installablecomponent.InstallableComponentVersion;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.NullableElements;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Guage set to report the Plugins' & IdP's installation and update statuses.
 */
public class InstallableComponentGuageSet extends AbstractIdentifiableInitializableComponent implements MetricSet  {

    /** Default prefix for metrics. */    
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.installedcomponent";
    
    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InstallableComponentGuageSet.class);

    /** The map of gauges. */
    @Nonnull @NonnullElements private final Map<String,Metric> gauges = new HashMap<>();

    /** Where to look for update information. */
    @Nonnull private List<URL> idpUpdateUrls = CollectionSupport.emptyList(); 

    /** How to reach out. */
    @NonnullAfterInit private HttpClient httpClient;

    /** any security parameters needed. */
    @Nullable private HttpClientSecurityParameters securityParams;
    
    /** IdP Version to check. */
    @NonnullAfterInit private InstallableComponentVersion idpVersion;
    
    /** Constructor. */
    public InstallableComponentGuageSet() {
        gauges.put(MetricRegistry.name(DEFAULT_METRIC_NAME, "plugins", "list"),
                new Gauge<Map<String, InstallableComponentVersion>>() {
                    public Map<String, InstallableComponentVersion> getValue() {
                        return getPluginList();
                    }
                });
        gauges.put(MetricRegistry.name(DEFAULT_METRIC_NAME, "plugins", "details"),
                new Gauge<Map<String, InstallableComponentDetails>>() {
                    public Map<String, InstallableComponentDetails> getValue() {
                        return getPluginDetails();
                    }
                });
        gauges.put(MetricRegistry.name(DEFAULT_METRIC_NAME, "idp", "details"),
                new Gauge<InstallableComponentDetails>() {
                    public InstallableComponentDetails getValue() {
                        return getIdPDetails();
                    }
                });

    }
    
    /** Set where to look for idp update info.
     * @param urls what to set.
     */
    public void setIdpUpdateUrls(@Nonnull final List<URL> urls) {
        idpUpdateUrls =  urls;
    }

    /** Set any {@link HttpClientSecurityParameters}.
     * @param params what to set.
     */
    public void setSecurityParams(@Nullable final HttpClientSecurityParameters params) {
        securityParams = params;
    }

    /** Set the {@link HttpClient} to use.
     * @param client what to set.
     */
    public void setHttpClient(@Nonnull final HttpClient client) {
        httpClient = Constraint.isNotNull(client, "HttpClient cannot be null");
    }
    
    /** 
     * Null safe IdP Version.
     * 
     * @return the IdP version
     */
    @Nonnull InstallableComponentVersion getIdPVersion() {
        checkComponentActive();
        assert idpVersion!=null;
        return idpVersion;
    }

    /**
     * Return the list of installed components and their versions.
     * 
     * @return map where the key is the PluginId and the value is the version
     */
    @Nonnull @NotLive private Map<String, InstallableComponentVersion> getPluginList() {
        final Map<String, InstallableComponentVersion> result = new HashMap<>();
        final Iterator<IdPPlugin> plugins = ServiceLoader.load(IdPPlugin.class).iterator();
        while (plugins.hasNext()) {
            final IdPPlugin plugin = plugins.next();
            result.put(plugin.getPluginId(), new InstallableComponentVersion(plugin));
        }
        return CollectionSupport.copyToMap(result);
    }

    private Map<String, InstallableComponentDetails> getPluginDetails() {
        @NullableElements final Map<URL, Properties> pluginInfoCache = new HashMap<>();
        final Map<String, InstallableComponentDetails> result = new HashMap<>();
        final Iterator<IdPPlugin> plugins = ServiceLoader.load(IdPPlugin.class).iterator();
        while (plugins.hasNext()) {
            final IdPPlugin plugin = plugins.next();
            assert plugin!=null;
            final Properties properties = lookupIdPProperties(plugin, pluginInfoCache);
            if (properties==null) {
                log.error("Could not located Plugin version information");
                continue;
            }
            final InstallableComponentInfo info = new PluginInfo(plugin.getPluginId(), properties);
            final InstallableComponentVersion pluginVersion = new InstallableComponentVersion(plugin);
            final VersionInfo verInfo = info.getAvailableVersions().get(pluginVersion);
            if (verInfo == null) {
                log.error("Could not located Plugin version information");
                continue;
            }
            final InstallableComponentVersion newPluginVersion =
                    InstallableComponentSupport.getBestVersion(getIdPVersion(), pluginVersion, info);
            result.put(plugin.getPluginId(),
                    new InstallableComponentDetails(verInfo.getSupportLevel(), newPluginVersion));
        }    
        return CollectionSupport.copyToMap(result);
    }

    /**
     * Look in the cache for the URL and return the properties if its there, 
     * otherwise reach out to the URL and load the properties.
     * 
     * @param plugin the {@link IdPPlugin} to consider
     * @param pluginInfoCache the cache of already looked up info.  Also serves as a negative cache
     * 
     * @return IdP update properties
     */
    @Nullable private Properties lookupIdPProperties(@NotNull final IdPPlugin plugin,
            @NullableElements final Map<URL, Properties> pluginInfoCache) {
        final List<URL> urls;
        try {
            urls = plugin.getUpdateURLs();
        } catch (final IOException e) {
            log.error("Could not locate plugin {} update urls", plugin.getPluginId(), e);
            return null;
        }
        for (final URL url:urls) {
            if (pluginInfoCache.containsKey(url)) {
                return pluginInfoCache.get(url);
            }
        }
        assert httpClient != null;
        final Properties result = InstallableComponentSupport.loadInfo(urls, httpClient, securityParams);
        for (final URL url:urls) {
            // Note - negative caching too.
            pluginInfoCache.put(url, result);
        }
        return result;
    }

    /**
     * Return information about the Installation update state of the IdP.
     * 
     * @return a {@link InstallableComponentDetails}
     */
    @Nullable private InstallableComponentDetails getIdPDetails() {
        try {
            assert httpClient!=null;
            final Properties properties =
                    InstallableComponentSupport.loadInfo(idpUpdateUrls, httpClient, securityParams);
            if (properties == null) {
                log.error("Could not locate IdP update information");
                return null;
            }
            final InstallableComponentInfo info = new IdPInfo(properties);
            final VersionInfo verInfo = info.getAvailableVersions().get(getIdPVersion());
            if (verInfo == null) {
                log.error("Could not located IdP version information");
                return null;
            }
            final InstallableComponentVersion newIdPVersion =
                    InstallableComponentSupport.getBestVersion(getIdPVersion(), getIdPVersion(), info);
            return new InstallableComponentDetails(verInfo.getSupportLevel(), newIdPVersion);
        } catch (final Throwable t) {
            log.error("Check for IdP update status failed unexpectedly", t);
            return null;
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (httpClient == null) {
            throw new ComponentInitializationException("Http Client was null");
        }
        final String idpVersionStr = Version.getVersion();
        if (idpVersionStr == null) {
            // So things work inside eclipse.
            log.error("Could not find Current IdP Version, assuming V5.0.0");
            idpVersion = new InstallableComponentVersion(5,0,0);
        } else {
            idpVersion = new InstallableComponentVersion(idpVersionStr); 
        }
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Metric> getMetrics() {
        return gauges;
    }

    record InstallableComponentDetails(@Nonnull @NotEmpty SupportLevel supportedState,
            @Nullable InstallableComponentVersion updateVersion) { };
    
}
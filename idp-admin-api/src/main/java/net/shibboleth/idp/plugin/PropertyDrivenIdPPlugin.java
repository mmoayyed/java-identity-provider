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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.module.PropertyDrivenIdPModule;
import net.shibboleth.shared.annotation.constraint.NonNegative;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Implementation of {@link IdPPlugin} relying on Java {@link Properties}.
 * 
 * @since 4.1.0
 */
public abstract class PropertyDrivenIdPPlugin extends AbstractIdPPlugin {

    /** Default name of plugin properties resource. */
    @Nonnull @NotEmpty public static final String DEFAULT_RESOURCE = "plugin.properties";

    /** Property for plugin ID. */
    @Nonnull @NotEmpty public static final String PLUGIN_ID_PROPERTY = "plugin.id";

    /** Property for plugin version. */
    @Nonnull @NotEmpty public static final String PLUGIN_VERSION_PROPERTY = "plugin.version";

    /** Property for plugin license. */
    @Nonnull @NotEmpty public static final String PLUGIN_LICENSE_PROPERTY = "plugin.license";

    /** Prefix of property for plugin update URL. */
    @Nonnull @NotEmpty public static final String PLUGIN_URL_PROPERTY = "plugin.url.";

    /** Property for plugin's required modules. */
    @Nonnull @NotEmpty public static final String PLUGIN_REQ_MODULES_PROPERTY = "plugin.modules.required";

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(PropertyDrivenIdPModule.class);

    /** Properties describing plugin. */
    @Nonnull private final Properties pluginProperties;

    /** Non-defaulted plugin ID. */
    @Nullable private String pluginId;
    
    /** Handles parsing of plugin version. */
    @Nullable private PluginVersion pluginVersion;
    
    /** Plugin update URLs. */
    @Nonnull @NonnullElements private List<URL> updateURLs = Collections.emptyList();
    
    /** Required modules. */
    @Nonnull @NonnullElements private Set<String> requiredModules = Collections.emptySet();

    /**
     * Constructor.
     *
     * @param claz type of object used to locate default module.properties resource
     * 
     * @throws IOException if unable to read file
     * @throws PluginException if the plugin is not in a valid state
     */
    public PropertyDrivenIdPPlugin(@Nonnull final Class<? extends IdPPlugin> claz) throws IOException, PluginException {
        this(claz.getResourceAsStream(DEFAULT_RESOURCE));
    }
    
    /**
     * Constructor.
     *
     * @param inputStream property stream
     * 
     * @throws IOException if unable to read file
     * @throws PluginException if the plugin is not in a valid state
     */
    public PropertyDrivenIdPPlugin(@Nonnull final InputStream inputStream)
            throws IOException, PluginException {
        pluginProperties = new Properties();
        pluginProperties.load(inputStream);
        load();
    }

    /**
     * Constructor.
     *
     * @param properties property set
     * 
     * @throws PluginException if the plugin is not in a valid state
     */
    public PropertyDrivenIdPPlugin(@Nonnull final Properties properties) throws PluginException {
        pluginProperties = Constraint.isNotNull(properties, "Properties cannot be null");
        load();
    }
    
    /**
     * Load plugin information from properties.
     * 
     * @throws PluginException on errors
     */
    protected void load() throws PluginException {
        
        pluginId = StringSupport.trimOrNull(pluginProperties.getProperty(PLUGIN_ID_PROPERTY));
        
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) {
            version = StringSupport.trimOrNull(pluginProperties.getProperty(PLUGIN_VERSION_PROPERTY));
            if (version == null) {
                throw new PluginException("No plugin version property or package attribute available for " + pluginId);
            }
        } else {
            log.debug("Ignoring plugin '{}' version property in favor of package manifest", pluginId);
        }
        
        try {
            pluginVersion = new PluginVersion(version);
        } catch (final NumberFormatException e) {
            throw new PluginException(e);
        }

        final List<URL> urls = new ArrayList<>();
        
        for (Integer urlnum = 1; ; ++urlnum) {
            
            final String urlstr = pluginProperties.getProperty(PLUGIN_URL_PROPERTY + urlnum.toString());
            if (urlstr == null) {
                break;
            }
            
            try {
                urls.add(new URL(urlstr));
            } catch (final MalformedURLException e) {
                log.error("Unable to convert property value '{}' to URL", urlstr, e);
            }
        }
        
        urls.addAll(getDefaultUpdateURLs());
        
        updateURLs = List.copyOf(urls);
        
        requiredModules = Set.copyOf(StringSupport.normalizeStringCollection(
                StringSupport.stringToList(pluginProperties.getProperty(PLUGIN_REQ_MODULES_PROPERTY, ""), ",")));

        log.debug("Plugin {} loaded", getPluginId());
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty public String getPluginId() {
        if (pluginId != null) {
            return pluginId;
        }
        return super.getPluginId();
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public List<URL> getUpdateURLs() {
        return updateURLs;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @Unmodifiable @NotLive public Set<String> getRequiredModules() {
        return requiredModules;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public String getLicenseFileLocation() {
        return pluginProperties.getProperty(PLUGIN_LICENSE_PROPERTY);
    }

    /** {@inheritDoc} */
    @NonNegative public int getMajorVersion() {
        assert pluginVersion != null;
        return pluginVersion.getMajor();
    }

    /** {@inheritDoc} */
    @NonNegative public int getMinorVersion() {
        assert pluginVersion != null;
        return pluginVersion.getMinor();
    }

    /** {@inheritDoc} */
    @NonNegative public int getPatchVersion() {
        assert pluginVersion != null;
        return pluginVersion.getPatch();
    }
    
    /**
     * Provides default update locations to use.
     * 
     * @return default update locations
     * @throws PluginException if a derived class throws it (see derived classes)
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive protected List<URL> getDefaultUpdateURLs() throws PluginException {
        return Collections.emptyList();
    }
    
}
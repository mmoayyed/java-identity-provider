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
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

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
    
    protected void load() throws PluginException {
        
        pluginId = StringSupport.trimOrNull(pluginProperties.getProperty(PLUGIN_ID_PROPERTY));
        
        String version = StringSupport.trimOrNull(pluginProperties.getProperty(PLUGIN_VERSION_PROPERTY));
        if (version == null) {
            version = getClass().getPackage().getImplementationVersion();
        }
        
        if (version == null) {
            throw new PluginException("No plugin version property or package attribute available");
        }
        
        try {
            pluginVersion = new PluginVersion(version);
        } catch (final NumberFormatException e) {
            throw new PluginException(e);
        }

        final List<URL> urls = new ArrayList<>();
        
        for (Integer urlnum = 1; ; ++urlnum) {
            
            String urlstr = pluginProperties.getProperty(PLUGIN_URL_PROPERTY + urlnum.toString());
            if (urlstr == null) {
                break;
            } else if (urlstr.startsWith("/")) {
                urlstr = getUpdatePrefix() + urlstr;
            }
            
            try {
                urls.add(new URL(urlstr));
            } catch (final MalformedURLException e) {
                log.error("Unable to convert property value '{}' to URL", urlstr, e);
            }
        }
        
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
    @Nonnull @NonnullElements public List<URL> getUpdateURLs() {
        return updateURLs;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements public Set<String> getRequiredModules() {
        return requiredModules;
    }

    /** {@inheritDoc} */
    @NonNegative public int getMajorVersion() {
        return pluginVersion.getMajor();
    }

    /** {@inheritDoc} */
    @NonNegative public int getMinorVersion() {
        return pluginVersion.getMinor();
    }

    /** {@inheritDoc} */
    @NonNegative public int getPatchVersion() {
        return pluginVersion.getPatch();
    }
    
    /**
     * Gets the default update URL prefix for any relative update locations.
     * 
     * @return update URL prefix
     */
    @Nonnull protected String getUpdatePrefix() {
        return "";
    }
    
}
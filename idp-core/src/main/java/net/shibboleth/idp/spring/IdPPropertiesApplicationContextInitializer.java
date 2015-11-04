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

package net.shibboleth.idp.spring;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.StringUtils;

/**
 * An {@link ApplicationContextInitializer} which attempts to add a properties file property source to the application
 * context environment. The 'conf/idp.properties' file is searched for in well known locations. The 'idp.home' property
 * will be set to the normalized search location if the properties file is found and the property is not already set.
 */
public class IdPPropertiesApplicationContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /** IdP home property. */
    @Nonnull @NotEmpty public static final String IDP_HOME_PROPERTY = "idp.home";

    /** Property that points to more property sources. */
    @Nonnull @NotEmpty public static final String IDP_ADDITIONAL_PROPERTY = "idp.additionalProperties";

    /** Target resource to be searched for. */
    @Nonnull public static final String IDP_PROPERTIES = "/conf/idp.properties";

    /** Well known search locations. */
    @Nonnull public static final String[] SEARCH_LOCATIONS = {"/opt/shibboleth-idp",};

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(IdPPropertiesApplicationContextInitializer.class);

    /** {@inheritDoc} */
    @Override public void initialize(@Nonnull final ConfigurableApplicationContext applicationContext) {
        log.debug("Initializing application context '{}'", applicationContext);

        final ArrayList<String> searchLocations = new ArrayList<>();
        final String homeProperty = applicationContext.getEnvironment().getProperty(IDP_HOME_PROPERTY);
        if (homeProperty != null) {
            log.debug("Prepending idp.home property value '{}' to well-known search locations", homeProperty);
            searchLocations.add(homeProperty);
        }

        searchLocations.addAll(Arrays.asList(getSearchLocations()));

        log.debug("Attempting to find '{}' at search locations '{}'", getSearchTarget(), searchLocations);
        for (final String searchLocation : searchLocations) {

            final String searchPath = searchLocation + getSearchTarget();

            log.debug("Attempting to find resource '{}'", searchPath);
            final Resource resource = applicationContext.getResource(searchPath);

            if (resource.exists()) {
                log.debug("Found resource '{}' at search path '{}'", resource, searchPath);

                final Properties properties = loadProperties(null, resource);
                if (properties == null) {
                    log.warn("Unable to load properties from resource '{}'", resource);
                    return;
                }

                // See if we need to set idp.home as a property ourselves...
                if (homeProperty == null) {
                    if ("classpath:".equals(searchLocation) || (resource instanceof ClassPathResource)) {
                        setIdPHomeProperty(searchLocation, properties);
                    } else {
                        final String searchLocationAbsolutePath = Paths.get(searchLocation).toAbsolutePath().toString();
                        setIdPHomeProperty(searchLocationAbsolutePath, properties);
                    }
                }

                loadAdditionalPropertySources(applicationContext, searchLocation, properties);

                logProperties(properties);

                addPropertySourceToApplicationContext(applicationContext, resource.toString(), properties);

                return;
            }
        }

        log.warn("Unable to find '{}' at well known locations '{}'", getSearchTarget(), getSearchLocations());
    }

    /**
     * Get the target resource to be searched for {@link #IDP_PROPERTIES}.
     * 
     * @return the target resource to be searched for {@link #IDP_PROPERTIES}
     */
    @Nonnull public String getSearchTarget() {
        return IDP_PROPERTIES;
    }

    /**
     * Get the well known search locations {@link #SEARCH_LOCATIONS}.
     * 
     * @return the well known search locations {@link #SEARCH_LOCATIONS}
     */
    @Nonnull public String[] getSearchLocations() {
        return SEARCH_LOCATIONS;
    }

    /**
     * Load properties from the resource.
     * 
     * @param sink if non-null use this instance as the target
     * @param resource the resource
     * @return properties loaded from the resource or {@code  null} if loading failed
     */
    @Nullable public Properties loadProperties(@Nullable final Properties sink, @Nonnull final Resource resource) {
        Constraint.isNotNull(resource, "Resource cannot be null");
        try {
            final Properties properties;
            if (sink != null) {
                properties = sink;
            } else {
                properties = new Properties();
            }
            PropertiesLoaderUtils.fillProperties(properties, resource);
            return properties;
        } catch (final IOException e) {
            log.warn("Unable to load properties from resource '{}'", resource, e);
            return null;
        }
    }

    /**
     * Load additional property sources.
     * 
     * File names of additional property sources are defined by {@link #IDP_ADDITIONAL_PROPERTY}, and are resolved
     * relative to the given search location.
     * 
     * @param applicationContext the application context
     * @param searchLocation the location from which additional property sources are resolved
     * @param properties the properties to be filled with additional property sources
     */
    public void loadAdditionalPropertySources(@Nonnull final ConfigurableApplicationContext applicationContext,
            @Nonnull final String searchLocation, @Nonnull final Properties properties) {
        final String additionalSources = properties.getProperty(IDP_ADDITIONAL_PROPERTY);
        if (additionalSources != null) {
            final String[] sources = additionalSources.split(",");
            for (final String source : sources) {
                final String trimmedSource = StringSupport.trimOrNull(source);
                if (trimmedSource == null) {
                    continue;
                }
                log.debug("Attempting to load properties from resource '{}'", trimmedSource);
                final String pathifiedSource = searchLocation + trimmedSource;
                final Resource additionalResource = applicationContext.getResource(pathifiedSource);
                if (additionalResource.exists()) {
                    log.debug("Found resource '{}' at search path '{}'", additionalResource, pathifiedSource);
                    if (loadProperties(properties, additionalResource) == null) {
                        log.warn("Unable to load properties from resource '{}'", additionalResource);
                        continue;
                    }
                } else {
                    log.warn("Unable to find resource '{}'", additionalResource);
                }
            }
        }
    }

    /**
     * Log property names and values at debug level, suppressing properties whose name matches 'password'.
     * 
     * @param properties the properties to log
     */
    public void logProperties(@Nonnull final Properties properties) {
        if (log.isDebugEnabled()) {
            final Pattern pattern = Pattern.compile("password|credential", Pattern.CASE_INSENSITIVE);
            for (final String name : new TreeSet<>(properties.stringPropertyNames())) {
                final Object value = pattern.matcher(name).find() ? "<suppressed>" : properties.get(name);
                log.debug("Loaded property '{}'='{}'", name, value);
            }
        }
    }

    /**
     * Add property source to the application context environment with lowest precedence.
     * 
     * @param applicationContext the application context
     * @param name the name of the property source to be added to the application context
     * @param properties the properties added to the application context
     */
    public void addPropertySourceToApplicationContext(@Nonnull final ConfigurableApplicationContext applicationContext,
            @Nonnull final String name, @Nonnull final Properties properties) {
        applicationContext.getEnvironment().getPropertySources()
                .addLast(new PropertiesPropertySource(name, properties));
    }

    /**
     * Normalize the path by calling {@link StringUtils#cleanPath(String)}.
     * 
     * @deprecated
     * 
     * @param path the input path
     * @return the normalized path.
     */
    @Deprecated @Nonnull public String normalizePath(@Nonnull final String path) {
        Constraint.isNotNull(path, "Path cannot be null");
        final String normalized = StringUtils.cleanPath(path);
        log.debug("Normalized path '{}' to '{}'", path, normalized);
        return normalized;
    }

    /**
     * Set the {@link #IDP_HOME_PROPERTY} property to the given path if not already set.
     * 
     * @param path the property value
     * @param properties the properties
     */
    public void setIdPHomeProperty(@Nonnull final String path, @Nonnull final Properties properties) {
        Constraint.isNotNull(path, "Path cannot be null");
        Constraint.isNotNull(properties, "Properties cannot be null");

        if (properties.getProperty(IDP_HOME_PROPERTY) != null) {
            log.debug("Will not set '{}' property because it is already set.", IDP_HOME_PROPERTY);
            return;
        }

        log.debug("Setting '{}' property to '{}'", IDP_HOME_PROPERTY, path);

        properties.setProperty(IDP_HOME_PROPERTY, path);
    }

}

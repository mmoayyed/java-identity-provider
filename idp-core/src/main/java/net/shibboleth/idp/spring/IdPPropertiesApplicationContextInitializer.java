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
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.logic.Constraint;

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
public class IdPPropertiesApplicationContextInitializer implements
        ApplicationContextInitializer<ConfigurableApplicationContext> {

    /** IdP home property. */
    @Nonnull public static final String IDP_HOME_PROPERTY = "idp.home";

    /** Target resource to be searched for. */
    @Nonnull public static final String IDP_PROPERTIES = "/conf/idp.properties";

    /** Well known search locations. */
    @Nonnull public static final String[] SEARCH_LOCATIONS = {System.getProperty("idp.home", "."),
            System.getProperty("user.dir") + "/shibboleth-idp", "/opt/shibboleth-idp", "..", "classpath:",};

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(IdPPropertiesApplicationContextInitializer.class);

    /** {@inheritDoc} */
    @Override public void initialize(@Nonnull final ConfigurableApplicationContext applicationContext) {
        log.debug("Initializing application context '{}'", applicationContext);

        log.debug("Attempting to find '{}' at well known locations '{}'", getSearchTarget(), getSearchLocations());
        for (String searchLocation : getSearchLocations()) {

            final String searchPath = searchLocation + getSearchTarget();

            log.debug("Attempting to find resource '{}'", searchPath);
            final Resource resource = applicationContext.getResource(searchPath);

            if (resource.exists()) {
                log.debug("Found resource '{}' at search path '{}'", resource, searchPath);

                final Properties properties = loadProperties(resource);
                if (properties == null) {
                    log.warn("Unable to load properties from resource '{}'", resource);
                    return;
                }

                if ("classpath:".equals(searchLocation) || (resource instanceof ClassPathResource)) {
                    setIdPHomeProperty(searchLocation, properties);
                } else {
                    final String searchLocationAbsolutePath = Paths.get(searchLocation).toAbsolutePath().toString();
                    setIdPHomeProperty(searchLocationAbsolutePath, properties);
                }

                final PropertiesPropertySource propertySource =
                        new PropertiesPropertySource(resource.toString(), properties);

                applicationContext.getEnvironment().getPropertySources().addLast(propertySource);

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
     * Load properties from the resource and log property names and values at debug level, suppressing properties whose
     * name matches 'password'.
     * 
     * @param resource the resource
     * @return properties loaded from the resource or {@code  null} if loading failed
     */
    @Nullable public Properties loadProperties(@Nonnull final Resource resource) {
        Constraint.isNotNull(resource, "Resource cannot be null");
        try {
            final Properties properties = PropertiesLoaderUtils.loadProperties(resource);
            if (log.isDebugEnabled()) {
                log.debug("Loading properties from resource '{}'", resource);
                final Pattern pattern = Pattern.compile("password", Pattern.CASE_INSENSITIVE);
                for (final String name : new TreeSet<String>(properties.stringPropertyNames())) {
                    final Object value = pattern.matcher(name).find() ? "<suppressed>" : properties.get(name);
                    log.debug("Loaded property '{}'='{}'", name, value);
                }
            }
            return properties;
        } catch (IOException e) {
            log.warn("Unable to load properties from resource '{}'", resource, e);
            return null;
        }
    }

    /**
     * Normalize the path by calling {@link StringUtils#cleanPath(String)}.
     * 
     * @param path the input path
     * @return the normalized path.
     */
    @Nonnull public String normalizePath(@Nonnull final String path) {
        Constraint.isNotNull(path, "Path cannot be null");
        final String normalized = StringUtils.cleanPath(path);
        log.debug("Normalized path '{}' to '{}'", path, normalized);
        return normalized;
    }

    /**
     * 
     * Set the {@link #IDP_HOME_PROPERTY} property to the given path if not already set and if not set as a system
     * property.
     * 
     * The property value will be normalized by calling {@link #normalizePath(String)}.
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

        if (System.getProperty(IDP_HOME_PROPERTY) != null) {
            log.debug("Will not set '{}' property because it is already set as a system property '{}'",
                    IDP_HOME_PROPERTY, System.getProperty(IDP_HOME_PROPERTY));
            return;
        }

        final String normalizedPath = normalizePath(path);

        log.debug("Setting '{}' property to '{}'", IDP_HOME_PROPERTY, normalizedPath);

        properties.setProperty(IDP_HOME_PROPERTY, normalizedPath);
    }

}

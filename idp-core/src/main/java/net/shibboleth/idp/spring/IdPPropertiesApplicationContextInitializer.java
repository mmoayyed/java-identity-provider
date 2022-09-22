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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * An {@link ApplicationContextInitializer} which appends properties to the application context's environment.
 * 
 * Properties are loaded from {@link #IDP_PROPERTIES} as well as additional property files specified by
 * {@link #IDP_ADDITIONAL_PROPERTY} if set, or if absent, by locating all files under idp.home/conf/ that
 * end in ".properties".
 * 
 * The {@link #IDP_PROPERTIES} file is searched for in the well location returned by {@link #getSearchLocation()}.
 * 
 * If not already set, the {@link #IDP_HOME_PROPERTY} will be set to the first search location in which the
 * {@link #IDP_PROPERTIES} file is found.
 * 
 * A {@link ConstraintViolationException} will be thrown if the property files can not be found or loaded and
 * {@link #isFailFast(ConfigurableApplicationContext)} returns true.
 */
public class IdPPropertiesApplicationContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /** IdP home property. */
    @Nonnull @NotEmpty public static final String IDP_HOME_PROPERTY = "idp.home";

    /** Property that points to more property sources. */
    @Nonnull @NotEmpty public static final String IDP_ADDITIONAL_PROPERTY = "idp.additionalProperties";

    /** Property that controls auto-search for property sources. */
    @Nonnull @NotEmpty public static final String IDP_AUTOSEARCH_PROPERTY = "idp.searchForProperties";

    /** Target resource to be searched for. */
    @Nonnull public static final String IDP_PROPERTIES = "/conf/idp.properties";

    /** Well known search location. */
    @Nonnull public static final String SEARCH_LOCATION = "/opt/shibboleth-idp";

    /** Property controlling whether to fail fast. */
    @Nonnull public static final String FAILFAST_PROPERTY = "idp.initializer.failFast";

    /** Class logger. */
    @Nonnull private static final Logger LOG =
            LoggerFactory.getLogger(IdPPropertiesApplicationContextInitializer.class);

    /** {@inheritDoc} */
    @Override public void initialize(@Nonnull final ConfigurableApplicationContext applicationContext) {
        LOG.debug("Initializing application context '{}'", applicationContext);

        // TODO: Override default property replacement syntax.
        // We can't do this now because it would break web.xml's use of ${idp.home}
        // If we end up breaking web.xml later, I think we could force that in line.
        // See IDP-1642
        // applicationContext.getEnvironment().setPlaceholderPrefix("%{");
        // applicationContext.getEnvironment().setPlaceholderSuffix("}");
        
        final String searchLocation = selectSearchLocation(applicationContext);
        LOG.debug("Attempting to find '{}' at search location '{}'", getSearchTarget(), searchLocation);

        final String searchPath = searchLocation + getSearchTarget();

        LOG.debug("Attempting to find resource '{}'", searchPath);
        final Resource resource = applicationContext.getResource(searchPath);

        if (resource.exists()) {
            LOG.debug("Found resource '{}' at search path '{}'", resource, searchPath);

            final Properties properties = loadProperties(null, resource);
            if (properties == null) {
                if (isFailFast(applicationContext)) {
                    LOG.error("Unable to load properties from resource '{}'", resource);
                    throw new ConstraintViolationException("Unable to load properties from resource");
                }
                LOG.warn("Unable to load properties from resource '{}'", resource);
                return;
            }

            if ("classpath:".equals(searchLocation) || resource instanceof ClassPathResource) {
                setIdPHomeProperty(searchLocation, properties);
            } else {
                String searchLocationAbsolutePath = Paths.get(searchLocation).toAbsolutePath().toString();
                // Minimal normalization required on Windows to allow SWF's flow machinery to work.
                // Just replace backslashes with forward slashes.
                if (File.separatorChar == '\\') {
                    searchLocationAbsolutePath = searchLocationAbsolutePath.replace('\\', '/');
                }
                setIdPHomeProperty(searchLocationAbsolutePath, properties);
            }

            loadAdditionalPropertySources(applicationContext, searchLocation, properties);

            logProperties(properties);

            appendPropertySource(applicationContext, resource.toString(), properties);

        } else if (isFailFast(applicationContext)) {
            LOG.error("Unable to find '{}' at '{}'", getSearchTarget(), searchLocation);
            throw new ConstraintViolationException(
                    "Unable to find '" + getSearchTarget() + "' at '" + searchLocation + "'");
        } else {
            LOG.warn("Unable to find '{}' at '{}'", getSearchTarget(), searchLocation);
        }
    }

    /**
     * Get the target resource to be searched for. Defaults to {@link #IDP_PROPERTIES}.
     * 
     * @return the target resource to be searched for
     */
    @Nonnull public String getSearchTarget() {
        return IDP_PROPERTIES;
    }

    /**
     * Get the well known search location. Defaults to {@link #SEARCH_LOCATION}.
     * 
     * @return the well known search locations
     */
    @Nonnull public String getSearchLocation() {
        return SEARCH_LOCATION;
    }

    /**
     * Select the location used to search for the target. Prefers the user-defined search location defined by
     * {@link #IDP_HOME_PROPERTY} in the application context. Defaults to the well-known search location returned from
     * {@link #getSearchLocation()}.
     * 
     * @param applicationContext the application context
     * @return the search location used to search for the target
     * @throws ConstraintViolationException if the user-defined search location is empty or ends with '/' and
     *             {@link #isFailFast(ConfigurableApplicationContext)} is true
     */
    @Nonnull public String selectSearchLocation(@Nonnull final ConfigurableApplicationContext applicationContext) {

        Constraint.isNotNull(applicationContext, "Application context cannot be null");
        final String homeProperty = applicationContext.getEnvironment().getProperty(IDP_HOME_PROPERTY);
        if (homeProperty != null && isFailFast(applicationContext)) {
            Constraint.isNotEmpty(homeProperty, "idp.home cannot be empty");
            Constraint.isFalse(homeProperty.endsWith("/"), "idp.home cannot end with '/'");
        }
        return (homeProperty != null) ? homeProperty : getSearchLocation();
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
            final Properties holder = new Properties();
            try (final InputStream is = resource.getInputStream()) {
                final String filename = resource.getFilename();
                if (filename != null && filename.endsWith(".xml")) {
                    holder.loadFromXML(is);
                } else {
                    holder.load(is);
                }
            }
            
            if (sink == null) {
                return holder;
            }

            // Check for duplicates before adding.
            for (final Map.Entry<Object,Object> entry : holder.entrySet()) {
                if (sink.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
                    LOG.warn("Ignoring duplicate property '{}'", entry.getKey());
                }
            }

            return sink;
        } catch (final IOException e) {
            LOG.warn("Unable to load properties from resource '{}'", resource, e);
            return null;
        }
    }

// Checkstyle: AnonInnerLength OFF
    /** 
     * Find out all the additional property files we need to load.
     *   
     * @param searchLocation Where to search from
     * @param properties the content of idp.properties so far
     * 
     * @return a collection of paths
     */
    public static Collection<String> getAdditionalSources(@Nonnull final String searchLocation,
                            @Nonnull final Properties properties) {
        
        final Collection<String> sources = new ArrayList<>();
       
        final Boolean autosearch = Boolean.valueOf(properties.getProperty(IDP_AUTOSEARCH_PROPERTY, "false"));
        if (autosearch) {
            final Path searchRoot = Path.of(searchLocation).resolve("conf");
            if (searchRoot.toFile().isDirectory()) {
                final Path registryRoot = searchRoot.resolve("attributes");
                final String idpPropertiesNative = Path.of(IDP_PROPERTIES).toString();
               
                try {
                    Files.walkFileTree(searchRoot, Collections.singleton(FileVisitOption.FOLLOW_LINKS),
                            Integer.MAX_VALUE,
                            new FileVisitor<Path>() {

                            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                                if (dir.equals(registryRoot)) {
                                    return FileVisitResult.SKIP_SUBTREE;
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                                if (attrs.isRegularFile()
                                        && file.getFileName().toString().endsWith(".properties")
                                        && !file.toString().endsWith(idpPropertiesNative)) {
                                    sources.add(file.toString());
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
                                LOG.error("Error accessing {}", file.toString(), exc);
                                return FileVisitResult.CONTINUE;
                            }

                            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) {
                                return FileVisitResult.CONTINUE;
                            }

                    });
                } catch (final IOException e) {
                    LOG.error("Error searching for additional properties", e);
                }
            }
        }
       
        final String additionalSources = properties.getProperty(IDP_ADDITIONAL_PROPERTY);
        if (additionalSources != null) {
            final String[] split = additionalSources.split(",");
            for (final String s : split) {
                final String trimmedSource = StringSupport.trimOrNull(s);
                if (trimmedSource != null) {
                    sources.add(searchLocation + trimmedSource);
                }
            }
        }
        return sources;
    }
// Checkstyle: AnonInnerLength ON

    
    /**
     * Load additional property sources.
     * 
     * File names of additional property sources are defined by {@link #IDP_ADDITIONAL_PROPERTY}, and are resolved
     * relative to the given search location.
     * 
     * @param applicationContext the application context
     * @param searchLocation the location from which additional property sources are resolved
     * @param properties the properties to be filled with additional property sources
     * @throws ConstraintViolationException if an error occurs loading the additional property sources and
     *             {@link #isFailFast(ConfigurableApplicationContext)} is true
     */
    public void loadAdditionalPropertySources(@Nonnull final ConfigurableApplicationContext applicationContext,
            @Nonnull final String searchLocation, @Nonnull final Properties properties) {
        
        for (final String source : getAdditionalSources(searchLocation, properties)) {
            LOG.debug("Attempting to load properties from resource '{}'", source);
            final Resource additionalResource = applicationContext.getResource(source);
            if (additionalResource.exists()) {
                LOG.debug("Found property resource '{}'", additionalResource);
                if (loadProperties(properties, additionalResource) == null) {
                    if (isFailFast(applicationContext)) {
                        LOG.error("Unable to load properties from resource '{}'", additionalResource);
                        throw new ConstraintViolationException("Unable to load properties from resource");
                    }
                    LOG.warn("Unable to load properties from resource '{}'", additionalResource);
                    continue;
                }
            } else {
                LOG.warn("Unable to find property resource '{}' (check {}?)", additionalResource,
                        IDP_ADDITIONAL_PROPERTY);
            }
        }
    }
    
    /**
     * Log property names and values at debug level, suppressing properties whose name matches 'password',
     * 'credential', 'secret', or 'salt'.
     * 
     * @param properties the properties to log
     */
    public void logProperties(@Nonnull final Properties properties) {
        if (LOG.isDebugEnabled()) {
            final Pattern pattern = Pattern.compile("password|credential|secret|salt|key", Pattern.CASE_INSENSITIVE);
            for (final String name : new TreeSet<>(properties.stringPropertyNames())) {
                final Object value = pattern.matcher(name).find() ? "<suppressed>" : properties.get(name);
                LOG.debug("Loaded property '{}'='{}'", name, value);
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
    public void appendPropertySource(@Nonnull final ConfigurableApplicationContext applicationContext,
            @Nonnull final String name, @Nonnull final Properties properties) {
        applicationContext.getEnvironment().getPropertySources()
                .addLast(new PropertiesPropertySource(name, properties));
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
            LOG.debug("Will not set '{}' property because it is already set.", IDP_HOME_PROPERTY);
            return;
        }

        LOG.debug("Setting '{}' property to '{}'", IDP_HOME_PROPERTY, path);

        properties.setProperty(IDP_HOME_PROPERTY, path);
    }

    /**
     * Whether we fail immediately if the config is bogus. Defaults to true. Controlled by the value of the
     * {@link #FAILFAST_PROPERTY}.
     * <b>This functionality is reserved for use in tests </b> where is is usually used to allow
     * tests to be run in the presence of partial configuration.
     * 
     * @param applicationContext the application context
     * @return whether we fail immediately if the config is faulty or incomplete.
     */
    public boolean isFailFast(@Nonnull final ConfigurableApplicationContext applicationContext) {
        Constraint.isNotNull(applicationContext, "Application context cannot be null");
        final String failFast = applicationContext.getEnvironment().getProperty(FAILFAST_PROPERTY);
        return (failFast == null) ? true : Boolean.parseBoolean(failFast);
    }

}

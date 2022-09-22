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

package net.shibboleth.idp.profile.spring.factory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.VfsResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;
import org.springframework.webflow.config.FlowDefinitionResource;
import org.springframework.webflow.core.collection.AttributeMap;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Derivation of SWF-supplied resource factory for flow definitions.
 * 
 * <p>This implementation overrides the behavior of the built-in factory with regard to handling
 * absolute paths while still supporting relative paths.</p>
 */
public class FlowDefinitionResourceFactory {

    /** Classpath scheme prefix. */
    @Nonnull @NotEmpty private static final String CLASSPATH_SCHEME = "classpath:";

    /** Wildcard classpath scheme prefix. */
    @Nonnull @NotEmpty private static final String CLASSPATH_STAR_SCHEME = "classpath*:";

    /** File scheme prefix. */
    @Nonnull @NotEmpty private static final String FILESYSTEM_SCHEME = "file:";

    /** Path separator. */
    @Nonnull @NotEmpty private static final String SLASH = "/";

    /** Spring resource loader. */
    @Nonnull private final ResourceLoader resourceLoader;

    /**
     * Creates a new flow definition resource factory using the specified resource loader.
     * 
     * @param loader the resource loader
     */
    public FlowDefinitionResourceFactory(@Nonnull final ResourceLoader loader) {
        resourceLoader = Constraint.isNotNull(loader, "The resource loader cannot be null");
    }

    /**
     * Create a flow definition resource from the path location provided. The returned resource
     * will be configured with the provided attributes and flow id.
     * 
     * @param basePath base location to use for a relative path
     * @param path the encoded {@link Resource} path.
     * @param attributes the flow definition meta attributes to configure
     * @param flowId the flow definition id to configure
     * 
     * @return the flow definition resource
     */
    public FlowDefinitionResource createResource(@Nullable final String basePath,
            @Nonnull @NotEmpty final String path, @Nonnull final AttributeMap<Object> attributes,
            @Nonnull @NotEmpty final String flowId) {
        Constraint.isNotEmpty(path, "Flow path cannot be null or empty");
        Constraint.isNotEmpty(flowId, "Flow ID cannot be null or empty");
        
        Resource resource;
        if (basePath == null || isAbsolute(path)) {
            resource = resourceLoader.getResource(path);
        } else {
            resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                try {
                    String localBasePath = basePath;
                    if (!localBasePath.endsWith(SLASH)) {
                        // the basePath must end with a slash to create a relative resource
                        localBasePath = basePath + SLASH;
                    }
                    resource = resourceLoader.getResource(localBasePath).createRelative(path);
                } catch (final IOException e) {
                    throw new IllegalStateException("The base path cannot be resolved from '" + basePath + "'", e);
                }
            }
        }
        
        return new FlowDefinitionResource(flowId, resource, attributes);
    }

    /**
     * Create an array of flow definition resources from the path pattern location provided.
     * 
     * <p>Unlike in the explicit case, we must have a base location provided here to properly
     * compute the flow IDs.</p>
     * 
     * @param basePath base location for a relative path and in truncating to obtain the flow IDs
     * @param pattern the encoded {@link Resource} path pattern
     * @param attributes the flow definition meta attributes to configure
     * 
     * @return the flow definition resources
     * @throws IOException if resolving the resources fails
     */
    @Nonnull public Collection<FlowDefinitionResource> createResources(@Nonnull @NotEmpty final String basePath,
            @Nonnull @NotEmpty final String pattern, @Nonnull final AttributeMap<Object> attributes)
                    throws IOException {
        Constraint.isNotEmpty(basePath, "Base location cannot be null or empty");
        Constraint.isNotEmpty(pattern, "Flow pattern cannot be null or empty");
        if (!(resourceLoader instanceof ResourcePatternResolver)) {
            throw new IllegalStateException(
                    "Cannot create flow definition resources from patterns without a ResourcePatternResolver");
        }

        final Resource[] resources;
        final ResourcePatternResolver resolver = (ResourcePatternResolver) resourceLoader;
        
        if (isAbsolute(pattern)) {
            resources = resolver.getResources(pattern);
        } else {
            if (basePath.endsWith(SLASH) || pattern.startsWith(SLASH)) {
                resources = resolver.getResources(basePath + pattern);
            } else {
                resources = resolver.getResources(basePath + SLASH + pattern);
            }
        }
        
        if (resources.length == 0) {
            return Collections.emptyList();
        }
        
        final Collection<FlowDefinitionResource> flowResources = new ArrayList<>(resources.length);
        for (final Resource resource : resources) {
            flowResources.add(new FlowDefinitionResource(getFlowId(basePath, resource), resource, attributes));
        }
        return flowResources;
    }

    /**
     * Obtains the flow id from the flow resource. By default, the flow id becomes the portion of the path between the
     * basePath and the filename.
     * 
     * <p>This is the key override from the SWF version, because it only operates on pattern-based flows, and it
     * assumes a base path to strip. Explicit flow mappings are all assumed to have a flow ID explicitly assigned.</p>
     * 
     * @param basePath the base path applied to the computation
     * @param flowResource the flow resource
     * 
     * @return the flow id
     * 
     * @throws IOException if unable to obtain information about the underlying resource 
     */
    protected String getFlowId(@Nonnull @NotEmpty final String basePath, @Nonnull final Resource flowResource)
            throws IOException {
        final String localBasePath = removeScheme(basePath);
        final String filePath;
        if (flowResource instanceof ContextResource) {
            filePath = ((ContextResource) flowResource).getPathWithinContext();
        } else if (flowResource instanceof ClassPathResource) {
            filePath = ((ClassPathResource) flowResource).getPath();
        } else if (flowResource instanceof FileSystemResource) {
            filePath = truncateFilePath(((FileSystemResource) flowResource).getPath(), localBasePath);
        } else if (flowResource instanceof UrlResource || flowResource instanceof VfsResource) {
            filePath = truncateFilePath(flowResource.getURL().getPath(), localBasePath);
        } else {
            // Default to the filename.
            return StringUtils.stripFilenameExtension(flowResource.getFilename());
        }

        int beginIndex = 0;
        int endIndex = filePath.length();
        if (filePath.startsWith(localBasePath)) {
            beginIndex = localBasePath.length();
        } else if (filePath.startsWith(SLASH + localBasePath)) {
            beginIndex = localBasePath.length() + 1;
        }
        if (filePath.startsWith(SLASH, beginIndex)) {
            // ignore a leading slash
            beginIndex++;
        }
        if (filePath.lastIndexOf(SLASH) >= beginIndex) {
            // ignore the filename
            endIndex = filePath.lastIndexOf(SLASH);
        } else {
            // There is no path info, default to the filename.
            return StringUtils.stripFilenameExtension(flowResource.getFilename());
        }
        return filePath.substring(beginIndex, endIndex);
    }

    /**
     * If the file path contains the base path, then the part after the base path is returned,
     * otherwise the entire file path is returned.
     * 
     * @param filePath file path to examine
     * @param basePath base path to strip
     * 
     * @return result as above
     */
    @Nonnull @NotEmpty private String truncateFilePath(@Nonnull @NotEmpty final String filePath,
            @Nonnull @NotEmpty final String basePath) {
        final int basePathIndex = filePath.lastIndexOf(basePath);
        if (basePathIndex != -1) {
            return filePath.substring(basePathIndex);
        }
        return filePath;
    }

    /**
     * Check if a path starts with a known scheme.
     * 
     * @param path path to check
     * 
     * @return true iff the path starts with a known scheme
     */
    private boolean isAbsolute(@Nonnull @NotEmpty final String path) {
        if (path.startsWith(CLASSPATH_SCHEME)) {
            return true;
        } else if (path.startsWith(CLASSPATH_STAR_SCHEME)) {
            return true;
        } else if (path.startsWith(FILESYSTEM_SCHEME)) {
            return true;
        }
        return false;
    }
    
    /**
     * Remove the scheme from a path.
     * 
     * @param path path to strip
     * 
     * @return the input with the scheme removed.
     */
    private String removeScheme(@Nonnull @NotEmpty final String path) {
        if (path.startsWith(CLASSPATH_SCHEME)) {
            return path.substring(CLASSPATH_SCHEME.length());
        } else if (path.startsWith(FILESYSTEM_SCHEME)) {
            return path.substring(FILESYSTEM_SCHEME.length());
        } else if (path.startsWith(CLASSPATH_STAR_SCHEME)) {
            return path.substring(CLASSPATH_STAR_SCHEME.length());
        } else {
            return path;
        }
    }

}
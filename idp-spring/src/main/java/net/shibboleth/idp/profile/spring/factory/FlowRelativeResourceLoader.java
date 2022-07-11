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

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import net.shibboleth.ext.spring.resource.ConditionalResourceResolver;

/**
 * This code is extended from org.springframework.webflow.engine.builder.model.FlowRelativeResourceLoader
 * with modifications to support proper lookup of resources via both filesystem and classpath along with
 * custom protocol-specific loaders.
 * 
 * This fills a gap for cases where the Spring {@link ResourceLoader} itself is fully replaced, versus
 * relying solely on customized behavior in the Spring contexts themselves.
 */
class FlowRelativeResourceLoader extends DefaultResourceLoader {

    /** Flow resource for relative lookup. */
    private Resource flowResource;

    /**
     * Constructor.
     *
     * @param resource flow resource for relative lookup
     */
    public FlowRelativeResourceLoader(final Resource resource) {
        flowResource = resource;
        getProtocolResolvers().add(new ConditionalResourceResolver());
    }

    public ClassLoader getClassLoader() {
        return flowResource.getClass().getClassLoader();
    }

    /** {@inheritDoc} */
    @Override
    public Resource getResource(final String location) {
        
        try {
            final Resource r = super.getResource(location);
            if (r.exists()) {
                return r;
            }
        } catch (final Exception e) {
            // May happen if resource wrapper throws during exists() call.
        }
        
        if (location.startsWith(CLASSPATH_URL_PREFIX)) {
            return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()),
                    getClassLoader());
        }
        return createFlowRelativeResource(location);
    }

    private Resource createFlowRelativeResource(final String location) {
        try {
            return flowResource.createRelative(location);
        } catch (final IOException e) {
            final IllegalArgumentException iae = new IllegalArgumentException(
                    "Unable to access a flow relative resource at location '" + location + "'");
            iae.initCause(e);
            throw iae;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Overrides the standard behavior of path-only resources and treats them as file paths if the path exists. Note
     * that this differs from the ordinary Spring contexts that default to file paths because paths are treated as
     * absolute if they are in fact absolute.
     * </p>
     */
    @Override
    protected Resource getResourceByPath(final String path) {
        try {
            final Resource r = new FileSystemResource(path);
            if (r.exists()) {
                return r;
            }
        } catch (final Exception e) {
            // May happen if resource wrapper throws during exists() call.
        }
        return super.getResourceByPath(path);
    }
    
}
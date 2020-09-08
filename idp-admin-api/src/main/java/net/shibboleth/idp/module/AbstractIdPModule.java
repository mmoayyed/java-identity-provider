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

package net.shibboleth.idp.module;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.opensaml.security.httpclient.HttpClientSecuritySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * {@link IdPModule} base class implementing basic file management.
 * 
 * @since 4.1.0
 */
public abstract class AbstractIdPModule implements IdPModule {

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(AbstractIdPModule.class);
    
    /** Module resources. */
    @Nonnull @NonnullElements private Collection<BasicModuleResource> moduleResources;
    
    /** Constructor. */
    public AbstractIdPModule() {
        moduleResources = Collections.emptyList();
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Collection<ModuleResource> getResources() {
        return List.copyOf(moduleResources);
    }
    
    /**
     * Sets the module resources to manage.
     * 
     * @param resources resources to manage
     */
    public void setResources(@Nullable @NonnullElements final Collection<BasicModuleResource> resources) {
        if (resources != null) {
            moduleResources = List.copyOf(resources);
        } else {
            moduleResources = Collections.emptyList();
        }
    }
    
    /** {@inheritDoc} */
    public boolean isEnabled(@Nonnull final ModuleContext moduleContext) {
        
        log.debug("Module {} checking enabled status", getId());
        
        if (moduleResources.isEmpty()) {
            log.debug("Module {} is always enabled", getId());
            return true;
        }

        for (final ModuleResource resource : moduleResources) {
            final Path resolved = moduleContext.getIdPHome().resolve(resource.getDestination());
            log.debug("Module {}: resolved resource destination {}", getId(), resolved);
            if (resolved.toFile().exists()) {
                log.debug("Module {}: resource destination {} exists, module is enabled", getId(), resolved);
                return true;
            }
        }
        
        log.debug("Module {} is not enabled", getId());
        return false;
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements public Map<ModuleResource, ResourceResult> enable(
            @Nonnull final ModuleContext moduleContext) throws ModuleException {
        if (isHttpClientRequired() && moduleContext.getHttpClient() == null) {
            throw new ModuleException("HTTP client required but not available");
        }
        
        log.debug("Module {} enabling", getId());
        
        final Map<ModuleResource,ResourceResult> results;
        
        if (!moduleResources.isEmpty()) {
            results = new HashMap<>(moduleResources.size());

            for (final ModuleResource resource : moduleResources) {
                results.put(resource, ((BasicModuleResource) resource).enable(moduleContext));
            }
        } else {
            results = Collections.emptyMap();
        }
        
        log.debug("Module {} enabled", getId());
        return results;
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements public Map<ModuleResource, ResourceResult> disable(
            @Nonnull final ModuleContext moduleContext, final boolean clean) throws ModuleException {
        log.debug("Module {} disabling", getId());

        final Map<ModuleResource,ResourceResult> results;
        
        if (!moduleResources.isEmpty()) {
            results = new HashMap<>(moduleResources.size());
            for (final ModuleResource resource : moduleResources) {
                results.put(resource, ((BasicModuleResource) resource).disable(moduleContext, clean));
            }
        } else {
            results = Collections.emptyMap();
        }
        
        log.debug("Module {} disabled", getId());
        return results;
    }
    
    /**
     * Models a specific resource managed by a module.
     */
    class BasicModuleResource implements ModuleResource {
        
        /** Source. */
        @Nonnull @NotEmpty private final String source;
        
        /** Destination. */
        @Nonnull private final Path destination;
        
        /** Replacement criteria. */
        private final boolean replace;
        
        /**
         * Constructor.
         *
         * @param src source
         * @param dest destination
         * @param shouldReplace whether to replace when enabling
         */
        public BasicModuleResource(@Nonnull @NotEmpty final String src, @Nonnull final Path dest,
                final boolean shouldReplace) {
            source = Constraint.isNotNull(StringSupport.trimOrNull(src), "Source cannot be null");
            destination = Constraint.isNotNull(dest, "Destination cannot be null");
            replace = shouldReplace;
        }

        /** {@inheritDoc} */
        public int hashCode() {
            return source.hashCode();
        }

        /** {@inheritDoc} */
        public boolean equals(final Object obj) {
            if (obj instanceof ModuleResource) {
                return source.equals(((ModuleResource) obj).getSource()) &&
                        destination.equals(((ModuleResource) obj).getDestination());
            }
            return false;
        }

        /** {@inheritDoc} */
        @Nonnull public String getSource() {
            return source;
        }
        
        /** {@inheritDoc} */
        @Nonnull public Path getDestination() {
            return destination;
        }
        
        /** {@inheritDoc} */
        public boolean isReplace() {
            return replace;
        }
        
        /**
         * Gets whether the resource has been altered at its destination from the source material.
         * 
         * @param moduleContext context for module operations
         * 
         * @return true iff the resource has been changed
         */
        public boolean hasChanged(@Nonnull final ModuleContext moduleContext) {

            try (final InputStream dest = getDestinationStream(moduleContext)) {
                if (dest != null) {
                    final byte[] destHash;
                    
                    final MessageDigest digest = MessageDigest.getInstance("SHA1");
                    try (final OutputStream destSink = OutputStream.nullOutputStream();
                            final DigestOutputStream destDigest = new DigestOutputStream(destSink, digest)) {
                        dest.transferTo(destDigest);
                        destHash = digest.digest();
                    }
                    
                    try (final InputStream src = getSourceStream(moduleContext)) {
                        if (src != null) {
                            try (final OutputStream srcSink = OutputStream.nullOutputStream();
                                    final DigestOutputStream srcDigest = new DigestOutputStream(srcSink, digest)) {
                                src.transferTo(srcDigest);
                                return !Arrays.equals(destHash, digest.digest());
                            }
                        }
                        log.debug("Module {} resource {} does not exist at source", getId(), source);
                        return true;
                    }
                }
                
                log.debug("Module {} resource {} does not exist at destination", getId(), source);
                return false;
            } catch (final IOException e) {
                log.error("Module {} resource {} raised error while checking contents", getId(), source, e);
                return true;
            } catch (final NoSuchAlgorithmException e) {
                log.error("Module {} resource {} raised error while checking contents", getId(), source, e);
                return true;
            }
        }
        
        /**
         * Access the source as a stream.
         * 
         * @param moduleContext context for module operations
         * 
         * @return a stream or null if the source does not exist
         * 
         * @throws IOException on failure
         */
        @Nullable private InputStream getSourceStream(@Nonnull final ModuleContext moduleContext)
                throws IOException {
            
            if (source.startsWith("https://") || source.startsWith("http://")) {
                try {
                    return connect(moduleContext, new URI(source));
                } catch (final URISyntaxException e) {
                    throw new IOException(e);
                }
            }
            return getClass().getResourceAsStream(source);
        }

        /**
         * Connect to the given URI and return the HTTP response stream.
         *
         * @param moduleContext module context
         * @param uri resource location
         * 
         * @return input stream of response
         * 
         * @throws IOException on errors
         */
        @Nonnull private InputStream connect(@Nonnull final ModuleContext moduleContext, @Nonnull final URI uri)
                throws IOException {
            
            final HttpClientContext clientContext = HttpClientContext.create();
            HttpClientSecuritySupport.marshalSecurityParameters(clientContext,
                    moduleContext.getHttpClientSecurityParameters(), true);
            HttpResponse response = null;
            try {
                log.debug("Module {} fetching HTTP resource {}", getId(), uri);
                final HttpGet request = new HttpGet(uri);
                response = moduleContext.getHttpClient().execute(request, clientContext);
                HttpClientSecuritySupport.checkTLSCredentialEvaluated(clientContext, request.getURI().getScheme());
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IOException("HTTP request was unsuccessful");
                }
                
                // The response socket should be closed after the stream is closed.
                final InputStream ret = response.getEntity().getContent();
                response = null;
                return ret;
            } finally {
                if (response != null && CloseableHttpResponse.class.isInstance(response)) {
                    try {
                        CloseableHttpResponse.class.cast(response).close();
                    } catch (final IOException e) {
                        log.debug("Error closing HttpResponse", e);
                    }
                }
            }
        }
        
        /**
         * Access the destination as a stream.
         * 
         * @param moduleContext context for module operations
         * 
         * @return a stream or null if the destination does not exist
         * 
         * @throws IOException on failure
         */
        @Nullable private InputStream getDestinationStream(@Nonnull final ModuleContext moduleContext)
                throws IOException {
            
            final Path destPath = moduleContext.getIdPHome().resolve(destination);
            if (Files.exists(destPath)) {
                try {
                    return Files.newInputStream(destPath, StandardOpenOption.READ);
                } catch (final IOException e) {
                    log.error("Module {} unable to read destination resource {}", getId(), destPath, e);
                }
            }
            
            return null;
        }

        /**
         * Enable the supplied resource.
         * 
         * @param moduleContext module context
         * 
         * @return result of operation
         * 
         * @throws ModuleException if an error occurs
         */
        @Nonnull private ResourceResult enable(@Nonnull final ModuleContext moduleContext) throws ModuleException {
            log.debug("Module {} enabling resource {}", getId(), source);

            final boolean hasChanged = hasChanged(moduleContext);
            
            try (final InputStream srcStream = getSourceStream(moduleContext)) {
                if (srcStream == null) {
                    throw new IOException("Source stream was null");
                }

                final Path destPath;
                final ResourceResult result;
                
                if (hasChanged) {
                    if (isReplace()) {
                        destPath = moduleContext.getIdPHome().resolve(destination);
                        Files.copy(destPath, destPath.resolveSibling(destPath.getFileName() + ".idpsave"),
                                StandardCopyOption.REPLACE_EXISTING);
                        log.debug("Module {} preserved {}", getId(), destPath);
                        result = ResourceResult.REPLACED;
                    } else {
                        final Path basePath = moduleContext.getIdPHome().resolve(destination);
                        destPath = basePath.resolveSibling(basePath.getFileName() + ".idpnew");
                        result = ResourceResult.ADDED;
                    }
                    
                } else {
                    destPath = moduleContext.getIdPHome().resolve(destination);
                    result = ResourceResult.CREATED;
                }
                
                if (!destPath.startsWith(moduleContext.getIdPHome())) {
                    log.error("Module {} attempted to create file outside of IdP installation: {}", getId(), destPath);
                    throw new ModuleException("Module asked to create file outside of IdP installation");
                }

                Files.copy(srcStream, destPath, StandardCopyOption.REPLACE_EXISTING);
                log.debug("Module {} created {}", getId(), destPath);
                return result;
            } catch (final IOException e) {
                log.error("Module {} unable to enable resource {}", getId(), source);
                throw new ModuleException(e);
            }
        }

        /**
         * Disable the supplied resource, either removing or renaming.
         * 
         * @param moduleContext module context
         * @param clean true iff resource should be removed
         * 
         * @return result of operation
         * 
         * @throws ModuleException if an error occurs
         */
        @Nonnull private ResourceResult disable(@Nonnull final ModuleContext moduleContext, final boolean clean)
                throws ModuleException {
            
            final ResourceResult result;
            final Path resolved = moduleContext.getIdPHome().resolve(destination);
            log.debug("Module {} resolved resource destination {}", getId(), resolved);
            if (Files.exists(resolved)) {
                try {
                    if (clean || !hasChanged(moduleContext)) {
                        log.debug("Module {} removing resource {}", getId(), resolved);
                        Files.delete(resolved);
                        result = ResourceResult.REMOVED;
                    } else {
                        log.debug("Module {} backing up resource {}", getId(), resolved);
                        Files.move(resolved, resolved.resolveSibling(resolved.getFileName() + ".idpsave"),
                                StandardCopyOption.REPLACE_EXISTING);
                        result = ResourceResult.SAVED;
                    }
                } catch (final IOException e) {
                    log.error("Module {} failed to disable {}", getId(), resolved);
                    throw new ModuleException(e);
                }
            } else {
                log.debug("Module {} resource {} missing, ignoring", getId(), resolved);
                result = ResourceResult.MISSING;
            }
            
            return result;
        }
        
    }

}
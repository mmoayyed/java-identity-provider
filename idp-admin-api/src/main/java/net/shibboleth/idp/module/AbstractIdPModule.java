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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    public void enable(@Nonnull final ModuleContext moduleContext) throws ModuleException {
        if (isHttpClientRequired() && moduleContext.getHttpClient() == null) {
            throw new ModuleException("HTTP client required but not available");
        }
        
        log.debug("Module {} enabling", getId());
        for (final ModuleResource resource : moduleResources) {
            ((BasicModuleResource) resource).enable(moduleContext);
        }
        log.info("Module {} enabled", getId());
    }

    /** {@inheritDoc} */
    public void disable(@Nonnull final ModuleContext moduleContext, final boolean clean) throws ModuleException {
        log.debug("Module {} disabling", getId());
        for (final ModuleResource resource : moduleResources) {
            ((BasicModuleResource) resource).disable(moduleContext, clean);
        }
        log.info("Module {} disabled", getId());
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
                                return digest.digest().equals(destHash);
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
                // TODO http
                return null;
            }
            return getClass().getResourceAsStream(source);
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
            final File destFile = moduleContext.getIdPHome().resolve(destination).toFile();
            if (destFile.exists()) {
                return new FileInputStream(destFile);
            }
            return null;
        }

        /**
         * Enable the supplied resource.
         * 
         * @param moduleContext module context
         * 
         * @throws ModuleException if an error occurs
         */
        private void enable(@Nonnull final ModuleContext moduleContext) throws ModuleException {
            log.debug("Module {} enabling resource {}", getId(), source);

            final boolean hasChanged = hasChanged(moduleContext);
            
            try (final InputStream srcStream = getSourceStream(moduleContext)) {
                if (srcStream == null) {
                    throw new IOException("Source stream was null");
                }

                
                final File destFile;
                
                if (hasChanged) {
                    if (isReplace()) {
                        final File renamedFile = moduleContext.getIdPHome().resolve(destination).toFile();
                        if (renamedFile.renameTo(new File(renamedFile.getPath() + ".idpsave"))) {
                            log.info("Module {} preserved {}", getId(), renamedFile);
                        } else {
                            throw new ModuleException("Unable to rename " + renamedFile);
                        }
                        destFile = moduleContext.getIdPHome().resolve(destination).toFile();
                    } else {
                        destFile = new File(moduleContext.getIdPHome().resolve(destination).toString() + ".idpnew");
                    }
                    
                } else {
                    destFile = moduleContext.getIdPHome().resolve(destination).toFile();
                }

                try (final OutputStream destStream = new FileOutputStream(destFile)) {
                    srcStream.transferTo(destStream);
                    log.info("Module {} created {}", getId(), destFile);
                }

            } catch (final IOException e) {
                log.error("Module {} unable to enable resource {}", getId(), source, e);
                throw new ModuleException(e);
            }
        }

        /**
         * Disable the supplied resource, either removing or renaming.
         * 
         * @param moduleContext module context
         * @param clean true iff resource should be removed
         * 
         * @throws ModuleException if an error occurs
         */
        private void disable(@Nonnull final ModuleContext moduleContext, final boolean clean) throws ModuleException {
            
            final Path resolved = moduleContext.getIdPHome().resolve(destination);
            log.debug("Module {} resolved resource destination {}", getId(), resolved);
            final File file = resolved.toFile();
            if (file.exists()) {
                if (clean) {
                    log.info("Module {} removing resource {}", getId(), file);
                    if (!file.delete()) {
                        throw new ModuleException("Unable to remove resource " + file);
                    }
                } else {
                    log.info("Module {} moving aside resource {}", getId(), file);
                    if (!file.renameTo(new File(file.toString() + ".idpsave"))) {
                        throw new ModuleException("Unable to rename resource " + file);
                    }
                }
            } else {
                log.info("Module {} resource {} missing, ignoring", getId(), file);
            }
        }
        
    }

}
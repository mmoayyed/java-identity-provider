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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.client.HttpClient;

import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.IdentifiedComponent;

/**
 * This interface is exported (via the service API) by every IdP module.
 * 
 * @since 4.1.0
 */
public interface IdPModule extends IdentifiedComponent {
    
    /**
     * Gets module name.
     * 
     * @param moduleContext optional context for supplying i18n input
     * 
     * @return a human-readable name for the module
     */
    @Nonnull @NotEmpty String getName(@Nullable final ModuleContext moduleContext);
    
    /**
     * Gets module description.
     * 
     * @param moduleContext optional context for supplying i18n input
     * 
     * @return a human-readable description for the module
     */
    @Nullable @NotEmpty String getDescription(@Nullable final ModuleContext moduleContext);

    /**
     * Gets module URL.
     * 
     * @return a URL for obtaining additional information about the module
     */
    @Nullable @NotEmpty String getURL();
    
    /**
     * Gets the plugin name of the module's owner, if the module is provided by a plugin.
     * 
     * @return plugin ID, if any
     */
    @Nullable @NotEmpty String getOwnerId();
    
    /**
     * Gets whether module enablement requires access to an {@link HttpClient}.
     * 
     * @return true iff enabling the module requires HTTP client
     */
    boolean isHttpClientRequired();

    /**
     * Gets resources managed by this module.
     * 
     * @return resources managed by this module
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Collection<ModuleResource> getResources();
    
    /**
     * Gets whether the module is enabled.
     * 
     * <p>The status of "enabled" is meant to reflect whether a deployer has previously
     * or implicitly enabled the module, not necessarily whether the module is fully or
     * properly configured or in use.</p>
     * 
     * @param moduleContext module context
     * 
     * @return true iff the module is enabled
     */
    boolean isEnabled(@Nonnull final ModuleContext moduleContext);
    
    /**
     * Enable the module.
     * 
     * <p>This operation MUST be idempotent.</p>
     * 
     * @param moduleContext module context
     * 
     * @return summary of resource results
     * 
     * @throws ModuleException if not successful 
     */
    @Nonnull @NonnullElements Map<ModuleResource,ResourceResult> enable(@Nonnull final ModuleContext moduleContext)
            throws ModuleException;

    /**
     * Disable the module.
     * 
     * <p>This operation MUST be idempotent with respect to the value of the input parameter.</p>
     * 
     * @param moduleContext module context
     * @param clean if true, the module should attempt to fully remove traces of previous
     *  use in a potentially destructive fashion
     * 
     * @return summary of resource results
     * 
     * @throws ModuleException if not successful 
     */
    @Nonnull @NonnullElements Map<ModuleResource,ResourceResult> disable(@Nonnull final ModuleContext moduleContext,
            final boolean clean) throws ModuleException;
    
    /**
     * Interface to a resource managed by the module.
     */
    interface ModuleResource {
        
        /**
         * Gets the source location of the resource.
         * 
         * <p>This may be a URL or a local path that will be assumed a classpath.</p>
         * 
         * @return source location
         */
        @Nonnull public String getSource();
        
        /**
         * Gets the destination for the resource.
         * 
         * @return destination path
         */
        @Nonnull public Path getDestination();
        
        /**
         * Gets whether the resource should be config(replace) or config(noreplace) in RPM specfile parlance.
         * 
         * @return true iff the resource should be replaced with the original preserved
         */
        public boolean isReplace();

        /**
         * Gets whether the resource, if missing, should not act as a module-disabled signal.
         * 
         * @return true iff the resource may be removed by a deployer without disabling the module
         */
        public boolean isOptional();

        /**
         * Gets whether the resource should be marked executable where applicable.
         * 
         * @return true iff the resource should be marked executable
         */
        public boolean isExecutable();
}
 
    /** Resource management outcome. */
    public enum ResourceResult {
        /** Resource was created. */
        CREATED,
        
        /** Resource was created and old resource saved. */
        REPLACED,
        
        /** Resource was created alongside original. */
        ADDED,
        
        /** Resource was removed. */
        REMOVED,
        
        /** Old resource was preserved. */
        SAVED,
        
        /** Resource was missing. */
        MISSING,
    };
    
}
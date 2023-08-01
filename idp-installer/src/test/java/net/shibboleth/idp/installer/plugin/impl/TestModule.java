/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.installer.plugin.impl;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.profile.module.ModuleContext;
import net.shibboleth.profile.module.ModuleException;
import net.shibboleth.shared.collection.CollectionSupport;

@SuppressWarnings("javadoc")
public class TestModule implements IdPModule {
    
    @Nullable final ModuleException throwOnEnable;
    @Nullable final ModuleException throwOnDisable;
    @Nonnull final String id;
    boolean enabled;
    
    public TestModule(@Nonnull String name, ModuleException enable, ModuleException disable) {
        throwOnEnable = enable;
        throwOnDisable = disable;
        id = name;
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    public @Nonnull String getName(@Nullable final ModuleContext moduleContext) {
        return id;
    }

    /** {@inheritDoc} */
    public String getDescription(@Nullable  final ModuleContext moduleContext) {
        return null;
    }

    /** {@inheritDoc} */
    public String getURL() {
        return null;
    }
    
    /** {@inheritDoc} */
    public String getOwnerId() {
        return null;
    }

    /** {@inheritDoc} */
    public boolean isHttpClientRequired() {
        return false;
    }

    /** {@inheritDoc} */
    public @Nonnull Collection<ModuleResource> getResources() {
        return CollectionSupport.emptyList();
    }

    /** {@inheritDoc} */
    public boolean isEnabled(@Nonnull final ModuleContext moduleContext) {
        return enabled;
    }

    /** {@inheritDoc} */
    public @Nonnull Map<ModuleResource, ResourceResult> enable(@Nonnull final ModuleContext moduleContext) throws ModuleException {
        if (throwOnEnable != null) {
            throw throwOnEnable;
        }
        enabled = true;
        return CollectionSupport.emptyMap();
    }

    /** {@inheritDoc} */
    public @Nonnull Map<ModuleResource, ResourceResult> disable(@Nonnull final ModuleContext moduleContext, boolean clean)
            throws ModuleException {
        if (throwOnDisable != null) {
            throw throwOnDisable;
        }
        enabled = false;
        return CollectionSupport.emptyMap();
    }
    
}
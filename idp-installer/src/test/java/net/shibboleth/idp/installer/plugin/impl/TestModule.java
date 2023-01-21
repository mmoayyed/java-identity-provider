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

package net.shibboleth.idp.installer.plugin.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.module.ModuleContext;
import net.shibboleth.idp.module.ModuleException;

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
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    public boolean isEnabled(ModuleContext moduleContext) {
        return enabled;
    }

    /** {@inheritDoc} */
    public @Nonnull Map<ModuleResource, ResourceResult> enable(ModuleContext moduleContext) throws ModuleException {
        if (throwOnEnable != null) {
            throw throwOnEnable;
        }
        enabled = true;
        return Collections.emptyMap();
    }

    /** {@inheritDoc} */
    public @Nonnull Map<ModuleResource, ResourceResult> disable(ModuleContext moduleContext, boolean clean)
            throws ModuleException {
        if (throwOnDisable != null) {
            throw throwOnDisable;
        }
        enabled = false;
        return Collections.emptyMap();
    }
    
}
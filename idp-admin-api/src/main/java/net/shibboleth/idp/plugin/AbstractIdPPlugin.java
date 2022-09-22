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

package net.shibboleth.idp.plugin;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;

/**
 * A base class implementing {@link IdPPlugin} that defaults common settings.
 * 
 * @since 4.1.0
 */
public abstract class AbstractIdPPlugin implements IdPPlugin {
    
    /** Modules to enable on install. */
    @Nonnull @NonnullElements private Set<IdPModule> enableModules;

    /** Modules to disable on removal. */
    @Nonnull @NonnullElements private Set<IdPModule> disableModules;

    /** Constructor. */
    public AbstractIdPPlugin() {
        enableModules = Collections.emptySet();
        disableModules = Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getPluginId() {
        return getClass().getPackageName();
    }

    /** {@inheritDoc} */
    @Nonnegative public int getPatchVersion() {
        return 0;
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public Set<String> getRequiredModules() {
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public Set<IdPModule> getEnableOnInstall() {
        return enableModules;
    }

    /** {@inheritDoc} */
    public String getLicenseFileLocation() {
        return null;
    }

    /**
     * Set the modules to enable on install.
     * 
     * @param modules modules to enable
     */
    protected void setEnableOnInstall(@Nonnull @NonnullElements final Set<IdPModule> modules) {
        enableModules = Set.copyOf(modules);
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public Set<IdPModule> getDisableOnRemoval() {
        return disableModules;
    }

    /**
     * Set the modules to disable on removal.
     * 
     * @param modules modules to disable
     */
    protected void setDisableOnRemoval(@Nonnull @NonnullElements final Set<IdPModule> modules) {
        disableModules = Set.copyOf(modules);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IdPModule && getPluginId().equals(((IdPPlugin) obj).getPluginId());
    }


    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getPluginId().hashCode();
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "IdPPlugin " + getPluginId();
    }

}
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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

/**
 * This interface is exported (via the service API) by every IdP plugin.
 * 
 * @since 4.1.0
 */
public interface IdPPlugin {
    
    /** Return the unique identifier for the plugin.  This name <em>MUST</em> be
     * <ul>
     * <li> renderable in all file systems (for instance alphanumerics, '-' and '.' only)</li>
     * <li> unique.  This is best done using java module guidance</li>
     * </ul>
     * For instance <code>org.example.plugins.myplugin</code>
     *
     * @return The id of this plugin.
     */
    @Nonnull @NotEmpty String getPluginId();

    /** Return the places to look for information for this plugin package.
     * The format of the (property) file at this location is fixed.
     * 
     * @return Zero or more URLs
     * @throws IOException if the resource construction failed.
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive List<URL> getUpdateURLs() throws IOException;
    
    /** Return the major version, (as defined by the 
     * <a href="https://wiki.shibboleth.net/confluence/display/DEV/Java+Product+Version+Policy">
     * Java Product Version Policy</a>.
     * @return The major version.
     */
    @Nonnegative int getMajorVersion();

    /** Return the minor version, (as defined by the 
     * <a href="https://wiki.shibboleth.net/confluence/display/DEV/Java+Product+Version+Policy">
     * Java Product Version Policy</a>.
     * @return The minor version.
     */
    @Nonnegative int getMinorVersion();
    
    /** Return The patch version, (as defined by the 
     * <a href="https://wiki.shibboleth.net/confluence/display/DEV/Java+Product+Version+Policy">
     * Java Product Version Policy</a>.
     * @return The patch version.
     */
    @Nonnegative int getPatchVersion();
    
    /** Return the classpath location of the license file to emit
     * when --license is specified.
     * @return the location
     */
    @Nullable String getLicenseFileLocation();

    /**
     * Get the IDs of any {@link IdPModule}s required for installation of this plugin.
     * 
     * @return module IDs that are required
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive Set<String> getRequiredModules();

    /**
     * Get the modules to enable after plugin installation or upgrade.
     * 
     * @return modules to enable
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive Set<IdPModule> getEnableOnInstall();

    /**
     * Get the modules to disable after plugin removal.
     * 
     * @return modules to disable
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive Set<IdPModule> getDisableOnRemoval();

}
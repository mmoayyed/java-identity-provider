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
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.Pair;

/**
 * This interface is exported (via the service API) by every IdP plugin.
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

    /** Return the list of (idp.home) relative paths (of files, <em>not directories </em>) 
     * to copy from the distribution into the IdP installation.
     *
     * <em>Not currently supported</em>
     * 
     * <p>These files are copied non-destructively (if the file already exists
     * then it is not copied).  Some paths are disallowed (for instance dist and system).
     * Directories are created if needed</p>
     * <p>
     * The dist folder is always copied, so no files from it should be included.</p>
     *
     * @return The list of paths.
     */
    @Nonnull @NonnullElements List<Path> getFilePathsToCopy();
    
    /** <p>Return the list of files <em>not directories </em> to get from 'external'
     * sources. This allows external content to be downloaded during installation.</p>
     *
     * <p>The first part of the pair is the source URL,
     * the second is a path relative to idp.home.  These can include files
     * going to dist\edit-webapp in which case the path is expected to have the
     * plugin id appended. Sub directories are created if needed</p>
     *
     * @return The list.
     * @throws IOException if the resource construction failed.
     */
    @Nonnull @NonnullElements List<Pair<URL, Path>> getExternalFilePathsToCopy() throws IOException;

    /** Return the places to look for information for this plugin package.
     * The format of the (property) file at this location is fixed.
     * 
     * @return Zero or more URLs
     * @throws IOException if the resource construction failed.
     */
    @Nonnull @NonnullElements List<URL> getUpdateURLs() throws IOException;
    
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
    
    /**
     * Get the IDs of any {@link IdPModule}s required for installation of this plugin.
     * 
     * @return module IDs that are required
     */
    @Nonnull @NonnullElements Set<String> getRequiredModules();

    /**
     * Get the modules to enable after plugin installation or upgrade.
     * 
     * @return modules to enable
     */
    @Nonnull @NonnullElements Set<IdPModule> getEnableOnInstall();

    /**
     * Get the modules to disable after plugin removal.
     * 
     * @return modules to disable
     */
    @Nonnull @NonnullElements Set<IdPModule> getDisableOnRemoval();

}
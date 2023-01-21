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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import net.shibboleth.idp.installer.impl.InstallationLogger;
import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.module.IdPModule.ModuleResource;
import net.shibboleth.idp.module.IdPModule.ResourceResult;
import net.shibboleth.idp.module.ModuleContext;
import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.logic.Constraint;

/** An object which does installation rollback in its {@link AutoCloseable#close()} method. 
 *
 */
public class RollbackPluginInstall implements AutoCloseable {

    /** logger.  */
    @Nonnull private final Logger log = InstallationLogger.getLogger(RollbackPluginInstall.class);

    /** The modules enabled when the {@link IdPPlugin} was installed. */
    @Live @Nonnull private List<IdPModule> modulesEnabled = new ArrayList<>();

    /** The modules disabled when the {@link IdPPlugin} was installed. */
    @Live @Nonnull private List<IdPModule> modulesDisabled = new ArrayList<>();

    /** The files copied in as the {@link IdPPlugin} was installed. */
    @Live @Nonnull private List<Path> filesCopied = new ArrayList<>();
   
    /** The files renamed away during the installation. */
    @Live @Nonnull private List<Pair<Path, Path>> filesRenamedAway = new ArrayList<>();

    /** The {@link ModuleContext} that the module subsystem needs.*/
    @Nonnull private final ModuleContext moduleContext;

    /** Module Changes.*/
    @Nonnull private final Map<ModuleResource, ResourceResult> moduleChanges;

    /**
     * Constructor.
     * @param context The Module Context
     * @param changes Where to capture module changes
     */
    public RollbackPluginInstall(final @Nonnull ModuleContext context,
            final @Nonnull Map<ModuleResource, ResourceResult> changes) {
        moduleContext = Constraint.isNotNull(context, "Module context should be non null");
        moduleChanges = Constraint.isNotNull(changes, "Module changes should be non null");
    }

    /** What was enabled?
     * @return Returns the modules enabled as the plugin was installed.
     */
    @Live @Nonnull public List<IdPModule> getModulesEnabled() {
        return modulesEnabled;
    }

    /** What was enabled?
     * @return Returns the modules disabled as the plugin was installed.
     */
    @Live @Nonnull public List<IdPModule> getModulesDisabled() {
        return modulesDisabled;
    }

    /** What was copied?
     * @return Returns the files copied as part of the install
     */
    @Live @Nonnull public List<Path> getFilesCopied() {
        return filesCopied;
    }

    /** What was renamed away?
     * @return Returns the filesRenamedAway.
     */
    @Live @Nonnull public List<Pair<Path, Path>> getFilesRenamedAway() {
        return filesRenamedAway;
    }

    /** Traverse the {@link #modulesEnabled} list disabling modules.
     * @return true if we did any work.
     */
    private boolean rollbackEnabledModules() {
        if (modulesEnabled.isEmpty()) {
            return false;
        }
        for (int i = modulesEnabled.size()-1; i >=0; i--) {
            final IdPModule module = modulesEnabled.get(i);
            try {
                log.trace("Deleting {}", module.getId());
                captureChanges(module.disable(moduleContext, false));
            } catch (final Throwable t) {
                log.error("Could not disable {}: ", module.getId(), t);
            }            
        }
        return true;
    }

    /** Traverse the {@link #modulesDisabled} list re-enabling modules.
     * @return true if we did any work.
     */
    private boolean rollbackDisabledModules() {
        if (modulesDisabled.isEmpty()) {
            return false;
        }
        for (int i = modulesDisabled.size()-1; i >=0; i--) {
            final IdPModule module = modulesDisabled.get(i);
            try {
                log.trace("Deleting {}", module.getId());
                captureChanges(module.enable(moduleContext));
            } catch (final Throwable t) {
                log.error("Could not disable {}, continuing ", module.getId(), t);
            }            
        }
        return true;
    }

    /** Traverse the {@link #filesCopied} list deleting the files.
     * @return true if we did any work.
     */
    private boolean rollbackCopies() {
        if (filesCopied.isEmpty()) {
            return false;
        }
        for (int i = filesCopied.size()-1; i >=0; i--) {
            final Path path = filesCopied.get(i);
            try {
                log.trace("Deleting {}", path);
                Files.delete(path);
            } catch (final Throwable t) {
                log.error("Could not delete {}, continuing ", path, t);
                path.toFile().deleteOnExit();
            }
        }
        return true;
    }

    /** Traverse the {@link #filesRenamedAway} list copying the files back.
     * @return true if we did any work.
     */
    private boolean rollbackRenamed() {
        if (filesRenamedAway.isEmpty()) {
            return false;
        }
        for (int i = filesRenamedAway.size()-1; i >=0; i--) {
            final Pair<Path, Path> filePair = filesRenamedAway.get(i);
            final Path from = filePair.getFirst();
            final Path to = filePair.getSecond();
            assert from != null && to != null;
            try (final InputStream in = new BufferedInputStream(
                         new FileInputStream(to.toFile()));
                 final OutputStream out = new BufferedOutputStream(
                         new FileOutputStream(from.toFile()))) {
                log.trace("Copying {} to {}", filePair.getSecond());
                in.transferTo(out);
            } catch (final Throwable t) {
                log.error("Could not copy {} to {}, continuing ", filePair.getSecond(), filePair.getFirst(), t);
            }
        }
        return true;        
    }

    /** Capture module changes.
     * @param changes what has changed
     */
    private void captureChanges(final  Map<ModuleResource,ResourceResult> changes) {
        for (final Entry<ModuleResource, ResourceResult> entry: changes.entrySet()) {
            moduleChanges.put(entry.getKey(), entry.getValue());
        }
    }

    /** Perform the rollback.  This is done in reverse order from the install,
     * which is to say the the lists are iterated over backwards and the order is
     * Enabled (which are disabled), then copied (which are deleted) then renamed (which are copied).
     */
    protected void rollback() {
        boolean workPerformed = rollbackEnabledModules();
        workPerformed |= rollbackCopies();
        workPerformed |= rollbackRenamed();
        workPerformed |= rollbackDisabledModules();
        
        if (!workPerformed) {
            log.debug("Rollback/Uninstall.  No work done");
        } else {
            log.info("Rollback Complete");
        }
    }
    
    /** Signal that the operation completed and that rollback won't be needed. */
    public void completed() {
        modulesEnabled = CollectionSupport.emptyList();
        modulesDisabled = CollectionSupport.emptyList();
        filesCopied = CollectionSupport.emptyList();
        filesRenamedAway = CollectionSupport.emptyList();
    }

    /** {@inheritDoc} */
    public void close() {
        rollback();
    }
}

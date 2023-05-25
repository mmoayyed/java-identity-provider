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

package net.shibboleth.idp.installer.impl;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nonnull;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Copy;
import org.slf4j.Logger;

import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Copy the distribution to the final location.  Prior to doing so
 * take a backup of the old distribution. "The final location" means
 * the dist, doc and system subdirectories.
 */
public final class CopyDistribution {

    /** Log. */
    private final Logger log = LoggerFactory.getLogger(CopyDistribution.class);

    /** Properties for the job. */
    @Nonnull private final InstallerProperties installerProps;

    /** Constructor.
     * @param props The environment for the work.
     */
    public CopyDistribution(@Nonnull final InstallerProperties props) {
        installerProps = props;
    }

    /** Copy the distribution from the dstribution to their new locations, having
     * first taken a backup.
     * @throws BuildException if badness occurs
     */
    public void execute() throws BuildException {
        deleteOld();
        copyDist();
        copyBinDoc();
    }

    /** Helper for the {@link #deleteOld()} method.
     * @param what what to delete
     * @param excludes what to exclude
     */
    private void delete(final Path what, final String excludes) {
        if (!Files.exists(what)) {
            log.debug("{} doesn't exist, nothing to delete", what);
        } else if (!Files.isDirectory(what)) {
            log.error("Corrupt install: {} is not a directory", what);
            throw new BuildException("Corrupt install - not a directory");
        } else {
            log.debug("Deleting {} ", what);
            InstallerSupport.deleteTree(what, excludes);
        }
    }

    /** Delete old copies of bin/lib (leaving bin for scripts), dist, doc and system.
     * system has to be unprotected first which also means we need to create it too.
     * @throws BuildException if badness occurs
     */
    protected void deleteOld() {
        delete(installerProps.getTargetDir().resolve("bin").resolve("lib"), null);
        delete(installerProps.getTargetDir().resolve("dist"), "plugin-webapp/** plugin-contents/**");
        delete(installerProps.getTargetDir().resolve("doc"), null);
        final Path system = installerProps.getTargetDir().resolve("system");
        if (Files.exists(system)) {
            InstallerSupport.setReadOnly(system, false);
            delete(system, null);
        }
    }

    /** Helper for the {@link #copyDist()} and
     *  {@link #copyBinDoc()} methods.
     * @param srcDist the source distribution.
     * @param dist the dist directory
     * @param to the subfolder name
     * @param overwrite whether we want to overwrite
     * @throws BuildException if badness occurs
     */
    private void distCopy(final Path srcDist, final Path dist, final String to, final boolean overwrite)
            throws BuildException {
        final Path toPath =  dist.resolve(to);
        final Path fromPath = srcDist.resolve(to);
        log.debug("Copying distribution from {} to {}", fromPath, toPath);
        final Copy copy = InstallerSupport.getCopyTask(fromPath, toPath);
        copy.setOverwrite(overwrite);
        copy.execute();
    }
    
    /** Helper for the {@link #copyDist()} and
     *  {@link #copyBinDoc()} methods.
     * @param srcDist the source distribution.
     * @param dist the dist directory
     * @param to the subfolder name
     * @throws BuildException if badness occurs
     */
    private void distCopy(final Path srcDist, final Path dist, final String to) throws BuildException {
        distCopy(srcDist, dist, to, false);
    }


    /** Populate the dist folder.
     * @throws BuildException if badness occurs
     */
    protected void copyDist() {
        final Path dist = installerProps.getTargetDir().resolve("dist");
        InstallerSupport.createDirectory(dist);
        final Path src = installerProps.getSourceDir();
        if (!Files.exists(src)) {
            log.error("Source distribution {} not found.", src);
            throw new BuildException("Source distribution not found");
        }
        distCopy(src, dist, "webapp");
    }

    /** Populate the per distribution (but non dist) folders.
     * @throws BuildException if badness occurs
     */
    protected void copyBinDoc() {
        distCopy(installerProps.getSourceDir(), installerProps.getTargetDir(), "doc");
        final Path fromPath = installerProps.getSourceDir().resolve("bin").resolve("lib");
        final Path toPath = installerProps.getTargetDir().resolve("dist").resolve("binlib");
        log.debug("Copying distribution from {} to {}", fromPath, toPath);
        final Copy copy = InstallerSupport.getCopyTask(fromPath, toPath);
        copy.setOverwrite(false);
        copy.execute();
    }
}

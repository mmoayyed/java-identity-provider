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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Copy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copy the distribution to its final location.
 */
public final class CopyDistributions {

    /** Log. */
    private final Logger log = LoggerFactory.getLogger(CopyDistributions.class);

    /** Properties for the job. */
    private final InstallerProperties installerProps;

    /** Constructor.
     * @param props The environment for the work.
     */
    public CopyDistributions(final InstallerProperties props) {
        installerProps = props;
    }

    /** Copy the distribution from the dstribution to its new location.
     * @param ip what drives the install.
     * @throws BuildException if badness occurs
     */
    public void execute(final InstallerProperties ip) throws BuildException {
        backupOld();
        deleteOld();
        copyDist();
        copyBinDocSystem();
        createUserFolders();
    }

    /** Helper for the {@link #backupOld(InstallerProperties)} method.
     * @param from where from
     * @param to where to.
     * @throws BuildException if badness occurs
     */
    private void backup(final Path from, final Path to) throws BuildException {
        log.debug("Backing up From {} to {}", from, to);
        final Copy copy = InstallerSupport.getCopyTask(from, to);
        copy.setFailOnError(false);
        copy.execute();
    }

    /** Copy bin, edit-webapp, dist and doc to old-date-time.
     * @throws BuildException if badness occurs
     */
    protected void backupOld() throws BuildException {
        final Path backup = installerProps.getTargetDir().resolve("Old-" + Instant.now().toString());
        InstallerSupport.createDirectory(backup);
        backup(installerProps.getTargetDir().resolve("edit-webapp"), backup.resolve("edit-webapp"));
        backup(installerProps.getTargetDir().resolve("doc"), backup.resolve("doc"));
        backup(installerProps.getTargetDir().resolve("system"), backup.resolve("system"));
    }

    /** Helper for the delete {@link #deleteOld(InstallerProperties)} method.
     * @param what what to delete
     */
    private void delete(final Path what) {
        if (!Files.exists(what)) {
            log.debug("{} doesn't exist, ignoring", what);
        } else if (Files.isDirectory(what)) {
            throw new BuildException("Corrupt install " + what + " is not a directory");
        } else {
            log.debug("Deleteing {} ", what);
            try {
                DeletingVisitor.deleteTree(what);
            } catch (final IOException e) {
                log.warn("Deleting {} failed", what, e);
            }
        }
    }

    /** Delete old copies of bin/lib (leaving bin for scripts), disty, doc and system.
     * system has to be unprotected first which also means we need to create it too.
     * @throws BuildException if badness occurs
     */
    protected void deleteOld() {
        delete(installerProps.getTargetDir().resolve("bin").resolve("lib"));
        delete(installerProps.getTargetDir().resolve("dist"));
        delete(installerProps.getTargetDir().resolve("doc"));
        final Path system = installerProps.getTargetDir().resolve("system");
        if (Files.exists(system)) {
            log.debug("Clearing  {} readonly (id Windows)", system);
            InstallerSupport.setReadOnly(system, false);
        }
        delete(system);
    }


    /** Helper for the delete {@link #copyDist(InstallerProperties)} and
     *  {@link #copyBinDocSystem(InstallerProperties)} methods.
     * @param srcDist the source distribution.
     * @param dist the dist directory
     * @param to the subfolder name
     * @throws BuildException if badness occurs
     */
    private void distCopy(final Path srcDist, final Path dist, final String to) throws BuildException {
        final Path toPath =  dist.resolve(to);
        final Path fromPath = srcDist.resolve(to);
        log.debug("Copying distribution from {} to {}", fromPath, toPath);
        final Copy copy = InstallerSupport.getCopyTask(fromPath, toPath);
        copy.execute();
    }

    /** Populate the dist folder.
     * @throws BuildException if badness occurs
     */
    protected void copyDist() {
        final Path dist = installerProps.getTargetDir().resolve("dist");
        InstallerSupport.createDirectory(dist);
        final Path src = installerProps.getSourceDir().resolve("dist");
        if (!Files.exists(src)) {
            log.error("Source distribution {} not found", src);
            throw new BuildException("Source distribution not found");
        }
        distCopy(src, dist, "conf");
        distCopy(src, dist, "flows");
        distCopy(src, dist, "messages");
        distCopy(src, dist, "views");
        distCopy(src, dist, "webapp");
    }

    /** Populate the per distribution (but non dist) folders.
     * @throws BuildException if badness occurs
     */
    protected void copyBinDocSystem() {
        distCopy(installerProps.getSourceDir(), installerProps.getTargetDir(), "bin");
        distCopy(installerProps.getSourceDir(), installerProps.getTargetDir(), "doc");
        distCopy(installerProps.getSourceDir(), installerProps.getTargetDir(), "system");
    }

    /** Create (if they do not exist) the user editable folders, suitable for
     * later population during update or install.
     * @throws BuildException if badness occurs
     */
    protected void createUserFolders() {
        final Path target = installerProps.getTargetDir();
        InstallerSupport.createDirectory(target.resolve("conf"));
        InstallerSupport.createDirectory(target.resolve("credentials"));
        InstallerSupport.createDirectory(target.resolve("flows"));
        InstallerSupport.createDirectory(target.resolve("logs"));
        InstallerSupport.createDirectory(target.resolve("messages"));
        InstallerSupport.createDirectory(target.resolve("metadata"));
        InstallerSupport.createDirectory(target.resolve("views"));
        InstallerSupport.createDirectory(target.resolve("war"));
    }
}

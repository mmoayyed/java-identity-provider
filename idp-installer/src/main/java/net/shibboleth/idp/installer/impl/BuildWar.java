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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nonnull;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Jar;
import org.slf4j.Logger;

import net.shibboleth.idp.Version;
import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Code to build the war file during an install or on request.
 * 
 * <p>This code:</p>
 * <ul>
 * <li>Deletes any old detritus</li>
 * <li>Creates a directory called webapp.tmp and populates it from the dist folder</li>
 * <li>Overwrites this from edit-webapp</li>
 * <li>Deletes the old idp.war</li>
 * <li>Builds a jar file called idp.war</li>
 * <li>Deletes webapp.tmp</li>
 * </ul>
 */
public final class BuildWar {

    /** Log. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BuildWar.class);

    /** Location of the install for the job. */
    @Nonnull private final Path targetDir;

    /** Constructor.
     * @param idpHome Where to install to.
     */
    public BuildWar(@Nonnull final Path idpHome) {
        targetDir = Constraint.isNotNull(idpHome, "IdPHome should not be null");
        Constraint.isTrue(Files.exists(targetDir), "Target Dir " + targetDir + " does not exist");
    }

    /** Method to do a single overlay into webapp.
     *
     * @param from Where to copy from.
     * @param webAppTo Where to copy to.
     * @throws BuildException if unexpected badness occurs.
     */
    private void overlayWebapp(@Nonnull final Path from, @Nonnull final Path webAppTo) throws BuildException {
        if (!Files.exists(from)) {
            return;
        }
        final Copy overlay = InstallerSupport.getCopyTask(from, webAppTo, "**/*.idpnew");
        overlay.setOverwrite(true);
        overlay.setPreserveLastModified(true);
        overlay.setFailOnError(true);
        overlay.setVerbose(log.isDebugEnabled());
        log.info("Overlay from {} to {}", from, webAppTo);
        overlay.execute();
    }

    /** Method to do the work of building the war.
     * @throws BuildException if unexpected badness occurs.
     */
    public void execute() throws BuildException {
        final Path warFile = targetDir.resolve("war").resolve("idp.war");

        log.info("Rebuilding {}, Version {}", warFile.toAbsolutePath(), Version.getVersion());
        InstallerSupport.deleteTree(targetDir.resolve("webpapp"));
        final Path webAppTmp =targetDir.resolve("webpapp.tmp");
        InstallerSupport.deleteTree(webAppTmp);
        final Path dist = targetDir.resolve("dist");
        final Path distWebApp =  dist.resolve("webapp");
        assert distWebApp!=null && webAppTmp != null;
        final Copy initial = InstallerSupport.getCopyTask(distWebApp, webAppTmp);
        initial.setPreserveLastModified(true);
        initial.setFailOnError(true);
        initial.setVerbose(log.isDebugEnabled());
        log.info("Initial populate from {} to {}", distWebApp, webAppTmp);
        initial.execute();

        final Path pluginWebapp = targetDir.resolve("dist").resolve("plugin-webapp");
        final Path editWebapp = targetDir.resolve("edit-webapp");
        assert pluginWebapp!=null && editWebapp!=null;
        overlayWebapp(pluginWebapp, webAppTmp);
        overlayWebapp(editWebapp, webAppTmp);

        final File warFileFile = warFile.toFile();
        if (warFileFile.exists() && !warFile.toFile().delete()) {
            log.warn("Could not delete old war file: {}", warFile);
            log.warn("Fix and rerun the build command");
        }
        final Jar jarTask = InstallerSupport.createJarTask(webAppTmp, warFile);
        log.info("Creating war file {}", warFile);
        jarTask.execute();
        InstallerSupport.deleteTree(webAppTmp);
    }
}

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

package net.shibboleth.idp.installer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nonnull;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Jar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.Version;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

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
public final class BuildWar extends AbstractInitializableComponent {

    /** Log. */
    private final Logger log = LoggerFactory.getLogger(BuildWar.class);

    /** Location of the install for the job. */
    private final Path targetDir;

    /** Constructor.
     * @param props The environment for the work.
     * @param installState  Where we are right now.
     */
    public BuildWar(@Nonnull final InstallerProperties props, @Nonnull final CurrentInstallState installState) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(props);
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(installState);
        targetDir = props.getTargetDir();
    }

    /** Constructor.
     * @param idpHome Where to install to.
     */
    public BuildWar(final Path idpHome) {
        targetDir = Constraint.isNotNull(idpHome, "IdPHome should not be null");
    }


    /** Method to do a single overlay into webapp.
     *
     * @param from Where to copy from.
     * @param webAppTo Where to copy to.
     * @throws BuildException if unexpected badness occurs.
     */
    private void overlayWebapp(final Path from, final Path webAppTo) throws BuildException {
        if (!Files.exists(from)) {
            return;
        }
        final Copy overlay = InstallerSupport.getCopyTask(from, webAppTo);
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
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        final Path warFile = targetDir.resolve("war").resolve("idp.war");

        log.info("Rebuilding {}, Version {}", warFile.toAbsolutePath(), Version.getVersion());
        InstallerSupport.deleteTree(targetDir.resolve("webpapp"));
        final Path webAppTmp =targetDir.resolve("webpapp.tmp");
        InstallerSupport.deleteTree(webAppTmp);
        final Path dist = targetDir.resolve("dist");
        final Path distWebApp =  dist.resolve("webapp");
        final Copy initial = InstallerSupport.getCopyTask(distWebApp, webAppTmp);
        initial.setPreserveLastModified(true);
        initial.setFailOnError(true);
        initial.setVerbose(log.isDebugEnabled());
        log.info("Initial populate from {} to {}", distWebApp, webAppTmp);
        initial.execute();

        overlayWebapp(targetDir.resolve("dist").resolve("plugin-webapp"), webAppTmp);
        overlayWebapp(targetDir.resolve("edit-webapp"), webAppTmp);

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

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (!Files.exists(targetDir)) {
            throw new ComponentInitializationException("Target Dir " + targetDir + " does not exist");
        }
    }
}

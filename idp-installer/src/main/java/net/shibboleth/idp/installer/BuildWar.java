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
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Jar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.Version;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

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

    /** Properties for the job. */
    private final InstallerProperties installerProps;

    /** Constructor.
     * @param props The environment for the work.
     * @param installState  Where we are right now.
     */
    public BuildWar(final InstallerProperties props, final CurrentInstallState installState) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(props);
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(installState);
        installerProps = props;
    }

    /** Method to do a single overlay into webapp.
     *
     * @param from Where to copy from.
     * @param webAppTo Where to copy to.
     * @throws BuildException if unexpected badness occurs.
     */
    private void overlayWebapp(final Path from, final Path webAppTo) throws BuildException {
        final Copy overlay = InstallerSupport.getCopyTask(from, webAppTo);
        overlay.setOverwrite(true);
        overlay.setPreserveLastModified(true);
        overlay.setFailOnError(true);
        overlay.setVerbose(log.isDebugEnabled());
        log.info("Overlay from {} to {}", from, webAppTo);
        overlay.execute();
    }

    /** Enumerate all the plugin webapps and deal with them.
     * @param parent the 'dist' folder
     * @param to target
     * @throws BuildException as badness occurrs
     */
    private void overlayPluginWebapps(final Path parent, final Path to) throws BuildException {
        final FileSystem fs = parent.getFileSystem();
        final PathMatcher folderMatcher = fs.getPathMatcher("glob:edit-webapp-*");
        try {
            Files.list(parent).
                filter(Files::isDirectory).
                filter(e -> folderMatcher.matches(e.getFileName())).
                forEach(e -> overlayWebapp(e, to));
        } catch (final IOException e) {
            throw new BuildException(e);
        }
    }

    /** Method to do the work of building the war.
     * @throws BuildException if unexpected badness occurs.
     */
    public void execute() throws BuildException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        final Path target = installerProps.getTargetDir();
        final Path warFile = target.resolve("war").resolve("idp.war");

        log.info("Rebuilding {}, Version {}", warFile.toAbsolutePath(), Version.getVersion());
        InstallerSupport.deleteTree(target.resolve("webpapp"));
        final Path webAppTmp =target.resolve("webpapp.tmp");
        InstallerSupport.deleteTree(webAppTmp);
        final Path dist = target.resolve("dist");
        final Path distWebApp =  dist.resolve("webapp");
        final Copy initial = InstallerSupport.getCopyTask(distWebApp, webAppTmp);
        initial.setPreserveLastModified(true);
        initial.setFailOnError(true);
        initial.setVerbose(log.isDebugEnabled());
        log.info("Initial populate from {} to {}", distWebApp, webAppTmp);
        initial.execute();

        overlayPluginWebapps(dist, webAppTmp);
        overlayWebapp(target.resolve("edit-webapp"), webAppTmp);

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

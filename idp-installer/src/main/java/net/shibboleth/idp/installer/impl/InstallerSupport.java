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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.optional.windows.Attrib;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.selectors.PresentSelector;
import org.apache.tools.ant.types.selectors.PresentSelector.FilePresence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** General common names and helper functions for the installer. */
public final class InstallerSupport {

    /** The name of the file and the property with the current V4 installation value.*/
    public static final String VERSION_NAME = "idp.installed.version";

    /** The name of the file and the property with the previous installation value.*/
    public static final String PREVIOUS_VERSION_NAME = "idp.previous.installed.version";

    /** A psuedo ant-project as parent. */
    private static final Project ANT_PROJECT = new Project();

    /** Log. */
    private static Logger log = LoggerFactory.getLogger(InstallerSupport.class);

    /** Private Constructor. */
    private InstallerSupport() {}

    /** Method to create a directory and log same.
     * @param dir what to create
     * @throws BuildException if bad ness occurs
     */
    protected static void createDirectory(final Path dir) throws BuildException{
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
                log.debug("Created directory {}", dir);
            } catch (final IOException e) {
                log.error("Could no create {}", dir, e);
               throw new BuildException(e);
            }
        }
    }

    /** Copy files.  We use ant rather than {@link Files#copy(Path, Path, java.nio.file.CopyOption...)}
     * because the latter has issues with the Windows ReadOnly Attribute, and the former is tried
     * and tested technology.
     * @param from where to copy from
     * @param to where to copy to
     * @return a partially populated {@link Copy} task
     */
    protected static Copy getCopyTask(final Path from, final Path to) {
        final Copy result = new Copy();
        result.setTodir(to.toFile());
        final FileSet fromSet = new FileSet();
        fromSet.setDir(from.toFile());
        result.addFileset(fromSet);
        result.setProject(ANT_PROJECT);
        return result;
    }

    /** Populate a with all the missing files.
     * @param from where to go from
     * @param to where to go to
     * @throws BuildException if basness occurrs
     * Based on (for instance the following ant<code>
        &lt;!-- flows: copy from dist if not already present --&gt;
        &lt;mkdir dir="${idp.target.dir}/flows" /&gt;
        &lt;copy todir="${idp.target.dir}/flows"&gt;
            &lt;fileset dir="${idp.target.dir}/dist/flows" includes="**\/*"&gt;
                &lt;present present="srconly" targetdir="${idp.target.dir}/flows" /&gt;
            &lt;/fileset &gt;
        &lt;/copy&gt;
        </code>
     *
     */
    protected static void copyDirIfNotPresent(final Path from, final Path to) throws BuildException {
        createDirectory(to);
        final Copy copy = new Copy();
        copy.setTodir(to.toFile());
        final FileSet fromSet = new FileSet();
        fromSet.setDir(from.toFile());
        fromSet.setIncludes("**/**");
        final PresentSelector present = new PresentSelector();
        present.setPresent((FilePresence) FilePresence.getInstance(FilePresence.class, "srconly"));
        present.setTargetdir(from.toFile());
        fromSet.addPresent(present);
        copy.addFileset(fromSet);
        copy.setProject(ANT_PROJECT);
        copy.execute();
        log.debug("Copied not-previously-existing files from {} to {}", from, to);

    }

    /** On Windows sets the readOnly attribute recursively.
     * @param directory where
     * @param readOnly what to set it as
     */
    protected static void setReadOnly(final Path directory, final boolean readOnly) {
        final Attrib attrib = new Attrib();
        attrib.setReadonly(readOnly);
        final FileSet where = new FileSet();
        where.setDir(directory.toFile());
        where.setIncludes("**/**");
        attrib.addFileset(where);
        attrib.setProject(ANT_PROJECT);
        attrib.execute();
    }

    /** Delete the tree.
     * @param where where
     * @throws BuildException if badness occurrs
     */
    public static void deleteTree(final Path where) throws BuildException {
        if (!Files.exists(where)) {
            log.debug("Directory {} does not exist. Skipping delete.", where);
            return;
        }
        if (!Files.isDirectory(where) ) {
            log.error("Directory to be delete {} was a file");
            throw new BuildException("Wanted a directory, found a file");
        }
        final Delete delete = new Delete();
        delete.setDir(where.toFile());
        delete.setFailOnError(false);
        delete.execute();
    }
    
    /** Return a {@link Jar} task.
     * @param baseDir where from
     * @param destFile where to
     * @return the jar task
     */
    public static Jar createJarTask(final Path baseDir, final Path destFile) {
        final Jar jarTask = new Jar();
        jarTask.setBasedir(baseDir.toFile());
        jarTask.setDestFile(destFile.toFile());
        jarTask.setProject(InstallerSupport.ANT_PROJECT);
        return jarTask;
    }
    
}

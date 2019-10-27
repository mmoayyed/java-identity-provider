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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Chmod;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.taskdefs.optional.unix.Chgrp;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.selectors.PresentSelector;
import org.apache.tools.ant.types.selectors.PresentSelector.FilePresence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** General common names and helper functions for the Installer. 
 * This is not intended for general use.
 */
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
     * @throws BuildException if badness occurs
     */
    public static void createDirectory(final Path dir) throws BuildException{
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
                log.debug("Created directory {}", dir);
            } catch (final IOException e) {
                log.error("Could not create {}", dir, e);
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
    public static Copy getCopyTask(final Path from, final Path to) {
        final Copy result = new Copy();
        result.setTodir(to.toFile());
        final FileSet fromSet = new FileSet();
        fromSet.setDir(from.toFile());
        result.addFileset(fromSet);
        result.setProject(ANT_PROJECT);
        result.setVerbose(log.isDebugEnabled());
        return result;
    }

    /** Populate a with all the missing files.
     * @param from where to go from
     * @param to where to go to
     * @throws BuildException if badness occurrs
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
    public static void copyDirIfNotPresent(final Path from, final Path to) throws BuildException {
        createDirectory(to);
        final Copy copy = new Copy();
        copy.setTodir(to.toFile());
        final FileSet fromSet = new FileSet();
        fromSet.setDir(from.toFile());
        fromSet.setIncludes("**/**");
        final PresentSelector present = new PresentSelector();
        present.setPresent((FilePresence) FilePresence.getInstance(FilePresence.class, "srconly"));
        present.setTargetdir(to.toFile());
        fromSet.addPresent(present);
        copy.addFileset(fromSet);
        copy.setProject(ANT_PROJECT);
        copy.setVerbose(log.isDebugEnabled());
        copy.execute();
        log.debug("Copied not-previously-existing files from {} to {}", from, to);

    }

    /** On Windows sets the readOnly attribute on a file.
     * @param file where
     * @param readOnly what to set it as
     * @throws BuildException if badness occurs
     */
    private static void setReadOnlyFile(final Path file, final boolean readOnly) throws BuildException {
        if (readOnly) {
            log.debug("Setting readonly bits on file {}", file);
        } else {
            log.debug("Clearing readonly bits on file {}", file);
        }
        final String line;
        if (readOnly) {
            line = "cmd /c attrib +r \"" + file.toString() + "\"";
        } else {
            line = "cmd /c attrib -r \"" + file.toString() + "\"";
        }
        final String[] command = line.split(" ");

        final Execute exec = new Execute();
        exec.setCommandline(command);
        exec.setAntRun(ANT_PROJECT);
        try {
            exec.execute();
        } catch (final IOException e) {
            log.warn("{} failed: ", line, e);
            throw new BuildException(e);
        }
    }

    /** On Windows sets the readOnly attribute recursively on a directory.
     * @param directory where
     * @param readOnly what to set it as
     * @throws BuildException if badness occurs
     */
    public static void setReadOnlyDir(final Path directory, final boolean readOnly) throws BuildException {
        if (readOnly) {
            log.debug("Recursively setting readonly bits on directory {}", directory);
        } else {
            log.debug("Recursively clearing readonly bits on directory {}", directory);
        }
        final String line;
        if (readOnly) {
            line = "cmd /c attrib /s +r *";
        } else {
            line = "cmd /c attrib /s -r *";
        }
        final String[] command = line.split(" ");

        final Execute exec = new Execute();
        exec.setCommandline(command);
        exec.setWorkingDirectory(directory.toFile());
        exec.setAntRun(ANT_PROJECT);
        try {
            exec.execute();
        } catch (final IOException e) {
            log.warn("{} failed: ", line, e);
            throw new BuildException(e);
        }
    }

    /** On Windows sets the readOnly attribute on a file or recursively on a directory.
     * @param path where
     * @param readOnly what to set it as
     * @throws BuildException if badness occurs
     */
    public static void setReadOnly(final Path path, final boolean readOnly) throws BuildException {
        if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
            log.debug("Not windows. Not [re]setting readonly bit");
            return;
        }
        if (Files.isDirectory(path)) {
            setReadOnlyDir(path, readOnly);
        } else {
            setReadOnlyFile(path, readOnly);
        }
    }

    /** On Non Windows sets the file mode.
     * @param directory where
     * @param permissions what to set
     * @param includes what to include
     * @throws BuildException if badness occurs
     */
    public static void setMode(final Path directory, final String permissions, final String includes)
            throws BuildException {
        log.debug("Performing chmod {} on {} including {}", permissions, directory, includes);
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            log.debug("Windows. Not performing chmod");
            return;
        }
        final Chmod chmod = new Chmod();
        chmod.setProject(ANT_PROJECT);
        chmod.setPerm(permissions);
        chmod.setDir(directory.toFile());
        chmod.setIncludes(includes);
        chmod.setVerbose(log.isDebugEnabled());
        chmod.execute();
    }

    /** On Non Windows sets the files (only) group.
     * @param directory where
     * @param group what to set
     * @param includes what to include
     * @throws BuildException if badness occurs
     */
    public static void setGroup(final Path directory, final String group, final String includes)
            throws BuildException {
        log.debug("Performing chgrp {} on {} including {}", group, directory, includes);
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            log.debug("Windows. Not performing chown");
            return;
        }
        final Chgrp chgrp = new Chgrp();
        chgrp.setProject(ANT_PROJECT);
        chgrp.setVerbose(log.isDebugEnabled());
        chgrp.setGroup(group);
        final FileSet fileSet = new FileSet();
        fileSet.setDir(directory.toFile());
        fileSet.setIncludes(includes);
        chgrp.addFileset(fileSet);
        chgrp.execute();
    }

    /** Delete the tree.
     * @param where where
     * @throws BuildException if badness occurs
     */
    public static void deleteTree(final Path where) throws BuildException {
        if (!Files.exists(where)) {
            log.debug("Directory {} does not exist. Skipping delete.", where);
            return;
        }
        if (!Files.isDirectory(where) ) {
            log.error("Directory to be deleted ({}) was a file");
            throw new BuildException("Wanted a directory, found a file");
        }
        log.debug("Deleting tree {}", where);
        final Delete delete = new Delete();
        delete.setProject(ANT_PROJECT);
        delete.setDir(where.toFile());
        delete.setFailOnError(false);
        // Logic for setVerbose is inverted
        // https://bz.apache.org/bugzilla/show_bug.cgi?id=63887
        delete.setVerbose(!log.isDebugEnabled());
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

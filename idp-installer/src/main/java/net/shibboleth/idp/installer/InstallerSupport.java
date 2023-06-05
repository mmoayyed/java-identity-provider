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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
import org.opensaml.security.httpclient.HttpClientSecurityContextHandler;
import org.slf4j.Logger;

import net.shibboleth.idp.installer.plugin.impl.LoggingVisitor;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.spring.httpclient.resource.HTTPResource;

/** General common names and helper functions for the IdP and Plugin Installers.
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
    @Nonnull private static Logger log = LoggerFactory.getLogger(InstallerSupport.class);

    /** Private Constructor. */
    private InstallerSupport() {}

    /** Method to create a directory and log same.
     * @param dir what to create
     * @throws BuildException if badness occurs
     */
    public static void createDirectory(@Nonnull final Path dir) throws BuildException{
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
                log.debug("Created directory {}", dir);
            } catch (final IOException e) {
                log.error("Could not create {}: {}", dir, e.getMessage());
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
    @Nonnull public static Copy getCopyTask(@Nonnull final Path from, @Nonnull final Path to) {
        return getCopyTask(from, to, "**/.gitkeep");
    }

    /** Copy files.  We use ant rather than {@link Files#copy(Path, Path, java.nio.file.CopyOption...)}
     * because the latter has issues with the Windows ReadOnly Attribute, and the former is tried
     * and tested technology.
     * @param from where to copy from
     * @param to where to copy to
     * @param excludes pattern to exclude
     * @return a partially populated {@link Copy} task
     */
    @Nonnull public static Copy getCopyTask(@Nonnull final Path from, @Nonnull final Path to, @Nonnull final String excludes) {
        final Copy result = new Copy();
        result.setTodir(to.toFile());
        final FileSet fromSet = new FileSet();
        fromSet.setDir(from.toFile());
        final String[] excludes = {exclude};
        fromSet.appendExcludes(excludes );
        result.setPreserveLastModified(true);
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
    public static void copyDirIfNotPresent(@Nonnull final Path from, @Nonnull final Path to) throws BuildException {
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
    private static void setReadOnlyFile(@Nonnull final Path file, final boolean readOnly) throws BuildException {
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
            log.warn("{} failed: {}", line, e.getMessage());
            throw new BuildException(e);
        }
    }

    /** On Windows sets the readOnly attribute recursively on a directory.
     * @param directory where
     * @param readOnly what to set it as
     * @throws BuildException if badness occurs
     */
    public static void setReadOnlyDir(@Nonnull final Path directory, final boolean readOnly) throws BuildException {
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
            log.warn("{} failed: {}", line, e.getMessage());
            throw new BuildException(e);
        }
    }

    /** On Windows sets the readOnly attribute on a file or recursively on a directory.
     * @param path where
     * @param readOnly what to set it as
     * @throws BuildException if badness occurs
     */
    public static void setReadOnly(@Nonnull final Path path, final boolean readOnly) throws BuildException {
        if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
            log.debug("Not windows. Not [re]setting readonly bit");
            return;
        }
        if (!Files.exists(path) ) {
            log.debug("Directory {} does not exist, not performing Attrib -/+r", path);
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
    public static void setMode(@Nonnull final Path directory, @Nonnull final String permissions, @Nonnull final String includes)
            throws BuildException {
        if (!Files.exists(directory) ) {
            log.debug("Directory {} does not exist, not performing chmod", directory);
            return;
        }
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
    public static void setGroup(@Nonnull final Path directory, @Nonnull final String group, @Nonnull final String includes)
            throws BuildException {
        if (!Files.exists(directory) ) {
            log.debug("Directory {} does not exist, not performing chgrp", directory);
            return;
        }
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
    public static void deleteTree(@Nullable final Path where) throws BuildException {
        deleteTree(where, null);
    }

    /** Delete the tree.
     * @param where where
     * @param excludes wildcards to exclude
     * @throws BuildException if badness occurs
     */
    public static void deleteTree(@Nullable final Path where, @Nullable final String excludes) throws BuildException {
        if (where == null) {
            return;
        }
        if (where == null || !Files.exists(where)) {
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
        delete.setFailOnError(false);
        delete.setIncludeEmptyDirs(true);
        if (excludes != null) {
            final FileSet set = new FileSet();
            set.setExcludes(excludes);
            set.setIncludes("**/**");
            set.setDir(where.toFile());
            delete.addFileset(set);
        } else {
            delete.setDir(where.toFile());
        }
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

    /** Download helper method.
     * @param baseResource where to go for the file
     * @param handler HttpClientSecurityContextHandler to use
     * @param downloadDirectory where to download to
     * @param fileName the file name
     * @throws IOException as required
     */
    public static void download(@Nonnull final HTTPResource baseResource,
            @Nonnull final HttpClientSecurityContextHandler handler,
            @Nonnull final Path downloadDirectory,
            @Nonnull final String fileName) throws IOException {
        final HTTPResource httpResource = baseResource.createRelative(fileName, handler);
        final Path filePath = downloadDirectory.resolve(fileName);
        log.info("Downloading from {}", httpResource.getDescription());
        log.debug("Downloading to {}", filePath);
        try (final OutputStream fileOut = new ProgressReportingOutputStream(new FileOutputStream(filePath.toFile()))) {
            httpResource.getInputStream().transferTo(fileOut);
        }
    }

    /** Return the canonical path.
     * @param from the path we get given
     * @return the canonicalized one
     * @throws IOException  as from {@link File#getCanonicalFile()}
     */
    @SuppressWarnings("null")
    @Nonnull
    public static Path canonicalPath(@Nonnull final Path from) throws IOException {
        return from.toFile().getCanonicalFile().toPath();
    }

    /** Rename Files into the provided tree.
     * @param fromBase The root directory of the from files
     * @param toBase The root directory to rename to
     * @param fromFiles The list of files (inside fromBase) to rename
     * @param renames All the work as it is done
     * @throws IOException If any of the file operations fail
     */
    public static void renameToTree(@Nonnull final Path fromBase,
            @Nonnull final Path toBase,
            @Nonnull final List<Path> fromFiles,
            @Nonnull @Live final List<Pair<Path, Path>> renames) throws IOException {
        if (!Files.exists(toBase)) {
            Files.createDirectories(toBase);
        }
        for (final Path path : fromFiles) {
            if (!Files.exists(path)) {
                log.info("File {} was not renamed away because it does not exist", path);
                continue;
            }
            final Path relName = fromBase.relativize(path);
            log.trace("Relative name {}", relName);
            final Path to = toBase.resolve(relName);
            Files.createDirectories(to.getParent());
            Files.move(path,to);
            renames.add(new Pair<>(path, to));
        }
    }

    /** Traverse "from" looking to see if any of the files are already in "to".
     * @param from source directory
     * @param to target directory
     * @return true if there was a match
     * @throws BuildException if anything threw and {@link IOException}
     */
    public static boolean detectDuplicates(@Nonnull final Path from, @Nullable final Path to) throws BuildException {

        if (to == null || !Files.exists(to)) {
            return false;
        }
        final NameClashVisitor detector = new NameClashVisitor(from, to);
        log.debug("Walking {}, looking for a name clash in {}", from, to);
        try {
            Files.walkFileTree(from, detector);
        } catch (final IOException e) {
            log.error("Failed during duplicate detection:", e);
            throw new BuildException(e);
        }
        return detector.wasNameClash();
    }

    /** Copy a directory tree and keep a log of what has changed.
     * @param from source directory
     * @param to target directory
     * @param pathsCopied the list of files copied up (including if there was a failure)
     * @throws BuildException from the copy
     */
    public static void copyWithLogging(@Nullable final Path from, 
            @Nonnull final Path to, @Nonnull @Live final List<Path> pathsCopied) throws BuildException {
        if (from == null || !Files.exists(from)) {
            return;
        }
        log.debug("Copying from {} to {}", from, to);
        final LoggingVisitor visitor = new LoggingVisitor(from, to);
        try {
            Files.walkFileTree(from, visitor);
        } catch (final IOException e) {
            pathsCopied.addAll(visitor.getCopiedList());
            log.error("Error copying files from {} to {}", from, to, e);
            throw new BuildException(e);
        }
        pathsCopied.addAll(visitor.getCopiedList());
    }

    /**
     * A @{link {@link FileVisitor} which detects (and logs) whether a copy would overwrite.
     */
    private static final class NameClashVisitor extends SimpleFileVisitor<Path> {
        /** did we find a duplicate. */
        private boolean nameClash;

        /** Path we are traversing. */
        private final Path from;

        /** Path where we check for Duplicates. */
        private final Path to;

        /**
         * Constructor.
         *
         * @param fromDir Path we are traversing
         * @param toDir Path where we check for Duplicates
         */
        public NameClashVisitor(@Nonnull final Path fromDir, @Nonnull final Path toDir) {
            from = fromDir;
            to = toDir;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            final Path relFile = from.relativize(file);
            final Path toFile = to.resolve(relFile);
            if (Files.exists(toFile)) {
                nameClash = true;
                log.warn("{} already exists", toFile);
            }
            return FileVisitResult.CONTINUE;
        }

        /** did we find a name clash?
         * @return whether we found a name clash.
         */
        public boolean wasNameClash() {
            return nameClash;
        }
    }

    /** Predicate to ask the user if they want to install the trust store provided. */
    public static class InstallerQuery implements Predicate<String> {

        /** What to say. */
        @Nonnull
        private final String promptText;

        /**
         * Constructor.
         * @param text What to say before the prompt information
         */
        public InstallerQuery(@Nonnull final String text) {
            promptText = Constraint.isNotNull(text, "Text should not be null");
        }

        /** {@inheritDoc} */
        public boolean test(final String keyString) {
            if (System.console() == null) {
                log.error("No Console Attached to installer");
                return false;
            }
            System.console().printf("%s:\n%s [yN] ", promptText, keyString);
            System.console().flush();
            final String result  = StringSupport.trimOrNull(System.console().readLine());
            return result != null && "y".equalsIgnoreCase(result.substring(0, 1));
        }
    }
}

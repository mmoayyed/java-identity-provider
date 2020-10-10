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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.collection.Pair;

/**
 * Support for copying files during plugin manipulation.
 */
public final class PluginInstallerSupport {
    
    /** Class logger. */
    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(PluginInstallerSupport.class);

    /** Constructor. */
    private PluginInstallerSupport() {
    }

    /** Return the canonical path.
     * @param from the path we get given
     * @return the canonicalized one
     * @throws IOException  as from {@link File#getCanonicalFile()}
     */
    static Path canonicalPath(final Path from) throws IOException {
        return from.toFile().getCanonicalFile().toPath();
    }

    /** Delete a directory tree. 
     * @param directory what to delete
     */
    public static void deleteTree(@Nullable final Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        LOG.debug("Deleting directory {}", directory);
        InstallerSupport.setReadOnly(directory, false);
        try {
            Files.walkFileTree(directory, new DeletingVisitor());
        } catch (final IOException e) {
            LOG.error("Couldn't delete {}", directory, e);
        }
    }
    
    /** Traverse "from" looking to see if any of the files are already in "to".
     * @param from source directory
     * @param to target directory
     * @return true if there was a match 
     * @throws BuildException if anything threw and {@link IOException}
     */
    public static boolean detectDuplicates(final Path from, final Path to) throws BuildException {
        
        if (to == null || !Files.exists(to)) {
            return false;
        }
        final NameClashVisitor detector = new NameClashVisitor(from, to);
        LOG.debug("Walking {}, looking for a name clash in {}", from, to);
        try {
            Files.walkFileTree(from, detector);
        } catch (final IOException e) {
            LOG.error("Failed during duplicate detection:", e);
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
    public static void copyWithLogging(final Path from, 
            final Path to, @Live final List<Path> pathsCopied) throws BuildException {
        if (from == null || !Files.exists(from)) {
            return;
        }
        LOG.debug("Copying from {} to {}", from, to);
        final LoggingVisitor visitor = new LoggingVisitor(from, to);
        try {
            Files.walkFileTree(from, visitor);
        } catch (final IOException e) {
            pathsCopied.addAll(visitor.getCopiedList());
            LOG.error("Error copying files from {} to {}", from, to, e);
            throw new BuildException(e);
        }
        pathsCopied.addAll(visitor.getCopiedList());
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
            @Nonnull final List<String> fromFiles,
            @Nonnull @Live final List<Pair<Path, Path>> renames) throws IOException {

        if (!Files.exists(toBase)) {
            Files.createDirectories(toBase);
        }
        for (final String file : fromFiles) {
            final Path path = Path.of(file);
            if (!Files.exists(path)) {
                LOG.info("File {} was not renamed away because it does not exist", file);
                continue;
            }
            final Path relName = fromBase.relativize(path);
            LOG.trace("Relative name {}", relName);
            final Path to = toBase.resolve(relName);
            Files.createDirectories(to.getParent());
            Files.move(path,to);
            renames.add(new Pair<>(path, to));
        }
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
        public NameClashVisitor(final Path fromDir, final Path toDir) {
            from = fromDir;
            to = toDir;
        }
        
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            final Path relFile = from.relativize(file);
            final Path toFile = to.resolve(relFile);
            if (Files.exists(toFile)) {
                nameClash = true;
                LOG.warn("{} already exists", toFile);
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
    
    /**
     * A @{link {@link FileVisitor} which detects (and logs) whether a copy would overwrite.
     */
    private static final class LoggingVisitor extends SimpleFileVisitor<Path> {
        /** How what files have we copied? */
        private final List<Path> copiedFiles = new ArrayList<>();

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
        public LoggingVisitor(final Path fromDir, final Path toDir) {
            from = fromDir;
            to = toDir;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            final Path relDir = from.relativize(dir);
            final Path toDir = to.resolve(relDir);
            if (!Files.exists(toDir)) {
                LOG.trace("Creating directory {}", toDir);
                Files.createDirectory(toDir);
            }
            return FileVisitResult.CONTINUE;
        };

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            final Path relFile = from.relativize(file);
            final Path toFile = to.resolve(relFile);
            copiedFiles.add(toFile);
            try(final InputStream in = new BufferedInputStream(new FileInputStream(file.toFile()));
                final OutputStream out = new BufferedOutputStream(new FileOutputStream(toFile.toFile()))) {
                in.transferTo(out);
            }
            return FileVisitResult.CONTINUE;
        }
        
        /** did we find a name clash?
         * @return whether we found a name clash.
         */
        public List<Path> getCopiedList() {
            return copiedFiles;
        }
    }

    /**
     * A @{link {@link FileVisitor} which deletes files.
     */
    private static final class DeletingVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            try {
                Files.delete(file);
            } catch (final IOException e) {
                LOG.error("Could not delete {}", file.toAbsolutePath(), e);
                file.toFile().deleteOnExit();
                // and carry on
            }
            return FileVisitResult.CONTINUE;
        }
        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            if (exc != null) {
                throw exc;
            }
            try {
                Files.delete(dir);
            } catch (final IOException e) {
                LOG.error("Could not delete {}", dir.toAbsolutePath(), e);
                dir.toFile().deleteOnExit();
                // and carry on
            }
            return FileVisitResult.CONTINUE;
        }
    }
}

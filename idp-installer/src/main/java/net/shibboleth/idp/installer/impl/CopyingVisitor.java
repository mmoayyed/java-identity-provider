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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.logic.Constraint;


/** {@link FileVisitor} which copies a directory tree.
 * This class is based on that found in the javadoc for {@link FileVisitor}.
 *
 */
public class CopyingVisitor extends SimpleFileVisitor<Path> {

    /** Class logger. */
    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(CopyingVisitor.class);

    /** The 'to' directory.*/
    private final Path source;

    /** The 'from' directory.*/
    private final Path target;

    /** Do we overwriting or leave exiting in place. */
    private final boolean overWrite;

    /**
     * Constructor.
     *
     * @param from source to copy from.
     * @param to target to Copy to.
     * @param replace do we replace or leave in place.
     */
    public CopyingVisitor(final Path from, final Path to, final boolean replace) {
        Constraint.isTrue(!Files.exists(to) || Files.isDirectory(to), "Destination should be a directory or not exist");
        Constraint.isTrue(Files.exists(from) && Files.isDirectory(from), "Source should be a directory and exist");

        source = from;
        target = to;
        overWrite = replace;
    }

    /** {@inheritDoc} */
    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        final Path targetDir = target.resolve(source.relativize(dir));
        try {
            if (!Files.exists(targetDir)) {
                LOG.debug("Creating Directory {}", targetDir);
                Files.copy(dir, targetDir);
            } else {
                LOG.debug("Directory {} exists already", targetDir);
            }
        } catch (final FileAlreadyExistsException e) {
             if (!Files.isDirectory(targetDir)) {
                 throw e;
             }
        }
        return FileVisitResult.CONTINUE;
    }

    /** {@inheritDoc} */
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        final Path targetFile = target.resolve(source.relativize(file));
        if (overWrite) {
            LOG.debug("Overwriting file {}", targetFile);
            Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } else if (!Files.exists(targetFile)) {
            LOG.debug("Creating file {}", targetFile);
            Files.copy(file, targetFile);
        } else {
            LOG.debug("File {} exists, not copied", targetFile);
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * Method to copy a directory tree.
     *
     * @param from source to copy from.
     * @param to target to Copy to.
     * @throws IOException if the tree walks fails
     */
    public static void copyTree(final Path from, final Path to) throws IOException {
        copyTree(from, to, false);
    }

    /**
     * Method to copy a directory tree.
     *
     * @param from source to copy from.
     * @param to target to Copy to.
     * @param overWrite do we leave existing files in place or overwrite them?
     * @throws IOException if the tree walks fails
     */
    public static void copyTree(final Path from, final Path to, final boolean overWrite) throws IOException {
        LOG.debug("Copying From {} to {} overwrite = {}", from, to, overWrite);
        final CopyingVisitor visitor = new CopyingVisitor(from, to, overWrite);
        Files.walkFileTree(from, visitor);
    }
}

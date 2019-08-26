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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.logic.Constraint;


/** {@link FileVisitor} which deletes a directory tree. 
 * This class is based on that found in the javadoc for {@link FileVisitor}. 
 *
 */
public class DeletingVisitor extends SimpleFileVisitor<Path> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DeletingVisitor.class);

    /** {@inheritDoc} */
    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
        if (e == null) {
            log.trace("Deleting Directory {}", dir);
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
        // directory iteration failed
        throw e;
    }

    /** {@inheritDoc} */
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        log.trace("Deleting file {}", file);
        Files.delete(file);
        return FileVisitResult.CONTINUE;
    }

    /**
     * Method to delete a tree.
     *
     * @param root where to delete
     * @throws IOException if the tree walks fails
     */
    public static void deleteTree(final Path root) throws IOException {
        Constraint.isTrue(!Files.exists(root) || Files.isDirectory(root),
                "Delete point should be a directory and must exist");
        Files.walkFileTree(root, new DeletingVisitor());
    }
}

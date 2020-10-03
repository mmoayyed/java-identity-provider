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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import net.shibboleth.idp.installer.InstallerSupport;

/**
 * set up state for testing.
 */
@SuppressWarnings("javadoc")
public class BasePluginTest {

    private static Logger log = LoggerFactory.getLogger(BasePluginTest.class);
    private static Path idpHome;

    @BeforeSuite public void setupIdpHome() throws IOException {
        idpHome = Files.createTempDirectory("PluginTests");
        Path from = new ClassPathResource("idphome-test").getFile().toPath();
        Files.walkFileTree(from, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path rel = from.relativize(dir);
                Path to = idpHome.resolve(rel);
                if (!Files.exists(to)) {
                    log.trace("Creating directory {}", to);
                    Files.createDirectory(to);
                }
                return FileVisitResult.CONTINUE;
            };
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path rel = from.relativize(file);
                Path to = idpHome.resolve(rel);
                log.trace("Copying from {} to {}", file, to);
                Files.copy(file,  to);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @AfterSuite public void teardownIdPHome() throws IOException {
        if (idpHome == null) {
            return;
        }
        InstallerSupport.setReadOnly(idpHome, false);
        PluginInstallerSupport.deleteTree(idpHome);
    }

    protected Path getIdpHome() {
        return idpHome;
    }
}

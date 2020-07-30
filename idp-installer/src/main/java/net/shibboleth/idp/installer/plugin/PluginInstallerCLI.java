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

package net.shibboleth.idp.installer.plugin;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import net.shibboleth.ext.spring.cli.AbstractCommandLine;
import net.shibboleth.idp.Version;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Command line for Plugin Installation.
 */
public final class PluginInstallerCLI extends AbstractCommandLine<PluginInstallerArguments> {

    /** Class logger. */
    @Nullable private Logger log;
    
    /** Where the IdP is installed to. */
    @Nullable private Path idpHome;
    
    /**
     * Constrained Constructor.
     */
    private PluginInstallerCLI() {
    }

    /** Set where the IdP is installed to.
     * @param home where
     */
    protected void setIdpHome(@Nullable final String home) {
        if (home == null) {
            getLogger().error("net.shibboleth.idp.cli.idp.home propert not send.  Could not find IdP Home directory");
            return;
        }
        idpHome = Path.of(home);
        
        if (!Files.exists(idpHome) || !Files.isDirectory(idpHome)) {
            getLogger().error("IdP Home Directory {} did not exist or was not a directory", idpHome);
            idpHome = null;
        }
    }
    
    /** Return where the IdP is installed to.
     * @return the home directory.
     */
    @Nullable protected Path getIdpHome() {
        return idpHome;
    }


    /** {@inheritDoc} */
    @Override
    @Nonnull protected Logger getLogger() {
        if (log == null) {
            log = LoggerFactory.getLogger(PluginInstallerCLI.class);
        }
        return log;
    }

    /** {@inheritDoc} */
    @Override
    protected Class<PluginInstallerArguments> getArgumentClass() {
        return PluginInstallerArguments.class;
    }

    /** {@inheritDoc} */
    @Override
    protected String getVersion() {
        return Version.getVersion();
    }
    
    /** {@inheritDoc} */
    protected List<Resource> getAdditionalSpringResources() {
        return List.of(
                new ClassPathResource("net/shibboleth/idp/installer/plugin/patchResources.xml"),
                new FileSystemResource(idpHome.resolve("conf").resolve("global.xml")));
    }
    
    /** {@inheritDoc} */
    protected int doRun(final PluginInstallerArguments args) {
        getLogger().warn("Starting");
        final int ret = super.doRun(args);
        if (ret != RC_OK) {
            return ret;
        }
        try {
            Files.walkFileTree(getIdpHome().resolve("conf"), new SimpleFileVisitor<Path>() {
                @Override 
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    getLogger().warn("File {}",file);
                    return FileVisitResult.CONTINUE;
                }
                @Override 
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    getLogger().warn("Dire {}",dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            getLogger().error("oops", e);
            return RC_IO;
        } 
        return ret;
    }
    
    /**
     * CLI entry point.
     * @param args arguments
     */
    public static void main(@Nonnull final String[] args) {
        final PluginInstallerCLI cli = new PluginInstallerCLI();
        cli.setIdpHome(StringSupport.trimOrNull(System.getProperty("net.shibboleth.idp.cli.idp.home")));
        if (cli.getIdpHome() == null) {
            System.exit(RC_INIT);
        } else {
            System.exit(cli.run(args));
        }
    }

}

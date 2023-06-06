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

package net.shibboleth.idp.module.core.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.annotation.Nonnull;

import org.springframework.util.ResourceUtils;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.module.impl.CoreIdPModule;
import net.shibboleth.profile.module.ModuleContext;
import net.shibboleth.profile.module.ModuleException;

/**
 * {@link IdPModule} implementation.
 * 
 * <p>This is a somewhat special module as it represents the "core" software
 * being installed or upgraded so has some different characteristics and methods.
 */
public final class Core extends CoreIdPModule {

    /**
     * Constructor.
     *  
     * @throws ModuleException on error
     * @throws IOException on error
     */
    public Core() throws IOException, ModuleException {
        super(Core.class);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEnabled(@Nonnull final ModuleContext moduleContext) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public Map<ModuleResource, ResourceResult> enable(@Nonnull final ModuleContext moduleContext)
            throws ModuleException {
        
        // First we need to ensure the basic directory layout is in place. On upgrade this won't have any effect.
        try {
            if (!moduleContext.getInstallLocation().startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
                final Path home = Path.of(moduleContext.getInstallLocation());
                Files.createDirectories(home.resolve("conf"));
                Files.createDirectories(home.resolve("credentials"));
                Files.createDirectories(home.resolve("metadata"));
                Files.createDirectories(home.resolve("flows"));
                Files.createDirectories(home.resolve("messages"));
                Files.createDirectories(home.resolve("views"));
                // Tidy up files we no longer need in V5
                Path antFile = home.resolve("bin").resolve("build.xml");
                if (Files.exists(antFile)) {
                    Files.delete(antFile);
                }
                antFile = home.resolve("bin").resolve("ant.bat");
                if (Files.exists(antFile)) {
                    Files.delete(antFile);
                }
                antFile = home.resolve("bin").resolve("ant.sh");
                if (Files.exists(antFile)) {
                    Files.delete(antFile);
                }
            }
        } catch (final Exception e) {
            throw new ModuleException(e);
        }
        
        return super.enable(moduleContext);
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public Map<ModuleResource, ResourceResult> disable(@Nonnull final ModuleContext moduleContext,
            final boolean clean) throws ModuleException {
        throw new ModuleException("This module cannot be disabled.");
    }

    
}
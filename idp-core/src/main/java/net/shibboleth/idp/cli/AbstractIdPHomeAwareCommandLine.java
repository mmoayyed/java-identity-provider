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

package net.shibboleth.idp.cli;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import net.shibboleth.ext.spring.cli.AbstractCommandLine;
import net.shibboleth.ext.spring.cli.CommandLineArguments;
import net.shibboleth.idp.spring.IdPPropertiesApplicationContextInitializer;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * An extension to {@link AbstractCommandLine} that understand that idp.home is set via a property
 * when called inside the IdP.
 *
 * @param <T> argument object type
 * 
 * @since 4.1.0
 */
public abstract class AbstractIdPHomeAwareCommandLine<T extends CommandLineArguments> extends AbstractCommandLine<T> {

    /** Where the IdP is installed to. */
    @Nullable private Path idpHome;
    
    /**
     * Constructor.
     */
    protected AbstractIdPHomeAwareCommandLine() {
        setIdpHome(StringSupport.trimOrNull(System.getProperty("net.shibboleth.idp.cli.idp.home")));
        setContextInitializer(this.new Initializer());
    }

    /** Set where the IdP is installed to.
     * @param home where
     */
    protected void setIdpHome(@Nullable final String home) {
        if (home == null) {
            getLogger().error("net.shibboleth.idp.cli.idp.home property not set, could not find IdP home directory");
            return;
        }
        idpHome = Path.of(home);
        
        if (!Files.exists(idpHome) || !Files.isDirectory(idpHome)) {
            getLogger().error("IdP home directory '{}' did not exist or was not a directory", idpHome);
            idpHome = null;
        }
    }
    
    /** 
     * Gets IdP installation location.
     * 
     * @return the home directory
     */
    @Nullable protected Path getIdpHome() {
        return idpHome;
    }
    
    /**
     * An {@link ApplicationContextInitializer} which knows about our idp.home.
     */
    private class Initializer extends IdPPropertiesApplicationContextInitializer {

        /** {@inheritDoc} */
        @Override @Nonnull public String selectSearchLocation(
                @Nonnull final ConfigurableApplicationContext applicationContext) {
            return idpHome.toString();
        }

        /** {@inheritDoc} */
        @Override @Nonnull public String getSearchLocation() {
            return idpHome.toString();
        }
    }
}

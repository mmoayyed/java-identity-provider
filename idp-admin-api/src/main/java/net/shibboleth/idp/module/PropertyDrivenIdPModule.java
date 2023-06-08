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

package net.shibboleth.idp.module;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.Nonnull;

import net.shibboleth.idp.Version;
import net.shibboleth.profile.module.ModuleException;
import net.shibboleth.profile.module.PropertyDrivenModule;

/**
 * Implementation of {@link IdPModule} relying on Java {@link Properties}.
 * 
 * @since 4.1.0
 */
public class PropertyDrivenIdPModule extends PropertyDrivenModule implements IdPModule {

    /**
     * Constructor.
     *
     * @param claz type of object used to locate default module.properties resource
     * 
     * @throws IOException if unable to read file
     * @throws ModuleException if the module is not in a valid state
     */
    public PropertyDrivenIdPModule(@Nonnull final Class<? extends IdPModule> claz) throws IOException, ModuleException {
        this(claz.getResourceAsStream(DEFAULT_RESOURCE));
    }
    
    /**
     * Constructor.
     *
     * @param inputStream property stream
     * 
     * @throws IOException if unable to read file
     * @throws ModuleException if the module is not in a valid state
     */
    public PropertyDrivenIdPModule(@Nonnull final InputStream inputStream)
            throws IOException, ModuleException {
        super(Version.getVersion(), inputStream);
    }

    /**
     * Constructor.
     *
     * @param properties property set
     * 
     * @throws ModuleException if the module is not in a valid state
     */
    public PropertyDrivenIdPModule(@Nonnull final Properties properties) throws ModuleException {
        super(Version.getVersion(), properties);
    }

    /** {@inheritDoc} */
    @Override @Nonnull
    public String getSaveExtension() {
        return IdPModule.IDPSAVE_EXT;
    }

    /** {@inheritDoc} */
    @Override @Nonnull
    public String getNewExtension() {
        return IdPModule.IDPNEW_EXT_BASE;
    }
}
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

package net.shibboleth.idp.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.Nonnull;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.profile.plugin.PluginException;
import net.shibboleth.profile.plugin.PropertyDrivenPlugin;

/**
 * Implementation of {@link IdPPlugin} relying on Java {@link Properties}.
 * 
 * @since 4.1.0
 */
public abstract class PropertyDrivenIdPPlugin extends PropertyDrivenPlugin<IdPModule> implements IdPPlugin {

    /**
     * Constructor.
     *
     * @param claz type of object used to locate default module.properties resource
     * 
     * @throws IOException if unable to read file
     * @throws PluginException if the plugin is not in a valid state
     */
    public PropertyDrivenIdPPlugin(@Nonnull final Class<? extends IdPPlugin> claz) throws IOException, PluginException {
        super(claz);
    }
    
    /**
     * Constructor.
     *
     * @param inputStream property stream
     * 
     * @throws IOException if unable to read file
     * @throws PluginException if the plugin is not in a valid state
     */
    public PropertyDrivenIdPPlugin(@Nonnull final InputStream inputStream)
            throws IOException, PluginException {
        super(inputStream);
    }

    /**
     * Constructor.
     *
     * @param properties property set
     * 
     * @throws PluginException if the plugin is not in a valid state
     */
    public PropertyDrivenIdPPlugin(@Nonnull final Properties properties) throws PluginException {
        super(properties);
    }
    
}
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.module.impl;

import java.io.IOException;

import javax.annotation.Nonnull;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.module.PropertyDrivenIdPModule;
import net.shibboleth.profile.module.ModuleException;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * Implementation base class for {@link IdPModule} that is shipped in a plugin
 * produced by the Shibboleth Project ourselves and for which the documentation
 * will be in the wiki in a fixed location.
 * 
 */
public class PluginIdPModule extends PropertyDrivenIdPModule {

    /** Documentation base URL. */
    @Nonnull @NotEmpty
    public static final String MODULE_URL_BASE = "https://wiki.shibboleth.net/confluence/display/IDPPLUGINS";
    
    /**
     * Constructor.
     *
     * @param claz implementation class of the module
     * 
     * @throws IOException if an I/O error occurs
     * @throws ModuleException if a generic error occurs
     */
    public PluginIdPModule(@Nonnull final Class<? extends IdPModule> claz) throws IOException, ModuleException {
        super(claz);
    }

    /**
     * {@inheritDoc}
     * 
     * The override prepends a known documentation prefix to all module URLs.
     */
    @Override
    protected void load() throws ModuleException {
        super.load();
        
        final String current = getURL();
        
        if (current != null && current.startsWith("/")) {
            setURL(MODULE_URL_BASE + current);
        }
    }

}
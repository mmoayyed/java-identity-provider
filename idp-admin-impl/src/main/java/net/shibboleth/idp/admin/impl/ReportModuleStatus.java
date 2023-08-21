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

package net.shibboleth.idp.admin.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.profile.module.ModuleContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Class to check that every {@link IdPPlugin} the required {@link IdPModule} available.
 */
public class ReportModuleStatus extends AbstractIdentifiableInitializableComponent {

    /** Log. */
    private final Logger log = LoggerFactory.getLogger(ReportModuleStatus.class);
    
    /** The IdOP Home dir. */
    @Nonnull private String idpHome="";

    /** Where IdP Home is.
     * @param input what to set.
     */
    public void setIdpHome(@Nonnull @NotEmpty String input) {
        idpHome = Constraint.isNotNull(input, "IdpHome not set");
    }
    
    /** Return all the enabled modules.
     * @return the module ids
     */
    @Nonnull private Set<String> getEnabledModules() {
        final HashSet<String> result = new HashSet<>();
        final ModuleContext mc = new ModuleContext(idpHome);
        final Iterator<IdPModule> modules = ServiceLoader.load(IdPModule.class).iterator();
        while (modules.hasNext()) {
            final IdPModule module = modules.next();
            if (module.isEnabled(mc)) {
                log.trace("Found enabled Module {}", module.getId());
                result.add(module.getId());
            } else {
                log.trace("Found disabled Module {}", module.getId());

            }
        }
        return result;
    }

    /** Check whether any plugin required a non enabled plugin. 
     * @param enabledModules
     */
    private void checkPlugins(@Nonnull Set<String> enabledModules) {
        final Iterator<IdPPlugin> plugins = ServiceLoader.load(IdPPlugin.class).iterator();
        while (plugins.hasNext()) {
            final IdPPlugin plugin = plugins.next();
            log.debug("Checking required modules for plugin {}", plugin.getPluginId());
            for (final String module : plugin.getRequiredModules()) {
                if (enabledModules.contains(module)) {
                    log.debug("Found required module {}", module);
                } else {
                    log.error("Module {} required by plugin {} not found", module, plugin.getPluginId());
                }
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        checkPlugins(getEnabledModules());
    }

}

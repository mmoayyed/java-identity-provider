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

package net.shibboleth.idp.installer.plugin.impl;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.annotation.Nonnull;

import org.springframework.core.io.ClassPathResource;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.profile.plugin.AbstractPlugin;
import net.shibboleth.shared.collection.CollectionSupport;

public class TestPlugin extends AbstractPlugin<IdPModule> implements IdPPlugin {

    /** {@inheritDoc} */
    @Override
    public @Nonnull String getPluginId() {
        return "net.shibboleth.plugin.test";
    }

    /** {@inheritDoc} */
    @Override
    public @Nonnull List<URL> getUpdateURLs() {
        ClassPathResource resource = new ClassPathResource("/net/shibboleth/idp/plugin/plugins.props");
        try {
            return CollectionSupport.singletonList(resource.getURL());
        } catch (IOException e) {
            return CollectionSupport.emptyList();
        }
    }
    
    /** {@inheritDoc} */
    public String getLicenseFileLocation() {
        return "/net/shibboleth/idp/plugin/test.license";
    }

    /** {@inheritDoc} */
    @Override
    public int getMajorVersion() {
        return 2;
    }

    /** {@inheritDoc} */
    @Override
    public int getMinorVersion() {
        return 0;
    }
    

    /** {@inheritDoc} */
    @Override
    public int getPatchVersion() {
        return 0;
    }
}

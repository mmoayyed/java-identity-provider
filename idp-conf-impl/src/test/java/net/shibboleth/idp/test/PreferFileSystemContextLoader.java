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

package net.shibboleth.idp.test;

import javax.annotation.Nonnull;

import org.springframework.test.context.web.GenericXmlWebContextLoader;
import org.springframework.test.context.web.WebMergedContextConfiguration;
import org.springframework.web.context.support.GenericWebApplicationContext;

import net.shibboleth.shared.spring.resource.ConditionalResourceResolver;
import net.shibboleth.shared.spring.resource.PreferFileSystemResourceLoader;

/**
 * An extension of {@link GenericXmlWebContextLoader} which sets the resource loader used by the application context to
 * {@link PreferFileSystemResourceLoader}.
 */
public class PreferFileSystemContextLoader extends GenericXmlWebContextLoader {

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Set the resource loader used by the application context to {@link PreferFileSystemResourceLoader}.
     * </p>
     */
    @Override protected void customizeContext(@Nonnull GenericWebApplicationContext context,
            @Nonnull WebMergedContextConfiguration webMergedConfig) {
        final PreferFileSystemResourceLoader loader = new PreferFileSystemResourceLoader();
        loader.addProtocolResolver(new ConditionalResourceResolver());
        context.setResourceLoader(loader);
    }

}
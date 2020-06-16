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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.tools.ant.BuildException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.plugin.PluginDescription;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 *  The class where the heavy lifting of managing a plugin happens. 
 */
public final class PluginInstaller extends AbstractInitializableComponent {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PluginInstaller.class);

    /** Where we are installing to. */
    @NonnullAfterInit private Path idpHome;
    
    /** What we are dealing with. */
    @NonnullAfterInit private String pluginId;
    
    /** Our TrustStore. */
    @NonnullAfterInit private TrustStore trustStore;
      
    /** set IdP Home.
     * @param home Where we are working from
     */
    public void setIdpHome(@Nonnull final Path home) {
        idpHome = Constraint.isNotNull(home, "IdPHome should be non-null");
    }

    /** Set the plugin in.
     * @param id The pluginId to set.
     */
    public void setPluginId( @Nonnull @NotEmpty final String id) {
        pluginId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Plugin id should be be non-null");
    }    

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        trustStore = new TrustStore();
        trustStore.setIdpHome(idpHome);
        trustStore.setPluginId(pluginId);
        trustStore.initialize();
        super.doInitialize();
    }
    
    /** Install the plugin from the provided URL.  Involves downloading
     *  the file and then doing a {@link #installPlugin(Path, String, boolean)}.
     * @param baseURL where we get the files from
     * @param fileName the name
     * @param isTgz true if this is tgz, false if this is zip
     */
    public void installPlugin(@Nonnull final URL baseURL,
                              @Nonnull @NotEmpty final String fileName,
                              final boolean isTgz) {
        
    }
    
    /** Install the plugin from a local path.
     * <ul><li> Check signature</li>
     * <li>Unpack to temp folder</li>
     * <li>Install from the folder</li></ul>
     * @param base the directory where the files are
     * @param fileName the name
     * @param isTgz true if this is tgz, false if this is zip
     */
    public  void installPlugin(@Nonnull final Path base, 
                               @Nonnull @NotEmpty final String fileName,
                               final boolean isTgz) {
        
    }
    
    /**
     * Return a list of the installed plugins.
     * @return All the plugins.
     */
    public List<PluginDescription> getInstalledPlugins() {
        try {
            final List<URL> urls = new ArrayList<>();
            
            for (final Path webApp : Files.newDirectoryStream(idpHome.resolve("dist"), "edit-webapp-*")) {
                for (final Path jar : Files.newDirectoryStream(webApp.resolve("WEB-INF").resolve("lib"))) {
                    urls.add(jar.toUri().toURL());
                }
            }
            
           try (final URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new))){
               
               return ServiceLoader.load(PluginDescription.class, loader).
                   stream().
                   map(ServiceLoader.Provider::get).
                   collect(Collectors.toList());
           }
            
        } catch (final IOException e) {
            throw new BuildException(e);
        }
    }
    
}


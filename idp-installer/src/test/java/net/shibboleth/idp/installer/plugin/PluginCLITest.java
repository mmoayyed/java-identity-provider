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

import static org.testng.Assert.assertEquals;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.annotations.Test;

import net.shibboleth.ext.spring.cli.AbstractCommandLine;

@SuppressWarnings("javadoc")
public class PluginCLITest extends BasePluginTest {
    
    @Test(enabled = true) public void TestCli() throws IOException {
        System.setProperty("net.shibboleth.idp.cli.idp.home",getIdpHome().toString());
        final Resource pluginInstaller = new ClassPathResource("conf/admin/plugin-installer.xml");
        final File plugin = getIdpHome().resolve("conf").resolve("admin").resolve("plugin-installer.xml").toFile();
        plugin.createNewFile();
        
        try (final InputStream is = pluginInstaller.getInputStream();
             final BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(plugin))) {
            is.transferTo(os);
        }
    
        assertEquals(PluginInstallerCLI.runMain(new String[] { plugin.getAbsolutePath(), "--verbose"}), 
                AbstractCommandLine.RC_OK);	
    }
}

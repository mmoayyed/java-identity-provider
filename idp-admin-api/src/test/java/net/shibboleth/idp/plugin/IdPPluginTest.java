package net.shibboleth.idp.plugin;
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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.module.ModuleException;
import net.shibboleth.idp.module.TestModule;
import net.shibboleth.shared.collection.CollectionSupport;

/**
 * Unit tests exercising plugin code.
 */
@SuppressWarnings("javadoc")
public class IdPPluginTest {
    
    private IdPPlugin testPlugin;
    
    @BeforeMethod
    public void setUp() throws Exception {
        final ServiceLoader<IdPPlugin> loader = ServiceLoader.load(IdPPlugin.class);
        final Optional<Provider<IdPPlugin>> opt =
                loader.stream().filter(p -> TestPlugin.class.equals(p.type())).findFirst();
        Assert.assertTrue(opt.isPresent());
        
        testPlugin = opt.get().get();
    }
        
    @Test
    public void testModule() throws IOException, ModuleException {
        Assert.assertEquals(testPlugin.getPluginId(), "net.shibboleth.idp.plugin.TestPlugIn");
        
        final List<URL> urls = testPlugin.getUpdateURLs();
        Assert.assertEquals(urls.size(), 2);
        Assert.assertEquals(urls.get(0).toString(), "https://www.example.org/plugin");
        Assert.assertEquals(urls.get(1).toString(), "https://backup.example.org/plugin");
        
        final IdPModule test = new TestModule();
        Assert.assertEquals(testPlugin.getEnableOnInstall(), CollectionSupport.singleton(test));
        Assert.assertEquals(testPlugin.getDisableOnRemoval(), CollectionSupport.singleton(test));
    }

}
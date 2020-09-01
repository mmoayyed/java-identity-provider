package net.shibboleth.idp.module.impl;
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

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.module.IdPModule.ModuleResource;
import net.shibboleth.idp.module.ModuleException;

/**
 * Unit tests exercising module code.
 */
public class IdPModuleTest {

    @Test
    public void testModule() {
        
        final ServiceLoader<IdPModule> loader = ServiceLoader.load(IdPModule.class);
        final Optional<Provider<IdPModule>> opt =
                loader.stream().filter(p -> TestModule.class.equals(p.type())).findFirst();
        
        Assert.assertTrue(opt.isPresent());
        
        final IdPModule module = opt.get().get();
        
        Assert.assertEquals(module.getId(), TestModule.class.getName());
        Assert.assertEquals(module.getName(), "Test module");
        Assert.assertEquals(module.getURL().toString(), "https://wiki.shibboleth.net/confluence/display/IDP4/Home");
        
        final Iterator<ModuleResource> resources = module.getResources().iterator();
        Assert.assertEquals(module.getResources().size(), 2);
        
        ModuleResource resource = resources.next();
        Assert.assertEquals(resource.getSource(), "net/shibboleth/idp/module/impl/test.xml");
        Assert.assertEquals(resource.getDestination(), Path.of("conf/test.xml"));
        
        resource = resources.next();
        Assert.assertEquals(resource.getSource(), "net/shibboleth/idp/module/impl/test.vm");
        Assert.assertEquals(resource.getDestination(), Path.of("views/test.vm"));
    }

    @Test
    public void testBadModules() {
        
        final ServiceLoader<IdPModule> loader = ServiceLoader.load(IdPModule.class);

        Optional<Provider<IdPModule>> opt =
                loader.stream().filter(p -> BadModule.class.equals(p.type())).findFirst();
        Assert.assertTrue(opt.isPresent());
        try {
            opt.get().get();
            Assert.fail("BadModule should have failed");
        } catch (final ServiceConfigurationError e) {
            Assert.assertTrue(e.getCause() instanceof ModuleException);
        }

        opt = loader.stream().filter(p -> BadModule2.class.equals(p.type())).findFirst();
        Assert.assertTrue(opt.isPresent());
        try {
            opt.get().get();
            Assert.fail("BadModule2 should have failed");
        } catch (final ServiceConfigurationError e) {
            Assert.assertTrue(e.getCause() instanceof ModuleException);
        }
    }

}
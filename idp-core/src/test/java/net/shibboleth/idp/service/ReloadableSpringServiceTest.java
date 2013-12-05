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

package net.shibboleth.idp.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

import junit.framework.Assert;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.testng.annotations.Test;

import com.google.common.io.ByteStreams;

/**
 * Test to exercise the {@link ReloadableSpringService}.
 * 
 */
public class ReloadableSpringServiceTest {

    private static final long RELOAD_DELAY = 100;

    private File testFile;

    private void createPopulatedFile(String dataPath) throws IOException {
        testFile = File.createTempFile("ReloadableSpringServiceTest", "xml");
        overwriteFileWith(dataPath);
    }

    private Resource testFileResource() {
        return new FileSystemResource(testFile);
    }

    private void overwriteFileWith(String newDataPath) throws IOException {
        final OutputStream stream = new FileOutputStream(testFile);
        ByteStreams.copy(new ClassPathResource(newDataPath).getInputStream(), stream);
        stream.close();
    }

    @Test public void reloadableService() throws IOException, InterruptedException {
        final ReloadableSpringService<TestServiceableComponent> service =
                new ReloadableSpringService<>(TestServiceableComponent.class);

        createPopulatedFile("net/shibboleth/idp/service/ServiceableBean1.xml");

        service.setFailFast(true);
        service.setId("Id");
        service.setReloadCheckDelay(RELOAD_DELAY);
        service.setServiceConfigurations(Collections.singletonList(testFileResource()));

        service.start();

        ServiceableComponent<TestServiceableComponent> component = service.getServiceableComponent();

        Assert.assertEquals(component.getComponent().getTheValue(), "One");
        Assert.assertFalse(component.getComponent().isDestroyed());

        component.unpinComponent();
        overwriteFileWith("net/shibboleth/idp/service/ServiceableBean2.xml");

        Thread.sleep(RELOAD_DELAY * 2);

        //
        // The reload will have destroyed the old component
        //
        Assert.assertTrue(component.getComponent().isDestroyed());

        component = service.getServiceableComponent();

        Assert.assertEquals(component.getComponent().getTheValue(), "Two");
    }

    @Test public void deferedReload() throws IOException, InterruptedException {
        final ReloadableSpringService<TestServiceableComponent> service =
                new ReloadableSpringService<>(TestServiceableComponent.class);

        createPopulatedFile("net/shibboleth/idp/service/ServiceableBean1.xml");

        service.setFailFast(true);
        service.setId("Id");
        service.setReloadCheckDelay(RELOAD_DELAY);
        service.setServiceConfigurations(Collections.singletonList(testFileResource()));

        service.start();

        ServiceableComponent<TestServiceableComponent> serviceableComponent = service.getServiceableComponent();
        TestServiceableComponent component = serviceableComponent.getComponent();

        Assert.assertEquals(component.getTheValue(), "One");
        Assert.assertFalse(component.isDestroyed());

        overwriteFileWith("net/shibboleth/idp/service/ServiceableBean2.xml");

        Thread.sleep(RELOAD_DELAY*2);

        //
        // The reload will not have destroyed the old component yet
        //
        Assert.assertFalse(component.isDestroyed());
        component.unpinComponent();

        serviceableComponent = service.getServiceableComponent();

        Assert.assertEquals(serviceableComponent.getComponent().getTheValue(), "Two");

        long count = 50;
        while (count > 0 && !component.isDestroyed()) {
            Thread.sleep(100);
            count--;
        }
        Assert.assertTrue("After 5 second initial component has still not be destroyed", component.isDestroyed());
    }
    
    @Test
    public void testFailFast() throws IOException, InterruptedException {
        final ReloadableSpringService<TestServiceableComponent> service =
                new ReloadableSpringService<>(TestServiceableComponent.class);

        createPopulatedFile("net/shibboleth/idp/service/BrokenBean1.xml");

        service.setFailFast(true);
        service.setId("Id");
        service.setReloadCheckDelay(0);
        service.setServiceConfigurations(Collections.singletonList(testFileResource()));

        try {
            service.start();
        }
        catch (BeanInitializationException e) {
            // OK
        }
        Assert.assertNull(service.getServiceableComponent());
        
        overwriteFileWith("net/shibboleth/idp/service/ServiceableBean2.xml");

        Thread.sleep(RELOAD_DELAY*2);
        Assert.assertNull(service.getServiceableComponent());
    }
    
    @Test
    public void testNotFailFast() throws IOException, InterruptedException {
        final ReloadableSpringService<TestServiceableComponent> service =
                new ReloadableSpringService<>(TestServiceableComponent.class);

        createPopulatedFile("net/shibboleth/idp/service/BrokenBean1.xml");

        service.setFailFast(false);
        service.setId("Id");
        service.setReloadCheckDelay(RELOAD_DELAY);
        service.setServiceConfigurations(Collections.singletonList(testFileResource()));

        try {
            service.start();
        }
        catch (BeanInitializationException e) {
            // OK
        }
        Assert.assertNull(service.getServiceableComponent());
        
        overwriteFileWith("net/shibboleth/idp/service/ServiceableBean2.xml");

        Thread.sleep(RELOAD_DELAY*2);
        final ServiceableComponent<TestServiceableComponent> serviceableComponent = service.getServiceableComponent();
        final TestServiceableComponent component = serviceableComponent.getComponent();
        Assert.assertEquals(component.getTheValue(), "Two");
        
        Assert.assertFalse(component.isDestroyed());
        component.unpinComponent();
        service.stop();

        long count = 50;
        while (count > 0 && !component.isDestroyed()) {
            Thread.sleep(100);
            count--;
        }
        Assert.assertTrue("After 5 second initial component has still not be destroyed", component.isDestroyed());
        
        
    }

}

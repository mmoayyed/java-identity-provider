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
        testFile.setLastModified(1);
    }

    private Resource testFileResource() {
        return new FileSystemResource(testFile);
    }

    private void overwriteFileWith(String newDataPath) throws IOException {
        final OutputStream stream = new FileOutputStream(testFile);
        ByteStreams.copy(new ClassPathResource(newDataPath).getInputStream(), stream);
        stream.close();
    }

    @Test(enabled=true) public void reloadableService() throws IOException, InterruptedException {
        final ReloadableSpringService<TestServiceableComponent> service =
                new ReloadableSpringService<>(TestServiceableComponent.class);

        createPopulatedFile("net/shibboleth/idp/service/ServiceableBean1.xml");

        service.setFailFast(true);
        service.setId("reloadableService");
        service.setReloadCheckDelay(RELOAD_DELAY);
        service.setServiceConfigurations(Collections.singletonList(testFileResource()));

        service.start();

        ServiceableComponent<TestServiceableComponent> serviceableComponent = service.getServiceableComponent();
        TestServiceableComponent component = serviceableComponent.getComponent();

        Assert.assertEquals(component.getTheValue(), "One");
        Assert.assertFalse(component.getComponent().isDestroyed());

        serviceableComponent.unpinComponent();
        overwriteFileWith("net/shibboleth/idp/service/ServiceableBean2.xml");

        long count = 70;
        while (count > 0 && !component.isDestroyed()) {
            Thread.sleep(RELOAD_DELAY);
            count--;
        }
        Assert.assertTrue("After 7 second initial component has still not be destroyed", component.isDestroyed());

        //
        // The reload will have destroyed the old component
        //
        Assert.assertTrue(serviceableComponent.getComponent().isDestroyed());

        serviceableComponent = service.getServiceableComponent();

        Assert.assertEquals(serviceableComponent.getComponent().getTheValue(), "Two");
        serviceableComponent.unpinComponent();
        service.stop();
    }

    @Test(enabled=true) public void deferedReload() throws IOException, InterruptedException {
        final ReloadableSpringService<TestServiceableComponent> service =
                new ReloadableSpringService<>(TestServiceableComponent.class);

        createPopulatedFile("net/shibboleth/idp/service/ServiceableBean1.xml");

        service.setFailFast(true);
        service.setId("deferedReload");
        service.setReloadCheckDelay(RELOAD_DELAY);
        service.setServiceConfigurations(Collections.singletonList(testFileResource()));

        service.start();

        ServiceableComponent<TestServiceableComponent> serviceableComponent = service.getServiceableComponent();
        TestServiceableComponent component = serviceableComponent.getComponent();

        Assert.assertEquals("One", component.getTheValue());
        Assert.assertFalse(component.isDestroyed());

        overwriteFileWith("net/shibboleth/idp/service/ServiceableBean2.xml");

        //
        // The reload will not have destroyed the old component yet
        //
        Assert.assertFalse(component.isDestroyed());

        long count = 70;
        TestServiceableComponent component2 = null;
        while (count > 0) {
            serviceableComponent = service.getServiceableComponent();
            component2 = serviceableComponent.getComponent();
            if ("Two".equals(component2.getTheValue())) {
                component2.unpinComponent();
                break;
            }
            component2.unpinComponent();
            component2 = null;
            Thread.sleep(RELOAD_DELAY);
            count--;
        }
        Assert.assertNotNull("After 7 second initial component has still not got new value", component2);

        component.unpinComponent();

        count = 70;
        while (count > 0 && !component.isDestroyed()) {
            Thread.sleep(RELOAD_DELAY);
            count--;
        }
        Assert.assertTrue("After 7 second initial component has still not be destroyed", component.isDestroyed());

        service.stop();
    }

    @Test public void testFailFast() throws IOException, InterruptedException {
        final ReloadableSpringService<TestServiceableComponent> service =
                new ReloadableSpringService<>(TestServiceableComponent.class);

        createPopulatedFile("net/shibboleth/idp/service/BrokenBean1.xml");

        service.setFailFast(true);
        service.setId("testFailFast");
        service.setReloadCheckDelay(0);
        service.setServiceConfigurations(Collections.singletonList(testFileResource()));

        try {
            service.start();
            Assert.fail("Expected to fail");
        } catch (BeanInitializationException e) {
            // OK
        }
        Assert.assertNull(service.getServiceableComponent());

        overwriteFileWith("net/shibboleth/idp/service/ServiceableBean2.xml");

        Thread.sleep(RELOAD_DELAY * 2);
        Assert.assertNull(service.getServiceableComponent());

        service.stop();
    }

    @Test public void testNotFailFast() throws IOException, InterruptedException {
        final ReloadableSpringService<TestServiceableComponent> service =
                new ReloadableSpringService<>(TestServiceableComponent.class);

        createPopulatedFile("net/shibboleth/idp/service/BrokenBean1.xml");

        service.setFailFast(false);
        service.setId("testNotFailFast");
        service.setReloadCheckDelay(RELOAD_DELAY);
        service.setServiceConfigurations(Collections.singletonList(testFileResource()));

        service.start();
        Assert.assertNull(service.getServiceableComponent());

        overwriteFileWith("net/shibboleth/idp/service/ServiceableBean2.xml");

        long count = 700;
        ServiceableComponent<TestServiceableComponent> serviceableComponent = service.getServiceableComponent();
        while (count > 0 && null == serviceableComponent) {
            Thread.sleep(RELOAD_DELAY);
            count--;
            serviceableComponent = service.getServiceableComponent();
        }
        Assert.assertNotNull("After 7 second component has still no initialized", serviceableComponent);
        final TestServiceableComponent component = serviceableComponent.getComponent();
        Assert.assertEquals("Two", component.getTheValue());

        Assert.assertFalse(component.isDestroyed());
        component.unpinComponent();
        service.stop();

        count = 70;
        while (count > 0 && !component.isDestroyed()) {
            Thread.sleep(RELOAD_DELAY);
            count--;
        }
        Assert.assertTrue("After 7 second component has still not be destroyed", component.isDestroyed());

    }

}

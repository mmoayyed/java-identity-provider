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

package net.shibboleth.idp.test.flows.paths;

import javax.servlet.ServletContextEvent;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.spring.IdPPropertiesApplicationContextInitializer;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

/**
 * TODO
 */
public class IdPPropertiesApplicationContextInitializerTest {

    @Test(expectedExceptions = BeanDefinitionStoreException.class) public void testNoInitializer() {
        final MockServletContext sc = new MockServletContext("");
        sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "classpath:/system/conf/global-system.xml");
        final ContextLoaderListener listener = new ContextLoaderListener();
        listener.contextInitialized(new ServletContextEvent(sc));
        WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testNotFound() {
        final MockServletContext sc = new MockServletContext("");
        sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "classpath:/system/conf/global-system.xml");
        sc.addInitParameter(ContextLoader.CONTEXT_INITIALIZER_CLASSES_PARAM,
                IdPPropertiesApplicationContextInitializer.class.getName());
        final ContextLoaderListener listener = new ContextLoaderListener();
        listener.contextInitialized(new ServletContextEvent(sc));
        WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testUserDefinedNotFound() {
        final MockServletContext sc = new MockServletContext("");
        sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "classpath:/system/conf/global-system.xml");
        sc.addInitParameter(ContextLoader.CONTEXT_INITIALIZER_CLASSES_PARAM,
                IdPPropertiesApplicationContextInitializer.class.getName());
        sc.addInitParameter("idp.home", "foo");
        final ContextLoaderListener listener = new ContextLoaderListener();
        listener.contextInitialized(new ServletContextEvent(sc));
        final WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
        Assert.assertTrue(wac.getEnvironment().containsProperty("idp.home"));
    }

    @Test public void testUserDefinedFound() {
        final MockServletContext sc = new MockServletContext("");
        sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "classpath:/system/conf/global-system.xml");
        sc.addInitParameter(ContextLoader.CONTEXT_INITIALIZER_CLASSES_PARAM,
                IdPPropertiesApplicationContextInitializer.class.getName());
        sc.addInitParameter("idp.home", "classpath:");
        final ContextLoaderListener listener = new ContextLoaderListener();
        listener.contextInitialized(new ServletContextEvent(sc));
        final WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
        Assert.assertTrue(wac.getEnvironment().containsProperty("idp.home"));
        Assert.assertEquals(wac.getEnvironment().getProperty("idp.home"), "classpath:");
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testUserDefinedEndsWithSlash() {
        final MockServletContext sc = new MockServletContext("");
        sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "classpath:/system/conf/global-system.xml");
        sc.addInitParameter(ContextLoader.CONTEXT_INITIALIZER_CLASSES_PARAM,
                IdPPropertiesApplicationContextInitializer.class.getName());
        sc.addInitParameter("idp.home", "/ends-with-slash/");
        final ContextLoaderListener listener = new ContextLoaderListener();
        listener.contextInitialized(new ServletContextEvent(sc));
        WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
    }

    // TODO test w/ false fail fast property
    
    // TODO
}

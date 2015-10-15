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

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.spring.IdPPropertiesApplicationContextInitializer;
import net.shibboleth.idp.test.PreferFileSystemApplicationContextInitializer;
import net.shibboleth.idp.test.PreferFileSystemContextLoader;

/**
 * Test flow IDs registered using the flow-registry base-path and flow-location-pattern.
 * 
 * Subclasses should set the idp.home property using the TestPropertySources annotation.
 */
@ContextConfiguration(
        locations = {
                "/system/conf/global-system.xml",
                "/system/conf/mvc-beans.xml",
                "/system/conf/webflow-config.xml",
                "/test/test-beans.xml",
                "/test/override-beans.xml",
                },
        initializers = {
                PreferFileSystemApplicationContextInitializer.class,
                IdPPropertiesApplicationContextInitializer.class,
                },
        loader = PreferFileSystemContextLoader.class)
@WebAppConfiguration
public abstract class AbstractIdPHomeTest extends AbstractTestNGSpringContextTests {

    // TODO IDP-812 Temporary disabled
    @Test(enabled = false) public void testAuthnConditionsFlowExists() {

        final FlowDefinitionRegistry registry = applicationContext.getBean(FlowDefinitionRegistry.class);

        Assert.assertTrue(registry.containsFlowDefinition("authn/conditions"), "Flow 'authn/conditions' not found.");
    }

}

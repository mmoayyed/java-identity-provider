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

package net.shibboleth.idp.profile.config.tests;

import net.shibboleth.idp.profile.config.AbstractProfileConfiguration;
import net.shibboleth.idp.profile.testing.MockProfileConfiguration;
import net.shibboleth.shared.logic.ConstraintViolationException;
import net.shibboleth.shared.logic.FunctionSupport;

import org.opensaml.security.config.BasicSecurityConfiguration;
import org.opensaml.security.config.SecurityConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/** Unit test for {@link AbstractProfileConfiguration}. */
public class AbstractProfileConfigurationTest {

    @Test
    public void testProfileId() {
        MockProfileConfiguration config = new MockProfileConfiguration("mock");
        Assert.assertEquals(config.getId(), "mock");

        try {
            config = new MockProfileConfiguration(null);
            Assert.fail();
        } catch (final ConstraintViolationException e) {

        }

        try {
            config = new MockProfileConfiguration("");
            Assert.fail();
        } catch (final ConstraintViolationException e) {

        }
    }

    @Test
    public void testSecurityConfiguration() {
        final MockProfileConfiguration config = new MockProfileConfiguration("mock");
        Assert.assertNotNull(config.getSecurityConfiguration(null));
        
        SecurityConfiguration securityConfig = new BasicSecurityConfiguration();
        config.setSecurityConfiguration(securityConfig);
        Assert.assertSame(config.getSecurityConfiguration(null), securityConfig);
    }

    @Test
    public void testIndirectSecurityConfiguration() {
        final MockProfileConfiguration config = new MockProfileConfiguration("mock");
        config.setSecurityConfiguration(null);
        final SecurityConfiguration securityConfig = new BasicSecurityConfiguration();
        config.setSecurityConfigurationLookupStrategy(FunctionSupport.constant(securityConfig));
        Assert.assertSame(config.getSecurityConfiguration(null), securityConfig);
    }

    @Test
    public void testInboundFlows() {
        final MockProfileConfiguration config = new MockProfileConfiguration("mock");
        final List<String> flows = Arrays.asList("foo", "bar");
        config.setInboundInterceptorFlows(flows);
        Assert.assertEquals(config.getInboundInterceptorFlows(null), flows);
    }

    @Test
    public void testIndirectInboundFlows() {
        final MockProfileConfiguration config = new MockProfileConfiguration("mock");
        final List<String> flows = Arrays.asList("foo", "bar");
        config.setInboundInterceptorFlowsLookupStrategy(FunctionSupport.constant(flows));
        Assert.assertEquals(config.getInboundInterceptorFlows(null), flows);
    }

    @Test
    public void testOutboundFlows() {
        final MockProfileConfiguration config = new MockProfileConfiguration("mock");
        final List<String> flows = Arrays.asList("foo", "bar");
        config.setOutboundInterceptorFlows(flows);
        Assert.assertEquals(config.getOutboundInterceptorFlows(null), flows);
    }

    @Test
    public void testIndirectOutboundFlows() {
        final MockProfileConfiguration config = new MockProfileConfiguration("mock");
        final List<String> flows = Arrays.asList("foo", "bar");
        config.setOutboundInterceptorFlowsLookupStrategy(FunctionSupport.constant(flows));
        Assert.assertEquals(config.getOutboundInterceptorFlows(null), flows);
    }

    @Test
    public void testDisallowedFeatures() {
        final MockProfileConfiguration config = new MockProfileConfiguration("mock");
        Assert.assertEquals(config.getDisallowedFeatures(null), 0);

        config.setDisallowedFeatures(0x1 | 0x4);
        Assert.assertTrue(config.isFeatureDisallowed(null, 0x1));
        Assert.assertFalse(config.isFeatureDisallowed(null, 0x2));
        Assert.assertTrue(config.isFeatureDisallowed(null, 0x4));
    }

    @Test
    public void testIndirectDisallowedFeatures() {
        final MockProfileConfiguration config = new MockProfileConfiguration("mock");
        Assert.assertEquals(config.getDisallowedFeatures(null), 0);

        config.setDisallowedFeaturesLookupStrategy(FunctionSupport.constant(0x1 | 0x4));
        Assert.assertTrue(config.isFeatureDisallowed(null, 0x1));
        Assert.assertFalse(config.isFeatureDisallowed(null, 0x2));
        Assert.assertTrue(config.isFeatureDisallowed(null, 0x4));
    }

}
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

package net.shibboleth.idp.profile.config;

import net.shibboleth.idp.relyingparty.MockProfileConfiguration;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
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
        Assert.assertNotNull(config.getSecurityConfiguration());
        
        SecurityConfiguration securityConfig = new SecurityConfiguration();
        config.setSecurityConfiguration(securityConfig);
        Assert.assertSame(config.getSecurityConfiguration(), securityConfig);
    }

    @Test
    public void testIndirectSecurityConfiguration() {
        final MockProfileConfiguration config = new MockProfileConfiguration("mock");
        config.setSecurityConfiguration(null);
        final SecurityConfiguration securityConfig = new SecurityConfiguration();
        config.setSecurityConfigurationLookupStrategy(
                FunctionSupport.<ProfileRequestContext,SecurityConfiguration>constant(securityConfig));
        Assert.assertSame(config.getSecurityConfiguration(), securityConfig);
    }

    @Test
    public void testInboundFlows() {
        final MockProfileConfiguration config = new MockProfileConfiguration("mock");
        final List<String> flows = Arrays.asList("foo", "bar");
        config.setInboundInterceptorFlows(flows);
        Assert.assertEquals(config.getInboundInterceptorFlows(), flows);
    }

    @Test
    public void testIndirectInboundFlows() {
        final MockProfileConfiguration config = new MockProfileConfiguration("mock");
        final List<String> flows = Arrays.asList("foo", "bar");
        config.setInboundFlowsLookupStrategy(FunctionSupport.<ProfileRequestContext,List<String>>constant(flows));
        Assert.assertEquals(config.getInboundInterceptorFlows(), flows);
    }

    @Test
    public void testOutboundFlows() {
        final MockProfileConfiguration config = new MockProfileConfiguration("mock");
        final List<String> flows = Arrays.asList("foo", "bar");
        config.setOutboundInterceptorFlows(flows);
        Assert.assertEquals(config.getOutboundInterceptorFlows(), flows);
    }

    @Test
    public void testIndirectOutboundFlows() {
        final MockProfileConfiguration config = new MockProfileConfiguration("mock");
        final List<String> flows = Arrays.asList("foo", "bar");
        config.setOutboundFlowsLookupStrategy(FunctionSupport.<ProfileRequestContext,List<String>>constant(flows));
        Assert.assertEquals(config.getOutboundInterceptorFlows(), flows);
    }
}
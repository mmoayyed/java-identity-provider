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

package net.shibboleth.idp.relyingparty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Predicates;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link RelyingPartyConfiguration}. */
public class RelyingPartyConfigurationTest {

    @SuppressWarnings("deprecation")
    @Test public void testConstruction() throws ComponentInitializationException {
        RelyingPartyConfiguration config = new RelyingPartyConfiguration();
        config.setId("foo");
        config.setResponderId("http://idp.example.org");
        config.setDetailedErrorsPredicate(Predicates.<ProfileRequestContext>alwaysTrue());
        config.initialize();
        Assert.assertEquals(config.getId(), "foo");
        Assert.assertEquals(config.getResponderId(), "http://idp.example.org");
        Assert.assertTrue(config.isDetailedErrors());
        Assert.assertTrue(config.getDetailedErrorsPredicate().apply(null));
        Assert.assertTrue(config.getProfileConfigurations().isEmpty());

        config = new RelyingPartyConfiguration();
        config.setId("foo");
        config.setResponderId("http://idp.example.org");
        config.setDetailedErrorsPredicate(Predicates.<ProfileRequestContext>alwaysFalse());
        config.initialize();
        Assert.assertEquals(config.getId(), "foo");
        Assert.assertEquals(config.getResponderId(), "http://idp.example.org");
        Assert.assertFalse(config.isDetailedErrors());
        Assert.assertFalse(config.getDetailedErrorsPredicate().apply(null));
        Assert.assertTrue(config.getProfileConfigurations().isEmpty());

        ArrayList<ProfileConfiguration> profileConfigs = new ArrayList<>();
        profileConfigs.add(new MockProfileConfiguration("foo"));
        profileConfigs.add(null);
        profileConfigs.add(new MockProfileConfiguration("bar"));

        config = new RelyingPartyConfiguration();
        config.setId("foo");
        config.setResponderId("http://idp.example.org");
        config.setProfileConfigurations(profileConfigs);
        config.initialize();
        Assert.assertEquals(config.getId(), "foo");
        Assert.assertEquals(config.getResponderId(), "http://idp.example.org");
        Assert.assertEquals(config.getProfileConfigurations().size(), 2);

        try {
            config = new RelyingPartyConfiguration();
            config.initialize();
            Assert.fail();
        } catch (final ComponentInitializationException e) {
            // expected this
        }

        try {
            config = new RelyingPartyConfiguration();
            config.setId("");
            config.initialize();
            Assert.fail();
        } catch (final ConstraintViolationException e) {
            // expected this
        }
    }

    @Test public void testProfileConfiguration() throws ComponentInitializationException {
        final ArrayList<ProfileConfiguration> profileConfigs = new ArrayList<>();
        profileConfigs.add(new MockProfileConfiguration("foo"));
        profileConfigs.add(new MockProfileConfiguration("bar"));

        final RelyingPartyConfiguration config = new RelyingPartyConfiguration();
        config.setId("foo");
        config.setResponderId("http://idp.example.org");
        config.setProfileConfigurations(profileConfigs);
        config.initialize();
        
        Assert.assertNotNull(config.getProfileConfiguration("foo"));
        Assert.assertNotNull(config.getProfileConfiguration("bar"));
        Assert.assertNull(config.getProfileConfiguration("baz"));
    }

    @Test public void testIndirectProfileConfiguration() throws ComponentInitializationException {
        final Map<String,ProfileConfiguration> profileConfigs = new HashMap<>();
        profileConfigs.put("foo", new MockProfileConfiguration("foo"));
        profileConfigs.put("bar", new MockProfileConfiguration("bar"));

        RelyingPartyConfiguration config = new RelyingPartyConfiguration();
        config.setId("foo");
        config.setResponderId("http://idp.example.org");
        config.setProfileConfigurationsLookupStrategy(
                FunctionSupport.<ProfileRequestContext,Map<String,ProfileConfiguration>>constant(profileConfigs));
        config.initialize();
        
        Assert.assertNotNull(config.getProfileConfiguration("foo"));
        Assert.assertNotNull(config.getProfileConfiguration("bar"));
        Assert.assertNull(config.getProfileConfiguration("baz"));
        
        config = new RelyingPartyConfiguration();
        config.setId("foo");
        config.setResponderId("http://idp.example.org");
        config.setProfileConfigurations(profileConfigs.values());
        config.setProfileConfigurationsLookupStrategy(
                FunctionSupport.<ProfileRequestContext,Map<String,ProfileConfiguration>>constant(null));
        config.initialize();
        
        Assert.assertNotNull(config.getProfileConfiguration("foo"));
        Assert.assertNotNull(config.getProfileConfiguration("bar"));
        Assert.assertNull(config.getProfileConfiguration("baz"));
    }

    @Test public void testIndirectResponderId() throws ComponentInitializationException {
        RelyingPartyConfiguration config = new RelyingPartyConfiguration();
        config.setId("foo");
        config.setResponderIdLookupStrategy(FunctionSupport.<ProfileRequestContext,String>constant("http://idp.example.org"));
        config.initialize();
        Assert.assertEquals(config.getResponderId(), "http://idp.example.org");

        config = new RelyingPartyConfiguration();
        config.setId("foo");
        config.setResponderId("http://idp.example.org");
        config.setResponderIdLookupStrategy(FunctionSupport.<ProfileRequestContext,String>constant(null));
        config.initialize();
        Assert.assertEquals(config.getResponderId(), "http://idp.example.org");
    }

}
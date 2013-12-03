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

package net.shibboleth.idp.relyingparty.impl;

import java.util.ArrayList;
import java.util.Iterator;

import org.opensaml.profile.context.ProfileRequestContext;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

/** Unit test for {@link ConditionalRelyingPartyConfigurationResolver}. */
public class ConditionalRelyingPartyConfigurationResolverTest {

    @Test public void testConstruction() {
        ConditionalRelyingPartyConfigurationResolver resolver;

        ArrayList<ConditionalRelyingPartyConfiguration> rpConfigs = new ArrayList<>();
        rpConfigs.add(new ConditionalRelyingPartyConfiguration("one", "foo", null, Predicates
                .<ProfileRequestContext> alwaysTrue()));
        rpConfigs.add(new ConditionalRelyingPartyConfiguration("two", "foo", null, Predicates
                .<ProfileRequestContext> alwaysFalse()));
        rpConfigs.add(new ConditionalRelyingPartyConfiguration("three", "foo", null, Predicates
                .<ProfileRequestContext> alwaysTrue()));

        resolver = new ConditionalRelyingPartyConfigurationResolver();
        resolver.setId("test");
        resolver.setRelyingPartyConfigurations(rpConfigs);
        Assert.assertEquals(resolver.getId(), "test");
        Assert.assertEquals(resolver.getRelyingPartyConfigurations().size(), 3);

        resolver = new ConditionalRelyingPartyConfigurationResolver();
        resolver.setId("test");
        Assert.assertEquals(resolver.getId(), "test");
        Assert.assertEquals(resolver.getRelyingPartyConfigurations().size(), 0);

        resolver = new ConditionalRelyingPartyConfigurationResolver();
        resolver.setId("test");
        Assert.assertEquals(resolver.getId(), "test");
        Assert.assertEquals(resolver.getRelyingPartyConfigurations().size(), 0);
    }

    @Test public void testResolve() throws Exception {
        ProfileRequestContext requestContext = new ProfileRequestContext();

        ConditionalRelyingPartyConfiguration config1 =
                new ConditionalRelyingPartyConfiguration("one", "foo", null,
                        Predicates.<ProfileRequestContext> alwaysTrue());
        ConditionalRelyingPartyConfiguration config2 =
                new ConditionalRelyingPartyConfiguration("two", "foo", null,
                        Predicates.<ProfileRequestContext> alwaysFalse());
        ConditionalRelyingPartyConfiguration config3 =
                new ConditionalRelyingPartyConfiguration("three", "foo", null,
                        Predicates.<ProfileRequestContext> alwaysTrue());

        ArrayList<ConditionalRelyingPartyConfiguration> rpConfigs = new ArrayList<>();
        rpConfigs.add(config1);
        rpConfigs.add(config2);
        rpConfigs.add(config3);

        ConditionalRelyingPartyConfigurationResolver resolver = new ConditionalRelyingPartyConfigurationResolver();
        resolver.setId("test");
        resolver.setRelyingPartyConfigurations(rpConfigs);
        resolver.initialize();

        Iterable<ConditionalRelyingPartyConfiguration> results = resolver.resolve(requestContext);
        Assert.assertNotNull(results);

        Iterator<ConditionalRelyingPartyConfiguration> resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), config1);
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), config3);
        Assert.assertFalse(resultItr.hasNext());

        ConditionalRelyingPartyConfiguration result = resolver.resolveSingle(requestContext);
        Assert.assertSame(result, config1);

        results = resolver.resolve(null);
        Assert.assertNotNull(results);

        resultItr = results.iterator();
        Assert.assertFalse(resultItr.hasNext());

        result = resolver.resolveSingle(null);
        Assert.assertNull(result);
    }
}
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

import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

/** Unit test for {@link RelyingPartyConfigurationResolver}. */
public class ActivatedRelyingPartyConfigurationResolverTest {

    @Test public void testConstruction() {
        ActivatedRelyingPartyConfigurationResolver resolver;

        ArrayList<ActivatedRelyingPartyConfiguration> rpConfigs = new ArrayList<ActivatedRelyingPartyConfiguration>();
        rpConfigs.add(new ActivatedRelyingPartyConfiguration("one", "foo", null, Predicates
                .<ProfileRequestContext> alwaysTrue()));
        rpConfigs.add(new ActivatedRelyingPartyConfiguration("two", "foo", null, Predicates
                .<ProfileRequestContext> alwaysFalse()));
        rpConfigs.add(new ActivatedRelyingPartyConfiguration("three", "foo", null, Predicates
                .<ProfileRequestContext> alwaysTrue()));

        resolver = new ActivatedRelyingPartyConfigurationResolver();
        resolver.setId("test");
        resolver.setRelyingPartyConfigurations(rpConfigs);
        Assert.assertEquals(resolver.getId(), "test");
        Assert.assertEquals(resolver.getRelyingPartyConfigurations().size(), 3);

        resolver = new ActivatedRelyingPartyConfigurationResolver();
        resolver.setId("test");
        Assert.assertEquals(resolver.getId(), "test");
        Assert.assertEquals(resolver.getRelyingPartyConfigurations().size(), 0);

        resolver = new ActivatedRelyingPartyConfigurationResolver();
        resolver.setId("test");
        Assert.assertEquals(resolver.getId(), "test");
        Assert.assertEquals(resolver.getRelyingPartyConfigurations().size(), 0);
    }

    @Test public void testResolve() throws Exception {
        ProfileRequestContext requestContext = new ProfileRequestContext();

        ActivatedRelyingPartyConfiguration config1 =
                new ActivatedRelyingPartyConfiguration("one", "foo", null,
                        Predicates.<ProfileRequestContext> alwaysTrue());
        ActivatedRelyingPartyConfiguration config2 =
                new ActivatedRelyingPartyConfiguration("two", "foo", null,
                        Predicates.<ProfileRequestContext> alwaysFalse());
        ActivatedRelyingPartyConfiguration config3 =
                new ActivatedRelyingPartyConfiguration("three", "foo", null,
                        Predicates.<ProfileRequestContext> alwaysTrue());

        ArrayList<ActivatedRelyingPartyConfiguration> rpConfigs = new ArrayList<ActivatedRelyingPartyConfiguration>();
        rpConfigs.add(config1);
        rpConfigs.add(config2);
        rpConfigs.add(config3);

        ActivatedRelyingPartyConfigurationResolver resolver = new ActivatedRelyingPartyConfigurationResolver();
        resolver.setId("test");
        resolver.setRelyingPartyConfigurations(rpConfigs);

        Iterable<ActivatedRelyingPartyConfiguration> results = resolver.resolve(requestContext);
        Assert.assertNotNull(results);

        Iterator<ActivatedRelyingPartyConfiguration> resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), config1);
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), config3);
        Assert.assertFalse(resultItr.hasNext());

        ActivatedRelyingPartyConfiguration result = resolver.resolveSingle(requestContext);
        Assert.assertSame(result, config1);

        results = resolver.resolve(null);
        Assert.assertNotNull(results);

        resultItr = results.iterator();
        Assert.assertFalse(resultItr.hasNext());

        result = resolver.resolveSingle(null);
        Assert.assertNull(result);
    }
}
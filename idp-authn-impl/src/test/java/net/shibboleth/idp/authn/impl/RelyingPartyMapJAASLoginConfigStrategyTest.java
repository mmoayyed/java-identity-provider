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

package net.shibboleth.idp.authn.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.logic.ConstraintViolationException;

@SuppressWarnings("javadoc")
public class RelyingPartyMapJAASLoginConfigStrategyTest {

    private static final String DEFAULT_JAAS_CONFIG = "ShibUserPassAuth";

    private RelyingPartyMapJAASLoginConfigStrategy strategy;

    private ProfileRequestContext profileRequestContext;

    private RelyingPartyContext relyingPartyContext;

    private String entityID = "https://www.example.org/saml";

    private HashMap<String, String> rpMap;
    
    private Object nullObj;

    @BeforeMethod
    public void setUp() {
        profileRequestContext = new ProfileRequestContext();

        relyingPartyContext = profileRequestContext.ensureSubcontext(RelyingPartyContext.class);
        relyingPartyContext.setRelyingPartyId(entityID);

        rpMap = new HashMap<>();
        // Deliberately inserting some whitespace here to test trimming of result returned.
        rpMap.put(entityID, "  MyJAAS   ");
    }

    @Test
    public void testNoRelyingPartyContext() {
        profileRequestContext.removeSubcontext(RelyingPartyContext.class);

        assert rpMap != null;
        strategy = new RelyingPartyMapJAASLoginConfigStrategy(rpMap);

        Collection<Pair<String,Subject>> result = strategy.apply(profileRequestContext);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        Pair<String,Subject> resultPair = result.iterator().next();
        Assert.assertNull(resultPair.getSecond());
        Assert.assertEquals(resultPair.getFirst(), DEFAULT_JAAS_CONFIG);
    }

    @Test
    public void testNoRelyingPartyId() {
        relyingPartyContext.setRelyingPartyId(null);

        assert rpMap != null;
        strategy = new RelyingPartyMapJAASLoginConfigStrategy(rpMap);

        Collection<Pair<String,Subject>> result = strategy.apply(profileRequestContext);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        Pair<String,Subject> resultPair = result.iterator().next();
        Assert.assertNull(resultPair.getSecond());
        Assert.assertEquals(resultPair.getFirst(), DEFAULT_JAAS_CONFIG);
    }

    @Test
    public void testNoMappingFound() {
        relyingPartyContext.setRelyingPartyId("SomeOtherRP");

        assert rpMap != null;
        strategy = new RelyingPartyMapJAASLoginConfigStrategy(rpMap);

        Collection<Pair<String,Subject>> result = strategy.apply(profileRequestContext);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        Pair<String,Subject> resultPair = result.iterator().next();
        Assert.assertNull(resultPair.getSecond());
        Assert.assertEquals(resultPair.getFirst(), DEFAULT_JAAS_CONFIG);
    }

    @Test
    public void testMappingFound() {
        assert rpMap != null;
        strategy = new RelyingPartyMapJAASLoginConfigStrategy(rpMap);

        Collection<Pair<String,Subject>> result = strategy.apply(profileRequestContext);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        Pair<String,Subject> resultPair = result.iterator().next();
        Assert.assertNull(resultPair.getSecond());
        Assert.assertEquals(resultPair.getFirst(), "MyJAAS");
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Test(expectedExceptions=ConstraintViolationException.class)
    public void testNullInputMap() {
        strategy = new RelyingPartyMapJAASLoginConfigStrategy((Map<String, String>) nullObj);
    }

}

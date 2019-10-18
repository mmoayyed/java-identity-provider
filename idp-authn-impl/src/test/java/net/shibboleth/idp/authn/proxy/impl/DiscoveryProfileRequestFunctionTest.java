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

package net.shibboleth.idp.authn.proxy.impl;

import net.shibboleth.idp.authn.impl.BaseAuthenticationContextTest;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import net.shibboleth.utilities.java.support.net.URISupport;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.webflow.test.MockFlowExecutionContext;
import org.springframework.webflow.test.MockFlowExecutionKey;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link DiscoveryProfileRequestFunction} unit test. */
public class DiscoveryProfileRequestFunctionTest extends BaseAuthenticationContextTest {
    
    private DiscoveryProfileRequestFunction function; 
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        ((MockFlowExecutionContext) ((MockRequestContext) src).getFlowExecutionContext()).setKey(new MockFlowExecutionKey("flowkey"));
        
        function = new DiscoveryProfileRequestFunction();
        function.setDiscoveryURLLookupStrategy(FunctionSupport.constant("https://ds.example.org/DS"));
        function.initialize();
    }
    
    @Test public void test() throws Exception {
        final URL url = new URL(function.apply(new Pair<>(src, prc)));
        
        Assert.assertEquals(url.getProtocol(), "https");
        Assert.assertEquals(url.getHost(), "ds.example.org");
        Assert.assertEquals(url.getPort(), -1);
        Assert.assertEquals(url.getPath(), "/DS");
        
        final List<Pair<String,String>> params = URISupport.parseQueryString(url.getQuery());
        Assert.assertEquals(params.size(), 2);
        
        final Map<String,String> map = new HashMap<>();
        params.forEach((p) -> map.put(p.getFirst(), p.getSecond()));
        
        Assert.assertEquals(map.get("entityID"), ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        Assert.assertEquals(map.get("return"), "http://localhost?execution=flowkey&_eventId_proceed=1");
    }

}
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

package net.shibboleth.idp.profile.logic;

import java.util.Map;

import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit test for {@link LoopDetectionPredicate}. */
public class LoopDetectionPredicateTest extends OpenSAMLInitBaseTestCase {

    private ProfileRequestContext prc;
    private RelyingPartyContext rpCtx;
    private LoopDetectionPredicate pred;
    
    @BeforeMethod
    public void setUp() {
        prc = new ProfileRequestContext();
        rpCtx = prc.getSubcontext(RelyingPartyContext.class, true);
        pred = new LoopDetectionPredicate();
        pred.setUsernameLookupStrategy(FunctionSupport.constant("jdoe.1"));
    }
    
    @Test
    public void testNoMap() {
        Assert.assertFalse(pred.test(prc));
        
        rpCtx.setRelyingPartyId("foo");
        Assert.assertFalse(pred.test(prc));
    }

    @Test
    public void testNoMatch() {
        pred.setRelyingPartyMap(Map.of("bar", "bar"));
        
        rpCtx.setRelyingPartyId("foo");
        Assert.assertFalse(pred.test(prc));
    }

    @Test
    public void testMatch() {
        pred.setRelyingPartyMap(Map.of("foo", "foo"));
        
        rpCtx.setRelyingPartyId("foo");
        Assert.assertFalse(pred.test(prc));
    }

    @Test
    public void testExceed() throws InterruptedException {
        pred.setRelyingPartyMap(Map.of("bar", "bar"));
        
        rpCtx.setRelyingPartyId("bar");
        for (int i=0; i<20; ++i) {
            Assert.assertFalse(pred.test(prc));
        }
        Assert.assertTrue(pred.test(prc));
    }

}
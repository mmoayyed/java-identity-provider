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

import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit test for {@link RelyingPartyIdPredicate}. */
@SuppressWarnings("javadoc")
public class RelyingPartyIdPredicateTest {

    private ProfileRequestContext prc;
    
    private RelyingPartyContext rpCtx;
    
    @BeforeMethod
    public void setUp() {
        prc = new ProfileRequestContext();
        rpCtx = prc.getSubcontext(RelyingPartyContext.class, true);
    }
    
    @Test
    public void testNone() throws ComponentInitializationException {
        final RelyingPartyIdPredicate pred = new RelyingPartyIdPredicate(CollectionSupport.emptySet());
        
        Assert.assertFalse(pred.test(prc));
        
        rpCtx.setRelyingPartyId("foo");
        Assert.assertFalse(pred.test(prc));
    }

    @Test
    public void testMatch() throws ComponentInitializationException {
        final RelyingPartyIdPredicate pred = new RelyingPartyIdPredicate(CollectionSupport.singleton("foo"));
        
        Assert.assertFalse(pred.test(prc));
        
        rpCtx.setRelyingPartyId("foo");
        Assert.assertTrue(pred.test(prc));
    }

    @Test
    public void testNoMatch() throws ComponentInitializationException {
        final RelyingPartyIdPredicate pred = new RelyingPartyIdPredicate(CollectionSupport.singleton("bar"));
        
        Assert.assertFalse(pred.test(prc));
        
        rpCtx.setRelyingPartyId("foo");
        Assert.assertFalse(pred.test(prc));
    }

}
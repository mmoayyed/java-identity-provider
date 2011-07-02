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

package net.shibboleth.idp.saml.relyingparty.idwsf;

import java.util.ArrayList;

import net.shibboleth.idp.saml.relyingparty.idwsf.SsosProfileConfiguration;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link SsosProfileConfiguration}. */
public class SsosProfileConfigurationTest {

    @Test
    public void testProfileId() {
        Assert.assertEquals(SsosProfileConfiguration.PROFILE_ID, "http://shibboleth.net/ns/profiles/liberty/ssos");

        SsosProfileConfiguration config = new SsosProfileConfiguration();
        Assert.assertEquals(config.getProfileId(), SsosProfileConfiguration.PROFILE_ID);
    }
    
    @Test
    public void testMaximumTokenDelegationChainLength(){
        SsosProfileConfiguration config = new SsosProfileConfiguration();
        Assert.assertEquals(config.getMaximumTokenDelegationChainLength(), 0);
        
        config.setMaximumTokenDelegationChainLength(10);
        Assert.assertEquals(config.getMaximumTokenDelegationChainLength(), 10);
    }
    
    @Test
    public void testAllowedDelegates(){
        SsosProfileConfiguration config = new SsosProfileConfiguration();
        Assert.assertNotNull(config.getAllowedDelegates());
        Assert.assertTrue(config.getAllowedDelegates().isEmpty());

        config.setAllowedDelegates(null);
        Assert.assertNotNull(config.getAllowedDelegates());
        Assert.assertTrue(config.getAllowedDelegates().isEmpty());

        ArrayList<String> delegates = new ArrayList<String>();
        delegates.add("foo");
        delegates.add(null);
        delegates.add("");
        delegates.add("foo");
        delegates.add("bar");

        config.setAllowedDelegates(delegates);
        Assert.assertNotSame(config.getAllowedDelegates(), delegates);
        Assert.assertNotNull(config.getAllowedDelegates());
        Assert.assertEquals(config.getAllowedDelegates().size(), 2);
        Assert.assertTrue(config.getAllowedDelegates().contains("foo"));
        Assert.assertTrue(config.getAllowedDelegates().contains("bar"));

        try {
            config.getAllowedDelegates().add("baz");
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }
}
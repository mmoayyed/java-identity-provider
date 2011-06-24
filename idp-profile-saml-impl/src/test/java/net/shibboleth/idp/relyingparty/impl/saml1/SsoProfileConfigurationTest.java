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

package net.shibboleth.idp.relyingparty.impl.saml1;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link SsoProfileConfiguration}. */
public class SsoProfileConfigurationTest {

    @Test
    public void testProfileId() {
        Assert.assertEquals(SsoProfileConfiguration.PROFILE_ID, "http://shibboleth.net/ns/profiles/saml1/sso");

        SsoProfileConfiguration config = new SsoProfileConfiguration();
        Assert.assertEquals(config.getProfileId(), SsoProfileConfiguration.PROFILE_ID);
    }
    
    @Test
    public void testIncludeAttributeStatement(){
        SsoProfileConfiguration config = new SsoProfileConfiguration();
        Assert.assertFalse(config.includeAttributeStatement());
        
        config.setIncludeAttributeStatement(true);
        Assert.assertTrue(config.includeAttributeStatement());
    }
}
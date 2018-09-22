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

package net.shibboleth.idp.saml.saml2.profile.config;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

import net.shibboleth.idp.saml.profile.config.BasicSAMLArtifactConfiguration;
import net.shibboleth.idp.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

/** Unit test for {@link AttributeQueryProfileConfiguration}. */
public class AttributeQueryProfileConfigurationTest {

    @Test
    public void testProfileId() {
        Assert.assertEquals(AttributeQueryProfileConfiguration.PROFILE_ID,
                "http://shibboleth.net/ns/profiles/saml2/query/attribute");

        final AttributeQueryProfileConfiguration config = new AttributeQueryProfileConfiguration();
        Assert.assertEquals(config.getId(), AttributeQueryProfileConfiguration.PROFILE_ID);
    }

    @Test
    public void testArtifactConfiguration() {
        final AttributeQueryProfileConfiguration config = new AttributeQueryProfileConfiguration();
        Assert.assertNull(config.getArtifactConfiguration());

        final SAMLArtifactConfiguration artifactConfiguration = new BasicSAMLArtifactConfiguration();
        config.setArtifactConfiguration(artifactConfiguration);

        Assert.assertSame(config.getArtifactConfiguration(), artifactConfiguration);
    }

    @Test
    public void testIndirectArtifactConfiguration() {
        final AttributeQueryProfileConfiguration config = new AttributeQueryProfileConfiguration();

        final SAMLArtifactConfiguration artifactConfiguration = new BasicSAMLArtifactConfiguration();
        config.setArtifactConfigurationLookupStrategy(
                FunctionSupport.<ProfileRequestContext,SAMLArtifactConfiguration>constant(artifactConfiguration));

        Assert.assertSame(config.getArtifactConfiguration(), artifactConfiguration);
    }
    
    @Test
    public void testSignArtifactRequests() {
        final AttributeQueryProfileConfiguration config = new AttributeQueryProfileConfiguration();
        
        config.setSignArtifactRequests(Predicates.<MessageContext>alwaysTrue());
        Assert.assertSame(config.getSignArtifactRequests(), Predicates.<MessageContext>alwaysTrue());
    }
     
    @Test
    public void testClientTLSArtifactRequests() {
        final AttributeQueryProfileConfiguration config = new AttributeQueryProfileConfiguration();
        
        config.setClientTLSArtifactRequests(Predicates.<MessageContext>alwaysTrue());
        Assert.assertSame(config.getClientTLSArtifactRequests(), Predicates.<MessageContext>alwaysTrue());
    }

}
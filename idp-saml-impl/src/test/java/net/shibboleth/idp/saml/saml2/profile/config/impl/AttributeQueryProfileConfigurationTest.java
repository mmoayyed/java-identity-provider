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

package net.shibboleth.idp.saml.saml2.profile.config.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.saml.profile.config.BasicSAMLArtifactConfiguration;
import net.shibboleth.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.shared.logic.FunctionSupport;

/** Unit test for {@link AttributeQueryProfileConfiguration}. */
@SuppressWarnings("javadoc")
public class AttributeQueryProfileConfigurationTest {

    @Test
    public void testProfileId() {
        final AttributeQueryProfileConfiguration config = new AttributeQueryProfileConfiguration();
        Assert.assertEquals(config.getId(), AttributeQueryProfileConfiguration.PROFILE_ID);
    }

    @Test
    public void testArtifactConfiguration() {
        final AttributeQueryProfileConfiguration config = new AttributeQueryProfileConfiguration();
        Assert.assertNull(config.getArtifactConfiguration(null));

        final SAMLArtifactConfiguration artifactConfiguration = new BasicSAMLArtifactConfiguration();
        config.setArtifactConfiguration(artifactConfiguration);

        Assert.assertSame(config.getArtifactConfiguration(null), artifactConfiguration);
    }

    @Test
    public void testIndirectArtifactConfiguration() {
        final AttributeQueryProfileConfiguration config = new AttributeQueryProfileConfiguration();

        final SAMLArtifactConfiguration artifactConfiguration = new BasicSAMLArtifactConfiguration();
        config.setArtifactConfigurationLookupStrategy(FunctionSupport.constant(artifactConfiguration));

        Assert.assertSame(config.getArtifactConfiguration(null), artifactConfiguration);
    }
    
    @Test
    public void testSignArtifactRequests() {
        final AttributeQueryProfileConfiguration config = new AttributeQueryProfileConfiguration();
        
        config.setSignArtifactRequests(true);
        Assert.assertTrue(config.isSignArtifactRequests(null));
    }
     
    @Test
    public void testClientTLSArtifactRequests() {
        final AttributeQueryProfileConfiguration config = new AttributeQueryProfileConfiguration();
        
        config.setClientTLSArtifactRequests(true);
        Assert.assertTrue(config.isClientTLSArtifactRequests(null));
    }

    @Test public void testSignAssertionsCriteria() {
        final AttributeQueryProfileConfiguration config = new AttributeQueryProfileConfiguration();

        config.setSignAssertions(false);
        Assert.assertFalse(config.isSignAssertions(null));
    }

    @Test public void testEncryptAssertionsPredicate() {
        final AttributeQueryProfileConfiguration config = new AttributeQueryProfileConfiguration();

        config.setEncryptAssertions(true);
        Assert.assertTrue(config.isEncryptAssertions(null));
    }

    @Test public void testEncryptAttributesPredicate() {
        final AttributeQueryProfileConfiguration config = new AttributeQueryProfileConfiguration();

        config.setEncryptAttributes(true);
        Assert.assertTrue(config.isEncryptAttributes(null));
    }

}
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

package net.shibboleth.idp.profile.spring.relyingparty.saml;

import java.util.Set;

import net.shibboleth.idp.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.idp.saml.profile.config.saml1.ArtifactResolutionProfileConfiguration;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SAML1ArtifactTest extends BaseSAMLProfileTest {

    @Test public void defaults() {

        ArtifactResolutionProfileConfiguration profile =
                getBean(ArtifactResolutionProfileConfiguration.class, true, "saml1artifact.xml");

        // defaults for AbstractSAMLProfileConfiguration
        assertConditionalPredicate(profile.getSignRequestsPredicate());
        assertFalsePredicate(profile.getSignAssertionsPredicate());
        assertConditionalPredicate(profile.getSignResponsesPredicate());
        Assert.assertEquals(profile.getAssertionLifetime(), 5 * 60 * 1000);
        Assert.assertTrue(profile.getAdditionalAudiencesForAssertion().isEmpty());
        Assert.assertTrue(profile.includeConditionsNotBefore());
        Assert.assertNull(profile.getArtifactConfiguration());
    }

    @Test public void values() {
        ArtifactResolutionProfileConfiguration profile =
                getBean(ArtifactResolutionProfileConfiguration.class, false, "beans.xml", "saml1artifactValues.xml");

        assertFalsePredicate(profile.getSignRequestsPredicate());
        assertFalsePredicate(profile.getSignAssertionsPredicate());
        assertConditionalPredicate(profile.getSignResponsesPredicate());

        Assert.assertEquals(profile.getAssertionLifetime(), 10 * 60 * 1000);

        final Set<String> audiences = profile.getAdditionalAudiencesForAssertion();
        Assert.assertEquals(audiences.size(), 1);
        Assert.assertEquals(audiences.iterator().next(), "NibbleAHappyWarthogNibbleAHappyWarthog");

        Assert.assertFalse(profile.includeConditionsNotBefore());

        final SAMLArtifactConfiguration artifact = profile.getArtifactConfiguration();
        Assert.assertEquals(artifact.getArtifactResolutionServiceURL(), "https://idp.example.org/Artifact/SAML1");
        Assert.assertEquals(artifact.getArtifactType().intValue(), 12340);
        Assert.assertEquals(artifact.getArtifactResolutionServiceIndex().intValue(), 43210);

    }

}

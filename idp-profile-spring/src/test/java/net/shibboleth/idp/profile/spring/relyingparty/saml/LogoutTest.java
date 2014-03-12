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

import java.util.Collection;
import java.util.Set;

import net.shibboleth.idp.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.idp.saml.profile.config.saml2.SingleLogoutProfileConfiguration;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LogoutTest extends BaseSAMLProfileTest {

    @Test public void defaults() {

        SingleLogoutProfileConfiguration profile = getBean(SingleLogoutProfileConfiguration.class, true, "saml/logout.xml");

        // defaults for AbstractSAML2ProfileConfiguration

        assertConditionalPredicate(profile.getEncryptAssertionsPredicate());
        assertFalsePredicate(profile.getEncryptNameIDsPredicate());

        Assert.assertEquals(profile.getProxyCount(), 0);
        Assert.assertTrue(profile.getProxyAudiences().isEmpty());

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
        SingleLogoutProfileConfiguration profile =
                getBean(SingleLogoutProfileConfiguration.class, false, "beans.xml", "saml/logoutValues.xml");

        assertTruePredicate(profile.getEncryptAssertionsPredicate());
        assertFalsePredicate(profile.getEncryptNameIDsPredicate());

        Assert.assertEquals(profile.getProxyCount(), 98);
        final Collection<String> proxyAudiences = profile.getProxyAudiences();
        Assert.assertEquals(proxyAudiences.size(), 1);
        Assert.assertTrue(proxyAudiences.contains("NibbleAHappyWarthog"));

        assertConditionalPredicate(profile.getSignRequestsPredicate());
        assertFalsePredicate(profile.getSignAssertionsPredicate());
        assertFalsePredicate(profile.getSignResponsesPredicate());

        Assert.assertEquals(profile.getAssertionLifetime(), 8 * 60 * 1000);

        final Set<String> audiences = profile.getAdditionalAudiencesForAssertion();
        Assert.assertEquals(audiences.size(), 0);

        Assert.assertTrue(profile.includeConditionsNotBefore());

        final SAMLArtifactConfiguration artifact = profile.getArtifactConfiguration();
        Assert.assertEquals(artifact.getArtifactResolutionServiceURL(), "https://idp.example.org/Logout");
        Assert.assertEquals(artifact.getArtifactType().intValue(), 7);
        Assert.assertEquals(artifact.getArtifactResolutionServiceIndex().intValue(), 3214);

    }

}

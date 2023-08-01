/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.saml1.profile.config.impl;

import net.shibboleth.saml.profile.config.BasicSAMLArtifactConfiguration;
import net.shibboleth.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.shared.logic.FunctionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

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
        config.setArtifactConfigurationLookupStrategy(
                FunctionSupport.<ProfileRequestContext,SAMLArtifactConfiguration>constant(artifactConfiguration));

        Assert.assertSame(config.getArtifactConfiguration(null), artifactConfiguration);
    }

    @Test public void testSignAssertionsCriteria() {
        final AttributeQueryProfileConfiguration config = new AttributeQueryProfileConfiguration();

        config.setSignAssertions(false);
        Assert.assertFalse(config.isSignAssertions(null));
    }
}
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

import java.util.List;

import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.idp.saml.saml2.profile.config.ECPProfileConfiguration;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SAML2ECPProfileTest extends BaseSAMLProfileTest {

    @Test public void defaults() {

        ECPProfileConfiguration profile = getBean(ECPProfileConfiguration.class, true, "saml/ecp.xml", "beans.xml");

        Assert.assertTrue(profile.includeAttributeStatement());
        Assert.assertFalse(profile.skipEndpointValidationWhenSigned());
        Assert.assertEquals(profile.getMaximumSPSessionLifetime(), 0);

        // defaults for AbstractSAML2ProfileConfiguration

        assertTruePredicate(profile.getEncryptAssertionsPredicate());
        assertFalsePredicate(profile.getEncryptNameIDsPredicate());

        Assert.assertEquals(profile.getProxyCount(), 0);
        Assert.assertTrue(profile.getProxyAudiences().isEmpty());

        // defaults for AbstractSAMLProfileConfiguration
        assertFalsePredicate(profile.getSignRequestsPredicate());
        assertFalsePredicate(profile.getSignAssertionsPredicate());
        assertTruePredicate(profile.getSignResponsesPredicate());
        Assert.assertEquals(profile.getAssertionLifetime(), 5 * 60 * 1000);
        Assert.assertTrue(profile.getAdditionalAudiencesForAssertion().isEmpty());
        Assert.assertTrue(profile.includeConditionsNotBefore());
        Assert.assertEquals(profile.getInboundSubflowId(), "SecurityPolicy.SAML2ECP");
        Assert.assertNull(profile.getOutboundSubflowId());

        Assert.assertNull(profile.getArtifactConfiguration());
    }

    @Test public void values() {
        BrowserSSOProfileConfiguration profile =
                getBean(BrowserSSOProfileConfiguration.class, true, "beans.xml", "saml/ecpValues.xml");

        Assert.assertFalse(profile.includeAttributeStatement());
        Assert.assertTrue(profile.skipEndpointValidationWhenSigned());
        Assert.assertEquals(profile.getMaximumSPSessionLifetime(), 1);

        assertTruePredicate(profile.getEncryptAssertionsPredicate());
        assertFalsePredicate(profile.getEncryptNameIDsPredicate());

        Assert.assertEquals(profile.getProxyCount(), 0);
        Assert.assertTrue(profile.getProxyAudiences().isEmpty());

        // defaults for AbstractSAMLProfileConfiguration
        assertFalsePredicate(profile.getSignRequestsPredicate());
        assertFalsePredicate(profile.getSignAssertionsPredicate());
        assertTruePredicate(profile.getSignResponsesPredicate());
        Assert.assertEquals(profile.getAssertionLifetime(), 5 * 60 * 1000);
        Assert.assertTrue(profile.getAdditionalAudiencesForAssertion().isEmpty());
        Assert.assertTrue(profile.includeConditionsNotBefore());
        Assert.assertEquals(profile.getInboundSubflowId(), "ecpibfid");
        Assert.assertEquals(profile.getOutboundSubflowId(), "ecpobfid");

        Assert.assertNull(profile.getArtifactConfiguration());

        Assert.assertEquals(profile.getDefaultAuthenticationMethods().size(), 1);
        final AuthnContextClassRefPrincipal authnMethod =
                (AuthnContextClassRefPrincipal) profile.getDefaultAuthenticationMethods().get(0);
        Assert.assertEquals(authnMethod.getAuthnContextClassRef().getAuthnContextClassRef(),
                "urn:oasis:names:tc:SAML:2.0:ac:classes:Password");

        final List<String> nameIDPrefs = profile.getNameIDFormatPrecedence();

        Assert.assertEquals(nameIDPrefs.size(), 2);
        Assert.assertTrue(nameIDPrefs.contains("three"));
        Assert.assertTrue(nameIDPrefs.contains("four"));
    }

    @Test(expectedExceptions = {BeanDefinitionParsingException.class,}) public void localityAddress() {
        getBean(BrowserSSOProfileConfiguration.class, true, "beans.xml", "saml/ecpLocalityAddress.xml");
    }

    @Test(expectedExceptions = {BeanDefinitionParsingException.class,}) public void localityDnsname() {
        getBean(BrowserSSOProfileConfiguration.class, true, "beans.xml", "saml/ecpLocalityDNSName.xml");
    }
}

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

package net.shibboleth.idp.attribute.filter.spring.saml;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.filter.policyrule.saml.impl.AttributeIssuerRegistrationAuthorityPolicyRule;
import net.shibboleth.idp.attribute.filter.spring.BaseAttributeFilterParserTest;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * test for {@link AttributeIssuerRegistrationAuthorityRuleParser}.
 */
public class AttributeIssuerRegistrationAuthorityRuleParserTest extends BaseAttributeFilterParserTest {


    @Test public void silentTrue() throws ComponentInitializationException {
        final AttributeIssuerRegistrationAuthorityPolicyRule rule =
                (AttributeIssuerRegistrationAuthorityPolicyRule) getPolicyRule("issuerAuthorityOne.xml");

        assertTrue(rule.isMatchIfMetadataSilent());
        final Set<String> issuers = rule.getRegistrars();

        assertEquals(issuers.size(), 2);
        assertTrue(issuers.contains("https://example.org/SilentTrue/One"));
        assertTrue(issuers.contains("https://example.org/SilentTrue/Two"));
    }

    @Test public void silentFalse() throws ComponentInitializationException {
        final AttributeIssuerRegistrationAuthorityPolicyRule rule
        = (AttributeIssuerRegistrationAuthorityPolicyRule) getPolicyRule("issuerAuthorityTwo.xml");

        assertTrue(rule.isMatchIfMetadataSilent());
        final Set<String> issuers = rule.getRegistrars();

        assertEquals(issuers.size(), 3);
        assertTrue(issuers.contains("https://example.org/SilentFalse/One"));
        assertTrue(issuers.contains("https://example.org/SilentFalse/Two"));
        assertTrue(issuers.contains("https://example.org/SilentFalse/Three"));
    }

}
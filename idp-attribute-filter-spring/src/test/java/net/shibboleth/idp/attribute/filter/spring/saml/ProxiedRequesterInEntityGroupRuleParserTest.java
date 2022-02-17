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

import org.springframework.beans.factory.BeanCreationException;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.filter.policyrule.saml.impl.ProxiedRequesterInEntityGroupPolicyRule;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.ProxiedRequesterInEntityGroupRuleParser;
import net.shibboleth.idp.attribute.filter.spring.testing.BaseAttributeFilterParserTest;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/** Unit test for {@link ProxiedRequesterInEntityGroupRuleParser}. */
public class ProxiedRequesterInEntityGroupRuleParserTest extends  BaseAttributeFilterParserTest {

    private void testRule(final String propValue, final boolean result) throws ComponentInitializationException {
        final ProxiedRequesterInEntityGroupPolicyRule rule =
                (ProxiedRequesterInEntityGroupPolicyRule) getPolicyRule("proxiedrequesterEG2.xml", contextWithPropertyValue(propValue));

        assertEquals(rule.getEntityGroup(), "urn:example.org");
        assertEquals(rule.isCheckAffiliations(), result);
    }

    /**
     * Failure test.
     * 
     * @throws ComponentInitializationException on error
     */
    @Test(expectedExceptions = {BeanCreationException.class}) public void failure() throws ComponentInitializationException {
        testRule("", false);
    }

    /**
     * Test true outcome.
     * 
     * @throws ComponentInitializationException on error
     */
    @Test public void egTrue() throws ComponentInitializationException {
        testRule("true", true);
    }

    /**
     * Test false outcome.
     *  
     * @throws ComponentInitializationException on error
     */
    @Test public void egFalse() throws ComponentInitializationException {
        testRule("false", false);
    }
}

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

import org.springframework.context.support.GenericApplicationContext;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.filter.AttributeRule;
import net.shibboleth.idp.attribute.filter.PolicyFromMatcher;
import net.shibboleth.idp.attribute.filter.matcher.saml.impl.AttributeInMetadataMatcher;
import net.shibboleth.idp.attribute.filter.spring.BaseAttributeFilterParserTest;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.MappedAttributeInMetadataRuleParser;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * test for {@link MappedAttributeInMetadataRuleParser}.
 */
public class MappedAttributeInMetadataRuleParserTest extends  BaseAttributeFilterParserTest {

     public void test(final String propValue, final boolean metadataSilentResult, final boolean onlyResult) throws ComponentInitializationException {
        GenericApplicationContext context = contextWithPropertyValue(propValue);
        setTestContext(context);
        context.setDisplayName("ApplicationContext: Matcher");

        final AttributeRule rule = getAttributeRulesAttributeFilterPolicy(MATCHER_PATH + "mappedInMetadata.xml", context).get(0);
        rule.initialize();
        AttributeInMetadataMatcher matcher = (AttributeInMetadataMatcher) rule.getMatcher();

        assertEquals(matcher.getMatchIfMetadataSilent(), metadataSilentResult);
        assertTrue(matcher.getOnlyIfRequired());
        assertTrue(matcher.getId().endsWith(":PermitRule"));

        final PolicyFromMatcher policyRule = (PolicyFromMatcher) getPolicyRuleFromAttributeFilterPolicy(context);
        matcher = (AttributeInMetadataMatcher) policyRule.getMatcher();
        assertTrue(matcher.getMatchIfMetadataSilent());
        assertEquals(matcher.getOnlyIfRequired(), onlyResult);
        assertTrue(matcher.getId().endsWith(":PRR"));
     }

     public void test(final String propValue, final boolean result) throws ComponentInitializationException {
         test(propValue, result, result);
     }

     @Test public void testTrue() throws ComponentInitializationException {
         test("true", true);
     }

     @Test public void testFalse() throws ComponentInitializationException {
         test("false", false);
     }

     @Test public void testEmpty() throws ComponentInitializationException {
         test("", false, true);
     }
}

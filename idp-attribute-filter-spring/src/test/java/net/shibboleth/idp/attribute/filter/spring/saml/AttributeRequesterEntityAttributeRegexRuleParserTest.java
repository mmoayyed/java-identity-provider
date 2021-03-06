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

import static org.testng.Assert.*;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.filter.policyrule.saml.impl.AttributeRequesterEntityAttributeRegexPolicyRule;
import net.shibboleth.idp.attribute.filter.spring.BaseAttributeFilterParserTest;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.AttributeRequesterEntityAttributeRegexRuleParser;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * test for {@link AttributeRequesterEntityAttributeRegexRuleParser}.
 */
public class AttributeRequesterEntityAttributeRegexRuleParserTest extends  BaseAttributeFilterParserTest {

    @SuppressWarnings("javadoc") @Test public void basic() throws ComponentInitializationException {
        final AttributeRequesterEntityAttributeRegexPolicyRule rule = (AttributeRequesterEntityAttributeRegexPolicyRule) getPolicyRule("requesterEARegex2.xml");
        assertEquals(rule.getValueRegex().pattern(), "^urn:example\\.org:policy:[^:]*$");
        assertEquals(rule.getAttributeName(), "urn:example.org:policy");
        assertFalse(rule.getIgnoreUnmappedEntityAttributes());
    }

}
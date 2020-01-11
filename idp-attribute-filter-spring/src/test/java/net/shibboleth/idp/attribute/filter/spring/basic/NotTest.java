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

package net.shibboleth.idp.attribute.filter.spring.basic;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule;
import net.shibboleth.idp.attribute.filter.matcher.logic.impl.NotMatcher;
import net.shibboleth.idp.attribute.filter.policyrule.logic.impl.NotPolicyRule;
import net.shibboleth.idp.attribute.filter.spring.BaseAttributeFilterParserTest;
import net.shibboleth.idp.attribute.filter.spring.basic.impl.NotMatcherParser;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * test for {@link NotMatcherParser}.
 */
@SuppressWarnings("javadoc")
public class NotTest extends BaseAttributeFilterParserTest {

    @Test public void matcher() throws ComponentInitializationException {
        NotMatcher what = (NotMatcher) getMatcher("not.xml");

        NotMatcher child = (NotMatcher) what.getNegatedMatcher();

        assertEquals(child.getNegatedMatcher().getClass(), Matcher.MATCHES_ALL.getClass());
    }

    @Test public void policy() throws ComponentInitializationException {
        NotPolicyRule what = (NotPolicyRule) getPolicyRule("not.xml");

        NotPolicyRule child = (NotPolicyRule) what.getNegatedRule();

        assertEquals(child.getNegatedRule().getClass(), PolicyRequirementRule.MATCHES_ALL.getClass());
    }
}

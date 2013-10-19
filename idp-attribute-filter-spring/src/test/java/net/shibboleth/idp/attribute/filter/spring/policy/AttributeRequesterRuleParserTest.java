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

package net.shibboleth.idp.attribute.filter.spring.policy;

import java.util.Map;
import java.util.Set;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.idp.attribute.filter.impl.matcher.DataSources;
import net.shibboleth.idp.attribute.filter.impl.policyrule.filtercontext.AttributeRequesterPolicyRule;
import net.shibboleth.idp.attribute.filter.spring.BaseAttributeFilterParserTest;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This tests not just the parsing of the rule, but also the construction of the complex tests.<br/>
 * <code>
 *  <PermitValueRule xsi:type="basic:AttributeRequesterString" value="https://service.example.edu/shibboleth-sp" />
 * </code><br/>
 * vs<br/>
 * <code>
 *  <afp:PolicyRequirementRule xsi:type="basic:AttributeRequesterString" value="https://service.example.edu/shibboleth-sp" />
 * </code><br/>
 */
public class AttributeRequesterRuleParserTest extends BaseAttributeFilterParserTest {

    private Map<String, IdPAttribute> epaUid;

    @BeforeClass public void setupAttributes() throws ComponentInitializationException, ResolutionException {

        epaUid = getAttributes("epa-uid.xml");
    }

    @Test public void policy() throws ComponentInitializationException {

        final PolicyRequirementRule rule = getPolicyRule("attributeRequester.xml");

        AttributeFilterContext filterContext = DataSources.populatedFilterContext("principal", "issuer", "http://example.org");
        Assert.assertEquals(rule.matches(filterContext), Tristate.FALSE);
        filterContext = DataSources.populatedFilterContext("principal", "issuer", "https://service.example.edu/shibboleth-sp");
        Assert.assertEquals(rule.matches(filterContext), Tristate.TRUE);

        final AttributeRequesterPolicyRule arRule = (AttributeRequesterPolicyRule) rule;
        Assert.assertEquals(arRule.getMatchString(), "https://service.example.edu/shibboleth-sp");
        Assert.assertTrue(arRule.getCaseSensitive());
    }
 
    @Test public void matcher() throws ComponentInitializationException {

        final Matcher matcher = getMatcher("attributeRequester.xml");

        AttributeFilterContext filterContext = DataSources.populatedFilterContext("principal", "issuer", "http://example.org");
        filterContext.setPrefilteredAttributes(epaUid.values());
        Set<AttributeValue> result = matcher.getMatchingValues(epaUid.get("uid"), filterContext);
        Assert.assertTrue(result.isEmpty());

        filterContext = DataSources.populatedFilterContext("principal", "issuer", "https://service.example.edu/shibboleth-sp");
        filterContext.setPrefilteredAttributes(epaUid.values());
        result = matcher.getMatchingValues(epaUid.get("uid"), filterContext);
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.iterator().next().getValue(), "daffyDuck");
    }
}

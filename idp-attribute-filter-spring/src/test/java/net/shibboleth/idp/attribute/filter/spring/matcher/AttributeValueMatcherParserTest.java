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

package net.shibboleth.idp.attribute.filter.spring.matcher;

import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.idp.attribute.filter.impl.matcher.AttributeValueStringMatcher;
import net.shibboleth.idp.attribute.filter.spring.BaseAttributeFilterParserTest;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * This tests not just the parsing of the rule, but also the construction of the complex tests.<br/>
 * <code>
 *  <PermitValueRule xsi:type="basic:AttributeValueString" value="jsmith" attributeId="uid" ignoreCase="true"/>
 * </code><br/>
 * vs<br/>
 * <code>
 *  <PermitValueRule xsi:type="basic:AttributeValueString" value="jsmith" ignoreCase="true"/>
 * </code><br/>
 * vs<br/>
 * <code>
 *  <afp:PolicyRequirementRule xsi:type="basic:AttributeValueString" value="jsmith" ignoreCase="true"/>
 * </code><br/>
 * vs<br/>
 * <code>
 *  <afp:PolicyRequirementRule xsi:type="basic:AttributeValueString" attributeId="uid" value="jsmith" ignoreCase="true"/>
 * </code><br/>
 */
public class AttributeValueMatcherParserTest extends BaseAttributeFilterParserTest {

    private Map<String, IdPAttribute> epaUid;

    private Map<String, IdPAttribute> epaUidJS;

    private Map<String, IdPAttribute> uidEpaJS;

    @BeforeClass public void setupAttributes() throws ComponentInitializationException, ResolutionException {

        epaUid = getAttributes("epa-uid.xml");
        epaUidJS = getAttributes("epa-uidwithjsmith.xml");
        uidEpaJS = getAttributes("uid-epawithjsmith.xml");
    }

    @Test public void targetedPolicy() throws ComponentInitializationException {

        final PolicyRequirementRule rule = getPolicyRule("attributeValueId.xml", true);

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUid.values());
        Assert.assertEquals(rule.matches(filterContext), Tristate.FALSE);

        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUidJS.values());
        Assert.assertEquals(rule.matches(filterContext), Tristate.TRUE);

        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(uidEpaJS.values());
        Assert.assertEquals(rule.matches(filterContext), Tristate.FALSE);
    }

    @Test public void unTargetedPolicy() throws ComponentInitializationException {

        final PolicyRequirementRule rule = getPolicyRule("attributeValueNoId.xml");

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUid.values());
        Assert.assertEquals(rule.matches(filterContext), Tristate.FALSE);

        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUidJS.values());
        Assert.assertEquals(rule.matches(filterContext), Tristate.TRUE);

        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(uidEpaJS.values());
        Assert.assertEquals(rule.matches(filterContext), Tristate.TRUE);
    }

    @Test public void unTargetedMatcher() throws ComponentInitializationException {

        final Matcher matcher = getMatcher("attributeValueNoId.xml");

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUid.values());
        Set<AttributeValue<?>> result = matcher.getMatchingValues(epaUid.get("uid"), filterContext);
        Assert.assertTrue(result.isEmpty());

        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUidJS.values());
        result = matcher.getMatchingValues(epaUidJS.get("uid"), filterContext);
        Assert.assertEquals(result.size(), 1);
        
        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(uidEpaJS.values());
        result = matcher.getMatchingValues(uidEpaJS.get("uid"), filterContext);
        Assert.assertTrue(result.isEmpty());
        
        AttributeValueStringMatcher avm = (AttributeValueStringMatcher) matcher;
        Assert.assertFalse(avm.getCaseSensitive());
        Assert.assertEquals(avm.getMatchString(), "jsmith");
    }

    @Test public void targetedMatcher() throws ComponentInitializationException {

        final Matcher matcher = getMatcher("attributeValueId.xml", true);

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUid.values());
        Set<AttributeValue<?>> result = matcher.getMatchingValues(epaUid.get("uid"), filterContext);
        Assert.assertTrue(result.isEmpty());

        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUidJS.values());
        result = matcher.getMatchingValues(epaUidJS.get("uid"), filterContext);
        Assert.assertEquals(result.size(), 2);
        
        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(uidEpaJS.values());
        result = matcher.getMatchingValues(uidEpaJS.get("uid"), filterContext);
        Assert.assertTrue(result.isEmpty());
    }
}

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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.FatalBeanException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.matcher.impl.AttributeValueStringMatcher;
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
        final PolicyRequirementRule rule = getPolicyRule("attributeValueId.xml");

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUid.values());
        assertEquals(rule.matches(filterContext), Tristate.FALSE);

        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUidJS.values());
        assertEquals(rule.matches(filterContext), Tristate.TRUE);

        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(uidEpaJS.values());
        assertEquals(rule.matches(filterContext), Tristate.FALSE);
    }

    @Test public void unTargetedPolicy() throws ComponentInitializationException {
        final PolicyRequirementRule rule = getPolicyRule("attributeValueNoId.xml");

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUid.values());
        assertEquals(rule.matches(filterContext), Tristate.FALSE);

        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUidJS.values());
        assertEquals(rule.matches(filterContext), Tristate.TRUE);

        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(uidEpaJS.values());
        assertEquals(rule.matches(filterContext), Tristate.TRUE);
    }

    @Test public void unTargetedMatcher() throws ComponentInitializationException {
        final Matcher matcher = getMatcher("attributeValueNoId.xml");

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUid.values());
        Set<IdPAttributeValue> result = matcher.getMatchingValues(epaUid.get("uid"), filterContext);
        assertTrue(result.isEmpty());

        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUidJS.values());
        result = matcher.getMatchingValues(epaUidJS.get("uid"), filterContext);
        assertEquals(result.size(), 1);
        
        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(uidEpaJS.values());
        result = matcher.getMatchingValues(uidEpaJS.get("uid"), filterContext);
        assertTrue(result.isEmpty());
        
        AttributeValueStringMatcher avm = (AttributeValueStringMatcher) matcher;
        assertFalse(avm.isCaseSensitive());
        assertEquals(avm.getMatchString(), "jsmith");
    }

    @Test public void targetedMatcher() throws ComponentInitializationException {

        final Matcher matcher = getMatcher("attributeValueId.xml");

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUid.values());
        Set<IdPAttributeValue> result = matcher.getMatchingValues(epaUid.get("uid"), filterContext);
        assertTrue(result.isEmpty());

        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUidJS.values());
        result = matcher.getMatchingValues(epaUidJS.get("uid"), filterContext);
        assertEquals(result.size(), 2);
        
        filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(uidEpaJS.values());
        result = matcher.getMatchingValues(uidEpaJS.get("uid"), filterContext);
        assertTrue(result.isEmpty());
    }
    
    @Test public void emptyCaseSensitive() throws ComponentInitializationException {

        try {
            getMatcher("attributeValueEmptyCaseSensitive.xml");
            fail("should have thrown an exception");
        } catch (FatalBeanException e) {
            assertEquals(org.xml.sax.SAXParseException.class, rootCause(e));
        } 
    }
    
    private void propertyCaseSensitive(final String propValue, final boolean result) throws ComponentInitializationException {
        final AttributeValueStringMatcher match = (AttributeValueStringMatcher )
            getMatcher("attributeValuePropertyCaseSensitive.xml", contextWithPropertyValue(propValue)); 

        assertEquals(result, match.isCaseSensitive());
    }
    
    @Test public void propertyTrueCaseSensitive() throws ComponentInitializationException {
        propertyCaseSensitive("true", true);
    }
    
    @Test public void propertyFalseCaseSensitive() throws ComponentInitializationException {
        propertyCaseSensitive("false", false);
    }

    @Test public void propertyEmptyCaseSensitive() throws ComponentInitializationException {
        propertyCaseSensitive("", false);
    }

}

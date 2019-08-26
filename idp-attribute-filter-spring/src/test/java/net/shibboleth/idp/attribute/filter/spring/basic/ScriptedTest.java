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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanCreationException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.matcher.impl.ScriptedMatcher;
import net.shibboleth.idp.attribute.filter.policyrule.impl.ScriptedPolicyRule;
import net.shibboleth.idp.attribute.filter.spring.BaseAttributeFilterParserTest;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.testing.TestSupport;

/** test for parsing scripted matchers and scripted parsers.
 *
 */
public class ScriptedTest extends BaseAttributeFilterParserTest {

    private Map<String, IdPAttribute> epaUid;
    
    private String getScript() {
        if (TestSupport.isJavaV8OrLater()) {
            return "scripted-v8.xml";
        }
        return "scripted.xml";
    }

    @BeforeClass public void setupAttributes() throws ComponentInitializationException, ResolutionException {

        epaUid = getAttributes("epa-uidwithjsmith.xml");
    }

    @Test public void policy() throws ComponentInitializationException {
        final ScriptedPolicyRule rule = (ScriptedPolicyRule) getPolicyRule(getScript());

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUid.values());
        assertEquals(rule.matches(filterContext), Tristate.FALSE);
    }
    
    @Test(expectedExceptions={BeanCreationException.class,}) public void policyNotFound() throws ComponentInitializationException {

        getPolicyRule("scriptedNotThere.xml");
    }
    
    @Test public void matcher()  throws ComponentInitializationException {
        final ScriptedMatcher matcher = (ScriptedMatcher) getMatcher(getScript());
        
        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(epaUid.values());
        Set<IdPAttributeValue> x = matcher.getMatchingValues(epaUid.get("uid"), filterContext);
        assertEquals(x.size(), 1);
        String val = ((StringAttributeValue) x.iterator().next()).getValue();
        assertTrue(val.equals("jsmith") || val.equals("daffyDuck"));
        
    }
    
    @Test public void customMatcher() throws ComponentInitializationException {
        
        final ScriptedMatcher what = (ScriptedMatcher) getMatcher(getScript());
        
        assertNull(what.getCustomObject());
        
    }

    @Test public void customPolicy() throws ComponentInitializationException {
        
        final ScriptedPolicyRule what = (ScriptedPolicyRule) getPolicyRule(getScript());
        
        final Map<?,?> custom = (Map<?,?>) what.getCustomObject();
     
        assertEquals(custom.size(), 1);
        assertEquals(custom.get("bar"), "foo");
        
    }

    
}

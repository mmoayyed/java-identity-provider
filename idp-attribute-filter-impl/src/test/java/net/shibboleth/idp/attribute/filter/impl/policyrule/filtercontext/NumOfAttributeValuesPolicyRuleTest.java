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

package net.shibboleth.idp.attribute.filter.impl.policyrule.filtercontext;

import java.util.Collections;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * test for {@link NumOfAttributeValuesPolicyRule}.
 */
public class NumOfAttributeValuesPolicyRuleTest {

    private final static String attrId = "attribute";
    private final static int testMin = 3;
    private final static int testMax = 5;
    
    
    @Test public void setterGetterInit() throws ComponentInitializationException {
        NumOfAttributeValuesPolicyRule rule = new NumOfAttributeValuesPolicyRule();
        Assert.assertNull(rule.getAttributeId());
        
        rule.setId("id");
        rule.setAttributeId(attrId);
        rule.setMaximumValues(testMax);
        rule.setMinimumValues(testMin);

        rule.initialize();
        
        Assert.assertEquals(rule.getAttributeId(), attrId);
        Assert.assertEquals(rule.getMaximumValues(), testMax);
        Assert.assertEquals(rule.getMinimumValues(), testMin);
    }
    
    @Test(expectedExceptions={ComponentInitializationException.class,}) 
    public void noAttrID() throws ComponentInitializationException {
        NumOfAttributeValuesPolicyRule rule = new NumOfAttributeValuesPolicyRule();
        rule.setId("id");
        rule.initialize();
    }
    
    @Test(expectedExceptions={ComponentInitializationException.class,}) 
    public void noMin() throws ComponentInitializationException {
        NumOfAttributeValuesPolicyRule rule = new NumOfAttributeValuesPolicyRule();
        rule.setId("id");
        rule.setAttributeId(attrId);
        rule.initialize();
    }

    @Test(expectedExceptions={ComponentInitializationException.class,})
    public void noMax() throws ComponentInitializationException {
        NumOfAttributeValuesPolicyRule rule = new NumOfAttributeValuesPolicyRule();
        rule.setId("id");
        rule.setAttributeId(attrId);
        rule.setMinimumValues(testMax);
        rule.initialize();
    }
    
    @Test(expectedExceptions={ConstraintViolationException.class,})
    public void badAttrId() {
        NumOfAttributeValuesPolicyRule rule = new NumOfAttributeValuesPolicyRule();
        rule.setId("id");
        rule.setAttributeId("  ");
    }
    
    @Test(expectedExceptions={ConstraintViolationException.class,})
    public void badMin()  {
        NumOfAttributeValuesPolicyRule rule = new NumOfAttributeValuesPolicyRule();
        rule.setId("id");
        rule.setMinimumValues(-6);
    }

    @Test(expectedExceptions={ConstraintViolationException.class,})
    public void badMax()  {
        NumOfAttributeValuesPolicyRule rule = new NumOfAttributeValuesPolicyRule();
        rule.setId("id");
        rule.setMaximumValues(0);
    }
    
    private AttributeFilterContext manufactureWith(int howMany) {
        return manufactureWith(attrId, howMany);
    }

    private AttributeFilterContext manufactureWith(String name, int howMany) {
        final Attribute attr = new Attribute(name);
        
        for (int i = 0; i < howMany; i++) {
            attr.getValues().add(new StringAttributeValue(Integer.toString(i)));
        }
        final AttributeFilterContext context = new AttributeFilterContext();
        context.setPrefilteredAttributes(Collections.singleton(attr));
        return context;
    }
    
    @Test public void  runRule()  throws ComponentInitializationException {
        NumOfAttributeValuesPolicyRule rule = new NumOfAttributeValuesPolicyRule();
        rule.setId("id");
        rule.setAttributeId(attrId);
        rule.setMaximumValues(testMax);
        rule.setMinimumValues(testMin);

        rule.initialize();
                
        Assert.assertEquals(rule.matches(manufactureWith(testMin-1)), Tristate.FALSE);
        Assert.assertEquals(rule.matches(manufactureWith(testMin)), Tristate.TRUE);
        Assert.assertEquals(rule.matches(manufactureWith(testMax)), Tristate.TRUE);
        Assert.assertEquals(rule.matches(manufactureWith(testMax+1)), Tristate.FALSE);
        
        Assert.assertEquals(rule.matches(manufactureWith("foo", testMin)), Tristate.FALSE);
    }
}

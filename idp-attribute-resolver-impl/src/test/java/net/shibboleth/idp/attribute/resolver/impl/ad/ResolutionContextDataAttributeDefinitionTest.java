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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Function;


/** Tests for {@link ResolutionContextDataAttributeDefinition} */
public class ResolutionContextDataAttributeDefinitionTest {

    private static final String TEST_ATTRIBUTE_NAME = "ContextFunction";
    private static final String TEST_ATTRIBUTE_VALUE_1 = "Value1";
    private static final String TEST_ATTRIBUTE_VALUE_2 = "Value2";

    
    @Test public void testInitDestroyParms() throws ComponentInitializationException, AttributeResolutionException {
        ResolutionContextDataAttributeDefinition attrDef = new ResolutionContextDataAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        
        try {
            attrDef.initialize();
            Assert.fail("no strategy");
        } catch (ComponentInitializationException ex) {
            // OK
        }
        
        try {
            attrDef.setDataExtractionStrategy(null);
            Assert.fail("Set null strategy");            
        } catch (ConstraintViolationException ex) {
            // OK
        }
        
        try {
            attrDef.doAttributeDefinitionResolve(new AttributeResolutionContext());
            Assert.fail("resolve before initialized");            
        } catch (UninitializedComponentException ex) {
            // OK
        }
        
        
        attrDef.setDataExtractionStrategy(new TestFunction(Collections.singleton(TEST_ATTRIBUTE_VALUE_1)));
        attrDef.initialize();
        try {
            attrDef.setDataExtractionStrategy(new TestFunction(Collections.singleton(TEST_ATTRIBUTE_VALUE_2)));
            Assert.fail("Set strategy after init");            
        } catch (UnmodifiableComponentException ex) {
            // OK
        }
        
        attrDef.destroy();
        try {
            attrDef.setDataExtractionStrategy(new TestFunction(Collections.singleton(TEST_ATTRIBUTE_VALUE_2)));
            Assert.fail("Set strategy after destroy");            
        } catch (DestroyedComponentException ex) {
            // OK
        }

        try {
            attrDef.initialize();
            Assert.fail("Init after destroy");            
        } catch (DestroyedComponentException ex) {
            // OK
        }
        
        try {
            attrDef.doAttributeDefinitionResolve(new AttributeResolutionContext());
            Assert.fail("Resolve after destroy");            
        } catch (DestroyedComponentException ex) {
            // OK
        }
    }
    
    @Test public void testNormalOperation() throws ComponentInitializationException, AttributeResolutionException {
        ResolutionContextDataAttributeDefinition attrDef = new ResolutionContextDataAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        attrDef.setDataExtractionStrategy(new TestFunction(null));
        attrDef.initialize();
        Set<AttributeValue> result = attrDef.doAttributeDefinitionResolve(new AttributeResolutionContext()).get().getValues();
        Assert.assertTrue(result.isEmpty());
        
        Set<String> values = new HashSet<String>();
        attrDef = new ResolutionContextDataAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        attrDef.setDataExtractionStrategy(new TestFunction(values));
        attrDef.initialize();
        result = attrDef.doAttributeDefinitionResolve(new AttributeResolutionContext()).get().getValues();
        Assert.assertTrue(result.isEmpty());
        
        values.add(TEST_ATTRIBUTE_VALUE_1);
        values.add(null);
        values.add(TEST_ATTRIBUTE_VALUE_2);
        attrDef = new ResolutionContextDataAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        Assert.assertNull(attrDef.getDataExtractionStrategy());
        attrDef.setDataExtractionStrategy(new TestFunction(values));
        Assert.assertNotNull(attrDef.getDataExtractionStrategy());
        attrDef.initialize();
        result = attrDef.doAttributeDefinitionResolve(new AttributeResolutionContext()).get().getValues();
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(new StringAttributeValue(TEST_ATTRIBUTE_VALUE_1)));
        Assert.assertTrue(result.contains(new StringAttributeValue(TEST_ATTRIBUTE_VALUE_2)));
    }
    
    private static class TestFunction implements Function<AttributeResolutionContext, Collection<? extends AttributeValue>> {

        private final Collection<String> returnValues;
        
        
        /**
         * Constructor.
         *
         * @param testAttributeValue
         */
        public TestFunction(Collection<String> testAttributeValues) {
            returnValues = testAttributeValues;
        }


        public Collection<? extends AttributeValue> apply(AttributeResolutionContext input) {
            if (returnValues == null) {
                return null;
            }
            Set<StringAttributeValue> result = new HashSet<StringAttributeValue>();
            for (String s: returnValues) {
                if (s == null) {
                    result.add(null);
                } else {
                    result.add(new StringAttributeValue(s));
                }
            }
            return result;
        }
        
    }
}

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

package net.shibboleth.idp.attribute.filtering;

import java.util.Arrays;
import java.util.Collection;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link AttributeValueFilterPolicy}
 */
public class AttributeValueFilterPolicyTest {

    @Test public void testInitDestroy() throws ComponentInitializationException {
        AttributeValueFilterPolicy policy = new AttributeValueFilterPolicy();
        MockAttributeValueMatcher matcher = new MockAttributeValueMatcher(); 
        policy.setValueMatcher(matcher);
        
        Assert.assertFalse(policy.isInitialized(), "Created - not initialized");
        Assert.assertFalse(matcher.isInitialized(), "Create - not initialized");
        Assert.assertFalse(policy.isDestroyed(), "Created - not destroyed");
        Assert.assertFalse(matcher.isDestroyed(), "Created - not destroyed");
        
        policy.setAttributeId("foo");
        policy.initialize();
        
        Assert.assertTrue(policy.isInitialized(), "Initialized");
        Assert.assertTrue(matcher.isInitialized(), "Initialized");
        Assert.assertFalse(policy.isDestroyed(), "Initialized - not destroyed");
        Assert.assertFalse(matcher.isDestroyed(), "Initialized - not destroyed");
        
        policy.destroy();
        Assert.assertTrue(policy.isDestroyed(), "Destroyed");
        Assert.assertTrue(matcher.isDestroyed(), "Destroyed");
        
        boolean thrown = false;
        try {            
            policy.initialize();
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "initialize after destroy");
        
    }
    
    @Test public void testAttributeId() throws ComponentInitializationException{
        AttributeValueFilterPolicy policy = new AttributeValueFilterPolicy();
        Assert.assertNotNull(policy.getAttributeId(), "AttributeId can never be null");

        boolean thrown = false;
        try {
            policy.setAttributeId(null);            
        } catch (AssertionError e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "null Attribute Id");
        Assert.assertNotNull(policy.getAttributeId(), "AttributeId can never be null");
        
        thrown = false;
        try {
            policy.setAttributeId("");            
        } catch (AssertionError e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "empty Attribute Id");
        Assert.assertNotNull(policy.getAttributeId(), "AttributeId can never be null");
        
        try {
            policy.initialize();            
        } catch (ComponentInitializationException e) {
            thrown = true;
        } 
        Assert.assertTrue(thrown, "init with no Id");
        
        policy = new AttributeValueFilterPolicy();
        policy.setAttributeId(" ID ");
        Assert.assertEquals(policy.getAttributeId(), "ID", "Get Attribute ID");
        
        policy.initialize();
        Assert.assertEquals(policy.getAttributeId(), "ID", "Get Attribute ID");
        
        thrown = false;
        try {
            policy.setAttributeId("foo");            
        } catch (UnmodifiableComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "SetAttributeId after init");
        Assert.assertEquals(policy.getAttributeId(), "ID", "Get Attribute ID");

        policy.destroy();
        thrown = false;
        try {
            policy.getAttributeId();
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "GetAttributeId after destroy");
        
        policy = new AttributeValueFilterPolicy();
        policy.destroy();
        thrown = false;
        try {
            policy.setAttributeId("foo");            
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "SetAttributeId after destroy");
    }
    
    @Test public void testMatchingPermittingValues() throws ComponentInitializationException {
        AttributeValueFilterPolicy policy = new AttributeValueFilterPolicy();
        policy.setAttributeId("foo");
        Assert.assertTrue(policy.isMatchingPermittedValues(), "MatchingPermitted Values - created");
        
        policy.setMatchingPermittedValues(false);     
        Assert.assertFalse(policy.isMatchingPermittedValues(), "MatchingPermitted Values - changed");
        policy.initialize();
        Assert.assertFalse(policy.isMatchingPermittedValues(), "MatchingPermitted Values - initialized");
        
        boolean thrown = false;
        try {
            policy.setMatchingPermittedValues(true);     
        } catch (UnmodifiableComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "MatchingPermitted Values - set after initialized");
        Assert.assertFalse(policy.isMatchingPermittedValues(), "MatchingPermitted Values - set after initialized");

        policy = new AttributeValueFilterPolicy();
        policy.setAttributeId(" foo");        
        policy.initialize();
        policy.destroy();
        thrown = false;
        try {
            policy.setMatchingPermittedValues(false);
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "setMatchingPermittedValues after destroy");
        
        thrown = false;
        try {
            policy.isMatchingPermittedValues();
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "isMatchingPermittedValues after destroy");
    }
    
    @Test public void testValueMatcher() throws ComponentInitializationException {
        AttributeValueFilterPolicy policy = new AttributeValueFilterPolicy();
        policy.setAttributeId("foo");
        Assert.assertNotNull(policy.getValueMatcher(), "AttributeValueMatcher - created");
        
        Assert.assertNotSame(policy.getValueMatcher(), AttributeValueMatcher.MATCHES_ALL, "AttributeValueMatcher - precondition for rest of test");
        
        policy.setValueMatcher(AttributeValueMatcher.MATCHES_ALL);     
        Assert.assertEquals(policy.getValueMatcher(), AttributeValueMatcher.MATCHES_ALL, "AttributeValueMatcher - changed");
        policy.initialize();
        Assert.assertEquals(policy.getValueMatcher(), AttributeValueMatcher.MATCHES_ALL, "AttributeValueMatcher - initialized");
        
        boolean thrown = false;
        try {
            policy.setValueMatcher(AttributeValueMatcher.MATCHES_NONE);     
        } catch (UnmodifiableComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "AttributeValueMatcher - set after initialized");
        Assert.assertEquals(policy.getValueMatcher(), AttributeValueMatcher.MATCHES_ALL, "AttributeValueMatcher - set after initialized");

        policy = new AttributeValueFilterPolicy();
        policy.setAttributeId(" foo");        
        policy.initialize();
        policy.destroy();
        thrown = false;
        try {
            policy.setValueMatcher(AttributeValueMatcher.MATCHES_NONE);     
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "setMatchingPermittedValues after destroy");
        
        thrown = false;
        try {
            policy.getValueMatcher();
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "isMatchingPermittedValues after destroy");
        
    }
    @Test public void testValidateApply() throws ComponentInitializationException, ComponentValidationException, AttributeFilteringException {
        MockAttributeValueMatcher matcher = new MockAttributeValueMatcher(); 
        
        final StringAttributeValue aStringAttributeValue = new StringAttributeValue("a");
        final StringAttributeValue bStringAttributeValue = new StringAttributeValue("b");
        final StringAttributeValue cStringAttributeValue = new StringAttributeValue("c");
        final StringAttributeValue dStringAttributeValue = new StringAttributeValue("d");
        final String ATTR_NAME = "one";
        final Attribute attribute1 = new Attribute(ATTR_NAME);
        attribute1.getValues().add(aStringAttributeValue);
        attribute1.getValues().add(bStringAttributeValue);
        attribute1.getValues().add(cStringAttributeValue);
        attribute1.getValues().add(dStringAttributeValue);
        
        matcher.setMatchingAttribute(ATTR_NAME);
        matcher.setMatchingValues(Arrays.asList(aStringAttributeValue, cStringAttributeValue));
        
        AttributeValueFilterPolicy policy = new AttributeValueFilterPolicy();
        policy.setValueMatcher(matcher);
        policy.setAttributeId(ATTR_NAME);
        
        boolean thrown = false;
        try {
            policy.validate();
        } catch (UninitializedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "Validate before init");

        thrown = false;
        try {
            policy.apply(new Attribute(ATTR_NAME), new AttributeFilterContext());
        } catch (UninitializedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "Validate before init");

        policy.initialize();
        
        policy.validate();
        Assert.assertTrue(matcher.getValidated(), "Validated");
        
        thrown = false;
        try {
            policy.apply(null, new AttributeFilterContext());
        } catch (AssertionError e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "Null attribute");

        thrown = false;
        try {
            policy.apply(new Attribute(ATTR_NAME), null);
        } catch (AssertionError e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "Null context");

        AttributeFilterContext context = new AttributeFilterContext();
        context.setPrefilteredAttributes(Arrays.asList(attribute1));

        policy.apply(attribute1, context);
        
        Collection<AttributeValue> result = context.getPermittedAttributeValues().get(ATTR_NAME);
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(aStringAttributeValue));
        Assert.assertTrue(result.contains(cStringAttributeValue));
        Assert.assertNull(context.getDeniedAttributeValues().get(ATTR_NAME));

        policy = new AttributeValueFilterPolicy();
        policy.setValueMatcher(matcher);
        policy.setAttributeId(ATTR_NAME);
        policy.setMatchingPermittedValues(false);
        policy.initialize();
        
        context = new AttributeFilterContext();
        context.setPrefilteredAttributes(Arrays.asList(attribute1));

        policy.apply(attribute1, context);
        
        result = context.getDeniedAttributeValues().get(ATTR_NAME);
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(aStringAttributeValue));
        Assert.assertTrue(result.contains(cStringAttributeValue));
        Assert.assertNull(context.getPermittedAttributeValues().get(ATTR_NAME));


        policy.destroy();

        thrown = false;
        try {
            policy.validate();
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "validate after destroy");
        
        thrown = false;
        try {
            policy.apply(attribute1, context);
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "validate after destroy");
    }
}

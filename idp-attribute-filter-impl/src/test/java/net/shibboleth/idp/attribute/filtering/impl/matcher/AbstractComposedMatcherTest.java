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

package net.shibboleth.idp.attribute.filtering.impl.matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.DestructableComponent;
import net.shibboleth.utilities.java.support.component.InitializableComponent;

import org.testng.Assert;
import org.testng.annotations.Test;

/** unit tests for {@link AbstractComposedMatcher}. */
public class AbstractComposedMatcherTest {

    @Test
    public void testInitDestroy() throws ComponentInitializationException, AttributeFilteringException {
        List<AttributeValueMatcher> firstList = new ArrayList<AttributeValueMatcher>(2);
        ComposedMatcher matcher = new ComposedMatcher(Collections.EMPTY_LIST);
        
        for (int i = 0; i < 2;i++) {
            firstList.add(new MyMatcher());
        }
        
        matcher.destroy();
        
        boolean thrown = false;
        try {
            matcher.validate();
        } catch (ComponentValidationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "Validate after destroy");

        thrown = false;
        try {
            matcher.initialize();
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
        
        Assert.assertTrue(thrown, "Initialize after destroy");

        for (int i = 0; i < 2;i++) {
            firstList.add(new MyMatcher());
        }
        matcher = new ComposedMatcher(firstList);
        
        thrown = false;
        try {
            matcher.validate();
        } catch (ComponentValidationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "Validate before initialize");

        thrown = false;
        try {
            matcher.getComposedMatchers().add(new MyMatcher());
        } catch (UnsupportedOperationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "Set into the returned list");
        
        matcher.initialize();
        
        for (int i = 0; i < 2;i++) {
            Assert.assertTrue(((InitializableComponent)firstList.get(i)).isInitialized(), "Element should be initialized");
            Assert.assertFalse(((DestructableComponent)firstList.get(i)).isDestroyed(), "Element should not be destroyed");
        }

        matcher.destroy();

        for (int i = 0; i < 2;i++) {
            Assert.assertTrue(((InitializableComponent)firstList.get(i)).isInitialized(), "Element should be initialized");
            Assert.assertTrue(((DestructableComponent)firstList.get(i)).isDestroyed(), "Element should  be destroyed");
        }
        thrown = false;
        try {
            matcher.initialize();
        } catch (DestroyedComponentException  e) {
            thrown = true;
        }
        
        Assert.assertTrue(thrown, "Initialize after destroy");

        matcher.destroy();
    }
    
    @Test
    public void testParams() throws ComponentInitializationException {
        ComposedMatcher matcher = new ComposedMatcher(null);

        Assert.assertTrue(matcher.getComposedMatchers().isEmpty(), "Initial state - no matchers");
        Assert.assertTrue(matcher.getComposedMatchers().isEmpty(), "Add null - no matchers");
        
        List<AttributeValueMatcher> list = new ArrayList<AttributeValueMatcher>();
        
        for (int i = 0; i < 30; i++) {
            list.add(null);
        }
        
        matcher = new ComposedMatcher(list);
        Assert.assertTrue(matcher.getComposedMatchers().isEmpty(), "Add List<null> - no matchers");
        
        list.set(2, new MyMatcher());
        list.set(3, new MyMatcher());
        list.set(7, new MyMatcher());
        list.set(11, new MyMatcher());
        list.set(13, new MyMatcher());
        list.set(17, new MyMatcher());
        list.set(19, new MyMatcher());
        list.set(23, new MyMatcher());
        list.set(29, new MyMatcher());
        Assert.assertTrue(matcher.getComposedMatchers().isEmpty(), "Change to input list - no matchers");

        matcher = new ComposedMatcher(list);
        Assert.assertEquals(matcher.getComposedMatchers().size(), 9, "Add a List with nulls");
        
        list.clear();
        Assert.assertEquals(matcher.getComposedMatchers().size(), 9, "Change to input list");

        matcher = new ComposedMatcher(list);
        Assert.assertTrue(matcher.getComposedMatchers().isEmpty(), "Empty list");

    }
    
    
    private class ComposedMatcher extends AbstractComposedMatcher {

        /**
         * Constructor.
         *
         * @param composedMatchers
         */
        public ComposedMatcher(Collection<AttributeValueMatcher> composedMatchers) {
            super(composedMatchers);
        }

        public Set<AttributeValue> getMatchingValues(Attribute attribute, AttributeFilterContext filterContext)
                throws AttributeFilteringException {
            return null;
        }
    }
    
    private class MyMatcher implements  AttributeValueMatcher, DestructableComponent, InitializableComponent {

        private boolean initialized;
        private boolean destroyed;
        
        public Set<AttributeValue> getMatchingValues(Attribute attribute, AttributeFilterContext filterContext)
                throws AttributeFilteringException {
            return null;
        }
        
        public boolean isInitialized() {
            return initialized;
        }

        public void initialize() throws ComponentInitializationException {
            initialized = true;
        }

        public boolean isDestroyed() {
            return destroyed;
        }

        public void destroy() {
            destroyed = true;
        }
    }
}
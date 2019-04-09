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

package net.shibboleth.idp.attribute.filter.matcher.impl;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Test For {@link AttributeValueStringMatcher}.
 */
public class AttributeValueStringMatcherTest {
    
    @Test public void testApply() throws ComponentInitializationException {
        AttributeValueStringMatcher matcher = new AttributeValueStringMatcher();
        matcher.setIgnoreCase(true);
        matcher.setMatchString(DataSources.TEST_STRING);
        matcher.setId("Test");
        matcher.initialize();
        
        assertTrue(matcher.compareAttributeValue(DataSources.STRING_VALUE));
        assertTrue(matcher.compareAttributeValue(DataSources.SCOPED_VALUE_VALUE_MATCH));
        assertFalse(matcher.compareAttributeValue(DataSources.SCOPED_VALUE_SCOPE_MATCH));
        assertFalse(matcher.compareAttributeValue(DataSources.BYTE_ATTRIBUTE_VALUE));
        assertFalse(matcher.compareAttributeValue(EmptyAttributeValue.NULL));
        assertFalse(matcher.compareAttributeValue(EmptyAttributeValue.ZERO_LENGTH));
        assertFalse(matcher.compareAttributeValue(null));
        assertTrue(matcher.compareAttributeValue(DataSources.OTHER_VALUE));
        
        AttributeValueStringMatcher nullMatcher = new AttributeValueStringMatcher();
        nullMatcher.setId("NullTest");
        nullMatcher.initialize();
        assertTrue(nullMatcher.compareAttributeValue(EmptyAttributeValue.NULL));
        assertFalse(nullMatcher.compareAttributeValue(EmptyAttributeValue.ZERO_LENGTH));
        assertFalse(nullMatcher.compareAttributeValue(DataSources.STRING_VALUE));

        AttributeValueStringMatcher emptyMatcher = new AttributeValueStringMatcher();
        emptyMatcher.setMatchString("");
        emptyMatcher.setId("EmptyTest");
        emptyMatcher.initialize();
        assertTrue(emptyMatcher.compareAttributeValue(EmptyAttributeValue.ZERO_LENGTH));
        assertFalse(emptyMatcher.compareAttributeValue(EmptyAttributeValue.NULL));
        assertFalse(emptyMatcher.compareAttributeValue(DataSources.STRING_VALUE));
    }

}

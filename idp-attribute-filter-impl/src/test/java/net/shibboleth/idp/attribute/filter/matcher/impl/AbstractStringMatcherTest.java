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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttributeValue;

/**
 * Tests for {@link AbstractStringMatcher}
 */
public class AbstractStringMatcherTest {

    @Test public void testSettersGetters() {
        AbstractStringMatcher matcher = new AbstractStringMatcher(){

            @Override
            protected boolean compareAttributeValue(IdPAttributeValue value) {
                return false;
            }};

        assertNull(matcher.getMatchString());
        assertFalse(matcher.isCaseSensitive());

        matcher.setCaseSensitive(true);
        assertTrue(matcher.isCaseSensitive());
        matcher.setCaseSensitive(false);
        assertFalse(matcher.isCaseSensitive());

        matcher.setMatchString(DataSources.TEST_STRING);
        assertEquals(matcher.getMatchString(), DataSources.TEST_STRING);
    }

    @Test public void testApply() {
        AbstractStringMatcher matcher = new AbstractStringMatcher() {

            @Override
            protected boolean compareAttributeValue(IdPAttributeValue value) {
                return false;
            }};
        matcher.setCaseSensitive(true);
        matcher.setMatchString(DataSources.TEST_STRING);

        assertTrue(matcher.stringCompare(DataSources.TEST_STRING));
        assertFalse(matcher.stringCompare(DataSources.TEST_STRING_UPPER));
        matcher.setCaseSensitive(false);
        assertTrue(matcher.stringCompare(DataSources.TEST_STRING));
        assertTrue(matcher.stringCompare(DataSources.TEST_STRING_UPPER));
        
        assertFalse(matcher.stringCompare(null));
    }

    @SuppressWarnings("deprecation")
    @Test public void testDeprecatedSettersGetters() {
        AbstractStringMatcher matcher = new AbstractStringMatcher(){

            @Override
            protected boolean compareAttributeValue(IdPAttributeValue value) {
                return false;
            }};

        Assert.assertNull(matcher.getMatchString());
        Assert.assertFalse(!matcher.isIgnoreCase());
        Assert.assertFalse(matcher.isCaseSensitive());

        matcher.setIgnoreCase(false);
        assertFalse(matcher.isIgnoreCase());
        matcher.setIgnoreCase(true);
        assertTrue(matcher.isIgnoreCase());

        matcher.setMatchString(DataSources.TEST_STRING);
        assertEquals(matcher.getMatchString(), DataSources.TEST_STRING);
    }

    @SuppressWarnings("deprecation")
    @Test public void testDeprecatedApply() {
        AbstractStringMatcher matcher = new AbstractStringMatcher() {

            @Override
            protected boolean compareAttributeValue(IdPAttributeValue value) {
                return false;
            }};
        matcher.setIgnoreCase(false);
        matcher.setMatchString(DataSources.TEST_STRING);

        assertTrue(matcher.stringCompare(DataSources.TEST_STRING));
        assertFalse(matcher.stringCompare(DataSources.TEST_STRING_UPPER));
        matcher.setIgnoreCase(true);
        assertTrue(matcher.stringCompare(DataSources.TEST_STRING));
        assertTrue(matcher.stringCompare(DataSources.TEST_STRING_UPPER));
        
        assertFalse(matcher.stringCompare(null));
    }


}

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

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link AbstractStringMatchFunctor}
 */
public class AbstractStringMatchFunctorTest {

    @Test public void testSettersGetters() {
        AbstractStringMatchFunctor matcher = new TestClass();

        Assert.assertNull(matcher.getMatchString());
        Assert.assertFalse(matcher.getCaseSensitive());

        matcher.setCaseSensitive(true);
        Assert.assertTrue(matcher.getCaseSensitive());
        matcher.setCaseSensitive(false);
        Assert.assertFalse(matcher.getCaseSensitive());

        matcher.setMatchString(DataSources.TEST_STRING);
        Assert.assertEquals(matcher.getMatchString(), DataSources.TEST_STRING);
    }

    @Test public void testApply() {
        AbstractStringMatchFunctor matcher = new TestClass() {};
        matcher.setCaseSensitive(true);
        matcher.setMatchString(DataSources.TEST_STRING);

        Assert.assertTrue(matcher.stringCompare(DataSources.TEST_STRING));
        Assert.assertFalse(matcher.stringCompare(DataSources.TEST_STRING_UPPER));
        matcher.setCaseSensitive(false);
        Assert.assertTrue(matcher.stringCompare(DataSources.TEST_STRING));
        Assert.assertTrue(matcher.stringCompare(DataSources.TEST_STRING_UPPER));
    }

    private class TestClass extends AbstractStringMatchFunctor {
        public boolean apply(@Nullable AttributeFilterContext input) {
            return false;
        }
    }
}

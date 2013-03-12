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

package net.shibboleth.idp.attribute.filtering.impl.predicate;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link BaseStringPredicate}
 */
public class BaseStringPredicateTest {
    

    @Test public void testSettersGetters() {
        BaseStringPredicate predicate = new BaseStringPredicate() {};
        
        Assert.assertNull(predicate.getMatchString());
        Assert.assertFalse(predicate.getCaseSensitive());
        
        predicate.setCaseSensitive(true);
        Assert.assertTrue(predicate.getCaseSensitive());
        predicate.setCaseSensitive(false);
        Assert.assertFalse(predicate.getCaseSensitive());
        
        predicate.setMatchString(DataSources.TEST_STRING);
        Assert.assertEquals(predicate.getMatchString(), DataSources.TEST_STRING);
    }
    
    @Test public void testApply() {
        BaseStringPredicate predicate = new BaseStringPredicate() {};
        predicate.setCaseSensitive(true);
        predicate.setMatchString(DataSources.TEST_STRING);

        Assert.assertTrue(predicate.apply(DataSources.TEST_STRING));
        Assert.assertFalse(predicate.apply(DataSources.TEST_STRING_UPPER));
        predicate.setCaseSensitive(false);
        Assert.assertTrue(predicate.apply(DataSources.TEST_STRING));
        Assert.assertTrue(predicate.apply(DataSources.TEST_STRING_UPPER));
        
        Assert.assertFalse(predicate.apply(new Integer(2)));
        
    }
    
}

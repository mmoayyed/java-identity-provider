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

package net.shibboleth.idp.attribute.resolver.impl.ad.mapped;

import java.util.regex.Pattern;

import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

/** test for {@link RegexValueMapping}. */
public class RegexValueMappingTest {
    
    // Constants from the 2.x IdP documentation
    private static final Pattern PATTERN = Pattern.compile("(.+), (.+)");
    private static final String RETURN_PATTERN = "$2 $1";
    private static final String MATCH_STRING = "Cantor, Scott";
    private static final String RESULT_STRING = "Scott Cantor";
    private static final String NO_MATCH_STRING = "Rod Widdowson";
    

    @Test public void TestConstructor() {
        try {
            new RegexValueMapping(null, RETURN_PATTERN);
            Assert.fail("null Pattern");
        } catch (ConstraintViolationException ex) {
            //OK
        }
        
        try {
            new RegexValueMapping(PATTERN, "");
            Assert.fail("empty Pattern");
        } catch (ConstraintViolationException ex) {
            //OK
        }

        try {
            new RegexValueMapping(PATTERN, null);
            Assert.fail("Null Pattern");
        } catch (ConstraintViolationException ex) {
            //OK
        }
    }
    
    @Test public void TestApply() {
        ValueMapping map = new RegexValueMapping(PATTERN, RETURN_PATTERN);
        try {
            map.apply(null);
            Assert.fail("Null Input");
        } catch (ConstraintViolationException ex) {
            //OK
        }
        Assert.assertEquals(map.apply(MATCH_STRING).get(), RESULT_STRING);
        Assert.assertFalse(map.apply(NO_MATCH_STRING).isPresent());
    }
}

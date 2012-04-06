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

import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

/** test for {@link ReturnDefaultIfAbsentValueMapping}. */
public class ReturnDefaultIfAbsentValueMappingTest {
    
    private static final String DEFAULT = "default";
    private static final String MATCHES = "offoobar";
    private static final String MATCHSTRING = "foo";    
    private static final String REPLACESTRING = "foo";    
    private static final String DOESNT_MATCH = "rabooffo"; 
    private static final ValueMapping TEST_MAPPER = new SubstringValueMapping(MATCHSTRING, false, REPLACESTRING);

    @Test public void TestConstructor() {
        try {
            new ReturnDefaultIfAbsentValueMapping(null, DEFAULT);
            Assert.fail("null Function");
        } catch (ConstraintViolationException ex) {
            //OK
        }
        
        try {
            new ReturnDefaultIfAbsentValueMapping(TEST_MAPPER, "");
            Assert.fail("empty returnValue");
        } catch (ConstraintViolationException ex) {
            //OK
        }

        try {
            new ReturnDefaultIfAbsentValueMapping(TEST_MAPPER, null);
            Assert.fail("Null returnValue");
        } catch (ConstraintViolationException ex) {
            //OK
        }
    }
    
    @Test public void TestApply() {
        ValueMapping map = new ReturnDefaultIfAbsentValueMapping(TEST_MAPPER, DEFAULT);
        try {
            map.apply(null);
            Assert.fail("Null Input");
        } catch (ConstraintViolationException ex) {
            //OK
        }
        Assert.assertEquals(map.apply(MATCHES).get(), REPLACESTRING);
        Assert.assertEquals(map.apply(DOESNT_MATCH).get(), DEFAULT);
    }
}

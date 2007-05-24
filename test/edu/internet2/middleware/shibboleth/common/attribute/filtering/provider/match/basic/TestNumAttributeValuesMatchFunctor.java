/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.basic;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.FilterProcessingException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.MatchFunctor;
import junit.framework.TestCase;

/**
 * test the @link(AnyMatchFunctor}
 */
public class TestNumAttributeValuesMatchFunctor extends BaseTestCase {

    public void setUp() throws Exception {
        super.setUp();
        matchFunctor = null;
    }
    
    public void testInRange() {
        //
        // In Base we set up "StringAttr" with 1 attribute and "IntAttr" with three.
        //
        performTest("InRange (1: 0-4)", new NumOfAttributeValuesMatchFunctor("StringAttr", 0, 4), true);
        performTest("InRange (1: 1-1)", new NumOfAttributeValuesMatchFunctor("StringAttr", 1, 1), true);
        performTest("InRange (3: 3-4)", new NumOfAttributeValuesMatchFunctor("IntegerAttr", 3, 4), true);
        performTest("InRange (3: 1-3)", new NumOfAttributeValuesMatchFunctor("IntegerAttr", 1, 3), true);
    }
    
    public void testOutOfRange() {
        //
        // In Base we set up "StringAttr" with 1 attribute and "IntAttr" with three.
        //
        performTest("OutRange (1: 2-4)", new NumOfAttributeValuesMatchFunctor("StringAttr", 2, 4), false);
        performTest("OutRange (1: 0-0)", new NumOfAttributeValuesMatchFunctor("StringAttr", 0, 0), false);
        performTest("OutRange (3: 0-3)", new NumOfAttributeValuesMatchFunctor("IntegerAttr", 0, 2), false);
        performTest("OutRange (3: 9-77)", new NumOfAttributeValuesMatchFunctor("IntegerAttr", 9, 77), false);
        
    }
}

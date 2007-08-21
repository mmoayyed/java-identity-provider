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

import java.util.ArrayList;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.MatchFunctor;

/**
 * test the @link(AnyMatchFunctor}.
 */
public class AndMatchFunctorTest extends BaseTestCase {

    /**
     *  Conatins the list of functors we continaully test.
     */
    private ArrayList<MatchFunctor> functors = new ArrayList<MatchFunctor>(3);

    /** {@inheritDoc} */
    public void setUp() throws Exception {
        super.setUp();
        matchFunctor = new AndMatchFunctor(functors);
    }
    
    /**
     * Test several possible configurations of and.
     */
    public void testAndFunction() {
        
        testBoth("null", new AndMatchFunctor(null), false);
        
        functors.clear();
        testBoth("Empty", false);
        
        //
        // And (TRUE)
        //
        functors.add(new AnyMatchFunctor());           
        testBoth("(TRUE)", true);
        
        //
        // And (TRUE, TRUE);
        //
        functors.add(new AnyMatchFunctor());           
        testBoth("(TRUE, TRUE)", true);
        
        //
        // And (TRUE, TRUE, TRUE);
        //
        functors.add(new AnyMatchFunctor());           
        testBoth("(TRUE, TRUE, TRUE)", true);

        //
        // And (TRUE, FALSE, TRUE);
        //
        functors.set(1, new NotMatchFunctor(new AnyMatchFunctor()));           
        testBoth("(TRUE, FALSE, TRUE)", false);

        //
        // And (TRUE, FALSE);
        //
        functors.remove(2);           
        testBoth("(TRUE, FALSE)", false);
        
        //
        // And (FALSE)
        //
        functors.remove(0);           
        testBoth("(FALSE)", false);
            
    }

}

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
 * test the @link(AnyMatchFunctor}
 */
public class OrMatchFunctorTest extends BaseTestCase {

    private ArrayList<MatchFunctor> functors = new ArrayList<MatchFunctor>(3);

    public void setUp() throws Exception {
        super.setUp();
        matchFunctor = new OrMatchFunctor(functors);
    }
    
    public void testOrFunction() {
        functors.clear();
        
        //
        // Or (TRUE)
        //
        functors.add(new AnyMatchFunctor());           
        testBoth("(TRUE)", true);
        
        //
        // Or(TRUE, TRUE);
        //
        functors.add(new AnyMatchFunctor());           
        testBoth("(TRUE, TRUE)", true);
        
        //
        // Or (TRUE, TRUE, TRUE);
        //
        functors.add(new AnyMatchFunctor());           
        testBoth("(TRUE, TRUE, TRUE)", true);

        //
        // And (TRUE, TRUE, FALSE);
        //
        functors.set(2, new NotMatchFunctor(new AnyMatchFunctor()));           
        testBoth("(TRUE, TRUE, FALSE)", true);

        //
        // Or (FALSE, FALSE, FALSE);
        //
        functors.set(0, new NotMatchFunctor(new AnyMatchFunctor()));           
        functors.set(1, new NotMatchFunctor(new AnyMatchFunctor()));           
        testBoth("(FALSE, FALSE, FALSE)", false);
        
        //
        // Or (FALSE)
        //
        functors.remove(0);           
        functors.remove(0);           
        testBoth("(FALSE)", false);
            
    }

}

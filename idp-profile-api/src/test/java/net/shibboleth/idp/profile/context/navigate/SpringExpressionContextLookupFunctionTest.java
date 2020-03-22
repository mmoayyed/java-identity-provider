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

package net.shibboleth.idp.profile.context.navigate;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for {@link SpringExpressionContextLookupFunction}.
 */
@SuppressWarnings("javadoc")
public class SpringExpressionContextLookupFunctionTest {

    
    @Test public void simpleTest() {
        SpringExpressionContextLookupFunction<ProfileRequestContext,Integer> func =
                new SpringExpressionContextLookupFunction<>(ProfileRequestContext.class, "99", Integer.class);
        Assert.assertEquals(func.apply(null), Integer.valueOf(99));
    }
    
    @Test public void customTest() {
        SpringExpressionContextLookupFunction<ProfileRequestContext,Integer> func =
                new SpringExpressionContextLookupFunction<>(ProfileRequestContext.class, "#custom + 1", Integer.class);
        func.setCustomObject(Integer.valueOf(99));
        Assert.assertEquals(func.apply(null), Integer.valueOf(100));
    }    
    
    @Test public void invalidOutputTest() {
        SpringExpressionContextLookupFunction<ProfileRequestContext,Integer> func =
                new SpringExpressionContextLookupFunction<>(ProfileRequestContext.class, "'foo'", Integer.class);
        func.setReturnOnError(-1);
        Assert.assertEquals(func.apply(null), Integer.valueOf(-1));
    }
    
    @Test public void exceptionTest() {
        SpringExpressionContextLookupFunction<ProfileRequestContext,Integer> func =
                new SpringExpressionContextLookupFunction<>(ProfileRequestContext.class, "1/0", Integer.class);
        func.setReturnOnError(-1);

        try {
            func.apply(null);
            Assert.fail("Expression should have raised exception");
        } catch (final Exception e) {
            
        }
        
        func.setHideExceptions(true);
        Assert.assertEquals(func.apply(null), Integer.valueOf(-1));
    }

}
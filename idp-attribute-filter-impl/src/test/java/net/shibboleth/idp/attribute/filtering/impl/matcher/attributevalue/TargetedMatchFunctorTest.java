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

package net.shibboleth.idp.attribute.filtering.impl.matcher.attributevalue;

import net.shibboleth.idp.attribute.filtering.impl.matcher.AbstractValueMatcherFunctor;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.testng.Assert;

/**
 * Tests for implementations of {@link TargetedMatchFunctor}.
 */
public class TargetedMatchFunctorTest {

    private static final String NAME = "foo";
    
    protected void setterGetterTest(TargetedMatchFunctor functor) throws ComponentInitializationException {
        
        Assert.assertNull(functor.getAttributeId());
        
        functor.setAttributeId(NAME);
        Assert.assertEquals(functor.getAttributeId(), NAME);
        Assert.assertNotEquals(functor.getAttributeId(), NAME.toUpperCase());
        
        if (functor instanceof AbstractValueMatcherFunctor) {
            AbstractValueMatcherFunctor id = (AbstractValueMatcherFunctor) functor;
            id.setId("Test");
        }

        functor.initialize();
        try {
            functor.setAttributeId(NAME);
            Assert.fail();
        } catch (UnmodifiableComponentException e) {
            // OK
        }
        
    }
}

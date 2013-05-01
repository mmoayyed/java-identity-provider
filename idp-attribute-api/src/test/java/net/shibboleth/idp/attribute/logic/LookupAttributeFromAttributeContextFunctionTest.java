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

package net.shibboleth.idp.attribute.logic;

import java.util.Arrays;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.opensaml.messaging.context.BaseContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Function;

/** Test for {@link LookupAttributeFromAttributeContextFunction}. */
public class LookupAttributeFromAttributeContextFunctionTest {

    private static final String NAME_ONE = "one";

    private static final String NAME_TWO = "two";

    private static final String NAME_THREE = "three";

    @Test public void lookupAttributeFromAttributeContextFunction() {

        final Attribute attrOne = new Attribute(NAME_ONE);
        final Attribute attrTwo = new Attribute(NAME_TWO);

        final AttributeContext attributeChildContext = new AttributeContext();
        attributeChildContext.setAttributes(Arrays.asList(attrOne, attrTwo));

        final BaseContext parent = new BaseContext() {};
        parent.addSubcontext(attributeChildContext);

        final BaseContext noneAtChildContext = new BaseContext() {};
        parent.addSubcontext(noneAtChildContext);

        final BaseContext grandChild = new BaseContext() {};
        noneAtChildContext.addSubcontext(grandChild);

        try {
            new LookupAttributeFromAttributeContextFunction(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // OK
        }

        try {
            new LookupAttributeFromAttributeContextFunction("");
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // OK
        }

        Function<BaseContext, Attribute> func = AttributeLogicSupport.lookupAttributeFromAttributeContext(NAME_ONE);
        try {
            func.apply(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // OK
        }

        Assert.assertNull(func.apply(parent));
        Assert.assertEquals(func.apply(attributeChildContext), attrOne);
        Assert.assertEquals(func.apply(noneAtChildContext), attrOne);
        Assert.assertEquals(func.apply(grandChild), attrOne);

        func = AttributeLogicSupport.lookupAttributeFromAttributeContext(NAME_THREE);

        Assert.assertNull(func.apply(noneAtChildContext));
        Assert.assertNull(func.apply(attributeChildContext));
        Assert.assertNull(func.apply(parent));

    }
}

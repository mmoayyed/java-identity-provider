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

package net.shibboleth.idp.attribute;

import java.util.Arrays;
import java.util.Collections;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link AttributeContext} class. */

public class AttributeContextTest {
    
    /** 
     * Test that the attributes from the supplied context cannot be modified
     * and that there as many as we expected. 
     */
    private void contextAttributes( AttributeContext context, int expectedSize) {
        Assert.assertEquals(context.getAttributes().size(), expectedSize);
        try {
            context.getAttributes().put("attr", new Attribute("attr") );
            Assert.fail();
        } catch (UnsupportedOperationException e) {

        }
    }
    
    @Test public void attributeContext() {
        AttributeContext context = new AttributeContext();
        
        context.setAttributes(Arrays.asList((Attribute)null, null));
        contextAttributes(context, 0);

        context.setAttributes(Arrays.asList(new Attribute("foo"), null));
        contextAttributes(context, 1);
        
        context.setAttributes(null);
        contextAttributes(context, 0);
        
        context.setAttributes(Collections.EMPTY_SET);
        contextAttributes(context, 0);
    }

}

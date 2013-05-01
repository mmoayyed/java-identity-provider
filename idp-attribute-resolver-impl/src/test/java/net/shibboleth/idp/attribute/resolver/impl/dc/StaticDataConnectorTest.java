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

package net.shibboleth.idp.attribute.resolver.impl.dc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.impl.dc.StaticDataConnector;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/** Tests for {@link StaticDataConnector}
 *
 */
public class StaticDataConnectorTest {

    @Test public void resolve() throws ComponentInitializationException, ResolutionException {
        Attribute attribute = new Attribute("attribute");
        attribute.setValues(Lists.newArrayList((AttributeValue) new StringAttributeValue("one"), new StringAttributeValue("two")));

        StaticDataConnector connector = new StaticDataConnector();
        connector.setId("Static");

        Assert.assertFalse(connector.getAttributes().isPresent());
        connector.setValues(null);
        Assert.assertFalse(connector.getAttributes().isPresent());

        try {
            connector.initialize();
            Assert.fail();
        } catch (ComponentInitializationException e) {
            //OK
        }
        
        List<Attribute> input = new ArrayList<Attribute>();
        connector.setValues(input);
        Assert.assertTrue(connector.getAttributes().isPresent());

        input.add(null);
        connector.setValues(input);
        Assert.assertTrue(connector.getAttributes().isPresent());
        

        input.add(attribute);
        input.add(null);
        input.add(new Attribute("thingy"));
        
        connector.setValues(input);
        connector.initialize();

        Assert.assertTrue(connector.getAttributes().isPresent());
        Assert.assertEquals(connector.getAttributes().get().size(), 2);

        AttributeResolutionContext context = new AttributeResolutionContext();
        Optional<Map<String, Attribute>> result = connector.doResolve(context);

        Assert.assertEquals(result.get().size(), 2);
        Assert.assertTrue(result.get().containsKey("attribute"));
        Assert.assertTrue(result.get().containsKey("thingy"));

    }
    
    @Test public void initDestroy() throws ComponentInitializationException, ResolutionException {

        StaticDataConnector connector = new StaticDataConnector();
        connector.setId("Static");

        List<Attribute> input = new ArrayList<Attribute>();
        input.add(null);
        connector.setValues(input);
        
        try {
            connector.resolve(new AttributeResolutionContext());
            Assert.fail();
        } catch (UninitializedComponentException e) {
            //OK
        }

        input.add(new Attribute("thingy"));
        
        connector.setValues(input);
        connector.initialize();

        try {
            connector.setValues(Collections.singletonList(new Attribute("whatever")));
            Assert.fail();
        } catch (UnmodifiableComponentException ex) {
            // OK
        }

        connector.destroy();

        try {
            connector.doDataConnectorResolve(new AttributeResolutionContext());
            Assert.fail();
        } catch (DestroyedComponentException e) {
            //OK
        }
    }

}

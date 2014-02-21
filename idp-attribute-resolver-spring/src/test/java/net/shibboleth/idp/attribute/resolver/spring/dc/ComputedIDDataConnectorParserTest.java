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

package net.shibboleth.idp.attribute.resolver.spring.dc;

import net.shibboleth.idp.attribute.resolver.impl.dc.ComputedIDDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * test for {@link ComputedIDDataConnectorParser}
 */
public class ComputedIDDataConnectorParserTest extends BaseAttributeDefinitionParserTest {
    
    @Test public void withSalt() throws ComponentInitializationException {
        ComputedIDDataConnector connector = getDataConnector("computed.xml", ComputedIDDataConnector.class);
        
        Assert.assertEquals(connector.getId(), "computed");
        Assert.assertEquals(connector.getSourceAttributeId(), "theSourceRemainsTheSame");
        Assert.assertEquals(connector.getGeneratedAttributeId(), "jenny");
        Assert.assertEquals(connector.getSalt(), "abcdefghijklmnopqrst".getBytes());

        connector.initialize();
    }

    @Test(enabled=false) public void withoutSalt()  {
        ComputedIDDataConnector connector = getDataConnector("computedNoSalt.xml", ComputedIDDataConnector.class, true);
        
        Assert.assertEquals(connector.getId(), "computedNoSalt");
        Assert.assertEquals(connector.getSourceAttributeId(), "theSourceRemainsDifferent");
        Assert.assertEquals(connector.getGeneratedAttributeId(), "computedId");
        Assert.assertNull(connector.getSalt());

        try {
            connector.initialize();
            Assert.fail();
        } catch (ComponentInitializationException e) {
            // OK
        }
    }

}

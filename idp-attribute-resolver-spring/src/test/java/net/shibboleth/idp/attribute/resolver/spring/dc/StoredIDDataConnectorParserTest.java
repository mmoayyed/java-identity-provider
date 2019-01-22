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

import org.bouncycastle.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.resolver.ResolverAttributeDefinitionDependency;
import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.StoredIDDataConnectorParser;
import net.shibboleth.idp.saml.attribute.resolver.impl.StoredIDDataConnector;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * test for {@link StoredIDDataConnectorParser}
 */
public class StoredIDDataConnectorParserTest extends BaseAttributeDefinitionParserTest {
    
    private void testIt(final StoredIDDataConnector connector) throws ComponentInitializationException {
        Assert.assertEquals(connector.getId(), "stored");
        Assert.assertEquals(connector.getGeneratedAttributeId(), "jenny");
        Assert.assertEquals(connector.getTransactionRetries(), 5);
        Assert.assertEquals(connector.getQueryTimeout(), 5000);
        Assert.assertEquals(connector.getFailFast(), false);
        Assert.assertTrue(Arrays.areEqual(connector.getRetryableErrors().toArray(), new String[]{"25000", "25001"}));
        
        connector.initialize();
    }
    
    @Test public void withSalt() throws ComponentInitializationException {
        final StoredIDDataConnector connector = getDataConnector("resolver/stored.xml", StoredIDDataConnector.class);
        
        final ResolverAttributeDefinitionDependency attrib = connector.getAttributeDependencies().iterator().next();
        Assert.assertEquals(attrib.getDependencyPluginId(), "theSourceRemainsTheSame");
        Assert.assertEquals(connector.getSalt(), "abcdefghijklmnopqrst".getBytes());
        testIt(connector);
    }

    @Test public void withOutSalt() throws ComponentInitializationException {
        final StoredIDDataConnector connector = getDataConnector("resolver/storedNoSalt.xml", StoredIDDataConnector.class);
        final ResolverAttributeDefinitionDependency attrib = connector.getAttributeDependencies().iterator().next();
        Assert.assertEquals(attrib.getDependencyPluginId(), "theSourceRemainsTheSame");
        testIt(connector);
    }
}
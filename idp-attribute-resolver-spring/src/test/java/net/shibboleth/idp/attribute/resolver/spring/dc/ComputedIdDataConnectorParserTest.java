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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Collection;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;
import org.testng.annotations.Test;

import net.shibboleth.ext.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.idp.attribute.impl.ComputedPairwiseIdStore;
import net.shibboleth.idp.attribute.impl.ComputedPairwiseIdStore.Encoding;
import net.shibboleth.idp.attribute.resolver.dc.impl.PairwiseIdDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.ComputedIdDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.testing.BaseAttributeDefinitionParserTest;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Test for {@link ComputedIdDataConnectorParser}.
 */
@SuppressWarnings("javadoc")
public class ComputedIdDataConnectorParserTest extends BaseAttributeDefinitionParserTest {
    
    @Test public void withSalt() throws ComponentInitializationException {
        final PairwiseIdDataConnector connector = getDataConnector("resolver/computed.xml", PairwiseIdDataConnector.class);
        final ComputedPairwiseIdStore store = (ComputedPairwiseIdStore) connector.getPairwiseIdStore();
        
        assertEquals(connector.getId(), "computed");
        assertEquals(connector.getGeneratedAttributeId(), "jenny");
        assertEquals(store.getSalt(), "abcdefghijklmnopqrst ".getBytes());
        assertEquals(connector.getSourceAttributeInformation(), "theSourceRemainsTheSame");
        assertEquals(store.getAlgorithm(), "SHA256");
        assertEquals(store.getEncoding(), Encoding.BASE32);

        assertTrue(connector.isInitialized());
    }

    @Test public void resolverDataConnector() throws ComponentInitializationException {
        final PairwiseIdDataConnector connector = getDataConnector("resolver/computedDataConnector.xml", PairwiseIdDataConnector.class);
        final ComputedPairwiseIdStore store = (ComputedPairwiseIdStore) connector.getPairwiseIdStore();
        assertFalse(connector.isExportAllAttributes());
        final Collection<String> exports = connector.getExportAttributes();
        assertEquals(exports.size(), 2);
        assertTrue(exports.contains("Joe"));
        assertTrue(exports.contains("Doe"));
        assertEquals(connector.getId(), "computed");
        assertEquals(connector.getGeneratedAttributeId(), "jenny");
        assertEquals(store.getSalt(), "abcdefghijklmnopqrst ".getBytes());
        assertEquals(connector.getSourceAttributeInformation(), "DC/theSourceRemainsTheSame");

        assertTrue(connector.isInitialized());
}

    @Test public void resolverNoSourceDependency() {
        final PairwiseIdDataConnector connector = getDataConnector("resolver/computedNoSource1.xml", PairwiseIdDataConnector.class);
        final ComputedPairwiseIdStore store = (ComputedPairwiseIdStore) connector.getPairwiseIdStore();
        
        assertEquals(connector.getId(), "computed");
        assertEquals(connector.getGeneratedAttributeId(), "jenny");
        assertEquals(store.getSalt(), "abcdefghijklmnopqrst ".getBytes());
        assertEquals(connector.getSourceAttributeInformation(), "theSourceRemainsTheSame");

    }

    @Test public void propertySalt()  {
        final String salt = "0123456789ABCDEF ";

        final MockPropertySource mockEnvVars = new MockPropertySource();
        mockEnvVars.setProperty("the.ComputedIDDataConnector.salt", salt);

        final GenericApplicationContext context = new FilesystemGenericApplicationContext() ;
        setTestContext(context);
        context.setDisplayName("ApplicationContext");

        final MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
        propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockEnvVars);

        final PropertySourcesPlaceholderConfigurer placeholderConfig = new PropertySourcesPlaceholderConfigurer();
        placeholderConfig.setPlaceholderPrefix("%{");
        placeholderConfig.setPlaceholderSuffix("}");
        placeholderConfig.setPropertySources(propertySources);

        context.addBeanFactoryPostProcessor(placeholderConfig);

        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions(DATACONNECTOR_FILE_PATH + "resolver/computedProperty.xml");

        
        beanDefinitionReader.setValidating(true);

        context.refresh();
       
        final PairwiseIdDataConnector connector =  context.getBean(PairwiseIdDataConnector.class);
        final ComputedPairwiseIdStore store = (ComputedPairwiseIdStore) connector.getPairwiseIdStore();
        
        assertEquals(store.getSalt(), salt.getBytes());
        assertTrue(store.isInitialized());
        assertTrue(connector.isInitialized());
    }

}

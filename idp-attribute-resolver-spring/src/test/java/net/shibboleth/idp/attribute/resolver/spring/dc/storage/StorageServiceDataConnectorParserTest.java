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

package net.shibboleth.idp.attribute.resolver.spring.dc.storage;

import static org.testng.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.opensaml.storage.StorageService;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockPropertySource;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

import net.shibboleth.ext.spring.resource.PreferFileSystemResourceLoader;
import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.NoResultAnErrorResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.storage.impl.StorageServiceDataConnector;
import net.shibboleth.idp.saml.impl.testing.TestSources;

/** Test for {@link StorageServiceDataConnectorParser}. */
@SuppressWarnings("javadoc")
public class StorageServiceDataConnectorParserTest {

    private static final String SCRIPT_PATH = "/net/shibboleth/idp/attribute/resolver/impl/dc/storage/";
    
    private GenericApplicationContext pendingTeardownContext = null;
    
    @AfterMethod public void tearDownTestContext() {
        if (null == pendingTeardownContext ) {
            return;
        }
        pendingTeardownContext.close();
        pendingTeardownContext = null;
    }
    
    private void setTestContext(final GenericApplicationContext context) {
        tearDownTestContext();
        pendingTeardownContext = context;
    }
    
    @Test public void v2Simple() throws Exception {
        
        final StorageServiceDataConnector connector =
                getDataConnector(null,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/storage/storage-attribute-resolver-v2-simple.xml");
        assertNotNull(connector);

        pendingTeardownContext.getBean(StorageService.class).create("foo", "bar", "testdata", null);

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        final Map<String,IdPAttribute> attrs = connector.resolve(context);

        assertEquals(attrs.size(), 1);
        
        assertEquals(attrs.get("test").getValues().size(), 1);
        assertEquals(((StringAttributeValue)attrs.get("test").getValues().get(0)).getValue(), "testdata");
        
        assertTrue(connector.getResultsCache().size() == 1);
    }    
    
    @Test public void v2Config() throws Exception {
        
        final MockPropertySource propSource = singletonPropertySource("context", "foo");
        propSource.setProperty("key", "bar");
        propSource.setProperty("scriptPath", (SCRIPT_PATH) + "test.js");
        
        final StorageServiceDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/storage/storage-attribute-resolver-v2.xml");
        assertNotNull(connector);

        pendingTeardownContext.getBean(StorageService.class).create("foo", "bar",
                Files.streamToString(getClass().getResourceAsStream(SCRIPT_PATH + "test.json")), null);

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        final Map<String,IdPAttribute> attrs = connector.resolve(context);

        assertEquals(attrs.size(), 2);
        
        assertEquals(attrs.get("foo").getValues().size(), 1);
        assertEquals(((StringAttributeValue)attrs.get("foo").getValues().get(0)).getValue(), "foo1");
        
        assertEquals(attrs.get("bar").getValues().size(), 2);
        assertEquals(((StringAttributeValue)attrs.get("bar").getValues().get(0)).getValue(), "bar1");
        assertEquals(((StringAttributeValue)attrs.get("bar").getValues().get(1)).getValue(), "bar2");
        
        assertTrue(connector.getResultsCache().size() == 1);
    }

     @Test(expectedExceptions=NoResultAnErrorResolutionException.class) public void v2Missing() throws Exception {
        
        final MockPropertySource propSource = singletonPropertySource("context", "foo");
        propSource.setProperty("key", "baz");
        propSource.setProperty("scriptPath", (SCRIPT_PATH) + "test.js");
        propSource.setProperty("missingerror", "true");
        
        final StorageServiceDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/storage/storage-attribute-resolver-v2.xml");
        assertNotNull(connector);
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        connector.resolve(context);
    }
    
    @Test public void v2MissingOk() throws Exception {
        
        final MockPropertySource propSource = singletonPropertySource("context", "foo");
        propSource.setProperty("key", "baz");
        propSource.setProperty("scriptPath", (SCRIPT_PATH) + "test.js");
        
        final StorageServiceDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/storage/storage-attribute-resolver-v2.xml");
        assertNotNull(connector);
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        final Map<String,IdPAttribute> attrs = connector.resolve(context);

        assertTrue(attrs.isEmpty());
    }
    
    @Test public void hybridConfig() throws Exception {
        final MockPropertySource propSource = singletonPropertySource("context", "foo");
        propSource.setProperty("key", "bar");
        propSource.setProperty("scriptPath", (SCRIPT_PATH) + "test.js");
        
        final StorageServiceDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/storage/storage-attribute-resolver-v2-hybrid.xml",
                        "net/shibboleth/idp/attribute/resolver/spring/dc/storage/storage-attribute-resolver-spring-context.xml");
        assertNotNull(connector);

        pendingTeardownContext.getBean(StorageService.class).create("foo", "bar",
                Files.streamToString(getClass().getResourceAsStream(SCRIPT_PATH + "test.json")), null);

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        final Map<String,IdPAttribute> attrs = connector.resolve(context);

        assertEquals(attrs.size(), 2);
        
        assertEquals(attrs.get("foo").getValues().size(), 1);
        assertEquals(((StringAttributeValue)attrs.get("foo").getValues().get(0)).getValue(), "foo1");
        
        assertEquals(attrs.get("bar").getValues().size(), 2);
        assertEquals(((StringAttributeValue)attrs.get("bar").getValues().get(0)).getValue(), "bar1");
        assertEquals(((StringAttributeValue)attrs.get("bar").getValues().get(1)).getValue(), "bar2");
        
        assertTrue(connector.getResultsCache().size() == 1);
    }
    
    private StorageServiceDataConnector getDataConnector(final PropertySource<?> propSource, final String... beanDefinitions)
            throws IOException {

        final ResourceLoader loader = new PreferFileSystemResourceLoader();
        
        final ApplicationContextBuilder builder = new ApplicationContextBuilder();
        builder.setName("ApplicationContext: " + StorageServiceDataConnectorParserTest.class);

        final Collection<String> defs = new ArrayList<>(Arrays.asList(beanDefinitions));
        defs.add("net/shibboleth/idp/attribute/resolver/spring/dc/storage/spring-beans.xml");

        builder.setServiceConfigurations(defs.stream().map(s -> loader.getResource(s)).collect(Collectors.toList()));

        if (propSource != null) {
            builder.setPropertySources(Collections.singletonList(propSource));
        }
        
        final GenericApplicationContext context = builder.build();

        setTestContext(context);
                
        return (StorageServiceDataConnector) context.getBean("myStorage");
    }

    private MockPropertySource singletonPropertySource(final String name, final String value) {
        final MockPropertySource propSource = new MockPropertySource("localProperties");
        propSource.setProperty(name, value);
        return propSource;
    }

}

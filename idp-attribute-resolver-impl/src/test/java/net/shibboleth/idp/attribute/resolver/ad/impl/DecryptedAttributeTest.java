/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http:www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.resolver.ad.impl;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue.EmptyType;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolverAttributeDefinitionDependency;
import net.shibboleth.idp.attribute.resolver.ResolverDataConnectorDependency;
import net.shibboleth.idp.attribute.resolver.ResolverTestSupport;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImpl;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImplTest;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resource.Resource;
import net.shibboleth.utilities.java.support.resource.TestResourceConverter;
import net.shibboleth.utilities.java.support.security.DataSealer;
import net.shibboleth.utilities.java.support.security.DataSealerException;
import net.shibboleth.utilities.java.support.security.impl.BasicKeystoreKeyStrategy;

/** Test for {@link DecryptedAttributeDefinition}. */
public class DecryptedAttributeTest {

    private static final String TEST_ATTRIBUTE_NAME = "decrypted";

    private DataSealer dataSealer;

    @BeforeClass public void setUp() throws ComponentInitializationException {
        ClassPathResource resource =
                new ClassPathResource("net/shibboleth/idp/attribute/resolver/impl/ad/SealerKeyStore.jks");
        final Resource keystoreResource = TestResourceConverter.of(resource);

        resource = new ClassPathResource("net/shibboleth/idp/attribute/resolver/impl/ad/SealerKeyStore.kver");
        final Resource versionResource = TestResourceConverter.of(resource);

        final BasicKeystoreKeyStrategy strategy = new BasicKeystoreKeyStrategy();
        
        strategy.setKeyAlias("secret");
        strategy.setKeyPassword("kpassword");

        strategy.setKeystorePassword("password");
        strategy.setKeystoreResource(keystoreResource);
        
        strategy.setKeyVersionResource(versionResource);

        strategy.initialize();
        
        dataSealer = new DataSealer();
        dataSealer.setKeyStrategy(strategy);
        dataSealer.initialize();
    }
    
    /**
     * Test resolution of an empty definition to nothing.
     * 
     * @throws ResolutionException if resolution failed.
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     */
    @Test public void empty() throws ResolutionException, ComponentInitializationException {
        final DecryptedAttributeDefinition decrypted = new DecryptedAttributeDefinition();
        decrypted.setId(TEST_ATTRIBUTE_NAME);
        try {
            decrypted.initialize();
            fail("no dependencies");
        } catch (final ComponentInitializationException e) {
            
        }
        decrypted.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("foo", "bar")));

        try {
            decrypted.initialize();
            fail("no DataSealer");
        } catch (final ComponentInitializationException e) {
            
        }
        
        decrypted.setDataSealer(dataSealer);
        decrypted.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        context.getSubcontext(AttributeResolverWorkContext.class, true);
        final IdPAttribute result = decrypted.resolve(context);

        assertTrue(result.getValues().isEmpty());
    }

    /**
     * Test when dependent on a data connector.
     * 
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     * @throws DataSealerException 
     */
    @Test public void dataConnector() throws ComponentInitializationException, DataSealerException {

        // Set the dependency on the data connector
        final DecryptedAttributeDefinition decrypted = new DecryptedAttributeDefinition();
        decrypted.setId(TEST_ATTRIBUTE_NAME);

        final Set<ResolverDataConnectorDependency> dependencySet = new LazySet<>();
        dependencySet.add(TestSources.makeDataConnectorDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR));
        decrypted.setDataConnectorDependencies(dependencySet);
        decrypted.setDataSealer(dataSealer);
        decrypted.initialize();

        // Generate encrypted data.
        final IdPAttribute attr1 = new IdPAttribute(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR);
        attr1.setValues(List.of(new StringAttributeValue(dataSealer.wrap(TestSources.COMMON_ATTRIBUTE_VALUE_STRING)),
                new StringAttributeValue(dataSealer.wrap(TestSources.CONNECTOR_ATTRIBUTE_VALUE_STRING))));

        final IdPAttribute attr2 = new IdPAttribute(TestSources.DEPENDS_ON_SECOND_ATTRIBUTE_NAME);
        attr2.setValues(List.of(new StringAttributeValue(dataSealer.wrap(TestSources.SECOND_ATTRIBUTE_VALUE_STRINGS[0])),
                new StringAttributeValue(dataSealer.wrap(TestSources.SECOND_ATTRIBUTE_VALUE_STRINGS[1]))));
        
        // And resolve
        final Set<DataConnector> connectorSet = new LazySet<>();
        connectorSet.add(TestSources.populatedStaticConnector(List.of(attr1,attr2)));

        final Set<AttributeDefinition> attributeSet = new LazySet<>();
        attributeSet.add(decrypted);

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", attributeSet, connectorSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
        } catch (final ResolutionException e) {
            fail("resolution failed", e);
        }

        final Collection<?> values = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME).getValues();
        assertEquals(values.size(), 2);
        assertTrue(values.contains(TestSources.COMMON_ATTRIBUTE_VALUE_RESULT), "looking for " + TestSources.COMMON_ATTRIBUTE_VALUE_STRING);
        assertTrue(values.contains(TestSources.CONNECTOR_ATTRIBUTE_VALUE_RESULT),
                "looking for " + TestSources.CONNECTOR_ATTRIBUTE_VALUE_STRING);
    }

    /**
     * Test when dependent on another attribute.
     * 
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     * @throws DataSealerException 
     */
    @Test public void attribute() throws ComponentInitializationException, DataSealerException {

        final DecryptedAttributeDefinition decrypted = new DecryptedAttributeDefinition();
        decrypted.setId(TEST_ATTRIBUTE_NAME);

        final Set<ResolverAttributeDefinitionDependency> dependencySet = new LazySet<>();
        dependencySet.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        decrypted.setAttributeDependencies(dependencySet);
        decrypted.setDataSealer(dataSealer);
        decrypted.initialize();

        final Set<AttributeDefinition> am = new LazySet<>();
        am.add(decrypted);
        
        final IdPAttribute dependency = new IdPAttribute(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);
        dependency.setValues(List.of(new StringAttributeValue(dataSealer.wrap(TestSources.COMMON_ATTRIBUTE_VALUE_STRING)),
                new StringAttributeValue(dataSealer.wrap(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_STRING))));
        am.add(TestSources.populatedStaticAttribute(dependency));

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", am, null);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
        } catch (final ResolutionException e) {
            fail("resolution failed", e);
        }
        final Collection<IdPAttributeValue> values = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        assertEquals(values.size(), 2);
        assertTrue(values.contains(TestSources.COMMON_ATTRIBUTE_VALUE_RESULT),
                "looking for value " + TestSources.COMMON_ATTRIBUTE_VALUE_STRING);
        assertTrue(values.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_RESULT),
                "looking for value " + TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_STRING);
    }
    
    /**
     * Test resolution of an empty definition to nothing.
     * 
     * @throws ResolutionException if resolution failed.
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     */
    @Test public void nullValue() throws ResolutionException, ComponentInitializationException {
        final List<IdPAttributeValue> values = new ArrayList<>(3);
        values.add(TestSources.COMMON_ATTRIBUTE_VALUE_RESULT);
        values.add(new EmptyAttributeValue(EmptyType.NULL_VALUE));
        final IdPAttribute attr = new IdPAttribute(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);

        attr.setValues(values);

        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1", attr));
        final ResolverDataConnectorDependency depend = TestSources.makeDataConnectorDependency("connector1", TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);

        final DecryptedAttributeDefinition decrypted = new DecryptedAttributeDefinition();
        decrypted.setId(TEST_ATTRIBUTE_NAME);
        decrypted.setDataConnectorDependencies(Collections.singleton(depend));
        decrypted.setDataSealer(dataSealer);
        decrypted.initialize();

        final IdPAttribute result = decrypted.resolve(resolutionContext);

        final List<IdPAttributeValue> outValues = result.getValues();
        assertEquals(outValues.size(), 1);
        assertFalse(outValues.contains(TestSources.COMMON_ATTRIBUTE_VALUE_RESULT));
        assertTrue(outValues.contains(new EmptyAttributeValue(EmptyType.NULL_VALUE)));
    }
    
}
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

package net.shibboleth.idp.attribute.filter.matcher.saml.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.metadata.resolver.filter.impl.NodeProcessingMetadataFilter;
import org.opensaml.saml.metadata.resolver.impl.ResourceBackedMetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.ext.spring.resource.ResourceHelper;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.saml.metadata.impl.ScopesNodeProcessor;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * Tests for {@link AttributeScopeMatchesShibMDScope} and {@link AttributeValueMatchesShibMDScope} matchers.
 */
public class ScopeMatchesShibMDScopeTests extends XMLObjectBaseTestCase {

    private final static String bothEntityID = "https://both.example.org/idp/shibboleth";
    private final static String noneEntityID = "https://none.example.org/idp/shibboleth";
    private final static String aaEntityID = "https://aa.example.org/idp/shibboleth";
    private final static String entityEntityID = "https://entity.example.org/idp/shibboleth";
    
    ResourceBackedMetadataResolver resolver;
    
    private final AttributeScopeMatchesShibMDScope scopeMatcher = new AttributeScopeMatchesShibMDScope();
    private final AttributeValueMatchesShibMDScope valueMatcher = new AttributeValueMatchesShibMDScope();
    
    
    @BeforeClass public void setup() throws IOException, ComponentInitializationException {
        scopeMatcher.setId("scopeMatcher");
        scopeMatcher.initialize();
        valueMatcher.setId("valueMatcher");
        valueMatcher.initialize();

        NodeProcessingMetadataFilter filter = new NodeProcessingMetadataFilter();
        filter.setNodeProcessors(List.of(new ScopesNodeProcessor()));
        filter.initialize();
        
        resolver = 
                new ResourceBackedMetadataResolver(ResourceHelper.of(
                        new ClassPathResource("/net/shibboleth/idp/filter/impl/saml/shibmd-metadata.xml")));
        resolver.setMetadataFilter(filter);
        resolver.setId("resolver");
        resolver.setParserPool(parserPool);
        resolver.initialize();
    }
    
    private AttributeFilterContext filterContextFor(final EntityDescriptor entity) {
        final SAMLMetadataContext metadataContext = new SAMLMetadataContext();
        metadataContext.setEntityDescriptor(entity);
        metadataContext.setRoleDescriptor(entity.getRoleDescriptors().get(0));
        
        final AttributeFilterContext result = new AttributeFilterContext();
        
        
        result.setIssuerMetadataContextLookupStrategy(e -> metadataContext);
        // prime value
        result.getIssuerMetadataContext();
        return result;
    }
    
    @Test public void aa() throws ResolverException {
        IdPAttribute testAttribute = new IdPAttribute("test");
        IdPAttributeValue resultValue1 = new ScopedStringAttributeValue("value", "aa.aa");
        IdPAttributeValue resultValue2 = new StringAttributeValue("aa");

        testAttribute.setValues(List.of(
                    new ScopedStringAttributeValue("value", "scope"),
                    resultValue1,
                    new ScopedStringAttributeValue("value", "entity"),
                    new StringAttributeValue("value"),
                    resultValue2));
        
        final EntityDescriptor entity = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(aaEntityID)));
        Set<IdPAttributeValue> result = scopeMatcher.getMatchingValues(testAttribute, filterContextFor(entity));
        assertEquals(result.size(), 1);
        assertTrue(result.contains(resultValue1));

        result = valueMatcher.getMatchingValues(testAttribute, filterContextFor(entity));
        assertEquals(result.size(), 1);
        assertTrue(result.contains(resultValue2));
    }

    @Test public void none() throws ResolverException {
        IdPAttribute testAttribute = new IdPAttribute("test");

        testAttribute.setValues(List.of(
                    new ScopedStringAttributeValue("value", "scope"),
                    new ScopedStringAttributeValue("value", "entity"),
                    new StringAttributeValue("value")
                    ));
        
        final EntityDescriptor entity = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(noneEntityID)));
        Set<IdPAttributeValue> result = scopeMatcher.getMatchingValues(testAttribute, filterContextFor(entity));
        assertTrue(result.isEmpty());

        assertTrue(valueMatcher.getMatchingValues(testAttribute, filterContextFor(entity)).isEmpty());
        assertTrue(valueMatcher.getMatchingValues(testAttribute, new AttributeFilterContext()).isEmpty());
    }

    @Test public void both() throws ResolverException {
        IdPAttribute testAttribute = new IdPAttribute("test");
        
        List<IdPAttributeValue> list = List.of(
                new ScopedStringAttributeValue("value", "aa.both"),
                new ScopedStringAttributeValue("value", "aa"),
                new ScopedStringAttributeValue("value", "entity.both"),
                new StringAttributeValue("entity"));

        testAttribute.setValues(list);
        
        final EntityDescriptor entity = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(bothEntityID)));
        Set<IdPAttributeValue> result = scopeMatcher.getMatchingValues(testAttribute, filterContextFor(entity));
        assertEquals(result.size(), 3);
        for (int i = 0; i < 3; i++) {
            assertTrue(result.contains(list.get(i)));
        }

        result = valueMatcher.getMatchingValues(testAttribute, filterContextFor(entity));
        assertEquals(result.size(), 1);
        assertTrue(result.contains(list.get(3)));
    }

    @Test public void entity() throws ResolverException {
        IdPAttribute testAttribute = new IdPAttribute("test");
        IdPAttributeValue resultValue1 = new ScopedStringAttributeValue("value", "entity.entity");
        IdPAttributeValue resultValue2 = new StringAttributeValue("entity");

        testAttribute.setValues(List.of(
                    new ScopedStringAttributeValue("value", "scope"),
                    resultValue1,
                    new StringAttributeValue("value"),
                    resultValue2));
        
        final EntityDescriptor entity = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(entityEntityID)));
        Set<IdPAttributeValue> result = scopeMatcher.getMatchingValues(testAttribute, filterContextFor(entity));
        assertEquals(result.size(), 1);
        assertTrue(result.contains(resultValue1));

        result = valueMatcher.getMatchingValues(testAttribute, filterContextFor(entity));
        assertEquals(result.size(), 1);
        assertTrue(result.contains(resultValue2));
    }

}

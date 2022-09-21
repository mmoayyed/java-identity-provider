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

package net.shibboleth.idp.saml.profile.logic.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.saml.common.profile.logic.EntityAttributesPredicate;
import org.opensaml.saml.common.profile.logic.EntityAttributesPredicate.Candidate;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.MetadataNodeProcessor;
import org.opensaml.saml.metadata.resolver.filter.impl.NodeProcessingMetadataFilter;
import org.opensaml.saml.metadata.resolver.impl.DOMMetadataResolver;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.saml.attribute.impl.AttributeMappingNodeProcessor;
import net.shibboleth.idp.saml.profile.logic.MappedEntityAttributesPredicate;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.spring.config.StringToDurationConverter;
import net.shibboleth.shared.spring.custom.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * Unit test for {@link EntityAttributesPredicate} and {@link MappedEntityAttributesPredicate}.
 */
public class EntityAttributesPredicateTest extends XMLObjectBaseTestCase {
    
    private String fooEntityID = "http://foo.example.org/shibboleth";
    
    private String barEntityID = "http://bar.example.org/shibboleth";
    
    private String bazEntityID = "http://baz.example.org/shibboleth";
    
    private GenericApplicationContext pendingTeardownContext = null;
    
    private MetadataResolver resolver = null;
    
    @BeforeClass
    protected void setUp() throws Exception {
        String mdFileName = "/net/shibboleth/idp/saml/profile/logic/attribute-mapping-metadata.xml";
        
        final Document mdDoc = parserPool.parse(getClass().getResourceAsStream(mdFileName));
        final DOMMetadataResolver mdProvider = new DOMMetadataResolver(mdDoc.getDocumentElement());
        
        List<MetadataNodeProcessor> processors = new ArrayList<>();
        processors.add(new AttributeMappingNodeProcessor(getService()));
        
        final NodeProcessingMetadataFilter nodeFilter =  new NodeProcessingMetadataFilter();
        nodeFilter.setNodeProcessors(processors);
        nodeFilter.initialize();
        
        mdProvider.setMetadataFilter(nodeFilter);
        mdProvider.setId("Test");
        mdProvider.initialize();

        resolver = mdProvider;
    }
    
    @AfterClass public void tearDownTestContext() {
        if (null == pendingTeardownContext ) {
            return;
        }
        pendingTeardownContext.close();
        pendingTeardownContext = null;
    }
    
    private void setTestContext(GenericApplicationContext context) {
        tearDownTestContext();
        pendingTeardownContext = context;
    }
    
    private EntityDescriptor getEntity(final String entityID) throws ResolverException {
        return resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(entityID)));
    }
    
    private ReloadableService<AttributeTranscoderRegistry> getService() {
        final GenericApplicationContext context = new GenericApplicationContext();
        setTestContext(context);
        context.setDisplayName("ApplicationContext: ");

        final ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        service.setConverters(new HashSet<>(Arrays.asList(new StringToDurationConverter())));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());
        
        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions("/net/shibboleth/idp/saml/profile/logic/attribute-registry-service.xml");
        context.refresh();

        return context.getBean(ReloadableService.class);
    }

    @Test
    public void testSimpleMatch() throws ResolverException {
        
        final Candidate tag = new Candidate("zorkmids", Attribute.BASIC);
        tag.setValues(Collections.singletonList("10"));
        
        final EntityAttributesPredicate predicate = new EntityAttributesPredicate(Collections.singletonList(tag));
        Assert.assertFalse(predicate.test(getEntity(fooEntityID)));
        Assert.assertTrue(predicate.test(getEntity(barEntityID)));
        Assert.assertFalse(predicate.test(getEntity(bazEntityID)));
    }    
    
    @Test
    public void testMultiLevelMatch() throws ResolverException {
        final Candidate tag1 = new Candidate("http://macedir.org/entity-category", Attribute.URI_REFERENCE);
        tag1.setValues(Collections.singletonList("http://refeds.org/category/research-and-scholarship"));

        final Candidate tag2 = new Candidate("urn:oasis:names:tc:SAML:profiles:subject-id:req", Attribute.URI_REFERENCE);
        tag2.setValues(Collections.singletonList("none"));

        final EntityAttributesPredicate predicate =
                new EntityAttributesPredicate(Arrays.asList(tag1, tag2), false, true);
        Assert.assertTrue(predicate.test(getEntity(fooEntityID)));
        Assert.assertFalse(predicate.test(getEntity(barEntityID)));
        Assert.assertFalse(predicate.test(getEntity(bazEntityID)));
    }
    
    @Test
    public void testSimpleMatchMapped() throws ResolverException {
        
        final Candidate tag = new Candidate("zorkmids");
        tag.setValues(Collections.singletonList("10"));
        
        final MappedEntityAttributesPredicate predicate = new MappedEntityAttributesPredicate(Collections.singletonList(tag));
        Assert.assertFalse(predicate.test(getEntity(fooEntityID)));
        Assert.assertTrue(predicate.test(getEntity(barEntityID)));
        Assert.assertFalse(predicate.test(getEntity(bazEntityID)));
    }

    @Test
    public void testMultiLevelMappedMatch() throws ResolverException {
        final Candidate tag1 = new Candidate("http://macedir.org/entity-category");
        tag1.setValues(Collections.singletonList("http://refeds.org/category/research-and-scholarship"));

        final Candidate tag2 = new Candidate("subject-id-req");
        tag2.setValues(Collections.singletonList("none"));

        final MappedEntityAttributesPredicate predicate =
                new MappedEntityAttributesPredicate(Arrays.asList(tag1, tag2), false, true);
        Assert.assertTrue(predicate.test(getEntity(fooEntityID)));
        Assert.assertFalse(predicate.test(getEntity(barEntityID)));
        Assert.assertFalse(predicate.test(getEntity(bazEntityID)));
    }

}
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

package net.shibboleth.idp.attribute.resolver.spring;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import net.shibboleth.ext.spring.config.StringToDurationConverter;
import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.idp.attribute.IdPRequestedAttribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.saml.attribute.mapping.AbstractSAMLAttributeMapper;
import net.shibboleth.idp.saml.attribute.mapping.AttributeMapper;
import net.shibboleth.idp.saml.attribute.mapping.impl.RequestedAttributesMapper;
import net.shibboleth.idp.saml.attribute.mapping.impl.ScopedStringAttributeValueMapper;
import net.shibboleth.idp.saml.attribute.mapping.impl.StringAttributeValueMapper;
import net.shibboleth.idp.saml.attribute.mapping.impl.XMLObjectAttributeValueMapper;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceException;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/** test the Auto generation of the attribute mapper */

public class AttributeMapperTest extends OpenSAMLInitBaseTestCase {

    private GenericApplicationContext pendingTeardownContext = null;
    
    @AfterMethod public void tearDownTestContext() {
        if (null == pendingTeardownContext ) {
            return;
        }
        pendingTeardownContext.close();
        pendingTeardownContext = null;
    }
    
    protected void setTestContext(GenericApplicationContext context) {
        tearDownTestContext();
        pendingTeardownContext = context;
    }

    @Test public void mapper() throws ComponentInitializationException, ServiceException, ResolutionException {

        GenericApplicationContext context = new GenericApplicationContext();
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + AttributeMapperTest.class);

        final ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        context.setDisplayName("ApplicationContext: ");
        service.setConverters(new HashSet<>(Arrays.asList(
                new StringToDurationConverter())));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());
        
        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions("net/shibboleth/idp/attribute/resolver/spring/mapperTest.xml");
        context.refresh();

        final ReloadableService<AttributeResolver> attributeResolverService = context.getBean(ReloadableService.class);

        attributeResolverService.initialize();

        ServiceableComponent<AttributeResolver> serviceableComponent = null;
        RequestedAttributesMapper attributesMapper = null;
        try {
            serviceableComponent = attributeResolverService.getServiceableComponent();

            attributesMapper = new RequestedAttributesMapper(serviceableComponent.getComponent());
        } finally {
            serviceableComponent.unpinComponent();
        }

        Collection<AttributeMapper<RequestedAttribute, IdPRequestedAttribute>> mappers = attributesMapper.getMappers();
        assertEquals(mappers.size(), 5);

        for (AttributeMapper<RequestedAttribute, IdPRequestedAttribute> mapper : mappers) {
            AbstractSAMLAttributeMapper sMappers = (AbstractSAMLAttributeMapper) mapper;
            if (mapper.getId().equals("MapperForfeduPersonScopedAffiliation")) {
                assertEquals(sMappers.getSAMLName(), "urn:oid:1.3.6.1.4.1.5923.1.1.1.9");
                assertEquals(sMappers.getAttributeFormat(), Attribute.URI_REFERENCE);
                assertEquals(sMappers.getAttributeIds().size(), 1);
                assertEquals(sMappers.getAttributeIds().get(0), "eduPersonScopedAffiliation");
                ScopedStringAttributeValueMapper valueMapper =
                        (ScopedStringAttributeValueMapper) sMappers.getValueMapper();
                assertEquals(valueMapper.getDelimiter(), "#");
            } else if (mapper.getId().equals("MapperForfeduPersonAssurance")) {
                assertEquals(sMappers.getSAMLName(), "urn:oid:1.3.6.1.4.1.5923.1.1.1.11");
                assertEquals(sMappers.getAttributeFormat(), Attribute.URI_REFERENCE);
                assertEquals(sMappers.getAttributeIds().size(), 2);
                assertEquals(sMappers.getAttributeIds().get(0), "eduPersonAssurance");
                assertEquals(sMappers.getAttributeIds().get(1), "otherPersonAssurance");
                assertTrue(sMappers.getValueMapper() instanceof StringAttributeValueMapper);
            } else if (mapper.getId().equals("MapperForfOeduPersonAssurance")) {
                assertEquals(sMappers.getSAMLName(), "urn:oid:1.3.6.1.4.1.5923.1.1.1.11");
                assertEquals(sMappers.getAttributeFormat(), "http://example.org/Format");
                assertEquals(sMappers.getAttributeIds().size(), 1);
                assertEquals(sMappers.getAttributeIds().get(0), "otherFormatPersonAssurance");
                assertTrue(sMappers.getValueMapper() instanceof StringAttributeValueMapper);
            } else if (mapper.getId().equals("MapperForfotherSAMLName")) {
                assertEquals(sMappers.getSAMLName(), "http://example.org/name/for/Attribute");
                assertEquals(sMappers.getAttributeFormat(), "http://example.org/Format");
                assertEquals(sMappers.getAttributeIds().size(), 1);
                assertEquals(sMappers.getAttributeIds().get(0), "eduPersonAssurance");
                assertTrue(sMappers.getValueMapper() instanceof StringAttributeValueMapper);
            } else if (mapper.getId().equals("MapperForfeduPersonTargetedID")) {
                assertEquals(sMappers.getSAMLName(), "urn:oid:1.3.6.1.4.1.5923.1.1.1.10");
                assertEquals(sMappers.getAttributeFormat(), Attribute.URI_REFERENCE);
                assertEquals(sMappers.getAttributeIds().size(), 1);
                assertEquals(sMappers.getAttributeIds().get(0), "eduPersonTID");
                assertTrue(sMappers.getValueMapper() instanceof XMLObjectAttributeValueMapper);
            } else {
                fail();
            }
        }
    }
}

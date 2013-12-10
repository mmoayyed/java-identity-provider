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

import java.util.Collection;

import net.shibboleth.idp.attribute.mapper.AbstractSAMLAttributeMapper;
import net.shibboleth.idp.attribute.mapper.AttributeMapper;
import net.shibboleth.idp.attribute.mapper.IdPRequestedAttribute;
import net.shibboleth.idp.attribute.mapper.impl.RequestedAttributesMapper;
import net.shibboleth.idp.attribute.mapper.impl.ScopedStringAttributeValueMapper;
import net.shibboleth.idp.attribute.mapper.impl.StringAttributeValueMapper;
import net.shibboleth.idp.attribute.mapper.impl.XmlObjectAttributeValueMapper;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.service.ReloadableService;
import net.shibboleth.idp.service.ServiceException;
import net.shibboleth.idp.service.ServiceableComponent;
import net.shibboleth.idp.spring.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** test the Auto generation of the attribute mapper */

public class AttributeMapperTest extends OpenSAMLInitBaseTestCase {

    @Test public void mapper() throws ComponentInitializationException, ServiceException, ResolutionException {

        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + AttributeResolverServiceTest.class);

        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions("net/shibboleth/idp/attribute/resolver/spring/mapperTest.xml");
        context.refresh();

        final ReloadableService<AttributeResolver> attributeResolverService = context.getBean(ReloadableService.class);

        attributeResolverService.start();

        ServiceableComponent<AttributeResolver> serviceableComponent = null;
        RequestedAttributesMapper attributesMapper = null;
        try {
            serviceableComponent = attributeResolverService.getServiceableComponent();

            attributesMapper = new RequestedAttributesMapper(serviceableComponent.getComponent());
        } finally {
            serviceableComponent.unpinComponent();
        }

        Collection<AttributeMapper<RequestedAttribute, IdPRequestedAttribute>> mappers = attributesMapper.getMappers();
        Assert.assertEquals(mappers.size(), 5);

        for (AttributeMapper<RequestedAttribute, IdPRequestedAttribute> mapper : mappers) {
            AbstractSAMLAttributeMapper sMappers = (AbstractSAMLAttributeMapper) mapper;
            if (mapper.getId().equals("feduPersonScopedAffiliation")) {
                Assert.assertEquals(sMappers.getSAMLName(), "urn:oid:1.3.6.1.4.1.5923.1.1.1.9");
                Assert.assertEquals(sMappers.getAttributeFormat(), Attribute.URI_REFERENCE);
                Assert.assertEquals(sMappers.getAttributeIds().size(), 1);
                Assert.assertEquals(sMappers.getAttributeIds().get(0), "eduPersonScopedAffiliation");
                ScopedStringAttributeValueMapper valueMapper =
                        (ScopedStringAttributeValueMapper) sMappers.getValueMapper();
                Assert.assertEquals(valueMapper.getDelimiter(), "#");
            } else if (mapper.getId().equals("feduPersonAssurance")) {
                Assert.assertEquals(sMappers.getSAMLName(), "urn:oid:1.3.6.1.4.1.5923.1.1.1.11");
                Assert.assertEquals(sMappers.getAttributeFormat(), Attribute.URI_REFERENCE);
                Assert.assertEquals(sMappers.getAttributeIds().size(), 2);
                Assert.assertEquals(sMappers.getAttributeIds().get(0), "eduPersonAssurance");
                Assert.assertEquals(sMappers.getAttributeIds().get(1), "otherPersonAssurance");
                Assert.assertTrue(sMappers.getValueMapper() instanceof StringAttributeValueMapper);
            } else if (mapper.getId().equals("fOeduPersonAssurance")) {
                Assert.assertEquals(sMappers.getSAMLName(), "urn:oid:1.3.6.1.4.1.5923.1.1.1.11");
                Assert.assertEquals(sMappers.getAttributeFormat(), "http://example.org/Format");
                Assert.assertEquals(sMappers.getAttributeIds().size(), 1);
                Assert.assertEquals(sMappers.getAttributeIds().get(0), "otherFormatPersonAssurance");
                Assert.assertTrue(sMappers.getValueMapper() instanceof StringAttributeValueMapper);
            } else if (mapper.getId().equals("fotherSAMLName")) {
                Assert.assertEquals(sMappers.getSAMLName(), "http://example.org/name/for/Attribute");
                Assert.assertEquals(sMappers.getAttributeFormat(), "http://example.org/Format");
                Assert.assertEquals(sMappers.getAttributeIds().size(), 1);
                Assert.assertEquals(sMappers.getAttributeIds().get(0), "eduPersonAssurance");
                Assert.assertTrue(sMappers.getValueMapper() instanceof StringAttributeValueMapper);
            } else if (mapper.getId().equals("feduPersonTargetedID")) {
                Assert.assertEquals(sMappers.getSAMLName(), "urn:oid:1.3.6.1.4.1.5923.1.1.1.10");
                Assert.assertEquals(sMappers.getAttributeFormat(), Attribute.URI_REFERENCE);
                Assert.assertEquals(sMappers.getAttributeIds().size(), 1);
                Assert.assertEquals(sMappers.getAttributeIds().get(0), "eduPersonTID");
                Assert.assertTrue(sMappers.getValueMapper() instanceof XmlObjectAttributeValueMapper);
            } else {
                Assert.fail();
            }
        }
    }
}

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

import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.metadata.resolver.filter.FilterException;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.Multimap;

import net.shibboleth.ext.spring.config.StringToDurationConverter;
import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.idp.attribute.AttributesMapContainer;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPRequestedAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.saml.metadata.impl.AttributeMappingNodeProcessor;
import net.shibboleth.utilities.java.support.service.ReloadableService;

/**
 * Test for {@link AttributeMappingNodeProcessor}.
 */
public class AttributeMappingNodeProcessorTest extends XMLObjectBaseTestCase {

    private EntityDescriptor entityDescriptor;

    private ReloadableService<AttributeTranscoderRegistry> service;

    private AttributeMappingNodeProcessor processor;

    private GenericApplicationContext pendingTeardownContext = null;
    
    @AfterClass public void tearDownTestContext() {
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

    @BeforeClass public void setup() {
        entityDescriptor = unmarshallElement("/net/shibboleth/idp/attribute/resolver/filter/withAttributes.xml");
        assertNotNull(entityDescriptor);
        service = getService();
        assertNotNull(service);
        processor = new AttributeMappingNodeProcessor(service);
    }

    private ReloadableService<AttributeTranscoderRegistry> getService() {
        GenericApplicationContext context = new GenericApplicationContext();
        setTestContext(context);
        context.setDisplayName("ApplicationContext: ");

        final ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        service.setConverters(new HashSet<>(Arrays.asList(new StringToDurationConverter())));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());
        
        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions(
                "/net/shibboleth/idp/attribute/resolver/spring/customBean.xml",
                "/net/shibboleth/idp/attribute/resolver/filter/service.xml");
        context.refresh();

        return context.getBean(ReloadableService.class);
    }

    // Tests use of default mapping behavior for URI-named, string-valued tags.
    @Test public void entityAttributes() throws FilterException {

        assertTrue(entityDescriptor.getObjectMetadata().get(AttributesMapContainer.class).isEmpty());

        processor.process(entityDescriptor);

        final AttributesMapContainer container =
                entityDescriptor.getObjectMetadata().get(AttributesMapContainer.class).get(0);

        final Multimap<String, IdPAttribute> map = container.get();

        assertEquals(map.size(), 1);
        Collection<IdPAttribute> attribute = map.get("http://macedir.org/entity-category");
        assertEquals(attribute.size(), 1);

        IdPAttribute attr = attribute.iterator().next();
        assertEquals(attr.getValues().size(), 1);
        StringAttributeValue sav = (StringAttributeValue) attr.getValues().iterator().next();

        assertEquals(sav.getValue(), "http://id.incommon.org/category/research-and-scholarship");
    }

    @Test public void requiredAttributes() throws FilterException {

        final AttributeConsumingService acs =
                entityDescriptor.getSPSSODescriptor("urn:oasis:names:tc:SAML:1.1:protocol")
                        .getDefaultAttributeConsumingService();

        assertTrue(acs.getObjectMetadata().get(AttributesMapContainer.class).isEmpty());

        processor.process(acs);

        final AttributesMapContainer container = acs.getObjectMetadata().get(AttributesMapContainer.class).get(0);

        final Multimap<String,IdPRequestedAttribute> map = container.get();

        assertEquals(map.size(), 3);

        Collection<IdPRequestedAttribute> attribute = map.get("dn1");
        assertEquals(attribute.size(), 1);
        IdPRequestedAttribute attr = attribute.iterator().next();
        assertTrue(attr.getValues().isEmpty());
        assertFalse(attr.getIsRequired());

        attribute = map.get("dn2");
        assertEquals(attribute.size(), 1);
        attr = attribute.iterator().next();
        assertTrue(attr.getValues().isEmpty());
        assertTrue(attr.getIsRequired());
        
        attribute = map.get("eppn");
        assertEquals(attribute.size(), 1);
        attr = attribute.iterator().next();
        assertTrue(attr.getValues().isEmpty());
        assertFalse(attr.getIsRequired());
    }
}

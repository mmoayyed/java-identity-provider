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

package net.shibboleth.idp.spring;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.ext.spring.config.DurationToLongConverter;
import net.shibboleth.ext.spring.config.StringToIPRangeConverter;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import net.shibboleth.utilities.java.support.xml.XmlConstants;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

/**
 * Helper class for performing some common Spring-related functions.
 */
public final class SpringSupport {

    /** Constructor. */
    private SpringSupport() {
    }

    /**
     * Creates a new, started, application context from the given configuration resources.
     * 
     * @param name name of the application context
     * @param configurationResources configuration resources
     * @param parentContext parent context, or null if there is no parent
     * 
     * @return the created context
     */
    public static GenericApplicationContext newContext(String name, List<Resource> configurationResources,
            ApplicationContext parentContext) {
        GenericApplicationContext context = new GenericApplicationContext(parentContext);
        context.setDisplayName("ApplicationContext:" + name);

        ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        service.setConverters(Sets.newHashSet(new DurationToLongConverter(), new StringToIPRangeConverter()));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());

        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions(configurationResources.toArray(new Resource[] {}));
        context.refresh();
        return context;
    }

    /**
     * Parse list of elements into bean definitions.
     * 
     * @param elements list of elements to parse
     * @param parserContext current parsing context
     * 
     * @return list of bean definitions
     */
    // TODO better javadoc, annotations
    @Nullable public static ManagedList<BeanDefinition> parseCustomElements(
            @Nullable @NonnullElements final Collection<Element> elements, @Nonnull final ParserContext parserContext) {
        if (elements == null) {
            return null;
        }

        ManagedList<BeanDefinition> definitions = new ManagedList<BeanDefinition>(elements.size());
        for (Element e : elements) {
            // TODO null check e
            definitions.add(parserContext.getDelegate().parseCustomElement(e));
        }

        return definitions;
    }

    /**
     * Creates a Spring bean factory from the supplied Spring beans element.
     * 
     * @param springBeans to create bean factory from
     * 
     * @return bean factory
     */
    @Nonnull public static BeanFactory createBeanFactory(@Nonnull final Element springBeans) {

        // Pull in the closest xsi:schemaLocation attribute we can find.
        if (!springBeans.hasAttributeNS(XmlConstants.XSI_SCHEMA_LOCATION_ATTRIB_NAME.getNamespaceURI(),
                XmlConstants.XSI_SCHEMA_LOCATION_ATTRIB_NAME.getLocalPart())) {
            Node parent = springBeans.getParentNode();
            while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
                final String schemaLoc =
                        ((Element) parent).getAttributeNS(
                                XmlConstants.XSI_SCHEMA_LOCATION_ATTRIB_NAME.getNamespaceURI(),
                                XmlConstants.XSI_SCHEMA_LOCATION_ATTRIB_NAME.getLocalPart());
                if (!Strings.isNullOrEmpty(schemaLoc)) {
                    springBeans.setAttributeNS(XmlConstants.XSI_SCHEMA_LOCATION_ATTRIB_NAME.getNamespaceURI(),
                            XmlConstants.XSI_SCHEMA_LOCATION_ATTRIB_NAME.getPrefix() + ':'
                                    + XmlConstants.XSI_SCHEMA_LOCATION_ATTRIB_NAME.getLocalPart(), schemaLoc);
                    break;
                } else {
                    parent = parent.getParentNode();
                }
            }
        }

        final GenericApplicationContext ctx = new GenericApplicationContext();
        final XmlBeanDefinitionReader definitionReader = new XmlBeanDefinitionReader(ctx);
        definitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        definitionReader.setNamespaceAware(true);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SerializeSupport.writeNode(springBeans, outputStream);
        definitionReader.loadBeanDefinitions(new InputSource(new ByteArrayInputStream(outputStream.toByteArray())));
        ctx.refresh();
        return ctx.getBeanFactory();
    }
}
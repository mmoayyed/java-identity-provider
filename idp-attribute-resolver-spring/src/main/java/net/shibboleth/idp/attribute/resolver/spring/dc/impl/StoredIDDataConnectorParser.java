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

package net.shibboleth.idp.attribute.resolver.spring.dc.impl;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.idp.saml.attribute.resolver.impl.StoredIDDataConnector;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for {@link StoredIDDataConnector}.
 */
public class StoredIDDataConnectorParser extends BaseComputedIDDataConnectorParser {

    /** Schema type name: dc: (legacy). */
    @Nonnull public static final QName TYPE_NAME_AD = new QName(DataConnectorNamespaceHandler.NAMESPACE, "StoredId");

    /** Schema type name: resolver: . */
    @Nonnull public static final QName TYPE_NAME_RESOLVER =
                new QName(AttributeResolverNamespaceHandler.NAMESPACE, "StoredId");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StoredIDDataConnectorParser.class);

    /** {@inheritDoc} */
    @Override protected Class<StoredIDDataConnector> getBeanClass(final Element element) {
        return StoredIDDataConnector.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder, "storedId");

        log.debug("{} doParse {}", getLogPrefix(), config);
        final String springResources = AttributeSupport.getAttributeValue(config, new QName("springResources"));
        final String beanDataSource = ManagedConnectionParser.getBeanDataSourceID(config);
        if (springResources != null) {
            log.warn("{} springResources is deprecated for the StoredIDDataConnector"
                    + ", consider using BeanManagedConnection", getLogPrefix());
            builder.addPropertyValue("dataSource", getDataSource(springResources.split(";")));
        } else if (beanDataSource != null) {
            builder.addPropertyReference("dataSource", beanDataSource);
        } else {
            builder.addPropertyValue("dataSource", getv2DataSource(config));
        }

        if (config.hasAttributeNS(null, "queryTimeout")) {
            builder.addPropertyValue("queryTimeout", config.getAttributeNS(null, "queryTimeout"));
        }

        if (config.hasAttributeNS(null, "transactionRetries")) {
            builder.addPropertyValue("transactionRetries",
                    StringSupport.trimOrNull(config.getAttributeNS(null, "transactionRetries")));
        }

        if (config.hasAttributeNS(null, "failFast")) {
            builder.addPropertyValue("failFast",
                    StringSupport.trimOrNull(config.getAttributeNS(null, "failFast")));
        }

        if (config.hasAttributeNS(null, "retryableErrors")) {
            builder.addPropertyValue("retryableErrors",
                    SpringSupport.getAttributeValueAsManagedList(config.getAttributeNodeNS(null, "retryableErrors")));
        }
    }

    /**
     * Creates a Spring bean factory from the supplied spring resources.
     * 
     * @param springResources to load bean definitions from
     * 
     * @return bean factory
     */
    @Nonnull protected BeanFactory createBeanFactory(@Nonnull final String... springResources) {
        final GenericApplicationContext ctx = new FilesystemGenericApplicationContext();
        final XmlBeanDefinitionReader definitionReader = new XmlBeanDefinitionReader(ctx);
        definitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        definitionReader.setNamespaceAware(true);
        definitionReader.loadBeanDefinitions(springResources);
        ctx.refresh();
        return ctx.getBeanFactory();
    }

    /**
     * Get the dataSource from the configuration.
     * 
     * @param springResource location of a spring resource.
     * @return the DataSource
     */
    protected DataSource getDataSource(@Nonnull final String... springResource) {
        final BeanFactory beanFactory = createBeanFactory(springResource);
        return beanFactory.getBean(DataSource.class);
    }

 
    /**
     * Get the dataSource from a v2 configuration.
     * 
     * @param config the DOM element under consideration.
     * @return the DataSource
     */
    protected BeanDefinition getv2DataSource(@Nonnull final Element config) {
        log.debug("{} Parsing v2 configuration", getLogPrefix());
        final ManagedConnectionParser parser = new ManagedConnectionParser(config);
        return parser.createDataSource();
    }
}
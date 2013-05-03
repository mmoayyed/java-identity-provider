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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.spring.AttributeResolverNamespaceHandler;
import net.shibboleth.idp.attribute.resolver.spring.BaseResolverPluginParser;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Base spring bean definition parser for data connectors. DataConnector implementations should provide a custom
 * BeanDefinitionParser by extending this class and overriding the
 * {@link #doParse(Element, ParserContext, BeanDefinitionBuilder)} method to parse any additional attributes or elements
 * it requires. Standard attributes and elements defined by the ResolutionPlugIn and DataConnector schemas will
 * automatically attempt to be parsed.
 */
public abstract class BaseDataConnectorParser extends BaseResolverPluginParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeResolverNamespaceHandler.NAMESPACE, "DataConnector");

    /** Failover data connector attribute name. */
    public static final QName FAILOVER_DATA_CONNECTOR_ELEMENT_NAME = new QName(
            AttributeResolverNamespaceHandler.NAMESPACE, "FailoverDataConnector");

    /** Spring beans element name. */
    public static final QName SPRING_BEANS_ELEMENT_NAME = new QName("http://www.springframework.org/schema/beans",
            "beans");

    /** cache for the log prefix - to save multiple recalculations. */
    private String logPrefix;

    /** Log4j logger. */
    private final Logger log = LoggerFactory.getLogger(BaseDataConnectorParser.class);

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        final List<Element> failoverConnector =
                ElementSupport.getChildElements(config, FAILOVER_DATA_CONNECTOR_ELEMENT_NAME);
        if (failoverConnector != null && !failoverConnector.isEmpty()) {
            String connectorId = StringSupport.trimOrNull(failoverConnector.get(0).getAttributeNS(null, "ref"));
            log.debug("{} setting the following failover data connector dependencies {}",
                    getLogPrefix(), connectorId);
            builder.addPropertyValue("failoverDataConnectorId", connectorId);
        }
    }

    /**
     * Iterates over the children of the supplied element looking for a spring <beans/> element. If the element contains
     * multiple <beans/> declarations, only the first is returned.
     * 
     * @param config to check for spring beans declaration
     *
     * @return spring beans element
     */
    @Nullable protected Element getSpringBeansElement(@Nullable final Element config) {
        final List<Element> configElements = ElementSupport.getChildElements(config, SPRING_BEANS_ELEMENT_NAME);
        if (configElements.size() > 0) {
            return configElements.get(0);
        }
        return null;
    }

    /**
     * Creates a Spring bean factory from the supplied Spring beans element.
     * 
     * @param springBeans to create bean factory from
     *
     * @return bean factory
     */
    @Nonnull protected BeanFactory createBeanFactory(@Nonnull final Element springBeans) {
        final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        final XmlBeanDefinitionReader definitionReader = new XmlBeanDefinitionReader(beanFactory);
        // TODO why does validation need to be turned off?
        definitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
        definitionReader.setNamespaceAware(true);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SerializeSupport.writeNode(springBeans, outputStream);
        definitionReader.loadBeanDefinitions(new InputSource(new ByteArrayInputStream(outputStream.toByteArray())));
        return beanFactory;
    }

    /**
     * Retrieves the bean of the supplied type from the supplied bean factory. Returns null if no bean definition is
     * found.
     * 
     * @param <T> type of bean to return
     * @param beanFactory to get the bean from
     * @param clazz type of the bean to retrieve
     *
     * @return spring bean
     */
    @Nullable protected <T> T getBean(@Nonnull final BeanFactory beanFactory, @Nonnull final Class<T> clazz) {
        T bean = null;
        try {
            bean = beanFactory.getBean(clazz);
            log.debug("created spring bean {}", bean);
        } catch (NoSuchBeanDefinitionException e) {
            log.debug("no spring bean configured of type {}", clazz);
        }
        return bean;
    }

    /**
     * return a string which is to be prepended to all log messages.
     * 
     * @return "Attribute Definition: '<definitionID>' :"
     */
    @Nonnull @NotEmpty protected String getLogPrefix() {
        if (null == logPrefix) {
            StringBuilder builder = new StringBuilder("Data Connector '").append(getDefinitionId()).append("':");
            logPrefix = builder.toString();
        }
        return logPrefix;
    }
}
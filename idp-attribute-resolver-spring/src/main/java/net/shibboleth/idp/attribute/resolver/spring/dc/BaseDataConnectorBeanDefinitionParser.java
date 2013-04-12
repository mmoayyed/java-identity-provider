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
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.shibboleth.idp.attribute.resolver.impl.dc.Validator;
import net.shibboleth.idp.attribute.resolver.spring.AttributeResolverNamespaceHandler;
import net.shibboleth.idp.attribute.resolver.spring.BaseResolverPluginBeanDefinitionParser;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

// TODO incomplete port from v2
/**
 * Base spring bean definition parser for data connectors. DataConnector implementations should provide a custom
 * BeanDefinitionParser by extending this class and overriding the
 * {@link #doParse(String, Element, Map, BeanDefinitionBuilder, ParserContext)} method to parse any additional
 * attributes or elements it requires. Standard attributes and elements defined by the ResolutionPlugIn and
 * DataConnector schemas will automatically attempt to be parsed.
 */
public abstract class BaseDataConnectorBeanDefinitionParser extends BaseResolverPluginBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeResolverNamespaceHandler.NAMESPACE, "DataConnector");

    /** Failover data connector attribute name. */
    public static final QName FAILOVER_DATA_CONNECTOR_ELEMENT_NAME = new QName(
            AttributeResolverNamespaceHandler.NAMESPACE, "FailoverDataConnector");

    /** Spring beans element name. */
    public static final QName SPRING_BEANS_ELEMENT_NAME = new QName("http://www.springframework.org/schema/beans",
            "beans");

    /** Log4j logger. */
    private final Logger log = LoggerFactory.getLogger(BaseDataConnectorBeanDefinitionParser.class);

    /** {@inheritDoc} */
    // TODO Needs refitted into the V3 skeleton
    protected void doParse(String pluginId, Element pluginConfig, Map<QName, List<Element>> pluginConfigChildren,
            BeanDefinitionBuilder pluginBuilder, ParserContext parserContext) {

        List<Element> failoverConnector = pluginConfigChildren.get(FAILOVER_DATA_CONNECTOR_ELEMENT_NAME);
        if (failoverConnector != null && !failoverConnector.isEmpty()) {
            String connectorId = StringSupport.trimOrNull(failoverConnector.get(0).getAttributeNS(null, "ref"));
            log.debug("Setting the following failover data connector dependencies for plugin {}: {}", pluginId,
                    connectorId);
            pluginBuilder.addPropertyValue("failoverDataConnectorIds", connectorId);
        }
    }

    /**
     * Iterates over the children of the supplied element looking for a spring <beans/> element. If the element contains
     * multiple <beans/> declarations, only the first is returned.
     * 
     * @param config to check for spring beans declaration
     * @return spring beans element
     */
    protected Element getSpringBeansElement(final Element config) {
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
     * @return bean factory
     */
    protected BeanFactory createBeanFactory(final Element springBeans) {
        final DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        final XmlBeanDefinitionReader dr = new XmlBeanDefinitionReader(bf);
        // TODO why does validation need to be turned off?
        dr.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
        dr.setNamespaceAware(true);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SerializeSupport.writeNode(springBeans, baos);
        dr.loadBeanDefinitions(new InputSource(new ByteArrayInputStream(baos.toByteArray())));
        return bf;
    }

    /**
     * Retrieves the bean of the supplied type from the supplied bean factory. Returns null if no bean definition is
     * found.
     * 
     * @param <T> type of bean to return
     * @param beanFactory to get the bean from
     * @param clazz type of the bean to retrieve
     * @return spring bean
     */
    protected <T> T getBean(final BeanFactory beanFactory, final Class<T> clazz) {
        T t = null;
        try {
            t = beanFactory.getBean(clazz);
            log.debug("created spring bean {}", t);
        } catch (NoSuchBeanDefinitionException e) {
            log.debug("no spring bean configured of type {}", clazz);
        }
        return t;
    }
}
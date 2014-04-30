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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.AbstractDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.AttributeResolverNamespaceHandler;
import net.shibboleth.idp.attribute.resolver.spring.BaseResolverPluginParser;
import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.w3c.dom.Element;

/**
 * Base spring bean definition parser for data connectors. DataConnector implementations should provide a custom
 * BeanDefinitionParser by extending this class and overriding the
 * {@link #doParse(Element, ParserContext, BeanDefinitionBuilder)} method to parse any additional attributes or elements
 * it requires. Standard attributes and elements defined by the ResolutionPlugIn and DataConnector schemas will
 * automatically attempt to be parsed.
 */
public abstract class AbstractDataConnectorParser extends BaseResolverPluginParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeResolverNamespaceHandler.NAMESPACE, "DataConnector");

    /** Failover data connector attribute name. */
    public static final QName FAILOVER_DATA_CONNECTOR_ELEMENT_NAME = new QName(
            AttributeResolverNamespaceHandler.NAMESPACE, "FailoverDataConnector");

    /** cache for the log prefix - to save multiple recalculations. */
    private String logPrefix;

    /** Log4j logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractDataConnectorParser.class);

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        final List<Element> failoverConnector =
                ElementSupport.getChildElements(config, FAILOVER_DATA_CONNECTOR_ELEMENT_NAME);
        if (failoverConnector != null && !failoverConnector.isEmpty()) {
            String connectorId = StringSupport.trimOrNull(failoverConnector.get(0).getAttributeNS(null, "ref"));
            log.debug("{} setting the following failover data connector dependencies {}", getLogPrefix(), connectorId);
            builder.addPropertyValue("failoverDataConnectorId", connectorId);
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
        final GenericApplicationContext ctx = new GenericApplicationContext();
        final XmlBeanDefinitionReader definitionReader = new XmlBeanDefinitionReader(ctx);
        definitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        definitionReader.setNamespaceAware(true);
        definitionReader.loadBeanDefinitions(springResources);
        ctx.refresh();
        return ctx.getBeanFactory();
    }

    /**
     * Returns the results of {@link Introspector#getBeanInfo(Class, Class)} for the supplied connector class.
     * 
     * @param connectorClass to introspect
     * 
     * @return property descriptors or null if an error occurred
     */
    @Nullable protected PropertyDescriptor[] getBeanPropertyDescriptors(
            @Nonnull final Class<? extends AbstractDataConnector> connectorClass) {
        PropertyDescriptor[] descriptors = null;
        try {
            final BeanInfo info = Introspector.getBeanInfo(connectorClass, AbstractDataConnector.class);
            descriptors = info.getPropertyDescriptors();
        } catch (IntrospectionException e) {
            log.error("could not retrieve bean info for class {}", connectorClass, e);
        }
        return descriptors;
    }

    /**
     * Gets the property descriptors for the supplied connector class and then retrieves the bean for each descriptor
     * type. If a bean is found it is added to the supplied builder.
     * 
     * @param builder to add property values to
     * @param beanFactory to retrieve bean configuration from
     * @param connectorClass to read property descriptors from
     */
    protected void addPropertyDescriptorValues(@Nonnull BeanDefinitionBuilder builder,
            @Nonnull BeanFactory beanFactory, @Nonnull final Class<? extends AbstractDataConnector> connectorClass) {
        for (PropertyDescriptor descriptor : getBeanPropertyDescriptors(connectorClass)) {
            log.debug("parsing property descriptor {}", descriptor);
            final Object value = SpringSupport.getBean(beanFactory, descriptor.getPropertyType());
            if (value != null) {
                builder.addPropertyValue(descriptor.getName(), value);
                log.debug("added property value {}", value);
            } else {
                log.debug("no configuration found for {}", descriptor.getPropertyType());
            }
        }
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
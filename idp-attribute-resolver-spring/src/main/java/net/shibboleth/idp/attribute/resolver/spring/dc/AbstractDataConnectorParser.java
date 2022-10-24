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

import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.resolver.AbstractDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.BaseResolverPluginParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.DataConnectorFactoryBean;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
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
    @Nonnull public static final QName ELEMENT_NAME = new QName(AttributeResolverNamespaceHandler.NAMESPACE,
            "DataConnector");

    /** Delay in retrying failed connector. */
    @Nonnull @NotEmpty public static final String ATTR_NORETRYDELAY = "noRetryDelay";

    /** semi colon separated resources to indicate external config. */
    @Nonnull @NotEmpty public static final String ATTR_SPRING_RESOURCE = "springResources";

    /**
     * A bean name for a {@link java.util.Collection}<code>&lt;</code>
     * {@link org.springframework.core.io.Resource}<code>&gt;</code>.
     */
    @Nonnull @NotEmpty public static final String ATTR_SPRING_RESOURCE_REF = "springResourcesRef";

    /**
     * A bean name for a {@link List}<code>&lt;</code>
     * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor}<code>&gt;</code>.
     */
    @Nonnull @NotEmpty public static final String ATTR_FACTORY_POSTPROCESSORS_REF = "factoryPostProcessorsRef";

    /**
     * A bean name for a {@link List}<code>&lt;</code>
     * {@link org.springframework.beans.factory.config.BeanPostProcessor}<code>&gt;</code>.
     */
    @Nonnull @NotEmpty public static final String ATTR_POSTPROCESSORS_REF = "postProcessorsRef";

    /**
     * Whether to export all attributes.
     *
     */
    @Nonnull @NotEmpty public static final String ATTR_EXPORT_ALL = "exportAllAttributes";

    /**
     * Which attributes to export.
     */
    @Nonnull @NotEmpty public static final String ATTR_EXPORT_NAMES = "exportAttributes";
    
    /**
     * Failfast LDAP, Realtional, Stored.
     */
    @Nonnull @NotEmpty public static final String ATTR_FAIL_FAST = "failFastInitialize";

    /** Failover data connector attribute name. */
    @Nonnull public static final QName FAILOVER_DATA_CONNECTOR_ELEMENT_NAME = new QName(
            AttributeResolverNamespaceHandler.NAMESPACE, "FailoverDataConnector");

    /** Log4j logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractDataConnectorParser.class);

    /**
     * Returns whether the element configuration is native spring or custom.
     * 
     * @param config the element under consideration.
     * @return whether this is native spring
     */
    protected boolean isNative(@Nonnull final Element config) {
        return config.hasAttributeNS(null, ATTR_SPRING_RESOURCE)
                || config.hasAttributeNS(null, ATTR_SPRING_RESOURCE_REF);
    }

    /** {@inheritDoc} */
    @Override protected final Class<?> getBeanClass(final Element element) {
        if (isNative(element)) {
            return DataConnectorFactoryBean.class;
        }
        return getNativeBeanClass();
    }

    /**
     * Per parser indication of what we are building.
     * 
     * @return the class
     */
    protected abstract Class<? extends AbstractDataConnector> getNativeBeanClass();

    //CheckStyle: MethodLength|CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        final List<Element> failoverConnector =
                ElementSupport.getChildElements(config, FAILOVER_DATA_CONNECTOR_ELEMENT_NAME);

        if (failoverConnector != null && !failoverConnector.isEmpty()) {
            if (failoverConnector.size() > 1) {
                 log.warn("{} More than one failover data connector specified, taking the first", 
                        getLogPrefix());                
            }
            final String connectorId = StringSupport.trimOrNull(failoverConnector.get(0).getAttributeNS(null, "ref"));
            log.debug("{} Setting the following failover data connector dependencies: {}", getLogPrefix(), connectorId);
            builder.addPropertyValue("failoverDataConnectorId", connectorId);
        }

        if (config.hasAttributeNS(null, ATTR_NORETRYDELAY)) {
            builder.addPropertyValue("noRetryDelay",
                    StringSupport.trimOrNull(config.getAttributeNS(null, ATTR_NORETRYDELAY)));
        }

        if (config.hasAttributeNS(null, ATTR_EXPORT_ALL)) {
            if (config.hasAttributeNS(null, ATTR_EXPORT_NAMES)) {
                log.warn("{} {} overrides {}", getLogPrefix(), ATTR_EXPORT_ALL, ATTR_EXPORT_NAMES);
            }
            DeprecationSupport.warnOnce(ObjectType.ATTRIBUTE, "exportAllAttributes", null,
                    "exportAttributes=\"attr1, attr2,...\"");
            builder.addPropertyValue("exportAllAttributes",
                    StringSupport.trimOrNull(config.getAttributeNS(null, ATTR_EXPORT_ALL)));
        } else if (config.hasAttributeNS(null, ATTR_EXPORT_NAMES)) {
            builder.addPropertyValue("exportAttributes",
                    SpringSupport.getAttributeValueAsList(config.getAttributeNodeNS(null, ATTR_EXPORT_NAMES)));
        }

        if (config.hasAttributeNS(null, ATTR_FAIL_FAST)) {
            // LDAP, Relational & HTTP only, limited in the schema
            builder.addPropertyValue("failFastInitialize", 
                    StringSupport.trimOrNull(config.getAttributeNS(null, ATTR_FAIL_FAST)));
        }

        if (isNative(config)) {
            // parse the configuration into a beanfactory and inject the resources as well
            builder.addPropertyValue("objectType", getNativeBeanClass());
            // it's a factory bean so we use the spring lifecycle directly
            builder.setInitMethodName(null);
            builder.setDestroyMethodName(null);
            if (config.hasAttributeNS(null, ATTR_SPRING_RESOURCE)) {
                DeprecationSupport.warnOnce(ObjectType.ATTRIBUTE, ATTR_SPRING_RESOURCE,
                        parserContext.getReaderContext().getResource().getDescription(), "<will be removed>");
                final String[] resources =
                        StringSupport.trimOrNull(config.getAttributeNS(null, ATTR_SPRING_RESOURCE)).split(";");
                log.debug("{} Native configuration from {}", getLogPrefix(), resources);
                builder.addPropertyValue("resources", resources);
            } else {
                DeprecationSupport.warnOnce(ObjectType.ATTRIBUTE, ATTR_SPRING_RESOURCE_REF,
                        parserContext.getReaderContext().getResource().getDescription(), "<will be removed>");
                final String resourceRef =
                        StringSupport.trimOrNull(config.getAttributeNS(null, ATTR_SPRING_RESOURCE_REF));
                log.debug("{} Native configuration from bean {}", getLogPrefix(), resourceRef);
                builder.addPropertyReference("resources", resourceRef);
            }
            if (config.hasAttributeNS(null, ATTR_FACTORY_POSTPROCESSORS_REF)) {
                DeprecationSupport.warnOnce(ObjectType.ATTRIBUTE, ATTR_FACTORY_POSTPROCESSORS_REF,
                        parserContext.getReaderContext().getResource().getDescription(), "<will be removed>");
                final String factoryPostProcessorsRef =
                        StringSupport.trimOrNull(config.getAttributeNS(null, ATTR_FACTORY_POSTPROCESSORS_REF));
                log.debug("{} Factory Bean Post Processors {}", getLogPrefix(), factoryPostProcessorsRef);
                builder.addPropertyReference("beanFactoryPostProcessors", factoryPostProcessorsRef);
            } else {
                log.debug("{} Adding default Factory Bean Post Processor: "
                        + "shibboleth.PropertySourcesPlaceholderConfigurer", getLogPrefix());
                builder.addPropertyReference("beanFactoryPostProcessors",
                        "shibboleth.PropertySourcesPlaceholderConfigurer");
            }
            if (config.hasAttributeNS(null, ATTR_POSTPROCESSORS_REF)) {
                DeprecationSupport.warnOnce(ObjectType.ATTRIBUTE, ATTR_POSTPROCESSORS_REF,
                        parserContext.getReaderContext().getResource().getDescription(), "<will be removed>");
                final String postProcessorsRef =
                        StringSupport.trimOrNull(config.getAttributeNS(null, ATTR_POSTPROCESSORS_REF));
                log.debug("{} Bean Post Processors {}", getLogPrefix(), postProcessorsRef);
                builder.addPropertyReference("beanPostProcessors", postProcessorsRef);
            }
        } else {
            doV2Parse(config, parserContext, builder);
        }
    }
    //CheckStyle: MethodLength|CyclomaticComplexity ON

    /**
     * Parse the supplied {@link Element} as a legacy format and populate the supplied {@link BeanDefinitionBuilder} as
     * required.
     * 
     * @param element the XML element being parsed
     * @param parserContext the object encapsulating the current state of the parsing process
     * @param builder used to define the {@code BeanDefinition}
     * @see #doParse(Element, BeanDefinitionBuilder)
     */
    protected abstract void doV2Parse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder);

    /**
     * Return a string which is to be prepended to all log messages.
     * 
     * @return "Data Connector: '&lt;definitionID&gt;' :"
     */
    @Override @Nonnull @NotEmpty protected String getLogPrefix() {
        final StringBuilder builder = new StringBuilder("Data Connector '").append(getDefinitionId()).append("':");
        return builder.toString();
    }
}
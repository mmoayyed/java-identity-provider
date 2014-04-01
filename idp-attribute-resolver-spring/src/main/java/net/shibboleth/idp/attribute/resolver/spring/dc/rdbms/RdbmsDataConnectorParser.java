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

package net.shibboleth.idp.attribute.resolver.spring.dc.rdbms;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.impl.dc.rdbms.RdbmsDataConnector;
import net.shibboleth.idp.attribute.resolver.impl.dc.rdbms.TemplatedExecutableStatementBuilder;
import net.shibboleth.idp.attribute.resolver.spring.dc.AbstractDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.CacheConfigParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.DataConnectorNamespaceHandler;
import net.shibboleth.idp.attribute.resolver.spring.dc.ManagedConnectionParser;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.DomTypeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Bean definition Parser for a {@link RdbmsDataConnector}. */
public class RdbmsDataConnectorParser extends AbstractDataConnectorParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(DataConnectorNamespaceHandler.NAMESPACE, "RelationalDatabase");

    /** Local name of attribute. */
    public static final QName ATTRIBUTE_ELEMENT_NAME = new QName(DataConnectorNamespaceHandler.NAMESPACE, "Attribute");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(RdbmsDataConnectorParser.class);

    /** {@inheritDoc} */
    @Override
    protected Class<RdbmsDataConnector> getBeanClass(@Nullable final Element element) {
        return RdbmsDataConnector.class;
    }

    /** {@inheritDoc} */
    @Override
    protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);
        log.debug("doParse {}", config);

        final String springResources = AttributeSupport.getAttributeValue(config, new QName("springResources"));
        if (springResources == null) {
            log.debug("parsing v2 configuration");
            doParseV2(config, parserContext, builder);
        } else {
            doParseInternal(config, createBeanFactory(springResources.split("\\s+")), builder);
        }
    }

    /**
     * Parses a Spring <beans/> configuration.
     * 
     * @param config RelationalDatabase containing Spring configuration
     * @param beanFactory containing spring beans
     * @param builder to initialize
     */
    protected void doParseInternal(@Nonnull final Element config, @Nonnull final BeanFactory beanFactory,
            @Nonnull final BeanDefinitionBuilder builder) {

        addPropertyDescriptorValues(builder, beanFactory, RdbmsDataConnector.class);

        final Boolean noResultAnError =
                AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(config, new QName(
                        "noResultIsError")));
        log.debug("parsed noResultAnError {}", noResultAnError);
        if (noResultAnError != null && noResultAnError.booleanValue()) {
            builder.addPropertyValue("noResultAnError", true);
        }
        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");
    }

    /**
     * Parses a version 2 configuration.
     * 
     * @param config RelationalDatabase containing v2 configuration
     * @param parserContext bean definition parsing context
     * @param builder to initialize
     */
    protected void doParseV2(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {

        final V2Parser v2Parser = new V2Parser(config);

        builder.addPropertyValue("DataSource", v2Parser.createDataSource());

        builder.addPropertyValue("executableSearchBuilder", v2Parser.createTemplateBuilder());

        builder.addPropertyValue("resultsCache", v2Parser.createCache());

        final String noResultIsError = AttributeSupport.getAttributeValue(config, new QName("noResultIsError"));
        if (noResultIsError != null) {
            builder.addPropertyValue("noResultAnError", noResultIsError);
        }
        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");
    }

    /** Utility class for parsing v2 schema configuration. */
    protected static class V2Parser {

        /** Base XML element. */
        private final Element configElement;

        /**
         * Creates a new V2Parser with the supplied RelationalDatabase element.
         * 
         * @param config RelationalDatabase element
         */
        public V2Parser(@Nonnull final Element config) {
            Constraint.isNotNull(config, "RelationalDatabase element cannot be null");
            configElement = config;
        }

        /**
         * Create the data source bean definition. See {@link ManagedConnectionParser}.
         *
         * @return data source bean definition
         */
        @Nonnull public BeanDefinition createDataSource() {
            final ManagedConnectionParser parser = new ManagedConnectionParser(configElement);
            return parser.createDataSource();
        }

        /**
         * Create the definition of the template driven search builder.
         * 
         * @return the bean definition for the template search builder.
         */
        @Nonnull public BeanDefinition createTemplateBuilder() {
            final BeanDefinitionBuilder templateBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(TemplatedExecutableStatementBuilder.class);
            templateBuilder.setInitMethodName("initialize");
            templateBuilder.setDestroyMethodName("destroy");

            String velocityEngineRef = StringSupport.trimOrNull(configElement.getAttribute("templateEngine"));
            if (null == velocityEngineRef) {
                velocityEngineRef = "shibboleth.VelocityEngine";
            }
            templateBuilder.addPropertyReference("velocityEngine", velocityEngineRef);

            templateBuilder.addPropertyValue("v2Compatibility", true);

            final String queryTimeout = AttributeSupport.getAttributeValue(configElement, new QName("queryTimeout"));
            if (queryTimeout != null) {
                final BeanDefinitionBuilder duration =
                        BeanDefinitionBuilder.rootBeanDefinition(V2Parser.class, "buildDuration");
                duration.addConstructorArgValue(queryTimeout);
                duration.addConstructorArgValue(1);
                templateBuilder.addPropertyValue("queryTimeout", duration.getBeanDefinition());
            } else {
                templateBuilder.addPropertyValue("queryTimeout", 5000);
            }

            final Element queryTemplate =
                    ElementSupport.getFirstChildElement(configElement, new QName(
                            DataConnectorNamespaceHandler.NAMESPACE, "QueryTemplate"));
            final String queryText = queryTemplate.getTextContent();
            templateBuilder.addPropertyValue("templateText", queryText);

            templateBuilder.setInitMethodName("initialize");
            templateBuilder.setDestroyMethodName("destroy");
            return templateBuilder.getBeanDefinition();
        }

        /**
         * Create the results cache. See {@link CacheConfigParser}.
         *
         * @return results cache
         */
        @Nullable public BeanDefinition createCache() {
            final CacheConfigParser parser = new CacheConfigParser(configElement);
            return parser.createCache();
        }

        /**
         * Converts the supplied duration to milliseconds and divides it by the divisor. Useful for modifying durations
         * while resolving property replacement.
         * 
         * @param duration string format
         * @param divisor to modify the duration with
         * 
         * @return result of the division
         */
        public static long buildDuration(final String duration, final long divisor) {
            return DomTypeSupport.durationToLong(duration) / divisor;
        }
    }
}
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

import java.util.Map;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.impl.dc.ExecutableSearchBuilder;
import net.shibboleth.idp.attribute.resolver.impl.dc.MappingStrategy;
import net.shibboleth.idp.attribute.resolver.impl.dc.Validator;
import net.shibboleth.idp.attribute.resolver.impl.dc.ldap.LdapDataConnector;
import net.shibboleth.idp.attribute.resolver.impl.dc.rdbms.FormatExecutableStatementBuilder;
import net.shibboleth.idp.attribute.resolver.impl.dc.rdbms.RdbmsDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.dc.BaseDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.DataConnectorNamespaceHandler;
import net.shibboleth.idp.attribute.resolver.spring.dc.ManagedConnectionParser;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.google.common.cache.Cache;

/** Bean definition Parser for a {@link RdbmsDataConnector}. */
public class RdbmsDataConnectorParser extends BaseDataConnectorParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(DataConnectorNamespaceHandler.NAMESPACE, "RelationalDatabase");

    /** Local name of attribute. */
    public static final QName ATTRIBUTE_ELEMENT_NAME = new QName(DataConnectorNamespaceHandler.NAMESPACE, "Attribute");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(RdbmsDataConnectorParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(final Element element) {
        return RdbmsDataConnector.class;
    }

    /** {@inheritDoc} */
    protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);
        log.debug("doParse {}", config);

        final Element springBeans = getSpringBeansElement(config);
        if (springBeans == null) {
            log.debug("parsing v2 configuration");
            doParseV2(config, parserContext, builder);
        } else {
            doParseInternal(config, parserContext, builder);
        }
    }

    /**
     * Parses a Spring <beans/> configuration.
     * 
     * @param config RelationalDatabase containing Spring configuration
     * @param parserContext bean definition parsing context
     * @param builder to initialize
     */
    protected void doParseInternal(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {

        final Element springBeans = getSpringBeansElement(config);
        final BeanFactory beanFactory = createBeanFactory(springBeans);
        addPropertyDescriptorValues(builder, beanFactory, RdbmsDataConnector.class);

        final Boolean noResultAnError =
                AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(config, new QName(
                        "noResultIsError")));
        log.debug("parsed noResultAnError {}", noResultAnError);
        if (noResultAnError != null && noResultAnError.booleanValue()) {
            builder.addPropertyValue("noResultAnError", true);
        }
        builder.setInitMethodName("initialize");
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

        final ManagedConnectionParser parser = new ManagedConnectionParser(config);

        final Boolean noResultAnError =
                AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(config, new QName(
                        "noResultIsError")));

        final String templateEngine = config.getAttribute("templateEngine");

        final DataSource datasource = parser.createDataSource();
        builder.addPropertyValue("DataSource", datasource);

        final Element queryTemplate =
                ElementSupport.getFirstChildElement(config, new QName(DataConnectorNamespaceHandler.NAMESPACE,
                        "QueryTemplate"));
        // TODO this should use the Templated builder once the velocity engine is working
        final FormatExecutableStatementBuilder statementBuilder =
                new FormatExecutableStatementBuilder(StringSupport.trimOrNull(queryTemplate.getTextContent()));
        Long queryTimeout =
                AttributeSupport.getDurationAttributeValueAsLong(AttributeSupport.getAttribute(config, new QName(
                        "queryTimeout")));
        if (queryTimeout == null) {
            queryTimeout = Long.valueOf(5000);
        }
        statementBuilder.setQueryTimeout(queryTimeout.intValue());
        builder.addPropertyValue("executableSearchBuilder", statementBuilder);

        // TODO add support for cacheResults and ResultCache
        if (noResultAnError != null && noResultAnError.booleanValue()) {
            builder.addPropertyValue("noResultAnError", true);
        }
        builder.setInitMethodName("initialize");
    }
}
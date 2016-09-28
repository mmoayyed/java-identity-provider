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

package net.shibboleth.idp.attribute.resolver.spring.dc.rdbms.impl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.dc.rdbms.impl.DataSourceValidator;
import net.shibboleth.idp.attribute.resolver.dc.rdbms.impl.RDBMSDataConnector;
import net.shibboleth.idp.attribute.resolver.dc.rdbms.impl.StringResultMappingStrategy;
import net.shibboleth.idp.attribute.resolver.dc.rdbms.impl.TemplatedExecutableStatementBuilder;
import net.shibboleth.idp.attribute.resolver.spring.dc.AbstractDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.CacheConfigParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.DataConnectorNamespaceHandler;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.ManagedConnectionParser;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Bean definition Parser for a {@link RDBMSDataConnector}. */
public class RDBMSDataConnectorParser extends AbstractDataConnectorParser {

    /** Schema type name - dc: (Legacy). */
    @Nonnull public static final QName TYPE_NAME_DC =
            new QName(DataConnectorNamespaceHandler.NAMESPACE, "RelationalDatabase");

    /** Schema type name - resolver:. */
    @Nonnull public static final QName TYPE_NAME_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "RelationalDatabase");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RDBMSDataConnectorParser.class);

    /** {@inheritDoc} */
    @Override protected Class<RDBMSDataConnector> getNativeBeanClass() {
        return RDBMSDataConnector.class;
    }

    /** {@inheritDoc} */
    @Override protected void doV2Parse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {

        log.debug("{} Parsing v2 configuration {}", getLogPrefix(), config);

        final V2Parser v2Parser = new V2Parser(config, getLogPrefix());

        final String dataSourceID = ManagedConnectionParser.getBeanDataSourceID(config);
        final BeanDefinition dataSource;
        if (dataSourceID != null) {
            dataSource = null;
            builder.addPropertyReference("DataSource", dataSourceID);
        } else {
            dataSource = v2Parser.createManagedDataSource();
            builder.addPropertyValue("DataSource", v2Parser.createManagedDataSource());
        }

        builder.addPropertyValue("executableSearchBuilder", v2Parser.createTemplateBuilder());

        final String connectionReadOnly = v2Parser.getConnectionReadOnly();
        if (connectionReadOnly != null) {
            builder.addPropertyValue("connectionReadOnly", connectionReadOnly);
        }

        final String mappingStrategyID = v2Parser.getBeanMappingStrategyID();
        if (mappingStrategyID != null) {
            builder.addPropertyReference("mappingStrategy", mappingStrategyID);
        } else {
            final BeanDefinition def = v2Parser.createMappingStrategy();
            if (def != null) {
                builder.addPropertyValue("mappingStrategy", def);
            }
        }
        
        final String validatorID = v2Parser.getBeanValidatorID();
        if (validatorID != null) {
            builder.addPropertyReference("validator", validatorID);
        } else {
            if (dataSourceID != null) {
                builder.addPropertyValue("validator", v2Parser.createValidator(dataSourceID));
            } else {
                builder.addPropertyValue("validator", v2Parser.createValidator(dataSource));
            }
        }

        final String resultCacheBeanID = CacheConfigParser.getBeanResultCacheID(config);
        
        if (null != resultCacheBeanID) {
           builder.addPropertyReference("resultsCache", resultCacheBeanID);
        } else {
            builder.addPropertyValue("resultsCache", v2Parser.createCache());
        }

        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");
    }

    /**
     * Utility class for parsing v2 schema configuration.
     * 
     */
    protected static class V2Parser {

        /** Base XML element. */
        private final Element configElement;

        /** Class logger. */
        private final Logger log = LoggerFactory.getLogger(V2Parser.class);

        /** Parent parser's log prefix.*/
        @Nonnull @NotEmpty private final String logPrefix;

        /**
         * Creates a new V2Parser with the supplied RelationalDatabase element.
         * 
         * @param config RelationalDatabase element
         * @param prefix the parent parser's log prefix.
         */
        public V2Parser(@Nonnull final Element config,  @Nonnull @NotEmpty final String prefix) {
            Constraint.isNotNull(config, "RelationalDatabase element cannot be null");
            configElement = config;
            logPrefix = prefix;
            // warn about deprecated schema
            if (AttributeSupport.hasAttribute(config, new QName("queryUsesStoredProcedure"))) {
                log.warn("{} queryUsesStoredProcedure property no longer supported and should be removed",
                        getLogPrefix());
            }
            if (AttributeSupport.hasAttribute(config, new QName("cacheResults"))) {
                log.warn("{} cacheResults property no longer supported.  Use the ResultCache element instead",
                        getLogPrefix());
            }

        }

        /**
         * Create the data source bean definition. See {@link ManagedConnectionParser}.
         * 
         * @return data source bean definition
         */
        @Nonnull public BeanDefinition createManagedDataSource() {
            final ManagedConnectionParser parser = new ManagedConnectionParser(configElement);
            return parser.createDataSource();
        }

        /**
         * Get the connectionReadOnly attribute value.
         *
         * @return  whether connections should be read only
         */
        @Nullable public String getConnectionReadOnly() {
            return AttributeSupport.getAttributeValue(configElement, new QName("readOnlyConnection"));
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
            }

            final List<Element> queryTemplates = ElementSupport.getChildElements(configElement, 
                            new QName(DataConnectorNamespaceHandler.NAMESPACE, "QueryTemplate"));
            queryTemplates.addAll(ElementSupport.getChildElements(configElement, 
                    new QName(AttributeResolverNamespaceHandler.NAMESPACE, "QueryTemplate")));
            
            if (queryTemplates.size() > 1) {
                log.warn("{} A maximum of 1 <QueryTemplate> should be specified; the first one has been used",
                        getLogPrefix());
            }
            
            final String queryText;
            if (!queryTemplates.isEmpty()) {
                queryText = queryTemplates.get(0).getTextContent();
            } else {
                queryText = null;
            }
            templateBuilder.addPropertyValue("templateText", queryText);

            templateBuilder.setInitMethodName("initialize");
            templateBuilder.setDestroyMethodName("destroy");
            return templateBuilder.getBeanDefinition();
        }
        
        /**
         * Get the bean ID of an externally defined mapping strategy.
         * 
         * @return mapping strategy bean ID
         */
        @Nullable public String getBeanMappingStrategyID() {
            return AttributeSupport.getAttributeValue(configElement, null, "mappingStrategyRef");
        }
        
        /**
         * Create the result mapping strategy. See {@link net.shibboleth.idp.attribute.resolver.dc.MappingStrategy}.
         * 
         * @return mapping strategy
         */
        @Nullable public BeanDefinition createMappingStrategy() {
            
            final BeanDefinitionBuilder mapper =
                    BeanDefinitionBuilder.genericBeanDefinition(StringResultMappingStrategy.class);
            final List<Element> columns = ElementSupport.getChildElementsByTagNameNS(configElement,
                    DataConnectorNamespaceHandler.NAMESPACE, "Column");
            columns.addAll(ElementSupport.getChildElementsByTagNameNS(configElement,
                    AttributeResolverNamespaceHandler.NAMESPACE, "Column"));
            if (!columns.isEmpty()) {
                final ManagedMap renamingMap = new ManagedMap();
                for (final Element column : columns) {
                    final String columnName = AttributeSupport.getAttributeValue(column, null, "columnName");
                    final String attributeId = AttributeSupport.getAttributeValue(column, null, "attributeID");
                    if (columnName != null && attributeId != null) {
                        renamingMap.put(columnName, attributeId);
                    }
                    
                    if (AttributeSupport.hasAttribute(column, new QName("type"))) {
                        LoggerFactory.getLogger(RDBMSDataConnectorParser.class).warn(
                                "{} Column type attribute is no longer supported", getLogPrefix());
                    }
                }
                mapper.addPropertyValue("resultRenamingMap", renamingMap);
            }
                        
            final String noResultIsError =
                    AttributeSupport.getAttributeValue(configElement, new QName("noResultIsError"));
            if (noResultIsError != null) {
                mapper.addPropertyValue("noResultAnError", noResultIsError);
            }

            final String multipleResultsIsError =
                    AttributeSupport.getAttributeValue(configElement, new QName("multipleResultsIsError"));
            if (multipleResultsIsError != null) {
                mapper.addPropertyValue("multipleResultsAnError", multipleResultsIsError);
            }
            return mapper.getBeanDefinition();
        }
        
        /**
         * Get the bean ID of an externally defined validator.
         * 
         * @return validator bean ID
         */
        @Nullable public String getBeanValidatorID() {
            return AttributeSupport.getAttributeValue(configElement, null, "validatorRef");
        }
        
        /**
         * Create the validator. See {@link net.shibboleth.idp.attribute.resolver.dc.Validator}.
         * 
         * @param dataSource to provide to the validator
         *
         * @return validator
         */
        @Nullable public BeanDefinition createValidator(final Object dataSource) {            
            final BeanDefinitionBuilder validator =
                    BeanDefinitionBuilder.genericBeanDefinition(DataSourceValidator.class);
            if (dataSource instanceof String) {
                validator.addConstructorArgReference((String) dataSource); 
            } else {
                validator.addConstructorArgValue(dataSource);
            }
            validator.addConstructorArgValue(true);
            return validator.getBeanDefinition();
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
        
        /** The parent parser's log prefix.
         * @return the log prefix.
         */
        @Nonnull @NotEmpty private String getLogPrefix() {
            return logPrefix;
        }

        /**
         * Converts the supplied duration to milliseconds and divides it by the divisor. Useful for modifying durations
         * while resolving property replacement.
         * 
         * @param duration the duration (which may have gone through spring translation from iso to long)
         * @param divisor to modify the duration with
         * 
         * @return result of the division
         */
        @Duration public static long buildDuration(@Duration final long duration, final long divisor) {
            return duration / divisor;
        } 
    }
    
}
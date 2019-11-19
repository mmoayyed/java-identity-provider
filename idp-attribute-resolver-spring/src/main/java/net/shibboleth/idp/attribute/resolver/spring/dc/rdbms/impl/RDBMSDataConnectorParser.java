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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.resolver.dc.rdbms.StringResultMappingStrategy;
import net.shibboleth.idp.attribute.resolver.dc.rdbms.impl.DataSourceValidator;
import net.shibboleth.idp.attribute.resolver.dc.rdbms.impl.RDBMSDataConnector;
import net.shibboleth.idp.attribute.resolver.dc.rdbms.impl.V2CompatibleTemplateExecutableStatementBuilder;
import net.shibboleth.idp.attribute.resolver.spring.dc.AbstractDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.CacheConfigParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.ManagedConnectionParser;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

/** Bean definition Parser for a {@link RDBMSDataConnector}. */
public class RDBMSDataConnectorParser extends AbstractDataConnectorParser {

    /** Schema type name. */
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

        final String searchBuilderID = v2Parser.getBeanSearchBuilderID();
        if (searchBuilderID != null) {
            builder.addPropertyReference("executableSearchBuilder", searchBuilderID);
        } else {
            final BeanDefinition def = v2Parser.createTemplateBuilder();
            if (def != null) {
                builder.addPropertyValue("executableSearchBuilder", def);
            }
        }

        if (v2Parser.getConnectionReadOnly() != null) {
            // V4 deprecation
            DeprecationSupport.warnOnce(ObjectType.ATTRIBUTE, "readOnlyConnection",
                    parserContext.getReaderContext().getResource().getDescription(),
                    "to modify the JDBC URI (default is false)");
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
            builder.addPropertyValue("resultsCache", v2Parser.createCache(parserContext));
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
         * Get the bean ID of an externally defined search builder.
         * 
         * @return search builder bean ID
         */
        @Nullable public String getBeanSearchBuilderID() {
            return AttributeSupport.getAttributeValue(configElement, null, "executableSearchBuilderRef");
        }
        
        /**
         * Create the definition of the template driven search builder.
         * 
         * @return the bean definition for the template search builder.
         */
        @Nonnull public BeanDefinition createTemplateBuilder() {
            final BeanDefinitionBuilder templateBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(V2CompatibleTemplateExecutableStatementBuilder.class);
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
                templateBuilder.addPropertyValue("queryTimeout", queryTimeout);
            }

            final List<Element> queryTemplates = ElementSupport.getChildElementsByTagNameNS(configElement, 
                    AttributeResolverNamespaceHandler.NAMESPACE, "QueryTemplate");
            
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
                    AttributeResolverNamespaceHandler.NAMESPACE, "Column");
            if (!columns.isEmpty()) {
                final ManagedMap<String, String> renamingMap = new ManagedMap<>();
                for (final Element column : columns) {
                    final String columnName = AttributeSupport.getAttributeValue(column, null, "columnName");
                    final String attributeId = AttributeSupport.getAttributeValue(column, null, "attributeID");
                    if (columnName != null && attributeId != null) {
                        renamingMap.put(columnName, attributeId);
                    }
                }
                mapper.addPropertyValue("resultRenamingMap", renamingMap);
            }
                        
            final String noResultIsError =
                    AttributeSupport.getAttributeValue(configElement, new QName("noResultIsError"));
            if (noResultIsError != null) {
                mapper.addPropertyValue("noResultAnError", SpringSupport.getStringValueAsBoolean(noResultIsError));
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
            validator.setInitMethodName("initialize");
            validator.setDestroyMethodName("destroy");
            if (dataSource instanceof String) {
                validator.addPropertyReference("dataSource", (String) dataSource); 
            } else {
                validator.addPropertyValue("dataSource", dataSource);
            }
            validator.addPropertyValue("throwValidateError", true);
            return validator.getBeanDefinition();
        }
        
        /**
         * Create the results cache. See {@link CacheConfigParser}.
         * 
         * @param parserContext bean parser context
         * 
         * @return results cache
         */
        @Nullable public BeanDefinition createCache(@Nonnull final ParserContext parserContext) {
            final CacheConfigParser parser = new CacheConfigParser(configElement);
            return parser.createCache(parserContext);
        }
        
        /** The parent parser's log prefix.
         * @return the log prefix.
         */
        @Nonnull @NotEmpty private String getLogPrefix() {
            return logPrefix;
        }
    }
    
}
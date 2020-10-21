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

package net.shibboleth.idp.attribute.resolver.spring.dc.storage.impl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.resolver.dc.storage.impl.ScriptedStorageMappingStrategy;
import net.shibboleth.idp.attribute.resolver.dc.storage.impl.StorageServiceDataConnector;
import net.shibboleth.idp.attribute.resolver.dc.storage.impl.TemplatedSearchBuilder;
import net.shibboleth.idp.attribute.resolver.spring.dc.AbstractDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.CacheConfigParser;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.ScriptTypeBeanParser;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Bean definition Parser for a {@link StorageServiceDataConnector}. */
public class StorageServiceDataConnectorParser extends AbstractDataConnectorParser {

    /** Schema type name. */
    @Nonnull public static final QName TYPE_NAME =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "StorageService");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StorageServiceDataConnectorParser.class);

    /** {@inheritDoc} */
    @Override protected Class<StorageServiceDataConnector> getNativeBeanClass() {
        return StorageServiceDataConnector.class;
    }
    
    /** {@inheritDoc} */
    @Override protected void doV2Parse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {

        log.debug("{} Parsing custom configuration {}", getLogPrefix(), config);

        final V2Parser v2Parser = new V2Parser(config, getLogPrefix());
        
        final String searchBuilderID = v2Parser.getBeanSearchBuilderID();
        if (searchBuilderID != null) {
            builder.addPropertyReference("executableSearchBuilder", searchBuilderID);
        } else {
            builder.addPropertyValue("executableSearchBuilder", v2Parser.createTemplateBuilder());
        }

        final String mappingStrategyID = v2Parser.getBeanMappingStrategyID();
        if (mappingStrategyID != null) {
            builder.addPropertyReference("mappingStrategy", mappingStrategyID);
            if (config.hasAttributeNS(null, "generatedAttributeID")) {
                log.warn("{} Ignoring generatedAttributeID in favor of explicit mapping strategy", getLogPrefix());
            }
        } else {
            final BeanDefinition def = v2Parser.createMappingStrategy(config.getAttributeNS(null, "id"));
            if (def != null) {
                builder.addPropertyValue("mappingStrategy", def);
                if (config.hasAttributeNS(null, "generatedAttributeID")) {
                    log.warn("{} Ignoring generatedAttributeID in favor of <RecordMapping> element", getLogPrefix());
                }
            } else {
                builder.addPropertyValue("generatedAttributeID", config.getAttributeNS(null, "generatedAttributeID"));
            }
        }
        
        final String resultCacheBeanID = CacheConfigParser.getBeanResultCacheID(config);
        if (null != resultCacheBeanID) {
           builder.addPropertyReference("resultsCache", resultCacheBeanID);
        } else {
            builder.addPropertyValue("resultsCache", v2Parser.createCache(parserContext));
        }
        
        builder.addPropertyReference("storageService", config.getAttributeNS(null, "storageServiceRef"));

        final String noResultIsError =
                AttributeSupport.getAttributeValue(config, new QName("noResultIsError"));
        if (noResultIsError != null) {
            builder.addPropertyValue("noResultAnError", SpringSupport.getStringValueAsBoolean(noResultIsError));
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
        @Nonnull private final Element configElement;

        /** Class logger. */
        @Nonnull private final Logger log = LoggerFactory.getLogger(V2Parser.class);

        /** Parent parser's log prefix.*/
        @Nonnull @NotEmpty private final String logPrefix;

        /**
         * Creates a new V2Parser with the supplied element.
         * 
         * @param config StorageService DataConnector element
         * @param prefix the parent parser's log prefix.
         */
        public V2Parser(@Nonnull final Element config,  @Nonnull @NotEmpty final String prefix) {
            Constraint.isNotNull(config, "StorageService DataConnector element cannot be null");
            configElement = config;
            logPrefix = prefix;
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
         * Create the definition of the search builder.
         * 
         * @return the bean definition for the search builder, or null
         */
        @Nullable public BeanDefinition createTemplateBuilder() {
            
            final List<Element> contextTemplates = ElementSupport.getChildElements(configElement, 
                    new QName(AttributeResolverNamespaceHandler.NAMESPACE, "ContextTemplate"));
            final List<Element> keyTemplates = ElementSupport.getChildElements(configElement, 
                    new QName(AttributeResolverNamespaceHandler.NAMESPACE, "KeyTemplate"));
            if (contextTemplates.size() == 0 || keyTemplates.size() == 0) {
                return null;
            }
            
            final BeanDefinitionBuilder templateBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(TemplatedSearchBuilder.class);
            templateBuilder.setInitMethodName("initialize");
            templateBuilder.setDestroyMethodName("destroy");

            String velocityEngineRef = StringSupport.trimOrNull(configElement.getAttributeNS(null, "templateEngine"));
            if (null == velocityEngineRef) {
                velocityEngineRef = "shibboleth.VelocityEngine";
            }
            templateBuilder.addPropertyReference("velocityEngine", velocityEngineRef);

            if (contextTemplates.size() > 1) {
                log.warn("{} A maximum of 1 <ContextTemplate> should be specified; the first one has been used",
                        getLogPrefix());
            } else if (keyTemplates.size() > 1) {
                log.warn("{} A maximum of 1 <KeyTemplate> should be specified; the first one has been used",
                        getLogPrefix());
            }
            
            final Element contextTemplate = contextTemplates.get(0);
            final Element keyTemplate = keyTemplates.get(0);
            
            if (configElement.hasAttributeNS(null, "customObjectRef")) {
                templateBuilder.addPropertyReference("customObject",
                        configElement.getAttributeNS(null, "customObjectRef"));
            }
            
            templateBuilder.addPropertyValue("contextTemplateText", contextTemplate.getTextContent());
            templateBuilder.addPropertyValue("keyTemplateText", keyTemplate.getTextContent());
            
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
         * Create the scripted result mapping strategy.
         * 
         * @param id the ID of the 
         * 
         * @return mapping strategy
         */
        @Nullable public BeanDefinition createMappingStrategy(@Nullable final String id) {

            final List<Element> mappings = ElementSupport.getChildElements(configElement, 
                    new QName(AttributeResolverNamespaceHandler.NAMESPACE, "RecordMapping"));
    
            if (mappings.size() > 1) {
                log.warn("{} A maximum of 1 <RecordMapping> should be specified; the first one has been used",
                        getLogPrefix());
            } else if (mappings.isEmpty()) {
                // No element means to fall back to simple record mapping.
                return null;
            }
            
            final BeanDefinitionBuilder mapper =
                    ScriptTypeBeanParser.parseScriptType(ScriptedStorageMappingStrategy.class, mappings.get(0));
            if (id != null) {
                mapper.addPropertyValue("logPrefix", id + ':');
            }
            
            return mapper.getBeanDefinition();
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
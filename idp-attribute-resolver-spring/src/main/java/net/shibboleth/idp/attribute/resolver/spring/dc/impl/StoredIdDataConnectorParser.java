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
import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.impl.JDBCPairwiseIdStore;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for {@link StoredIDDataConnector}.
 */
public class StoredIdDataConnectorParser extends ComputedIdDataConnectorParser {

    /** Schema type - resolver. */
    @Nonnull public static final QName TYPE_NAME_RESOLVER =
                new QName(AttributeResolverNamespaceHandler.NAMESPACE, "StoredId");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StoredIdDataConnectorParser.class);

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder, "storedId");

        builder.addPropertyValue("pairwiseIdStore", doJDBCPairwiseIdStore(config, parserContext));
    }

    /**
     * Parse the config and define a bean for a {@link JDBCPairwiseIdStore}.
     * 
     * @param config the XML element being parsed
     * @param parserContext the object encapsulating the current state of the parsing process
     * @return bean definition for the store object to inject
     */
    @Nonnull protected BeanDefinition doJDBCPairwiseIdStore(@Nonnull final Element config,
            @Nonnull final ParserContext parserContext) {
        
        final BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.genericBeanDefinition(JDBCPairwiseIdStore.class);
        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");

        final String beanDataSource = ManagedConnectionParser.getBeanDataSourceID(config);
        if (beanDataSource != null) {
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
            builder.addPropertyValue("verifyDatabase",
                    StringSupport.trimOrNull(config.getAttributeNS(null, "failFast")));
        }

        if (config.hasAttributeNS(null, "retryableErrors")) {
            builder.addPropertyValue("retryableErrors",
                    SpringSupport.getAttributeValueAsList(config.getAttributeNodeNS(null, "retryableErrors")));
        }

        if (config.hasAttributeNS(null, "salt")) {
            builder.addPropertyValue("initialValueStore", doComputedPairwiseIdStore(config, parserContext));
        }
        
        return builder.getBeanDefinition();
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
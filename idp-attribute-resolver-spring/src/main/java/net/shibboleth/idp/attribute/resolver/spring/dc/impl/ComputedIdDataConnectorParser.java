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

import net.shibboleth.idp.attribute.impl.ComputedPairwiseIdStore;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for configuring {@link ComputedIDDataConnector}.
 */
public class ComputedIdDataConnectorParser extends PairwiseIdDataConnectorParser {

    /** Schema type - resolver. */
    @Nonnull public static final QName TYPE_NAME_RESOLVER = new QName(AttributeResolverNamespaceHandler.NAMESPACE, 
            "ComputedId");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ComputedIdDataConnectorParser.class);
    
    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder, "computedId");

        builder.addPropertyValue("pairwiseIdStore", doComputedPairwiseIdStore(config, parserContext));
    }

    /**
     * Parse the config and define a bean for a {@link ComputedPairwiseIdStore}.
     * 
     * @param config the XML element being parsed
     * @param parserContext the object encapsulating the current state of the parsing process
     * @return bean definition for the store object to inject
     */
    @Nonnull protected BeanDefinition doComputedPairwiseIdStore(@Nonnull final Element config,
            @Nonnull final ParserContext parserContext) {
        
        final BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.genericBeanDefinition(ComputedPairwiseIdStore.class);
        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");

        if (config.hasAttributeNS(null, "algorithm")) {
            builder.addPropertyValue("algorithm", config.getAttributeNS(null, "algorithm"));
        }

        if (config.hasAttributeNS(null, "encoding")) {
            builder.addPropertyValue("encoding", config.getAttributeNS(null, "encoding"));
        }

        final String salt;
        if (config.hasAttributeNS(null, "salt")) {
            salt = config.getAttributeNS(null, "salt");
        } else {
            salt = null;
        }
        
        if (null == salt) {
            log.debug("{} No salt provided", getLogPrefix());
        } else {
            log.debug("{} See TRACE log for the salt value", getLogPrefix());
            log.trace("{} Salt: '{}'", getLogPrefix(), salt);
        }

        builder.addPropertyValue("salt", salt);

        return builder.getBeanDefinition();
    }

}
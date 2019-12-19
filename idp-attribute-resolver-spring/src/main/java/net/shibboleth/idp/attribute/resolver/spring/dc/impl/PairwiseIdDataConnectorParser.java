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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.idp.attribute.resolver.dc.impl.PairwiseIdDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.BaseResolverPluginParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.AbstractDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

/**
 * Spring bean definition parser for configuring
 * {@link net.shibboleth.idp.attribute.resolver.dc.impl.PairwiseIdDataConnector} variants.
 */
public class PairwiseIdDataConnectorParser extends BaseResolverPluginParser {

    /** Schema type - resolver. */
    @Nonnull public static final QName TYPE_NAME_RESOLVER = new QName(AttributeResolverNamespaceHandler.NAMESPACE, 
            "PairwiseId");
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PairwiseIdDataConnectorParser.class);

    /** {@inheritDoc} */
    @Override protected Class<PairwiseIdDataConnector> getBeanClass(final Element element) {
        return PairwiseIdDataConnector.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        doParse(config, parserContext, builder, "pairwiseId");
    }

    /**
     * Parse any common material for {@link net.shibboleth.idp.attribute.resolver.dc.impl.PairwiseIdDataConnector}.
     * 
     * @param config the DOM element under consideration.
     * @param parserContext Spring's context.
     * @param builder Spring's bean builder.
     * @param generatedIdDefaultName the name to give the generated Attribute if none was provided.
     */
    protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder, @Nullable final String generatedIdDefaultName) {
        super.doParse(config, parserContext, builder);
        
        final String generatedAttribute;
        if (config.hasAttributeNS(null, "generatedAttributeID")) {
            generatedAttribute = StringSupport.trimOrNull(config.getAttributeNS(null, "generatedAttributeID"));
        } else {
            generatedAttribute = generatedIdDefaultName;
        }
        builder.addPropertyValue("generatedAttributeId", generatedAttribute);
        
        // If this isn't one of the older hard-wired types, inject an arbitrary ID store ref.
        if (getClass().equals(PairwiseIdDataConnectorParser.class)) {
            builder.addPropertyReference("pairwiseIdStore", config.getAttributeNS(null, "pairwiseIdStoreRef"));
        }

        final List<Element> failoverConnector = ElementSupport.getChildElements(config, 
                AbstractDataConnectorParser.FAILOVER_DATA_CONNECTOR_ELEMENT_NAME);
        if (failoverConnector != null && !failoverConnector.isEmpty()) {
            if (failoverConnector.size() > 1) {
                log.warn("{} More than one failover data connector specified, taking the first", getLogPrefix());
            }
            
            final String connectorId = StringSupport.trimOrNull(failoverConnector.get(0).getAttributeNS(null, "ref"));
            log.debug("{} Setting the following failover data connector dependencies: {}", getLogPrefix(), connectorId);
            builder.addPropertyValue("failoverDataConnectorId", connectorId);
        }
    }

    /**
     * Return a string which is to be prepended to all log messages.
     * 
     * @return "Attribute Definition: '&lt;definitionID&gt;' :"
     */
    @Override @Nonnull @NotEmpty protected String getLogPrefix() {
        final StringBuilder builder = new StringBuilder("Data Connector '").append(getDefinitionId()).append("':");
        return builder.toString();
    }
    
}
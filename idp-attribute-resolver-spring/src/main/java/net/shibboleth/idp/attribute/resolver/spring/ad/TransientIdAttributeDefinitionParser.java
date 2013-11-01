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

package net.shibboleth.idp.attribute.resolver.spring.ad;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.impl.ad.TransientIdAttributeDefinition;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for {@link TransientIdAttributeDefinitionFactoryBean}s.
 */
public class TransientIdAttributeDefinitionParser extends BaseAttributeDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "TransientId");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(TransientIdAttributeDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class<TransientIdAttributeDefinition> getBeanClass(@Nullable Element element) {
        return TransientIdAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        Long lifetime = null;

        if (config.hasAttributeNS(null, "lifetime")) {
            lifetime = AttributeSupport.getDurationAttributeValueAsLong(config.getAttributeNodeNS(null, "lifetime"));
        }

        if (null != lifetime) {
            log.debug("{}: lifetime {}", getLogPrefix(), lifetime);
            builder.addPropertyValue("idLifetime", lifetime.longValue());
        }

        String idStore = "shibboleth.StorageService";
        if (config.hasAttributeNS(null, "storageServiceRef")) {
            idStore = StringSupport.trimOrNull(config.getAttributeNS(null, "storageServiceRef"));
        }

        log.debug("{} idStore '{}'", getLogPrefix(), idStore);
        builder.addPropertyReference("idStore", idStore);
    }
}

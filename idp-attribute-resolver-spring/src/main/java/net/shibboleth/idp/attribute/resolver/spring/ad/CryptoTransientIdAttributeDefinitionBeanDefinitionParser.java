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

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.impl.ad.CryptoTransientIdAttributeDefinition;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for {@link CryptoTransientIdAttributeDefinition}s.
 */
public class CryptoTransientIdAttributeDefinitionBeanDefinitionParser extends
        BaseAttributeDefinitionBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "CryptoTransientId");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(CryptoTransientIdAttributeDefinitionBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return CryptoTransientIdAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);
        Long lifetime = null;
        
        if (config.hasAttributeNS(null, "lifetime")) {
            lifetime = AttributeSupport.getDurationAttributeValueAsLong(config.getAttributeNodeNS(null, "lifetime"));
            log.debug("{} lifetime of {} specified.", getLogPrefix(), lifetime);
        }

        if (null != lifetime) {
            builder.addPropertyValue("idLifetime", lifetime.longValue());
        }

        builder.addPropertyReference("dataSealer",
                StringSupport.trimOrNull(config.getAttributeNS(null, "dataSealerRef")));
    }
}

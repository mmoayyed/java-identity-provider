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

package net.shibboleth.idp.attribute.resolver.spring.ad.mapped.impl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.resolver.ad.mapped.impl.MappedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.ad.BaseAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.AttributeDefinitionNamespaceHandler;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

/** Bean definition parser for a {@link MappedAttributeDefinition}. */
public class MappedAttributeDefinitionParser extends BaseAttributeDefinitionParser {

    /** Schema type name - ad: (legacy). */
    @Nonnull public static final QName TYPE_NAME_AD =
            new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "Mapped");

    /** Schema type name - resolver:. */
    @Nonnull public static final QName TYPE_NAME_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "Mapped");

    /** return Value element name - ad: (legacy). */
    @Nonnull public static final QName DEFAULT_VALUE_ELEMENT_NAME_AD =
            new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "DefaultValue");

    /** return Value element name - resolver:. */
    @Nonnull public static final QName DEFAULT_VALUE_ELEMENT_NAME_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "DefaultValue");

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(MappedAttributeDefinitionParser.class);

    /** {@inheritDoc} */
    @Override protected Class<MappedAttributeDefinition> getBeanClass(@Nullable final Element element) {
        return MappedAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        final List<Element> defaultValueElements =
                ElementSupport.getChildElements(config, DEFAULT_VALUE_ELEMENT_NAME_AD);
        defaultValueElements.addAll(ElementSupport.getChildElements(config, DEFAULT_VALUE_ELEMENT_NAME_RESOLVER));
        String defaultValue = null;
        String passThru = null;

        if (null != defaultValueElements && defaultValueElements.size() > 0) {
            if (defaultValueElements.size() > 1) {
                log.warn("{} More than one <DefaultValue> specified");
            }
            final Element defaultValueElement = defaultValueElements.get(0);
            defaultValue = StringSupport.trimOrNull(defaultValueElement.getTextContent());

            if (defaultValueElement.hasAttributeNS(null, "passThru")) {
                if (null != defaultValue) {
                    log.info("{} Default value and passThru both specified", getLogPrefix(), getDefinitionId());
                }
                passThru = StringSupport.trimOrNull(defaultValueElement.getAttributeNS(null, "passThru"));
                builder.addPropertyValue("passThru", passThru);
            }
        }

        final List<Element> valueMapElements = ElementSupport.getChildElements(config, ValueMapParser.TYPE_NAME_AD);
        valueMapElements.addAll(ElementSupport.getChildElements(config, ValueMapParser.TYPE_NAME_RESOLVER));
        if (null == valueMapElements || valueMapElements.size() == 0) {
            throw new BeanCreationException(
                    "Attribute Definition '" + getDefinitionId() + "' At least one ValueMap must be specified");
        }

        final List<BeanDefinition> valueMaps = SpringSupport.parseCustomElements(valueMapElements, parserContext);

        log.debug("{} passThru = {}, defaultValue = {}, {} value maps",
                new Object[] {getLogPrefix(), passThru, defaultValue, valueMaps.size(),});
        log.trace("{} Value maps {}", getLogPrefix(), valueMaps);

        builder.addPropertyValue("defaultValue", defaultValue);
        builder.addPropertyValue("valueMaps", valueMaps);
    }

    /** {@inheritDoc} */
    @Override protected boolean needsAttributeSourceID() {
        return true;
    }

}
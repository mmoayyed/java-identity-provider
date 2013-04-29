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

package net.shibboleth.idp.attribute.resolver.spring.ad.mapped;

import java.util.List;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.impl.ad.mapped.MappedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.ad.AttributeDefinitionNamespaceHandler;
import net.shibboleth.idp.attribute.resolver.spring.ad.BaseAttributeDefinitionBeanDefinitionParser;
import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Bean definition parser for a {@link MappedAttributeDefinition}. */
public class MappedAttributeDefinitionBeanDefinitionParser extends BaseAttributeDefinitionBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "Mapped");

    /** return Value element name. */
    public static final QName DEFAULT_VALUE_ELEMENT_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE,
            "DefaultValue");

    /** log. */
    private Logger log = LoggerFactory.getLogger(MappedAttributeDefinitionBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return MappedAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        final List<Element> defaultValueElements = ElementSupport.getChildElements(config, DEFAULT_VALUE_ELEMENT_NAME);
        String defaultValue = null;
        boolean passThru = false;

        if (null != defaultValueElements && defaultValueElements.size() > 0) {
            Element defaultValueElement = defaultValueElements.get(0);
            defaultValue = StringSupport.trimOrNull(defaultValueElement.getTextContent());

            if (defaultValueElement.hasAttributeNS(null, "passThru")) {
                if (null != defaultValue) {
                    log.info("Attribute Definition: '{}' default value and passThru both specified", getDefinitionId());
                }
                passThru =
                        AttributeSupport.getAttributeValueAsBoolean(defaultValueElement.getAttributeNodeNS(null,
                                "passThru"));
            }
        }

        final List<Element> valueMapElements =
                ElementSupport.getChildElements(config, ValueMapBeanDefinitionParser.TYPE_NAME);

        if (null == valueMapElements || valueMapElements.size() == 0) {
            throw new BeanCreationException("Attribute Definition '" + getDefinitionId()
                    + "' At least one ValueMap must be specified");
        }

        ManagedList<BeanDefinition> valueMaps = SpringSupport.parseCustomElements(valueMapElements, parserContext);

        log.debug("{} passThru = {}, defaultValue = {}, {} value maps.", new Object[] {
                getLogPrefix(), passThru, defaultValue, valueMaps.size(),});
        log.trace("{} value maps {}", getLogPrefix(), valueMaps);

        builder.addPropertyValue("defaultValue", defaultValue);
        builder.addPropertyValue("passThru", passThru);
        builder.addPropertyValue("valueMaps", valueMaps);
    }

}
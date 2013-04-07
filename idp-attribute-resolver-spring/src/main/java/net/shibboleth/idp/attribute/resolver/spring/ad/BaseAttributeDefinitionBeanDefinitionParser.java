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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.spring.AttributeResolverNamespaceHandler;
import net.shibboleth.idp.attribute.resolver.spring.BaseResolverPluginBeanDefinitionParser;
import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Base spring bean definition parser for attribute definitions. AttributeDefinition implementations should provide a
 * custom BeanDefinitionParser by extending this class and overriding the doParse() method to parse any additional
 * attributes or elements it requires. Standard attributes and elements defined by the ResolutionPlugIn and
 * AttributeDefinition schemas will automatically attempt to be parsed.
 */
public abstract class BaseAttributeDefinitionBeanDefinitionParser extends BaseResolverPluginBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeResolverNamespaceHandler.NAMESPACE,
            "AttributeDefinition");

    /** Local name of attribute encoder. */
    public static final QName ATTRIBUTE_ENCODER_ELEMENT_NAME = new QName(AttributeResolverNamespaceHandler.NAMESPACE,
            "AttributeEncoder");

    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(BaseAttributeDefinitionBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        final List<Element> displayNames =
                ElementSupport.getChildElements(config, new QName(AttributeResolverNamespaceHandler.NAMESPACE,
                        "DisplayName"));
        if (displayNames != null && !displayNames.isEmpty()) {
            final Map<Locale, String> names = processLocalizedElement(displayNames);
            log.debug("AttributeDefinition {}: setting displayNames {}", getDefinitionId(), names);
            builder.addPropertyValue("displayNames", names);
        }

        final List<Element> displayDescriptions =
                ElementSupport.getChildElements(config, new QName(AttributeResolverNamespaceHandler.NAMESPACE,
                        "DisplayDescription"));
        if (displayDescriptions != null && !displayDescriptions.isEmpty()) {
            final Map<Locale, String> names = processLocalizedElement(displayDescriptions);
            log.debug("AttributeDefinition {}: setting displayDescriptions {}", getDefinitionId(), names);
            builder.addPropertyValue("displayDescriptions", names);
        }

        Boolean dependencyOnly = new Boolean(false);
        if (config.hasAttributeNS(null, "dependencyOnly")) {
            dependencyOnly =
                    AttributeSupport.getAttributeValueAsBoolean(config.getAttributeNodeNS(null, "dependencyOnly"));
            if (null == dependencyOnly) {
                log.error("AttributeDefinition {}: value for 'dependencyOnly'"
                        + " should be 'true','0','false', or '0'", getDefinitionId());
                dependencyOnly = new Boolean(false);
            }
        }
        log.debug("Configuration for {}: setting displayDescriptions {}", config.getLocalName(), dependencyOnly);
        builder.addPropertyValue("dependencyOnly", dependencyOnly);
        
        if (config.hasAttributeNS(null, "sourceAttributeID")) {
            String sourceAttributeId = config.getAttributeNodeNS(null, "sourceAttributeID").getValue();
            log.debug("Configuration for {}: setting sourceAttributeId {}", config.getLocalName(), sourceAttributeId);
            builder.addPropertyValue("sourceAttributeId", sourceAttributeId);
        }

        final List<Element> attributeEncoders =
                ElementSupport.getChildElements(config, new QName(AttributeResolverNamespaceHandler.NAMESPACE,
                        "AttributeEncoder"));

        if (attributeEncoders != null && !attributeEncoders.isEmpty()) {
            log.debug("Configuration for {}: adding {} encoders.", getDefinitionId(), attributeEncoders.size());
            builder.addPropertyValue("attributeEncoders",
                    SpringSupport.parseCustomElements(attributeEncoders, parserContext));
        }
    }

    /**
     * Used to process string elements that contain an xml:lang attribute expressing localization.
     * 
     * @param elements list of elements, must not be null, may be empty
     * 
     * @return the localized string indexed by locale
     */
    protected Map<Locale, String> processLocalizedElement(List<Element> elements) {
        HashMap<Locale, String> localizedString = new HashMap<Locale, String>(elements.size());
        for (Element element : elements) {
            localizedString.put(AttributeSupport.getXMLLangAsLocale(element), element.getTextContent());
        }

        return localizedString;
    }
}
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

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.spring.AttributeResolverNamespaceHandler;
import net.shibboleth.idp.attribute.resolver.spring.BaseResolverPluginBeanDefinitionParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

// TODO incomplete port from v2
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
    protected void doParse(String pluginId, Element pluginConfig, Map<QName, List<Element>> pluginConfigChildren,
            BeanDefinitionBuilder pluginBuilder, ParserContext parserContext) {

        // String sourceAttributeId = pluginConfig.getAttributeNS(null, "sourceAttributeID");
        // log.debug("Setting source attribute ID for attribute definition {} to: {}", pluginId, sourceAttributeId);
        // pluginBuilder.addPropertyValue("sourceAttributeId", sourceAttributeId);

        // List<Element> displayNames = pluginConfigChildren.get(new QName(AttributeResolverNamespaceHandler.NAMESPACE,
        // "DisplayName"));
        // if (displayNames != null) {
        // log.debug("Setting {} display names for attribute definition {}", displayNames.size(), pluginId);
        // pluginBuilder.addPropertyValue("displayNames", processLocalizedElement(displayNames));
        // }

        // List<Element> displayDescriptions = pluginConfigChildren.get(new QName(
        // AttributeResolverNamespaceHandler.NAMESPACE, "DisplayDescription"));
        // if (displayDescriptions != null) {
        // log.debug("Setting {} display descriptions for attribute definition {}", displayDescriptions.size(),
        // pluginId);
        // pluginBuilder.addPropertyValue("displayDescriptions", processLocalizedElement(displayDescriptions));
        // }

        // boolean dependencyOnly = false;
        // if (pluginConfig.hasAttributeNS(null, "dependencyOnly")) {
        // dependencyOnly = XMLHelper.getAttributeValueAsBoolean(pluginConfig.getAttributeNodeNS(null,
        // "dependencyOnly"));
        // }
        // if (log.isDebugEnabled()) {
        // log.debug("Attribute definition {} produces attributes that are only dependencies: {}", pluginId,
        // dependencyOnly);
        // }
        // pluginBuilder.addPropertyValue("dependencyOnly", dependencyOnly);

        // pluginBuilder.addPropertyValue("attributeEncoders", SpringConfigurationUtils.parseInnerCustomElements(
        // pluginConfigChildren.get(ATTRIBUTE_ENCODER_ELEMENT_NAME), parserContext));
    }

    /**
     * Used to process string elements that contain an xml:lang attribute expressing localization.
     * 
     * @param elements list of elements, must not be null, may be empty
     * 
     * @return the localized string indexed by locale
     */
    // protected Map<Locale, String> processLocalizedElement(List<Element> elements) {
    // HashMap<Locale, String> localizedString = new HashMap<Locale, String>(elements.size());
    // for (Element element : elements) {
    // localizedString.put(XMLHelper.getLanguage(element), element.getTextContent());
    // }

    // return localizedString;
    // }
}
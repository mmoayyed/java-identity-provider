/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.attributeDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for scripted attribute configuration elements.
 */
public class TemplateAttributeDefinitionBeanDefinitionParser extends BaseAttributeDefinitionBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "Template");

    /** SourceValue element name. */
    public static final QName TEMPLATE_ELEMENT_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE,
            "Template");

    /** SourceValue element name. */
    public static final QName SOURCE_ATTRIBUTE_ELEMENT_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE,
            "SourceAttribute");

    /** Class logger. */
    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(TemplateAttributeDefinitionBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return TemplateAttributeDefinitionFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(String pluginId, Element pluginConfig, Map<QName, List<Element>> pluginConfigChildren,
            BeanDefinitionBuilder pluginBuilder, ParserContext parserContext) {
        super.doParse(pluginId, pluginConfig, pluginConfigChildren, pluginBuilder, parserContext);

        if (pluginConfigChildren.containsKey(TEMPLATE_ELEMENT_NAME)) {
            Element templateElement = pluginConfigChildren.get(TEMPLATE_ELEMENT_NAME).get(0);
            String attributeTemplate = DatatypeHelper.safeTrimOrNullString(templateElement.getTextContent());
            pluginBuilder.addPropertyValue("attributeTemplate", attributeTemplate);
        }

        List<String> sourceAttributes = new ArrayList<String>();
        for (Element element : pluginConfigChildren.get(SOURCE_ATTRIBUTE_ELEMENT_NAME)) {
            sourceAttributes.add(DatatypeHelper.safeTrimOrNullString(element.getTextContent()));
        }
        pluginBuilder.addPropertyValue("sourceAttributes", sourceAttributes);
        
        String velocityEngineRef = pluginConfig.getAttributeNS(null, "velocityEngine");
        pluginBuilder.addPropertyReference("velocityEngine", velocityEngineRef);
    }
}
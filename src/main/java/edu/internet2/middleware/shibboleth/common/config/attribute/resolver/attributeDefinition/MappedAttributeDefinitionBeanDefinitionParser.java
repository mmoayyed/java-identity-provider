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
import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.ValueMap;

/**
 * Spring bean definition parser for mapped attribute definition.
 */
public class MappedAttributeDefinitionBeanDefinitionParser extends BaseAttributeDefinitionBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "Mapped");

    /** ValueMap element name. */
    public static final QName VALUEMAP_ELEMENT_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE,
            "ValueMap");

    /** SourceValue element name. */
    public static final QName SOURCE_VALUE_ELEMENT_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE,
            "SourceValue");

    /** ReturnValue element name. */
    public static final QName RETURN_VALUE_ELEMENT_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE,
            "ReturnValue");

    /** DefaultValue element name. */
    public static final QName DEFAULT_VALUE_ELEMENT_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE,
            "DefaultValue");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(MappedAttributeDefinitionBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return MappedAttributeDefinitionFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(String pluginId, Element pluginConfig, Map<QName, List<Element>> pluginConfigChildren,
            BeanDefinitionBuilder pluginBuilder, ParserContext parserContext) {
        super.doParse(pluginId, pluginConfig, pluginConfigChildren, pluginBuilder, parserContext);

        List<ValueMap> valueMaps = processValueMaps(pluginId, pluginConfigChildren, pluginBuilder);
        pluginBuilder.addPropertyValue("valueMaps", valueMaps);

        if (pluginConfigChildren.containsKey(DEFAULT_VALUE_ELEMENT_NAME)) {
            List<Element> defaultValueElems = pluginConfigChildren.get(DEFAULT_VALUE_ELEMENT_NAME);
            String defaultValue = DatatypeHelper.safeTrimOrNullString(defaultValueElems.get(0).getTextContent());
            pluginBuilder.addPropertyValue("defaultValue", defaultValue);
            if (log.isDebugEnabled()) {
                log.debug("Attribute definition {} default value: {}", pluginId, defaultValue);
            }

            Element defaultValueElem = defaultValueElems.get(0);
            boolean passThru = false;
            if (defaultValueElem.hasAttributeNS(null, "passThru")) {
                passThru = XMLHelper.getAttributeValueAsBoolean(defaultValueElem.getAttributeNodeNS(null, "passThru"));
            }
            pluginBuilder.addPropertyValue("passThru", passThru);
            if (log.isDebugEnabled()) {
                log.debug("Attribute definition {} uses default value pass thru: {}", pluginId, passThru);
            }
        }

    }

    /**
     * Process the value map elements.
     * 
     * @param pluginId ID of this data connector
     * @param pluginConfigChildren configuration elements
     * @param pluginBuilder the bean definition parser
     * 
     * @return the list of value maps
     */
    protected List<ValueMap> processValueMaps(String pluginId, Map<QName, List<Element>> pluginConfigChildren,
            BeanDefinitionBuilder pluginBuilder) {
        List<ValueMap> maps = new ArrayList<ValueMap>();

        ValueMap valueMap;
        String returnValue;
        String sourceValue;
        boolean ignoreCase;
        boolean partialMatch;
        if (pluginConfigChildren.containsKey(VALUEMAP_ELEMENT_NAME)) {
            for (Element valueMapElem : pluginConfigChildren.get(VALUEMAP_ELEMENT_NAME)) {
                valueMap = new ValueMap();

                Map<QName, List<Element>> children = XMLHelper.getChildElements(valueMapElem);

                if (children.containsKey(RETURN_VALUE_ELEMENT_NAME)) {
                    List<Element> returnValueElems = children.get(RETURN_VALUE_ELEMENT_NAME);
                    returnValue = DatatypeHelper.safeTrimOrNullString(returnValueElems.get(0).getTextContent());
                    valueMap.setReturnValue(returnValue);
                }

                if (children.containsKey(SOURCE_VALUE_ELEMENT_NAME)) {
                    for (Element sourceValueElem : children.get(SOURCE_VALUE_ELEMENT_NAME)) {
                        sourceValue = DatatypeHelper.safeTrim(sourceValueElem.getTextContent());

                        if (sourceValueElem.hasAttributeNS(null, "ignoreCase")) {
                            ignoreCase = XMLHelper.getAttributeValueAsBoolean(sourceValueElem.getAttributeNodeNS(null,
                                    "ignoreCase"));
                        } else {
                            ignoreCase = false;
                        }

                        if (sourceValueElem.hasAttributeNS(null, "partialMatch")) {
                            partialMatch = XMLHelper.getAttributeValueAsBoolean(sourceValueElem.getAttributeNodeNS(
                                    null, "partialMatch"));
                        } else {
                            partialMatch = false;
                        }

                        valueMap.getSourceValues().add(valueMap.new SourceValue(sourceValue, ignoreCase, partialMatch));
                    }
                }

                maps.add(valueMap);
            }
        }

        return maps;
    }

}
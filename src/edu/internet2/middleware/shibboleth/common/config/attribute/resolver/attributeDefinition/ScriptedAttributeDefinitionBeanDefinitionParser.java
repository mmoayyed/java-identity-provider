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

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for scripted attribute configuration elements.
 */
public class ScriptedAttributeDefinitionBeanDefinitionParser extends BaseAttributeDefinitionBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "Script");

    /** Class logger. */
    private static Logger log = Logger.getLogger(ScriptedAttributeDefinitionBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return ScriptedAttribtueDefinitionFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(String pluginId, Element pluginConfig, Map<QName, List<Element>> pluginConfigChildren,
            BeanDefinitionBuilder pluginBuilder, ParserContext parserContext) {
        super.doParse(pluginId, pluginConfig, pluginConfigChildren, pluginBuilder, parserContext);

        String scriptLanguage = pluginConfig.getAttributeNS(null, "language");
        if (log.isDebugEnabled()) {
            log.debug("Attribute definition " + pluginId + " scripting language: " + scriptLanguage);
        }
        pluginBuilder.addPropertyValue("language", scriptLanguage);

        List<Element> scriptElem = pluginConfigChildren.get(new QName(AttributeDefinitionNamespaceHandler.NAMESPACE,
                "Script"));
        if (scriptElem != null && scriptElem.size() > 0) {
            String script = scriptElem.get(0).getTextContent();
            if (log.isDebugEnabled()) {
                log.debug("Attribute definition " + pluginId + " script: " + script);
            }
            pluginBuilder.addPropertyValue("script", script);
        } else {
            List<Element> scriptFileElem = pluginConfigChildren.get(new QName(
                    AttributeDefinitionNamespaceHandler.NAMESPACE, "ScriptFile"));
            if (scriptFileElem != null && scriptFileElem.size() > 0) {
                String scriptFile = scriptFileElem.get(0).getTextContent();
                if (log.isDebugEnabled()) {
                    log.debug("Attribute definition " + pluginId + " script file: " + scriptFile);
                }
                pluginBuilder.addPropertyValue("sciptFile", scriptFile);
            }
        }
    }
}
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.BeanCreationException;
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
    private final Logger log = LoggerFactory.getLogger(ScriptedAttributeDefinitionBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return ScriptedAttributeDefinitionFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(String pluginId, Element pluginConfig, Map<QName, List<Element>> pluginConfigChildren,
            BeanDefinitionBuilder pluginBuilder, ParserContext parserContext) {
        super.doParse(pluginId, pluginConfig, pluginConfigChildren, pluginBuilder, parserContext);

        String scriptLanguage = "javascript";
        if (pluginConfig.hasAttributeNS(null, "language")) {
            scriptLanguage = pluginConfig.getAttributeNS(null, "language");
        }
        log.debug("Attribute definition {} scripting language: {}", pluginId, scriptLanguage);
        pluginBuilder.addPropertyValue("language", scriptLanguage);

        String script = null;
        List<Element> scriptElem = pluginConfigChildren.get(new QName(AttributeDefinitionNamespaceHandler.NAMESPACE,
                "Script"));
        if (scriptElem != null && scriptElem.size() > 0) {
            script = scriptElem.get(0).getTextContent();
        } else {
            List<Element> scriptFileElem = pluginConfigChildren.get(new QName(
                    AttributeDefinitionNamespaceHandler.NAMESPACE, "ScriptFile"));
            if (scriptFileElem != null && scriptFileElem.size() > 0) {
                String scriptFile = scriptFileElem.get(0).getTextContent();
                try {
                    script = DatatypeHelper.inputstreamToString(new FileInputStream(scriptFile), null);
                } catch (IOException e) {
                    throw new BeanCreationException(MessageFormatter.format("Unable to read script file '{}'",
                            scriptFile), e);
                }
            }
        }

        if (script == null) {
            throw new BeanCreationException("No script specified for this attribute definition");
        }
        log.debug("Attribute definition {} script: {}", pluginId, script);
        pluginBuilder.addPropertyValue("script", script);
    }
}
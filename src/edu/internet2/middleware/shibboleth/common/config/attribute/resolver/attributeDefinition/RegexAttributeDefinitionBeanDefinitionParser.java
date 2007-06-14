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
import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring Bean Definition Parser for static data connector.
 */
public class RegexAttributeDefinitionBeanDefinitionParser extends BaseAttributeDefinitionBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "Regex");

    /** Class logger. */
    private static Logger log = Logger.getLogger(RegexAttributeDefinitionBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return RegexAttributeDefinitionFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(String pluginId, Element pluginConfig, Map<QName, List<Element>> pluginConfigChildren,
            BeanDefinitionBuilder pluginBuilder, ParserContext parserContext) {
        super.doParse(pluginId, pluginConfig, pluginConfigChildren, pluginBuilder, parserContext);

        String regex = DatatypeHelper.safeTrimOrNullString(pluginConfig.getAttributeNS(null, "scope"));
        if (log.isDebugEnabled()) {
            log.debug("Setting regex of attribute definition " + pluginId + " to: " + regex);
        }
        pluginBuilder.addPropertyValue("regex", regex);
        
        
        String replacement = DatatypeHelper.safeTrimOrNullString(pluginConfig.getAttributeNS(null, "replacement"));
        if (log.isDebugEnabled()) {
            log.debug("Setting replacement of attribute definition " + pluginId + " to: " + replacement);
        }
        pluginBuilder.addPropertyValue("replacement", replacement);
        
        
        boolean partialMatch = Boolean.parseBoolean(pluginConfig.getAttributeNS(null, "partialMatch"));
        if (log.isDebugEnabled()) {
            log.debug("Setting partialMatch of attribute definition " + pluginId + " to: " + partialMatch);
        }
        pluginBuilder.addPropertyValue("partialMatch", partialMatch);
        
        
        boolean ignoreCase = Boolean.parseBoolean(pluginConfig.getAttributeNS(null, "ignoreCase"));
        if (log.isDebugEnabled()) {
            log.debug("Setting ignoreCase of attribute definition " + pluginId + " to: " + ignoreCase);
        }
        pluginBuilder.addPropertyValue("ignoreCase", ignoreCase);
    }
}
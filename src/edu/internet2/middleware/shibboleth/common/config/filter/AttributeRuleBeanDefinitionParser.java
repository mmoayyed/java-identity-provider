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

package edu.internet2.middleware.shibboleth.common.config.filter;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.AttributeRule;
import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Spring bean definition parser to configure an {@link AttributeRule}.
 */
public class AttributeRuleBeanDefinitionParser extends AbstractBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE, "AttributeType");

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE, "AttributeRuleType");

    /** Class logger. */
    private static Logger log = Logger.getLogger(AttributeFilterPoliciesBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AttributeRule.class);

        String ruleId = DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null, "id"));
        if (log.isDebugEnabled()) {
            log.debug("Processing configuration for attribute rule " + ruleId);
        }

        builder.addConstructorArg(ruleId);

        if (log.isDebugEnabled()) {
            log.debug("Processing value filter definition");
        }
        NodeList valueFilterNodes = element.getElementsByTagNameNS(AttributeFilterNamespaceHandler.NAMESPACE,
                AttributeFilterPoliciesBeanDefinitionParser.VALUE_FILTER_ELEMENT_LOCAL_NAME);
        if (valueFilterNodes.getLength() > 0) {
            Element valueFilterElem = (Element) valueFilterNodes.item(0);
            if (valueFilterElem.hasAttributeNS(null, "ref")) {
                builder.addPropertyReference("valueFilter", DatatypeHelper.safeTrimOrNullString(valueFilterElem
                        .getAttributeNS(null, "ref")));
            } else {
                builder.addPropertyValue("valueFilter", SpringConfigurationUtils.parseCustomElement(valueFilterElem,
                        parserContext));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No value filter specified for attribute rule " + ruleId);
            }
        }

        return builder.getBeanDefinition();
    }
}
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

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.AttributeRule;
import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Spring bean definition parser to configure an {@link AttributeRule}.
 */
public class AttributeRuleBeanDefinitionParser extends AbstractBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE, "AttributeRule");

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE, "AttributeRuleType");

    /** Class logger. */
    private static Logger log = Logger.getLogger(AttributeFilterPolicyGroupBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AttributeRule.class);

        String ruleId = DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null, "id"));
        if (log.isDebugEnabled()) {
            log.debug("Processing configuration for attribute rule " + ruleId);
        }

        builder.addConstructorArg(ruleId);

        if (log.isDebugEnabled()) {
            log.debug("Processing permit value definition");
        }
        List<Element> permitValues = XMLHelper.getChildElementsByTagNameNS(element,
                AttributeFilterPolicyGroupBeanDefinitionParser.PERMIT_VALUE_ELEMENT_NAME.getNamespaceURI(),
                AttributeFilterPolicyGroupBeanDefinitionParser.PERMIT_VALUE_ELEMENT_NAME.getLocalPart());
        
        for (Element permitValue : permitValues) {
            if (permitValue.hasAttributeNS(null, "ref")) {
                builder.addPropertyReference("permitValue", DatatypeHelper.safeTrimOrNullString(permitValue
                        .getAttributeNS(null, "ref")));
            } else {
                builder.addPropertyValue("permitValue", SpringConfigurationUtils.parseCustomElement(permitValue,
                        parserContext));
            }
        }

        return builder.getBeanDefinition();
    }

    /** {@inheritDoc} */
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        return DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null, "id"));
    }
}
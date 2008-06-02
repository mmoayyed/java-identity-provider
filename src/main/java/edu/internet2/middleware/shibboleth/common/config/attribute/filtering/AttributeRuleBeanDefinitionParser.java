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

package edu.internet2.middleware.shibboleth.common.config.attribute.filtering;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.AttributeRule;
import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Spring bean definition parser to configure an {@link AttributeRule}.
 */
public class AttributeRuleBeanDefinitionParser extends BaseFilterBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE, "AttributeRule");

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE, "AttributeRuleType");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return AttributeRule.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element configElement, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(configElement, parserContext, builder);

        builder.addConstructorArgValue(DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null,
                "attributeID")));

        Map<QName, List<Element>> children = XMLHelper.getChildElements(configElement);

        List<Element> permitValueRule = children.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE,
                "PermitValueRule"));
        if (permitValueRule != null && !permitValueRule.isEmpty()) {
            builder.addPropertyValue("permitValueRule", SpringConfigurationUtils.parseCustomElement(permitValueRule
                    .get(0), parserContext));
        }

        List<Element> permitValueRuleRef = children.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE,
                "PermitValueRuleReference"));
        if (permitValueRuleRef != null && !permitValueRuleRef.isEmpty()) {
            String reference = getAbsoluteReference(configElement, "PermitValueRule", permitValueRuleRef.get(0)
                    .getTextContent());
            builder.addPropertyReference("permitValueRule", reference);
        }

        List<Element> denyValueRule = children
                .get(new QName(AttributeFilterNamespaceHandler.NAMESPACE, "DenyValueRule"));
        if (denyValueRule != null && !denyValueRule.isEmpty()) {
            builder.addPropertyValue("denyValueRule", SpringConfigurationUtils.parseCustomElement(denyValueRule.get(0),
                    parserContext));
        }

        List<Element> denyValueRuleRef = children.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE,
                "DenyValueRuleReference"));
        if (denyValueRuleRef != null && !denyValueRuleRef.isEmpty()) {
            String reference = getAbsoluteReference(configElement, "DenyValueRuleReference", denyValueRuleRef.get(0)
                    .getTextContent());
            builder.addPropertyReference("denyValueRule", reference);
        }
    }
}
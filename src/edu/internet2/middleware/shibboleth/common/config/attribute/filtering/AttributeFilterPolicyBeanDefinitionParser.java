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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Spring bean definition parser to configure an {@link AttributeFilterPolicyFactoryBean}.
 */
public class AttributeFilterPolicyBeanDefinitionParser extends BaseFilterBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE,
            "AttributeFilterPolicy");

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE,
            "AttributeFilterPolicyType");

    /** Class logger. */
    private static Logger log = LoggerFactory.getLogger(AttributeFilterPolicyBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return AttributeFilterPolicyFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        String policyId = DatatypeHelper.safeTrimOrNullString(config.getAttributeNS(null, "id"));
        log.info("Parsing configuration for attribute filter policy {}", policyId);
        builder.addPropertyValue("policyId", policyId);

        List<Element> children;
        Map<QName, List<Element>> childrenMap = XMLHelper.getChildElements(config);

        children = childrenMap.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE, "PolicyRequirementRule"));
        if (children != null && children.size() > 0) {
            builder.addPropertyValue("policyRequirement", SpringConfigurationUtils.parseCustomElement(children.get(0),
                    parserContext));
        } else {
            children = childrenMap.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE,
                    "PolicyRequirementRuleReference"));
            String reference = getAbsoluteReference(config, "PolicyRequirementRule", children.get(0).getTextContent());
            builder.addPropertyReference("policyRequirement", reference);
        }

        ManagedList attributeRules = new ManagedList();
        children = childrenMap.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE, "AttributeRule"));
        if (children != null && children.size() > 0) {
            attributeRules.addAll(SpringConfigurationUtils.parseCustomElements(children, parserContext));
        }

        children = childrenMap.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE, "AttributeRuleReference"));
        if (children != null && children.size() > 0) {
            String reference;
            for (Element child : children) {
                reference = getAbsoluteReference(config, "AttributeRule", child.getTextContent());
                attributeRules.add(new RuntimeBeanReference(reference));
            }
        }

        builder.addPropertyValue("attributeRules", attributeRules);
    }
}
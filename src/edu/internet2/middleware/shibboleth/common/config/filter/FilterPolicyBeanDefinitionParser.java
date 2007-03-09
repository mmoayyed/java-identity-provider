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

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.FilterPolicy;
import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Spring bean definition parser to configure an {@link FilterPolicy}.
 */
public class FilterPolicyBeanDefinitionParser extends AbstractBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE, "FilterPolicy");

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE, "FilterPolicyType");

    /** Class logger. */
    private static Logger log = Logger.getLogger(FilterPolicyBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected AbstractBeanDefinition parseInternal(Element filterPolicyElem, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(FilterPolicy.class);

        String policyId = DatatypeHelper.safeTrimOrNullString(filterPolicyElem.getAttributeNS(null, "id"));
        builder.addConstructorArg(policyId);

        if (log.isDebugEnabled()) {
            log.debug("Processing configuration for filter policy " + policyId);
        }
        processPolicyRequirement(builder, filterPolicyElem, parserContext);
        processAttributeRule(builder, filterPolicyElem, parserContext);

        return builder.getBeanDefinition();
    }

    /**
     * Process the policy requirement definition for this policy, if one exists.
     * 
     * @param builder policy bean builder
     * @param filterPolicyElem policy configuration element
     * @param parserContext current parsing context
     */
    protected void processPolicyRequirement(BeanDefinitionBuilder builder, Element filterPolicyElem,
            ParserContext parserContext) {
        if (log.isDebugEnabled()) {
            log.debug("Processing policy requirement definition");
        }
        NodeList nodes = filterPolicyElem.getElementsByTagNameNS(AttributeFilterNamespaceHandler.NAMESPACE,
                AttributeFilterPoliciesBeanDefinitionParser.POLICY_REQUIREMENT_ELEMENT_LOCAL_NAME);
        if (nodes.getLength() > 0) {
            Element element = (Element) nodes.item(0);
            if (element.hasAttributeNS(null, "ref")) {
                builder.addPropertyReference("policyRequirement", DatatypeHelper.safeTrimOrNullString(element
                        .getAttributeNS(null, "ref")));
            } else {
                builder.addPropertyValue("valueFilter", SpringConfigurationUtils.parseCustomElement(element,
                        parserContext));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No policy requirement definition defined");
            }
        }
    }

    /**
     * Process the attribute rule definitions for this policy, if any exists.
     * 
     * @param builder policy bean builder
     * @param filterPolicyElem policy configuration element
     * @param parserContext current parsing context
     */
    protected void processAttributeRule(BeanDefinitionBuilder builder, Element filterPolicyElem,
            ParserContext parserContext) {
        if (log.isDebugEnabled()) {
            log.debug("Processing attribute rule definitions");
        }
        NodeList nodes = filterPolicyElem.getElementsByTagNameNS(AttributeFilterNamespaceHandler.NAMESPACE,
                AttributeFilterPoliciesBeanDefinitionParser.ATTRIBUTE_RULE_ELEMENT_LOCAL_NAME);
        if (nodes.getLength() > 0) {
            builder.addPropertyValue("attributeRules", SpringConfigurationUtils.parseCustomElements(nodes, "ref",
                    parserContext));
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No attribute rule definitions defined");
            }
        }
    }
}
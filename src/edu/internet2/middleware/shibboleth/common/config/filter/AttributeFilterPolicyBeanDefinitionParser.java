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

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.AttributeFilterPolicy;
import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Spring bean definition parser to configure an {@link AttributeFilterPolicy}.
 */
public class AttributeFilterPolicyBeanDefinitionParser extends AbstractBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE,
            "AttributeFilterPolicy");

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE,
            "AttributeFilterPolicyType");

    /** Class logger. */
    private static Logger log = Logger.getLogger(AttributeFilterPolicyBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected AbstractBeanDefinition parseInternal(Element filterPolicyElem, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AttributeFilterPolicy.class);

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

        List<Element> policyRequirements = XMLHelper.getChildElementsByTagNameNS(filterPolicyElem,
                AttributeFilterPolicyGroupBeanDefinitionParser.POLICY_REQUIREMENT_ELEMENT_NAME.getNamespaceURI(),
                AttributeFilterPolicyGroupBeanDefinitionParser.POLICY_REQUIREMENT_ELEMENT_NAME.getLocalPart());

        for (Element policyRequirement : policyRequirements) {
            if (policyRequirement.hasAttributeNS(null, "ref")) {
                builder.addPropertyReference("policyRequirement", DatatypeHelper.safeTrimOrNullString(policyRequirement
                        .getAttributeNS(null, "ref")));
            } else {
                builder.addPropertyValue("policyRequirement", SpringConfigurationUtils.parseCustomElement(
                        policyRequirement, parserContext));
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
        List<Element> attributeRules = XMLHelper.getChildElementsByTagNameNS(filterPolicyElem,
                AttributeRuleBeanDefinitionParser.ELEMENT_NAME.getNamespaceURI(),
                AttributeRuleBeanDefinitionParser.ELEMENT_NAME.getLocalPart());

        builder.addPropertyValue("attributeRules", SpringConfigurationUtils.parseCustomElements(attributeRules, "ref",
                parserContext));
    }
}
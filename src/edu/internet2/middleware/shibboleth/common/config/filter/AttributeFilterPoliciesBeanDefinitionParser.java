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

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Spring bean definition parser for Shibboleth attribute filtering engine attribute filter policy.
 */
public class AttributeFilterPoliciesBeanDefinitionParser extends AbstractBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE,
            "AttributeFilterPolicies");

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE,
            "AttributeFilterPoliciesType");

    /** Local name of the policy requirement element. */
    public static final String POLICY_REQUIREMENT_ELEMENT_LOCAL_NAME = "PolicyRequirement";

    /** Local name of the attribute rule element. */
    public static final String ATTRIBUTE_RULE_ELEMENT_LOCAL_NAME = "AttributeRule";

    /** Local name of the value filter element. */
    public static final String VALUE_FILTER_ELEMENT_LOCAL_NAME = "ValueFilter";

    /** Local name of the filter policy element. */
    public static final String FILTER_POLICY_ELEMENT_LOCAL_NAME = "FilterPolicy";

    /** Class logger. */
    private static Logger log = Logger.getLogger(AttributeFilterPoliciesBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected AbstractBeanDefinition parseInternal(Element attribFilterPolcies, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AttributeFilterPolicies.class);

        String policyId = DatatypeHelper.safeTrimOrNullString(attribFilterPolcies.getAttributeNS(null, "id"));
        builder.addConstructorArg(policyId);

        if (log.isDebugEnabled()) {
            log.debug("Parsing attribute filter policy " + policyId);
        }

        NodeList nodes = attribFilterPolcies.getElementsByTagNameNS(AttributeFilterNamespaceHandler.NAMESPACE,
                FILTER_POLICY_ELEMENT_LOCAL_NAME);
        builder.addPropertyValue("filterPolicies", SpringConfigurationUtils.parseCustomElements(nodes, parserContext));

        nodes = attribFilterPolcies.getElementsByTagNameNS(AttributeFilterNamespaceHandler.NAMESPACE,
                POLICY_REQUIREMENT_ELEMENT_LOCAL_NAME);
        builder.addPropertyValue("policyRequirements", SpringConfigurationUtils.parseCustomElements(nodes, "ref",
                parserContext));

        nodes = attribFilterPolcies.getElementsByTagNameNS(AttributeFilterNamespaceHandler.NAMESPACE,
                ATTRIBUTE_RULE_ELEMENT_LOCAL_NAME);
        builder.addPropertyValue("attributeRules", SpringConfigurationUtils.parseCustomElements(nodes, "ref",
                parserContext));

        nodes = attribFilterPolcies.getElementsByTagNameNS(AttributeFilterNamespaceHandler.NAMESPACE,
                VALUE_FILTER_ELEMENT_LOCAL_NAME);
        builder.addPropertyValue("valueFilters", SpringConfigurationUtils.parseCustomElements(nodes, "ref",
                parserContext));

        return builder.getBeanDefinition();
    }
}
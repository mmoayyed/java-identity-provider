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

package edu.internet2.middleware.shibboleth.common.config.security;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Spring configuration parser for shibboleth security policies.
 */
public class ShibbolethSecurityPolicyBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Default element name. */
    public static final QName ELEMENT_NAME = new QName(SecurityNamespaceHandler.NAMESPACE, "SecurityPolicy");

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(SecurityNamespaceHandler.NAMESPACE, "SecurityPolicyType");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ShibbolethSecurityPolicyBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return ShibbolethSecurityPolicyFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        return element.getAttributeNS(null, "id");
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        log.info("Parsing configuration for {} security policy with id: {}", XMLHelper.getXSIType(element)
                .getLocalPart(), element.getAttributeNS(null, "id"));

        String policyId = element.getAttributeNS(null, "id");
        log.debug("Configuring security policy: {}", policyId);
        builder.addPropertyValue("policyId", policyId);

        builder.addPropertyValue("policyRules", SpringConfigurationUtils.parseInnerCustomElements(XMLHelper
                .getChildElementsByTagNameNS(element, SecurityNamespaceHandler.NAMESPACE, "Rule"), parserContext));
    }
}
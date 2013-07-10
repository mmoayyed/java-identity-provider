/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.filter.spring;

import java.util.List;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.filter.AttributeRule;
import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser to configure an {@link AttributeRule}.
 */
public class AttributeRuleParser extends BaseFilterParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE, "AttributeRule");

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE, "AttributeRuleType");

    /** PermitValueRule. */
    public static final QName PERMIT_VALUE_RULE = new QName(AttributeFilterNamespaceHandler.NAMESPACE,
            "PermitValueRule");

    /** PermitValueRuleReference. */
    public static final QName PERMIT_VALUE_REF = new QName(AttributeFilterNamespaceHandler.NAMESPACE,
            "PermitValueRuleReference");

    /** DenyValueRule. */
    public static final QName DENY_VALUE_RULE = new QName(AttributeFilterNamespaceHandler.NAMESPACE, "DenyValueRule");

    /** DenyValueRuleReference. */
    public static final QName DENY_VALUE_REF = new QName(AttributeFilterNamespaceHandler.NAMESPACE,
            "DenyValueRuleReference");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeRuleParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return AttributeRule.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        final String id = builder.getBeanDefinition().getAttribute("qualifiedId").toString();

        builder.addPropertyValue("id", id);

        final String attributeId = StringSupport.trimOrNull(config.getAttributeNS(null, "attributeID"));
        builder.addPropertyValue("attributeId", attributeId);
        log.info("attributeID '{}' for '{}'", attributeId, config);

        final List<Element> permitValueRule = ElementSupport.getChildElements(config, PERMIT_VALUE_RULE);
        final List<Element> permitValueReference = ElementSupport.getChildElements(config, PERMIT_VALUE_REF);
        final List<Element> denyValueRule = ElementSupport.getChildElements(config, DENY_VALUE_RULE);
        final List<Element> denyValueReference = ElementSupport.getChildElements(config, DENY_VALUE_REF);

        if (permitValueRule != null && !permitValueRule.isEmpty()) {

            final ManagedList<BeanDefinition> permitValueRules =
                    SpringSupport.parseCustomElements(permitValueRule, parserContext);
            log.debug("permitValueRules {}", permitValueRules);
            builder.addPropertyValue("matcher", permitValueRules.get(0));
            builder.addPropertyValue("isDenyRule", false);

        } else if (permitValueReference != null && !permitValueReference.isEmpty()) {

            final String referenceText = getReferenceText(permitValueReference.get(0));
            if (null == referenceText) {
                throw new BeanCreationException("Attribute Rule '" + id + "' no text or reference for "
                        + PERMIT_VALUE_REF);
            }

            final String reference = getAbsoluteReference(config, "PermitValueRule", referenceText);
            log.debug("Adding PermitValueRule reference to {}", reference);
            builder.addPropertyValue("matcher", new RuntimeBeanReference(reference));
            builder.addPropertyValue("isDenyRule", false);

        } else if (denyValueRule != null && !denyValueRule.isEmpty()) {

            final ManagedList<BeanDefinition> denyValueRules =
                    SpringSupport.parseCustomElements(denyValueRule, parserContext);
            log.debug("denyValueRules {}", denyValueRules);
            builder.addPropertyValue("matcher", denyValueRules.get(0));
            builder.addPropertyValue("isDenyRule", true);

        } else if (denyValueReference != null && !denyValueReference.isEmpty()) {

            final String referenceText = getReferenceText(permitValueReference.get(0));
            if (null == referenceText) {
                throw new BeanCreationException("Attribute Rule '" + id + "' no text or reference for "
                        + DENY_VALUE_REF);
            }
            final String reference = getAbsoluteReference(config, "DenyValueRule", referenceText);
            log.debug("Adding DenyValueRule reference to {}", reference);
            builder.addPropertyValue("matcher", new RuntimeBeanReference(reference));
            builder.addPropertyValue("isDenyRule", true);

        }
    }
}
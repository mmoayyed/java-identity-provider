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

package net.shibboleth.idp.attribute.filtering.spring;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.filtering.AttributeRule;
import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

// TODO incomplete port from v2
/**
 * Spring bean definition parser to configure an {@link AttributeRule}.
 */
public class AttributeRuleParser extends MatcherParser {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeRuleParser.class);

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

        String attributeId = StringSupport.trimOrNull(configElement.getAttributeNS(null, "attributeID"));
        builder.addPropertyValue("attributeId", attributeId);
        log.info("attributeID '{}' for '{}'", attributeId, configElement);

        Map<QName, List<Element>> children = ElementSupport.getIndexedChildElements(configElement);

        List<Element> permitValueRule =
                children.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE, "PermitValueRule"));
        if (permitValueRule != null && !permitValueRule.isEmpty()) {
            // TODO correct parse list instead of get(0)
            // builder.addPropertyValue("permitValueRule", SpringConfigurationUtils.parseInnerCustomElement(
            // permitValueRule.get(0), parserContext));
            // builder.addPropertyValue("permitValueRule",
            ManagedList<BeanDefinition> permitValueRules =
                    SpringSupport.parseCustomElements(permitValueRule, parserContext);
            log.debug("permitValueRules {}", permitValueRules);
            builder.addPropertyValue("permitRule", permitValueRules.get(0));
        }

        List<Element> permitValueRuleRef =
                children.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE, "PermitValueRuleReference"));
        if (permitValueRuleRef != null && !permitValueRuleRef.isEmpty()) {
            String reference =
                    getAbsoluteReference(configElement, "PermitValueRule", permitValueRuleRef.get(0).getTextContent());
            // builder.addPropertyReference("permitValueRule", reference);
            // TODO figure this out
        }

        List<Element> denyValueRule =
                children.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE, "DenyValueRule"));
        if (denyValueRule != null && !denyValueRule.isEmpty()) {
            // builder.addPropertyValue("denyValueRule", SpringConfigurationUtils.parseInnerCustomElement(denyValueRule
            // .get(0), parserContext));
            // builder.addPropertyValue("denyValueRule", SpringSupport.parseCustomElements(denyValueRule,
            // parserContext));
            // TODO figure this out
        }

        List<Element> denyValueRuleRef =
                children.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE, "DenyValueRuleReference"));
        if (denyValueRuleRef != null && !denyValueRuleRef.isEmpty()) {
            String reference =
                    getAbsoluteReference(configElement, "DenyValueRule", denyValueRuleRef.get(0).getTextContent());
            // builder.addPropertyReference("denyValueRule", reference);
            // TODO figure this out
        }
    }
}
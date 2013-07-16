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

package net.shibboleth.idp.attribute.filter.spring.basic;

import java.util.List;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.filter.impl.matcher.logic.NotMatcher;
import net.shibboleth.idp.attribute.filter.impl.policyrule.logic.NotPolicyRule;
import net.shibboleth.idp.attribute.filter.spring.BaseFilterParser;
import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

// TODO testing
/**
 * Bean definition parser for {@link NotMatcher} or {@link NotPolicyRule} objects.
 */
public class NotMatcherParser extends BaseFilterParser {
  
    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(AttributeFilterBasicNamespaceHandler.NAMESPACE, "NOT");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        if (isPolicyRule(element)) {
            return NotPolicyRule.class;
        }
        return NotMatcher.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element configElement, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(configElement, parserContext, builder);

        final String myId = builder.getBeanDefinition().getAttribute("qualifiedId").toString();

        builder.addPropertyValue("id", myId);

        List<Element> ruleElements =
                ElementSupport.getChildElementsByTagNameNS(configElement,
                        AttributeFilterBasicNamespaceHandler.NAMESPACE, "Rule");

        if (ruleElements != null && !ruleElements.isEmpty()) {

            builder.addConstructorArgValue(SpringSupport.parseCustomElements(ruleElements, parserContext).get(0));
        }
    }
}
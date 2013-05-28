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

import net.shibboleth.idp.attribute.filter.impl.matcher.logic.OrMatcher;
import net.shibboleth.idp.attribute.filter.spring.MatcherParser;
import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

// TODO incomplete
/**
 * Bean definition parser for {@link OrMatcher} objects.
 */
public class OrMatcherParser extends MatcherParser {
  
    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(AttributeFilterBasicNamespaceHandler.NAMESPACE, "OR");

    /** {@inheritDoc} */
    protected String getBeanClassName(Element element) {
        return OrMatcher.class.getName();
    }

    /** {@inheritDoc} */
    protected void doParse(Element configElement, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(configElement, parserContext, builder);

        List<Element> ruleElements =
                ElementSupport.getChildElementsByTagNameNS(configElement,
                        AttributeFilterBasicNamespaceHandler.NAMESPACE, "Rule");

        builder.addConstructorArgValue(SpringSupport.parseCustomElements(ruleElements, parserContext));

        // ruleElements =
        // XMLHelper.getChildElementsByTagNameNS(configElement, BasicMatchFunctorNamespaceHandler.NAMESPACE,
        // "RuleReference");
        // if (!ruleElements.isEmpty()) {
        // throw new BeanCreationException("RuleReference elements within an OR rule are not supported");
        // }
    }
}
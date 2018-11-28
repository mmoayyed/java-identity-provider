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

package net.shibboleth.idp.attribute.filter.spring.basic.impl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.filter.matcher.logic.impl.AndMatcher;
import net.shibboleth.idp.attribute.filter.policyrule.logic.impl.AndPolicyRule;
import net.shibboleth.idp.attribute.filter.spring.BaseFilterParser;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

/**
 * Bean definition parser for {@link AndMatcher} or {@link AndPolicyRule} objects.<br/>
 * These both take as a constructor a list of {@link net.shibboleth.idp.attribute.filter.Matcher} or
 * {@link net.shibboleth.idp.attribute.filter.PolicyRequirementRule} so the parsing code is common.
 */
public class AndMatcherParser extends BaseFilterParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(BaseFilterParser.NAMESPACE, "AND");

    /** {@inheritDoc} */
    @Override @Nonnull protected Class<?> getBeanClass(@Nonnull final Element element) {
        if (isPolicyRule(element)) {
            return AndPolicyRule.class;
        }
        return AndMatcher.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element configElement, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(configElement, parserContext, builder);

        final String myId = builder.getBeanDefinition().getAttribute("qualifiedId").toString();

        builder.addPropertyValue("id", myId);

        final List<Element> ruleElements =
                ElementSupport.getChildElementsByTagNameNS(configElement, BaseFilterParser.NAMESPACE, "Rule");

        builder.addPropertyValue("subsidiaries", SpringSupport.parseCustomElements(ruleElements, parserContext));

    }
}
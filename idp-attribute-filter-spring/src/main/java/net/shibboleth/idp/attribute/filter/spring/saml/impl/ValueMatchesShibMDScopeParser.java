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

package net.shibboleth.idp.attribute.filter.spring.saml.impl;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.idp.attribute.filter.matcher.saml.impl.AttributeValueMatchesShibMDScope;
import net.shibboleth.idp.attribute.filter.spring.BaseFilterParser;
import net.shibboleth.idp.attribute.filter.spring.matcher.BaseAttributeValueMatcherParser;

/**
 * Bean definition parser for {@link AttributeValueMatchesShibMDScope}.
 */
public class ValueMatchesShibMDScopeParser extends BaseAttributeValueMatcherParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(BaseFilterParser.NAMESPACE,
            "ValueMatchesShibMDScope");

    /** {@inheritDoc} */
    @Override @Nonnull protected Class<AttributeValueMatchesShibMDScope> getNativeBeanClass() {
        return AttributeValueMatchesShibMDScope.class;
    }

    /** {@inheritDoc} */
    protected void doNativeParse(final Element element,
            final ParserContext parserContext, final BeanDefinitionBuilder builder) {
        // Nothing to do
    }
}

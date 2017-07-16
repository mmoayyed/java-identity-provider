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

import net.shibboleth.idp.attribute.filter.policyrule.saml.impl.AttributeRequesterNameIDFormatExactPolicyRule;
import net.shibboleth.idp.attribute.filter.spring.BaseFilterParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.BasePolicyRuleParser;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser for {@link AttributeRequesterNameIDFormatExactPolicyRule}.
 */
public class AttributeRequesterNameIdFormatRuleParser extends BasePolicyRuleParser {

    /** Schema type- saml. */
    public static final QName SCHEMA_TYPE = new QName(AttributeFilterSAMLNamespaceHandler.NAMESPACE,
            "NameIDFormatExactMatch");

    /** Schema type- afp. */
    public static final QName SCHEMA_TYPE_AFP = new QName(BaseFilterParser.NAMESPACE,
            "NameIDFormatExactMatch");

    /** Schema type. */
    public static final QName SCHEMA_TYPE_V2 = new QName(AttributeFilterSAMLNamespaceHandler.NAMESPACE,
            "AttributeRequesterNameIDFormatExactMatch");

    /** {@inheritDoc} */
    @Override @Nonnull protected Class<AttributeRequesterNameIDFormatExactPolicyRule> getNativeBeanClass() {
        return AttributeRequesterNameIDFormatExactPolicyRule.class;
    }

    /** {@inheritDoc} */
    @Override protected QName getAFPName() {
        return SCHEMA_TYPE_AFP;
    }

    /** {@inheritDoc} */
    @Override protected void doNativeParse(@Nonnull final Element element, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {

        builder.addPropertyValue("nameIdFormat", 
                StringSupport.trimOrNull(element.getAttributeNS(null, "nameIdFormat")));
    }

}

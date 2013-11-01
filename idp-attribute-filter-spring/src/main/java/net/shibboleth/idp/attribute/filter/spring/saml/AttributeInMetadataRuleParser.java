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

package net.shibboleth.idp.attribute.filter.spring.saml;

// TODO TEST
import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.filter.impl.policyrule.saml.AttributeInMetadataPolicyRule;
import net.shibboleth.idp.attribute.filter.spring.policyrule.BasePolicyRuleParser;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Bean definition parser for {@link AttributeInMetadataPolicyRule}.
 */
public class AttributeInMetadataRuleParser extends BasePolicyRuleParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(AttributeFilterSAMLNamespaceHandler.NAMESPACE,
            "AttributeInMetadata");

    /** {@inheritDoc} */
    @Nonnull protected Class<AttributeInMetadataPolicyRule> getNativeBeanClass() {
        return AttributeInMetadataPolicyRule.class;
    }

    /** {@inheritDoc} */
    protected void doNativeParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, builder);

        boolean onlyIfRequired = true;
        if (config.hasAttributeNS(null, "onlyIfRequired")) {
            final Boolean value =
                    AttributeSupport.getAttributeValueAsBoolean(config.getAttributeNodeNS(null, "onlyIfRequired"));
            if (null == value) {
                throw new BeanCreationException("Invalid value of 'onlyIfRequired' in AttributeInMetadatRule");
            }
            onlyIfRequired = value;
        }
        builder.addPropertyValue("onlyIfRequired", onlyIfRequired);

        boolean matchIfMetadataSilent = false;
        if (config.hasAttributeNS(null, "matchIfMetadataSilent")) {
            final Boolean value =
                    AttributeSupport.getAttributeValueAsBoolean(
                            config.getAttributeNodeNS(null, "matchIfMetadataSilent"));
            if (null == value) {
                throw new BeanCreationException("Invalid value of 'matchIfMetadataSilent' in AttributeInMetadatRule");
            }
            matchIfMetadataSilent = value;
        }
        builder.addPropertyValue("matchIfMetadataSilent ", matchIfMetadataSilent);
    }
}

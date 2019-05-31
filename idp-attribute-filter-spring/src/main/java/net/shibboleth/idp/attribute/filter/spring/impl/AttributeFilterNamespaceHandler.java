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

package net.shibboleth.idp.attribute.filter.spring.impl;

import org.springframework.beans.factory.xml.BeanDefinitionParser;

import net.shibboleth.ext.spring.util.BaseSpringNamespaceHandler;
import net.shibboleth.idp.attribute.filter.spring.BaseFilterParser;
import net.shibboleth.idp.attribute.filter.spring.basic.impl.AndMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.basic.impl.AnyParser;
import net.shibboleth.idp.attribute.filter.spring.basic.impl.NotMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.basic.impl.OrMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.basic.impl.ScriptedMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.matcher.impl.AttributeScopeMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.matcher.impl.AttributeScopeRegexMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.matcher.impl.AttributeValueRegexMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.matcher.impl.AttributeValueStringMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.impl.AttributeIssuerRegexRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.impl.AttributeIssuerRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.impl.AttributeRequesterRegexRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.impl.AttributeRequesterRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.impl.InboundRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.impl.NumOfAttributeValuesRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.impl.OutboundRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.impl.PredicateRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.impl.PrincipalNameRegexRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.impl.PrincipalNameRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.impl.ProxiedRequesterRegexRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.impl.ProxiedRequesterRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.AttributeInMetadataRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.AttributeIssuerEntityAttributeExactRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.AttributeIssuerEntityAttributeRegexRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.AttributeIssuerRegistrationAuthorityRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.AttributeRequesterEntityAttributeExactRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.AttributeRequesterEntityAttributeRegexRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.AttributeRequesterInEntityGroupRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.AttributeRequesterNameIdFormatRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.AttributeRequesterRegistrationAuthorityRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.MappedAttributeInMetadataRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.ScopeMatchesShibMDScopeParser;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.ValueMatchesShibMDScopeParser;

/** Namespace handler for the attribute filtering engine. */
public class AttributeFilterNamespaceHandler extends BaseSpringNamespaceHandler {

    /** {@inheritDoc} */
    // Checkstyle: MethodLength OFF
    @Override
    public void init() {
        BeanDefinitionParser parser = new AttributeFilterPolicyGroupParser();
        
        registerBeanDefinitionParser(BaseFilterParser.AFP_ELEMENT_NAME, parser);
        registerBeanDefinitionParser(AttributeFilterPolicyGroupParser.TYPE_NAME, parser);

        parser = new AttributeFilterPolicyParser();
        registerBeanDefinitionParser(AttributeFilterPolicyParser.ELEMENT_NAME, parser);
        registerBeanDefinitionParser(AttributeFilterPolicyParser.TYPE_NAME, parser);

        parser = new AttributeRuleParser();
        registerBeanDefinitionParser(AttributeRuleParser.ELEMENT_NAME, parser);
        registerBeanDefinitionParser(AttributeRuleParser.TYPE_NAME, parser);
        
        // BASIC
        
        registerBeanDefinitionParser(AnyParser.SCHEMA_TYPE, new AnyParser());

        registerBeanDefinitionParser(AndMatcherParser.SCHEMA_TYPE, new AndMatcherParser());

        registerBeanDefinitionParser(OrMatcherParser.SCHEMA_TYPE, new OrMatcherParser());

        registerBeanDefinitionParser(NotMatcherParser.SCHEMA_TYPE, new NotMatcherParser());

        registerBeanDefinitionParser(InboundRuleParser.SCHEMA_TYPE, new InboundRuleParser());
        registerBeanDefinitionParser(OutboundRuleParser.SCHEMA_TYPE, new OutboundRuleParser());
        
        // Attribute/Matcher
        registerBeanDefinitionParser(AttributeValueStringMatcherParser.SCHEMA_TYPE,
                new AttributeValueStringMatcherParser());

        registerBeanDefinitionParser(AttributeScopeMatcherParser.SCHEMA_TYPE, new AttributeScopeMatcherParser());

        registerBeanDefinitionParser(AttributeValueRegexMatcherParser.SCHEMA_TYPE,
                new AttributeValueRegexMatcherParser());

        registerBeanDefinitionParser(AttributeScopeRegexMatcherParser.SCHEMA_TYPE,
                new AttributeScopeRegexMatcherParser());

        // Policy
        registerBeanDefinitionParser(AttributeRequesterRuleParser.SCHEMA_TYPE, new AttributeRequesterRuleParser());

        registerBeanDefinitionParser(AttributeRequesterRegexRuleParser.SCHEMA_TYPE,
                new AttributeRequesterRegexRuleParser());

        registerBeanDefinitionParser(ProxiedRequesterRuleParser.SCHEMA_TYPE, new ProxiedRequesterRuleParser());

        registerBeanDefinitionParser(ProxiedRequesterRegexRuleParser.SCHEMA_TYPE,
                new ProxiedRequesterRegexRuleParser());

        registerBeanDefinitionParser(AttributeIssuerRuleParser.SCHEMA_TYPE, new AttributeIssuerRuleParser());

        registerBeanDefinitionParser(AttributeIssuerRegexRuleParser.SCHEMA_TYPE,
                new AttributeIssuerRegexRuleParser());
        
        registerBeanDefinitionParser(PrincipalNameRuleParser.SCHEMA_TYPE, new PrincipalNameRuleParser());

        registerBeanDefinitionParser(PrincipalNameRegexRuleParser.SCHEMA_TYPE, new PrincipalNameRegexRuleParser());

        registerBeanDefinitionParser(NumOfAttributeValuesRuleParser.SCHEMA_TYPE,
                new NumOfAttributeValuesRuleParser());

        registerBeanDefinitionParser(ScriptedMatcherParser.SCHEMA_TYPE, new ScriptedMatcherParser());
        
        registerBeanDefinitionParser(PredicateRuleParser.SCHEMA_TYPE, new PredicateRuleParser());
        
        // SAML - 
        registerBeanDefinitionParser(AttributeRequesterEntityAttributeExactRuleParser.SCHEMA_TYPE,
                new AttributeRequesterEntityAttributeExactRuleParser());

        registerBeanDefinitionParser(AttributeIssuerEntityAttributeExactRuleParser.SCHEMA_TYPE,
                new AttributeIssuerEntityAttributeExactRuleParser());

        registerBeanDefinitionParser(AttributeRequesterEntityAttributeRegexRuleParser.SCHEMA_TYPE,
                new AttributeRequesterEntityAttributeRegexRuleParser());

        registerBeanDefinitionParser(AttributeIssuerEntityAttributeRegexRuleParser.SCHEMA_TYPE,
                new AttributeIssuerEntityAttributeRegexRuleParser());

        registerBeanDefinitionParser(AttributeRequesterNameIdFormatRuleParser.SCHEMA_TYPE,
                new AttributeRequesterNameIdFormatRuleParser());

        registerBeanDefinitionParser(AttributeRequesterInEntityGroupRuleParser.SCHEMA_TYPE,
                new AttributeRequesterInEntityGroupRuleParser());

        registerBeanDefinitionParser(AttributeInMetadataRuleParser.SCHEMA_TYPE,
                new AttributeInMetadataRuleParser());

        registerBeanDefinitionParser(MappedAttributeInMetadataRuleParser.SCHEMA_TYPE,
                new MappedAttributeInMetadataRuleParser());

        registerBeanDefinitionParser(AttributeRequesterRegistrationAuthorityRuleParser.SCHEMA_TYPE,
                new AttributeRequesterRegistrationAuthorityRuleParser());

        registerBeanDefinitionParser(AttributeIssuerRegistrationAuthorityRuleParser.SCHEMA_TYPE,
                new AttributeIssuerRegistrationAuthorityRuleParser());
        
        registerBeanDefinitionParser(ValueMatchesShibMDScopeParser.SCHEMA_TYPE, new ValueMatchesShibMDScopeParser());

        registerBeanDefinitionParser(ScopeMatchesShibMDScopeParser.SCHEMA_TYPE, new ScopeMatchesShibMDScopeParser());
    }
    // Checkstyle: MethodLength ON
}
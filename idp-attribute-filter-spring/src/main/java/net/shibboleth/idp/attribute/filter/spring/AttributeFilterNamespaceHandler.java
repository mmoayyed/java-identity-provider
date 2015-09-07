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

import net.shibboleth.ext.spring.util.BaseSpringNamespaceHandler;
import net.shibboleth.idp.attribute.filter.spring.basic.AndMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.basic.AnyParser;
import net.shibboleth.idp.attribute.filter.spring.basic.NotMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.basic.OrMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.basic.ScriptedMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.matcher.AttributeScopeMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.matcher.AttributeScopeRegexMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.matcher.AttributeValueRegexMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.matcher.AttributeValueStringMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.AttributeIssuerRegexRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.AttributeIssuerRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.AttributeRequesterRegexRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.AttributeRequesterRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.AuthenticationMethodRegexRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.AuthenticationMethodRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.NumOfAttributeValuesRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.PredicateRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.PrincipalNameRegexRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.PrincipalNameRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.AttributeInMetadataRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.AttributeRequesterEntityAttributeExactRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.AttributeRequesterEntityAttributeRegexRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.AttributeRequesterInEntityGroupRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.AttributeRequesterNameIdFormatRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.MappedAttributeInMetadataRuleParser;
import net.shibboleth.idp.attribute.filter.spring.saml.RegistrationAuthorityRuleParser;

import org.springframework.beans.factory.xml.BeanDefinitionParser;

/** Namespace handler for the attribute filtering engine. */
public class AttributeFilterNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:afp";

    /** {@inheritDoc} */
    // Checkstyle: MethodLength OFF
    @Override
    public void init() {
        BeanDefinitionParser parser = new AttributeFilterPolicyGroupParser();
        registerBeanDefinitionParser(AttributeFilterPolicyGroupParser.ELEMENT_NAME, parser);
        registerBeanDefinitionParser(AttributeFilterPolicyGroupParser.TYPE_NAME, parser);

        parser = new AttributeFilterPolicyParser();
        registerBeanDefinitionParser(AttributeFilterPolicyParser.ELEMENT_NAME, parser);
        registerBeanDefinitionParser(AttributeFilterPolicyParser.TYPE_NAME, parser);

        parser = new AttributeRuleParser();
        registerBeanDefinitionParser(AttributeRuleParser.ELEMENT_NAME, parser);
        registerBeanDefinitionParser(AttributeRuleParser.TYPE_NAME, parser);
        
        // BASIC
        
        registerBeanDefinitionParser(AnyParser.SCHEMA_TYPE_AFP, new AnyParser());

        registerBeanDefinitionParser(AndMatcherParser.SCHEMA_TYPE_AFP, new AndMatcherParser());

        registerBeanDefinitionParser(OrMatcherParser.SCHEMA_TYPE_AFP, new OrMatcherParser());

        registerBeanDefinitionParser(NotMatcherParser.SCHEMA_TYPE_AFP, new NotMatcherParser());

        // Attribute/Matcher
        registerBeanDefinitionParser(AttributeValueStringMatcherParser.SCHEMA_TYPE_AFP,
                new AttributeValueStringMatcherParser());

        registerBeanDefinitionParser(AttributeScopeMatcherParser.SCHEMA_TYPE_AFP, new AttributeScopeMatcherParser());

        registerBeanDefinitionParser(AttributeValueRegexMatcherParser.SCHEMA_TYPE_AFP,
                new AttributeValueRegexMatcherParser());

        registerBeanDefinitionParser(AttributeScopeRegexMatcherParser.SCHEMA_TYPE_AFP,
                new AttributeScopeRegexMatcherParser());

        // Policy
        registerBeanDefinitionParser(AttributeRequesterRuleParser.SCHEMA_TYPE_AFP, new AttributeRequesterRuleParser());

        registerBeanDefinitionParser(AttributeRequesterRegexRuleParser.SCHEMA_TYPE_AFP,
                new AttributeRequesterRegexRuleParser());

        registerBeanDefinitionParser(AttributeIssuerRuleParser.SCHEMA_TYPE_AFP, new AttributeIssuerRuleParser());

        registerBeanDefinitionParser(AttributeIssuerRegexRuleParser.SCHEMA_TYPE_AFP,
                new AttributeIssuerRegexRuleParser());

        registerBeanDefinitionParser(PrincipalNameRuleParser.SCHEMA_TYPE_AFP, new PrincipalNameRuleParser());

        registerBeanDefinitionParser(PrincipalNameRegexRuleParser.SCHEMA_TYPE_AFP, new PrincipalNameRegexRuleParser());

        registerBeanDefinitionParser(AuthenticationMethodRuleParser.SCHEMA_TYPE_AFP,
                new AuthenticationMethodRuleParser());

        registerBeanDefinitionParser(AuthenticationMethodRegexRuleParser.SCHEMA_TYPE_AFP,
                new AuthenticationMethodRegexRuleParser());

        registerBeanDefinitionParser(NumOfAttributeValuesRuleParser.SCHEMA_TYPE_AFP,
                new NumOfAttributeValuesRuleParser());

        registerBeanDefinitionParser(ScriptedMatcherParser.SCHEMA_TYPE_AFP, new ScriptedMatcherParser());
        
        registerBeanDefinitionParser(PredicateRuleParser.SCHEMA_TYPE_AFP, new PredicateRuleParser());
        
        // SAML - 
        registerBeanDefinitionParser(AttributeRequesterEntityAttributeExactRuleParser.SCHEMA_TYPE_AFP,
                new AttributeRequesterEntityAttributeExactRuleParser());

        registerBeanDefinitionParser(AttributeRequesterEntityAttributeRegexRuleParser.SCHEMA_TYPE_AFP,
                new AttributeRequesterEntityAttributeRegexRuleParser());

        registerBeanDefinitionParser(AttributeRequesterNameIdFormatRuleParser.SCHEMA_TYPE_AFP,
                new AttributeRequesterNameIdFormatRuleParser());

        registerBeanDefinitionParser(AttributeRequesterInEntityGroupRuleParser.SCHEMA_TYPE_AFP,
                new AttributeRequesterInEntityGroupRuleParser());

        registerBeanDefinitionParser(AttributeInMetadataRuleParser.ATTRIBUTE_IN_METADATA_AFP,
                new AttributeInMetadataRuleParser());

        registerBeanDefinitionParser(MappedAttributeInMetadataRuleParser.MAPPED_ATTRIBUTE_IN_METADATA_AFP,
                new MappedAttributeInMetadataRuleParser());

        registerBeanDefinitionParser(RegistrationAuthorityRuleParser.SCHEMA_TYPE_AFP,
                new RegistrationAuthorityRuleParser());
    }
    // Checkstyle: MethodLength ON
}
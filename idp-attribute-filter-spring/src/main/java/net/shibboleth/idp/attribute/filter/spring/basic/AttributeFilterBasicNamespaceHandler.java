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

import net.shibboleth.idp.attribute.filter.spring.matcher.AttributeScopeMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.matcher.AttributeScopeRegexMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.matcher.AttributeValueRegexMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.matcher.AttributeValueStringMatcherParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.AttributeRequesterRegexRuleParser;
import net.shibboleth.idp.attribute.filter.spring.policyrule.AttributeRequesterRuleParser;
import net.shibboleth.idp.spring.BaseSpringNamespaceHandler;

// TODO incomplete port from v2
/** Namespace handler for the attribute filter engine basic match functions. */
public class AttributeFilterBasicNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Basic match function namespace. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:afp:mf:basic";

    /** {@inheritDoc} */
    public void init() {

        registerBeanDefinitionParser(AnyParser.SCHEMA_TYPE, new AnyParser());

        // LOGIC

        registerBeanDefinitionParser(AndMatcherParser.SCHEMA_TYPE, new AndMatcherParser());

        registerBeanDefinitionParser(OrMatcherParser.SCHEMA_TYPE, new OrMatcherParser());

        registerBeanDefinitionParser(NotMatcherParser.SCHEMA_TYPE, new NotMatcherParser());

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

        /**
         * registerBeanDefinitionParser(AnyMatchFunctorBeanDefinitionParser.SCHEMA_TYPE, new
         * AnyMatchFunctorBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(AttributeIssuerRegexMatchFunctionBeanDefinitionParser.SCHEMA_TYPE, new
         * AttributeIssuerRegexMatchFunctionBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(AttributeIssuerStringMatchFunctionBeanDefinitionParser.SCHEMA_TYPE, new
         * AttributeIssuerStringMatchFunctionBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(AttributeValueStringMatchFunctionBeanDefinitionParser.SCHEMA_TYPE, new
         * AttributeValueStringMatchFunctionBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(AuthenticationMethodRegexMatchFunctionBeanDefinitionParser.SCHEMA_TYPE, new
         * AuthenticationMethodRegexMatchFunctionBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(AuthenticationMethodStringMatchFunctionBeanDefinitionParser.SCHEMA_TYPE, new
         * AuthenticationMethodStringMatchFunctionBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(NumOfAttributeValuesMatchFunctorBeanDefinitionParser.SCHEMA_TYPE, new
         * NumOfAttributeValuesMatchFunctorBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(OrMatchFunctorBeanDefinitionParser.SCHEMA_TYPE, new
         * OrMatchFunctorBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(PrincipalNameRegexMatchFunctionBeanDefinitionParser.SCHEMA_TYPE, new
         * PrincipalNameRegexMatchFunctionBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(PrincipalNameStringMatchFunctionBeanDefinitionParser.SCHEMA_TYPE, new
         * PrincipalNameStringMatchFunctionBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(ScriptMatchFunctorBeanDefinitionParser.SCHEMA_TYPE, new
         * ScriptMatchFunctorBeanDefinitionParser());
         **/
    }
}
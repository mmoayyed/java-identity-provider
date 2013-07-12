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

import net.shibboleth.idp.attribute.filter.spring.matcher.AttributeValueStringMatcherParser;
import net.shibboleth.idp.spring.BaseSpringNamespaceHandler;

// TODO incomplete port from v2
/** Namespace handler for the attribute filter engine basic match functions. */
public class AttributeFilterBasicNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Basic match function namespace. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:afp:mf:basic";

    /** {@inheritDoc} */
    public void init() {

        registerBeanDefinitionParser(AnyParser.SCHEMA_TYPE, new AnyParser());

        registerBeanDefinitionParser(OrMatcherParser.SCHEMA_TYPE, new OrMatcherParser());
        
        registerBeanDefinitionParser(AttributeValueStringMatcherParser.SCHEMA_TYPE,
                new AttributeValueStringMatcherParser());

        /**
         * registerBeanDefinitionParser(AndMatchFunctorBeanDefinitionParser.SCHEMA_TYPE, new
         * AndMatchFunctorBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(AnyMatchFunctorBeanDefinitionParser.SCHEMA_TYPE, new
         * AnyMatchFunctorBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(AttributeIssuerRegexMatchFunctionBeanDefinitionParser.SCHEMA_TYPE, new
         * AttributeIssuerRegexMatchFunctionBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(AttributeIssuerStringMatchFunctionBeanDefinitionParser.SCHEMA_TYPE, new
         * AttributeIssuerStringMatchFunctionBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(AttributeRequesterRegexMatchFunctionBeanDefinitionParser.SCHEMA_TYPE, new
         * AttributeRequesterRegexMatchFunctionBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(AttributeRequesterStringMatchFunctionBeanDefinitionParser.SCHEMA_TYPE, new
         * AttributeRequesterStringMatchFunctionBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(AttributeScopeRegexMatchFunctionBeanDefinitionParser.SCHEMA_TYPE, new
         * AttributeScopeRegexMatchFunctionBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(AttributeScopeStringMatchFunctionBeanDefinitionParser.SCHEMA_TYPE, new
         * AttributeScopeStringMatchFunctionBeanDefinitionParser());
         * 
         * registerBeanDefinitionParser(AttributeValueRegexMatchFunctionBeanDefinitionParser.SCHEMA_TYPE, new
         * AttributeValueRegexMatchFunctionBeanDefinitionParser());
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
         * registerBeanDefinitionParser(NotMatchFunctorBeanDefinitionParser.SCHEMA_TYPE, new
         * NotMatchFunctorBeanDefinitionParser());
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
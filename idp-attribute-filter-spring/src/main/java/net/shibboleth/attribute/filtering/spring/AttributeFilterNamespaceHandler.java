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

package net.shibboleth.attribute.filtering.spring;

import net.shibboleth.idp.spring.BaseSpringNamespaceHandler;

import org.springframework.beans.factory.xml.BeanDefinitionParser;

/** Namespace handler for the attribute filtering engine. */
public class AttributeFilterNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:afp";

    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(AttributeFilterServiceBeanDefinitionParser.SCHEMA_TYPE,
                new AttributeFilterServiceBeanDefinitionParser());

        BeanDefinitionParser parser = new AttributeFilterPolicyGroupBeanDefinitionParser();
        registerBeanDefinitionParser(AttributeFilterPolicyGroupBeanDefinitionParser.ELEMENT_NAME, parser);
        registerBeanDefinitionParser(AttributeFilterPolicyGroupBeanDefinitionParser.TYPE_NAME, parser);

        parser = new AttributeFilterPolicyBeanDefinitionParser();
        registerBeanDefinitionParser(AttributeFilterPolicyBeanDefinitionParser.ELEMENT_NAME, parser);
        registerBeanDefinitionParser(AttributeFilterPolicyBeanDefinitionParser.TYPE_NAME, parser);

        parser = new AttributeValueFilterPolicyBeanDefinitionParser();
        registerBeanDefinitionParser(AttributeValueFilterPolicyBeanDefinitionParser.ELEMENT_NAME, parser);
        registerBeanDefinitionParser(AttributeValueFilterPolicyBeanDefinitionParser.TYPE_NAME, parser);
    }
}
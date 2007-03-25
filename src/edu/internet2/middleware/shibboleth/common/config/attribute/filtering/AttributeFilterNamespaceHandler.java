/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.config.attribute.filtering;

import org.springframework.beans.factory.xml.BeanDefinitionParser;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 * Spring namespace handler for Shibboleth's attribute filtering engine implementation.
 */
public class AttributeFilterNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:afp";

    /** {@inheritDoc} */
    public void init() {
        BeanDefinitionParser parser = new AttributeFilterPolicyGroupBeanDefinitionParser();
        registerBeanDefinitionParser(AttributeFilterPolicyGroupBeanDefinitionParser.ELEMENT_NAME, parser);
        registerBeanDefinitionParser(AttributeFilterPolicyGroupBeanDefinitionParser.TYPE_NAME, parser);
        
        parser = new AttributeFilterPolicyBeanDefinitionParser();
        registerBeanDefinitionParser(AttributeFilterPolicyBeanDefinitionParser.ELEMENT_NAME, parser);
        registerBeanDefinitionParser(AttributeFilterPolicyBeanDefinitionParser.TYPE_NAME, parser);
        
        parser = new AttributeRuleBeanDefinitionParser();
        registerBeanDefinitionParser(AttributeRuleBeanDefinitionParser.ELEMENT_NAME, parser);
        registerBeanDefinitionParser(AttributeRuleBeanDefinitionParser.TYPE_NAME, parser);
    }
}
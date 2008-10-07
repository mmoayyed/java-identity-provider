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

package edu.internet2.middleware.shibboleth.common.config.service;

import java.util.List;

import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/** Base bean definition parser for service objects. */
public abstract class AbstractServiceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** {@inheritDoc} */
    protected void doParse(Element configElement, ParserContext parserContext, BeanDefinitionBuilder builder) {
        List<Element> configurationResources = XMLHelper.getChildElementsByTagNameNS(configElement,
                ServiceNamespaceHandler.NAMESPACE, "ConfigurationResource");
        builder.addPropertyValue("serviceConfigurations", SpringConfigurationUtils.parseInnerCustomElements(
                configurationResources, parserContext));

        for (String dependency : XMLHelper
                .getAttributeValueAsList(configElement.getAttributeNodeNS(null, "depends-on"))) {
            builder.addDependsOn(dependency);
        }

        builder.setInitMethodName("initialize");
    }

    /** {@inheritDoc} */
    protected String resolveId(Element configElement, AbstractBeanDefinition beanDef, ParserContext parserContext) {
        return configElement.getAttributeNS(null, "id");
    }
}
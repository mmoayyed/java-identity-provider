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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Spring configuration parser for {@link AttributeResolverBean}s.
 */
public class AttributeResolverBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(AttributeResolverNamespaceHandler.NAMESPACE,
            "AttributeResolverType");

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeResolverNamespaceHandler.NAMESPACE, 
            "AttributeResolver");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return AttributeResolverBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext context, BeanDefinitionBuilder builder) {
        Map<QName, List<Element>> configChildren = XMLHelper.getChildElements(config);
        List<Element> children;

        children = configChildren.get(new QName(AttributeResolverNamespaceHandler.NAMESPACE, "PrincipalConnector"));
        builder.addPropertyValue("principalConnectors", SpringConfigurationUtils
                        .parseCustomElements(children, context));

        children = configChildren.get(new QName(AttributeResolverNamespaceHandler.NAMESPACE, "DataConnector"));
        builder.addPropertyValue("dataConnectors", SpringConfigurationUtils.parseCustomElements(children, context));

        children = configChildren.get(new QName(AttributeResolverNamespaceHandler.NAMESPACE, "AttributeDefinition"));
        builder.addPropertyValue("attributeDefinitions", SpringConfigurationUtils
                .parseCustomElements(children, context));
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}
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

package net.shibboleth.idp.attribute.resolver.spring;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImpl;
import net.shibboleth.idp.attribute.resolver.spring.ad.BaseAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.AbstractDataConnectorParser;
import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Bean definition parser for an {@link net.shibboleth.idp.attribute.resolver.AttributeResolver}. <br/>
 * This creates a {@link AttributeResolverImpl} from a &lt;resolver:AttributeResolver&gt; definition.
 */
public class AttributeResolverParser extends AbstractSingleBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "AttributeResolver");

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(AttributeResolverNamespaceHandler.NAMESPACE,
            "AttributeResolverType");

    /** {@inheritDoc} */
    @Override protected Class<AttributeResolverImpl> getBeanClass(@Nullable Element element) {
        return AttributeResolverImpl.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(Element config, ParserContext context, BeanDefinitionBuilder builder) {

        final Map<QName, List<Element>> configChildren = ElementSupport.getIndexedChildElements(config);
        List<Element> children;

        // TODO principal connector
        // children = configChildren.get(new QName(AttributeResolverNamespaceHandler.NAMESPACE, "PrincipalConnector"));
        // SpringSupport.parseCustomElements(children, context);
        String id = StringSupport.trimOrNull(config.getAttributeNS(null, "id"));

        if (null == id) {
            // Compatibility with V2
            id = "Shibboleth.Resolver";
        }

        builder.addConstructorArgValue(id);

        children = configChildren.get(BaseAttributeDefinitionParser.ELEMENT_NAME);
        builder.addConstructorArgValue(SpringSupport.parseCustomElements(children, context));

        children = configChildren.get(AbstractDataConnectorParser.ELEMENT_NAME);
        builder.addConstructorArgValue(SpringSupport.parseCustomElements(children, context));
    }

    /** {@inheritDoc} */
    @Override protected boolean shouldGenerateId() {
        return true;
    }
}
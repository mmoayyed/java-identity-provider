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

package net.shibboleth.idp.attribute.resolver.spring.impl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.resolver.ResolverDataConnectorDependency;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.spring.ResolverPluginDependencyParser;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/** Bean definition parser for a {@link ResolverPluginDependency}. */
public class InputDataConnectorParser extends ResolverPluginDependencyParser {

    /** Element name. */
    @Nonnull public static final QName ELEMENT_NAME =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "InputDataConnector");

    /** Child Element Name. */
    @Nonnull @Deprecated public static final QName SOURCE_ATTRIBUTES =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "SourceAttribute");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InputDataConnectorParser.class);

    /** {@inheritDoc} */
    @Override protected Class<ResolverDataConnectorDependency> getBeanClass(@Nullable final Element element) {
        return ResolverDataConnectorDependency.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);
        
        final Attr attributes = config.getAttributeNodeNS(null, "attributeNames");
        final String allAttributes = StringSupport.trimOrNull(config.getAttributeNS(null, "allAttributes"));
        final List<Element> attributeElements = ElementSupport.getChildElements(config, SOURCE_ATTRIBUTES);
        if (attributes != null) {
            if (allAttributes != null) {
                log.error("attributeNames and allAttributes are mutually exclusive");
                throw new BeanCreationException(ELEMENT_NAME.getLocalPart()
                        + ": attributeNames and allAttributes are mutually exclusive");
            }
            if (!attributeElements.isEmpty()) {
                log.error("attributeNames and <SourceAttribute> are mutually exclusive");
                throw new BeanCreationException(ELEMENT_NAME.getLocalPart()
                        + ": attributeNames and child elements are mutually exclusive");
            }
            builder.addPropertyValue("attributeNames", SpringSupport.getAttributeValueAsList(attributes));
        } else if (allAttributes != null) {
            if (!attributeElements.isEmpty()) {
                log.error("allAttributes and <SourceAttribute> are mutually exclusive");
                throw new BeanCreationException(ELEMENT_NAME.getLocalPart()
                        + ": attributeNames and allAttributes are mutually exclusive");
            }
            builder.addPropertyValue("allAttributes", allAttributes);
        } else if (!attributeElements.isEmpty()) {
            final ManagedList<String> elementNameList = new ManagedList<>(attributeElements.size());
            for (final Element el:attributeElements) {
                elementNameList.add(el.getTextContent());
            }
            builder.addPropertyValue("attributeNames", elementNameList);
        } else {
            log.error("One of attributeNames, allAttributes or <SourceAttribute>  must be specified");
            throw new BeanCreationException(ELEMENT_NAME.getLocalPart()
                    + ": One of attributeNames, allAttributes or <SourceAttribute> must be specified ");
        }
    }
    
}
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

package net.shibboleth.idp.profile.spring.relyingparty.metadata.filter.impl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import org.opensaml.saml.metadata.resolver.filter.MetadataFilterChain;
import org.opensaml.saml.metadata.resolver.filter.impl.ByReferenceMetadataFilter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.ext.spring.util.AbstractCustomBeanDefinitionParser;
import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataProviderParser;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

/**
 * Parser for a &lt;ByReference&gt; filter.
 */
public class ByReferenceParser extends AbstractCustomBeanDefinitionParser {

    /** Element name. */
    @Nonnull public static final QName TYPE_NAME =
            new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE, "ByReference");

    /** {@inheritDoc} */
    @Override protected Class<?> getBeanClass(final Element element) {
        return ByReferenceMetadataFilter.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {
        builder.setLazyInit(false);

        final List<Element> children = ElementSupport.getChildElements(element,
                new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE, "MetadataFilters"));

        if (null != children && !children.isEmpty()) {
            
            final ManagedMap<Object,BeanDefinition> mappings = new ManagedMap<>();
            
            for (final Element child : children) {
                final List<Element> filters = ElementSupport.getChildElements(child,
                        new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE, "MetadataFilter"));
                if (filters != null && !filters.isEmpty()) {
                    final String providerRef = child.getAttributeNS(null, "providerRef");
                    final ManagedList<BeanDefinition> filterBeans =
                            SpringSupport.parseCustomElements(filters, parserContext, builder);
                    if (filterBeans != null) {
                        if (filterBeans.size() == 1) {
                            mappings.put(providerRef, filterBeans.get(0));
                        } else {
                            final BeanDefinitionBuilder chain =
                                    BeanDefinitionBuilder.genericBeanDefinition(MetadataFilterChain.class);
                            chain.addPropertyValue("filters", filterBeans);
                            mappings.put(providerRef, chain.getBeanDefinition());
                        }
                    }
                }
            }
            
            builder.addPropertyValue("filterMappings", mappings);
        }
    }

    /** {@inheritDoc} */
    @Override protected boolean shouldGenerateId() {
        return true;
    }

}
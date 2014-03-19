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

package net.shibboleth.idp.profile.spring.relyingparty.metadata;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilter;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser for a &lt;rp:MetadataProvider&gt; element.
 * 
 * TODO.
 */
public class MetadataProviderParser extends AbstractSingleBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(MetadataNamespaceHandler.NAMESPACE, "MetadataProvider");

    /** {@inheritDoc} */
    @Override protected Class<? extends MetadataResolver> getBeanClass(Element element) {
        return InternalMP.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    }

    public static class InternalMP implements MetadataResolver {

        /** {@inheritDoc} */
        @Override @Nonnull public Iterable<EntityDescriptor> resolve(CriteriaSet criteria) throws ResolverException {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override @Nullable public EntityDescriptor resolveSingle(CriteriaSet criteria) throws ResolverException {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override @Nullable public String getId() {
            return "the_id";
        }

        /** {@inheritDoc} */
        @Override public boolean isInitialized() {
            // TODO Auto-generated method stub
            return true;
        }

        /** {@inheritDoc} */
        @Override public void initialize() throws ComponentInitializationException {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        @Override public boolean isDestroyed() {
            // TODO Auto-generated method stub
            return false;
        }

        /** {@inheritDoc} */
        @Override public void destroy() {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        @Override public boolean isRequireValidMetadata() {
            // TODO Auto-generated method stub
            return false;
        }

        /** {@inheritDoc} */
        @Override public void setRequireValidMetadata(boolean requireValidMetadata) {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        @Override public MetadataFilter getMetadataFilter() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override public void setMetadataFilter(MetadataFilter newFilter) {
            // TODO Auto-generated method stub

        }

    }
}

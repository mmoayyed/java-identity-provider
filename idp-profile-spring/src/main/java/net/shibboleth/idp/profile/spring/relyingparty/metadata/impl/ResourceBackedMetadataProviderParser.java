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

package net.shibboleth.idp.profile.spring.relyingparty.metadata.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.opensaml.saml.metadata.resolver.impl.AbstractBatchMetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.FileBackedHTTPMetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.HTTPMetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.ResourceBackedMetadataResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.ext.spring.resource.ResourceHelper;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataProviderParser;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.HttpClientFactoryBean;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;

/**
 * Parser for a ResourceBackedMetadataProvider.
 * 
 * <p>This is the most complicated of the parsers. We reach into the resource and find out what sort it is
 * and them summon up an appropriate provider.</p>
 */
public class ResourceBackedMetadataProviderParser extends AbstractReloadingMetadataProviderParser {

    /** Element name. */
    @Nonnull public static final QName ELEMENT_NAME = new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE,
            "ResourceBackedMetadataProvider");

    /** Element name for the resource elements. */
    @Nonnull public static final QName RESOURCES_NAME = new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE,
            "MetadataResource");
    
    /** For direct injection of a Spring bean. **/
    @Nonnull public static final QName RESOURCE_REF = new QName("resourceRef");


    /** Log. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ResourceBackedMetadataProviderParser.class);

    /** {@inheritDoc} */
    @Override protected Class<? extends AbstractBatchMetadataResolver> getNativeBeanClass(final Element element) {

        if (AttributeSupport.hasAttribute(element, RESOURCE_REF)) {
            return ResourceBackedMetadataResolver.class;
        }

        throw new BeanCreationException("No resourceRef specified for ResourceBackedMetadataProvider");
    }

    /** {@inheritDoc} */
    @Override protected void doNativeParse(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {
        super.doNativeParse(element, parserContext, builder);

        final String beanRef = StringSupport.trimOrNull(AttributeSupport.getAttributeValue(element, RESOURCE_REF));

        if (null == beanRef) {
            log.error("{} must not be empty", RESOURCE_REF.getLocalPart());
            throw new BeanDefinitionParsingException(new Problem(
                    "Empty bean reference for a ResourceBackedMetadataProvider", new Location(parserContext
                            .getReaderContext().getResource())));
        }
        
        final BeanDefinitionBuilder resourceConverter =
                BeanDefinitionBuilder.genericBeanDefinition(ResourceHelper.class);
        resourceConverter.setLazyInit(true);
        resourceConverter.setFactoryMethod("of");
        resourceConverter.addConstructorArgReference(beanRef);
        builder.addConstructorArgValue(resourceConverter.getBeanDefinition());
    }
    
}
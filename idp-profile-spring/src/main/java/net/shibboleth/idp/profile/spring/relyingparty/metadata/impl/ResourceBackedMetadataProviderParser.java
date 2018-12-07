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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.resource.ResourceHelper;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataProviderParser;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.HttpClientFactoryBean;
import net.shibboleth.idp.profile.spring.resource.impl.ClasspathResourceParser;
import net.shibboleth.idp.profile.spring.resource.impl.ResourceNamespaceHandler;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

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

        final List<Element> resources = ElementSupport.getChildElements(element, RESOURCES_NAME);
        if (null == resources || resources.isEmpty()) {
            if (AttributeSupport.hasAttribute(element, RESOURCE_REF)) {
                return ResourceBackedMetadataResolver.class;
            }

            throw new BeanCreationException("No <Resource> specified for ResourceBackedMetadataProvider");
        }

        final QName qName = DOMTypeSupport.getXSIType(resources.get(0));
        if (null == qName) {
            log.error("No type specified for a <Resource> within a ResourceBackedMetadataProvider");
            throw new BeanCreationException(
                    "No type specified for a <Resource> within a ResourceBackedMetadataProvider");
        }
        log.debug("Comparing type '{}' against known Resources", qName.getLocalPart());

        if (ClasspathResourceParser.ELEMENT_NAME.equals(qName)) {
            return ResourceBackedMetadataResolver.class;
        } else if (ResourceNamespaceHandler.HTTP_ELEMENT_NAME.equals(qName)) {
            return HTTPMetadataResolver.class;
        } else if (ResourceNamespaceHandler.FILE_HTTP_ELEMENT_NAME.equals(qName)) {
            return FileBackedHTTPMetadataResolver.class;
        } else if (ResourceNamespaceHandler.FILESYSTEM_ELEMENT_NAME.equals(qName)) {
            return FilesystemMetadataResolver.class;
        }

        log.error("ResourceBackedMetadataProvider : Unrecognised resource type: {} ", qName.getLocalPart());
        throw new BeanCreationException("ResourceBackedMetadataProvider : Unrecognised resource type: "
                + qName.getLocalPart());
    }

    /** {@inheritDoc} */
    @Override protected void doNativeParse(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {
        super.doNativeParse(element, parserContext, builder);

        if (element.hasAttributeNS(null, "maxCacheDuration")) {
            log.error("{}: maxCacheDuration is not supported",
                    parserContext.getReaderContext().getResource().getDescription());
            throw new BeanDefinitionParsingException(new Problem("maxCacheDuration is not supported", new Location(
                    parserContext.getReaderContext().getResource())));
        }

        final List<Element> resources = ElementSupport.getChildElements(element, RESOURCES_NAME);
        if (resources.isEmpty()) {
            parseResource(StringSupport.trimOrNull(AttributeSupport.getAttributeValue(element, RESOURCE_REF)),
                    parserContext, builder);
            return;
        }
        DeprecationSupport.warnOnce(ObjectType.ELEMENT, "Resource",
                parserContext.getReaderContext().getResource().getDescription(),
                "resourceRef property");
        
        if (resources.size() != 1) {
            log.error("{}: Only one Resource may be supplied to a ResourceBackedMetadataProvider", parserContext
                    .getReaderContext().getResource().getDescription());
            throw new BeanDefinitionParsingException(new Problem(
                    "Only one Resource may be supplied to a ResourceBackedMetadataProvider", new Location(parserContext
                            .getReaderContext().getResource())));
        }

        ResourceNamespaceHandler.noFilters(resources.get(0), parserContext.getReaderContext());

        final QName qName = DOMTypeSupport.getXSIType(resources.get(0));
        log.debug("Dispatching based on type '{}'", qName.getLocalPart());

        if (ClasspathResourceParser.ELEMENT_NAME.equals(qName)) {

            parseResource(resources.get(0), parserContext, builder);

        } else if (ResourceNamespaceHandler.HTTP_ELEMENT_NAME.equals(qName)) {

            DeprecationSupport.warn(ObjectType.ELEMENT, ResourceNamespaceHandler.HTTP_ELEMENT_NAME.toString(),
                    parserContext.getReaderContext().getResource().getDescription(),
                    HTTPMetadataProviderParser.ELEMENT_NAME.toString());
            parseHTTPResource(resources.get(0), parserContext, builder);

        } else if (ResourceNamespaceHandler.FILE_HTTP_ELEMENT_NAME.equals(qName)) {

            DeprecationSupport.warn(ObjectType.ELEMENT, ResourceNamespaceHandler.FILE_HTTP_ELEMENT_NAME.toString(),
                    parserContext.getReaderContext().getResource().getDescription(),
                    FileBackedHTTPMetadataProviderParser.ELEMENT_NAME.toString());
            parseFileBackedHTTPResource(resources.get(0), parserContext, builder);

        } else if (ResourceNamespaceHandler.FILESYSTEM_ELEMENT_NAME.equals(qName)) {

            DeprecationSupport.warn(ObjectType.ELEMENT, ResourceNamespaceHandler.FILESYSTEM_ELEMENT_NAME.toString(),
                    parserContext.getReaderContext().getResource().getDescription(),
                    FilesystemMetadataProviderParser.ELEMENT_NAME.toString());
            parseFilesystemResource(resources.get(0), parserContext, builder);
        }
    }
    
    /**
     * Parse the provided Attribute and populate an appropriate {@link ResourceBackedMetadataResolver}.
     * 
     * @param beanReference the reference
     * @param parserContext the parser context
     * @param builder the builder for the {@link ResourceBackedMetadataResolver}.
     */
    private void parseResource(@Nullable final String beanReference, final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {

        if (null == beanReference) {
            log.error("{} must not be empty", RESOURCE_REF.getLocalPart());
            throw new BeanDefinitionParsingException(new Problem(
                    "Empty bean reference for a ResourceBackedMetadataProvider", new Location(parserContext
                            .getReaderContext().getResource())));
        }
        
        final BeanDefinitionBuilder resourceConverter =
                BeanDefinitionBuilder.genericBeanDefinition(ResourceHelper.class);
        resourceConverter.setLazyInit(true);
        resourceConverter.setFactoryMethod("of");
        resourceConverter.addConstructorArgReference(beanReference);
        builder.addConstructorArgValue(resourceConverter.getBeanDefinition());
    }

    
    /**
     * Parse the provided &lt;Resource&gt; and populate an appropriate {@link ResourceBackedMetadataResolver}.
     * 
     * @param element the &lt;Resource&gt; element
     * @param parserContext the parser context
     * @param builder the builder for the {@link ResourceBackedMetadataResolver}.
     */
    private void parseResource(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {

        final BeanDefinitionBuilder resourceConverter =
                BeanDefinitionBuilder.genericBeanDefinition(ResourceHelper.class);
        resourceConverter.setLazyInit(true);
        resourceConverter.setFactoryMethod("of");
        resourceConverter.addConstructorArgValue(parserContext.getDelegate().parseCustomElement(element));

        builder.addConstructorArgValue(resourceConverter.getBeanDefinition());
    }

    /**
     * Parse the provided &lt;Resource&gt; and populate an appropriate {@link HTTPMetadataResolver}.
     * 
     * <br/>
     * See {@link HTTPMetadataProviderParser#doParse(Element, ParserContext, BeanDefinitionBuilder)}.
     * 
     * @param element the &lt;Resource&gt; element
     * @param parserContext the parser context
     * @param builder the builder for the {@link HTTPMetadataResolver}.
     */
    private void parseHTTPResource(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {

        final BeanDefinitionBuilder clientBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(HttpClientFactoryBean.class);

        clientBuilder.setLazyInit(true);

        builder.addConstructorArgValue(clientBuilder.getBeanDefinition());
        builder.addConstructorArgValue(StringSupport.trimOrNull(element.getAttributeNS(null, "url")));
    }

    /**
     * Parse the provided &lt;Resource&gt; and populate an appropriate {@link FileBackedHTTPMetadataResolver}. <br/>
     * See {@link FileBackedHTTPMetadataProviderParser#doParse(Element, ParserContext, BeanDefinitionBuilder)}.
     * 
     * @param element the &lt;Resource&gt; element
     * @param parserContext the parser context
     * @param builder the builder for the {@link FileBackedHTTPMetadataResolver}.
     */
    private void
            parseFileBackedHTTPResource(final Element element, final ParserContext parserContext,
                    final BeanDefinitionBuilder builder) {

        parseHTTPResource(element, parserContext, builder);
        builder.addConstructorArgValue(StringSupport.trimOrNull(element.getAttributeNS(null, "file")));
    }

    /**
     * Parse the provided &lt;Resource&gt; and populate an appropriate {@link FilesystemMetadataResolver}.
     * 
     * <br/>
     * See {@link FilesystemMetadataProviderParser#doParse(Element, ParserContext, BeanDefinitionBuilder)}.
     * 
     * @param element the &lt;Resource&gt; element
     * @param parserContext the parser context
     * @param builder the builder for the {@link FilesystemMetadataResolver}.
     */
    private void parseFilesystemResource(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {
        builder.addConstructorArgValue(StringSupport.trimOrNull(element.getAttributeNS(null, "file")));
    }

}
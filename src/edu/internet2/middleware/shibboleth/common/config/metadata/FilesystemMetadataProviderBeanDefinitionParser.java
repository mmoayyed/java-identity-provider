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

package edu.internet2.middleware.shibboleth.common.config.metadata;

import java.io.File;

import javax.xml.namespace.QName;

import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for Shibboleth file system based metadata provider definition.
 */
public class FilesystemMetadataProviderBeanDefinitionParser extends BaseMetadataProviderDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName("urn:mace:shibboleth:2.0:metadata", "FilesystemMetadataProvider");

    /** Maintain expired metadata configuration option attribute name. */
    public static final String METADATA_FILE_ATTRIBUTE_NAME = "metadataFile";

    /** Maintain expired metadata configuration option attribute name. */
    public static final String MAINTAIN_EXPIRED_METADATA_ATTRIBUTE_NAME = "maintainExpiredMetadata";

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return FilesystemMetadataProvider.class;
    }

    /** {@inheritDoc} */
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(FilesystemMetadataProvider.class);
        parseCommonConfig(builder, element, parserContext);
        
        builder.setInitMethodName("initialize");
        builder.addPropertyReference("parserPool", "shibboleth.ParserPool");
        
        String metadataFile = element.getAttributeNS(null, METADATA_FILE_ATTRIBUTE_NAME);
        builder.addConstructorArg(new File(metadataFile));
        
        boolean maintainExpiredMetadata = Boolean.parseBoolean(element.getAttributeNS(null,
                MAINTAIN_EXPIRED_METADATA_ATTRIBUTE_NAME));
        builder.addPropertyValue("maintainExpiredMetadata", maintainExpiredMetadata);
        
        return builder.getBeanDefinition();
    }
}
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

import javax.xml.namespace.QName;

import org.opensaml.saml2.metadata.provider.FileBackedHTTPMetadataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Spring bean definition for Shibboleth file backed metadata provider. */
public class FileBackedHTTPMetadataProviderBeanDefinitionParser extends HTTPMetadataProviderBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(MetadataNamespaceHandler.NAMESPACE,
            "FileBackedHTTPMetadataProvider");
    
    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(FileBackedHTTPMetadataProviderBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return FileBackedHTTPMetadataProvider.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        String backingFile = element.getAttributeNS(null, "backingFile");
        builder.addConstructorArgValue(backingFile);
        
        // log.warn("Use of the FileBackedHTTPMetadataProvider is deprecated.  Please use the ResourceBackedMetadataProvider with the FileBackedHttpResource");
    }
}
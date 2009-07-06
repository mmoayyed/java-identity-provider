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

import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for Shibboleth file backed url metadata provider definition.
 */
public class HTTPMetadataProviderBeanDefinitionParser extends BaseMetadataProviderBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(MetadataNamespaceHandler.NAMESPACE, "HTTPMetadataProvider");

    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(HTTPMetadataProviderBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return HTTPMetadataProvider.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        builder.setInitMethodName("initialize");

        super.doParse(element, parserContext, builder);

        builder.addPropertyReference("parserPool", "shibboleth.ParserPool");

        String metadataURL = element.getAttributeNS(null, "metadataURL");
        builder.addConstructorArgValue(metadataURL);

        if (element.hasAttributeNS(null, "requestTimeout")) {
            builder.addConstructorArgValue(Integer.parseInt(DatatypeHelper.safeTrim(element.getAttributeNS(null,
                    "requestTimeout"))));
        } else {
            builder.addConstructorArgValue(10000);
        }

        if (element.hasAttributeNS(null, "cacheDuration")) {
            builder.addPropertyValue("maxCacheDuration", Integer.parseInt(DatatypeHelper.safeTrim(element
                    .getAttributeNS(null, "cacheDuration"))));
        } else {
            builder.addPropertyValue("maxCacheDuration", 2880);
        }

        if (element.hasAttributeNS(null, "maintainExpiredMetadata")) {
            builder.addPropertyValue("maintainExpiredMetadata", XMLHelper.getAttributeValueAsBoolean(element
                    .getAttributeNodeNS(null, "maintainExpiredMetadata")));
        } else {
            builder.addPropertyValue("maintainExpiredMetadata", false);
        }

        // log.warn("Use of the HTTPMetadataProvider is deprecated.  Please use the ResourceBackedMetadataProvider with a HttpResource");
    }
}
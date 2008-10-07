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

import java.util.List;

import javax.xml.namespace.QName;

import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.metadata.provider.DOMMetadataProvider;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for a OpenSAML2 DOMMetadataProvider.
 */
public class InlineMetadataProviderBeanDefinitionParser extends BaseMetadataProviderBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(MetadataNamespaceHandler.NAMESPACE, "InlineMetadataProvider");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return DOMMetadataProvider.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        builder.setInitMethodName("initialize");

        super.doParse(config, parserContext, builder);

        List<Element> metadataContent = XMLHelper.getChildElementsByTagNameNS(config, SAMLConstants.SAML20MD_NS,
                "EntitiesDescriptor");
        if (metadataContent.size() < 1) {
            metadataContent = XMLHelper.getChildElementsByTagNameNS(config, SAMLConstants.SAML20MD_NS,
                    "EntityDescriptor");
        }
        builder.addConstructorArgValue((Element) metadataContent.get(0));
    }
}
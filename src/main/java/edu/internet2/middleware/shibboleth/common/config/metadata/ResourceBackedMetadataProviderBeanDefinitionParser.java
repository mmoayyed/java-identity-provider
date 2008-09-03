/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
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

import org.opensaml.saml2.metadata.provider.ResourceBackedMetadataProvider;
import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/** Parser for {@link ResourceBackedMetadataProvider} definitions. */
public class ResourceBackedMetadataProviderBeanDefinitionParser extends BaseMetadataProviderBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(MetadataNamespaceHandler.NAMESPACE,
            "ResourceBackedMetadataProvider");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return ResourceBackedMetadataProvider.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);
        
        builder.setInitMethodName("initialize");

        String parserPoolRef = DatatypeHelper.safeTrimOrNullString(config.getAttributeNS(null, "parserPoolRef"));
        if (parserPoolRef == null) {
            parserPoolRef = "shibboleth.ParserPool";
        }
        builder.addPropertyReference("parserPool", parserPoolRef);

        NodeList resourceElems = config.getElementsByTagNameNS(MetadataNamespaceHandler.NAMESPACE, "MetadataResource");
        builder.addConstructorArgValue(SpringConfigurationUtils.parseCustomElement((Element) resourceElems.item(0),
                parserContext));
    }
}
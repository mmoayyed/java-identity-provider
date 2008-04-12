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

import org.opensaml.saml2.metadata.provider.ChainingMetadataProvider;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Spring bean definition parser for Shibboleth chaining metadata provider definition.
 */
public class ChainingMetadataProviderBeanDefinitionParser extends BaseMetadataProviderDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(MetadataNamespaceHandler.NAMESPACE, "ChainingMetadataProvider");

    /** {@inheritDoc} */
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ChainingMetadataProvider.class);
        parseCommonConfig(builder, element, parserContext);
        parseConfig(builder, element, parserContext);
        return builder.getBeanDefinition();
    }
    
    /**
     * Parses the configuration for this provider.
     * 
     * @param builder builder of the bean definition
     * @param element configuration element
     * @param context current parsing context
     */
    protected void parseConfig(BeanDefinitionBuilder builder, Element element, ParserContext context) {
        NodeList providerElems = element.getElementsByTagNameNS(MetadataNamespaceHandler.NAMESPACE, "MetadataProvider");
        if(providerElems != null){
            ManagedList providers = SpringConfigurationUtils.parseCustomElements(providerElems, context);        
            builder.addPropertyValue("providers", providers);
        }
    }
}
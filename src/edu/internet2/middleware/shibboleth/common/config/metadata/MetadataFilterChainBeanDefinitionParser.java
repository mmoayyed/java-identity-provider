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

import org.opensaml.saml2.metadata.provider.MetadataFilterChain;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Spring bean definition parser for {@link MetadataFilterChain}s.
 */
public class MetadataFilterChainBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(MetadataNamespaceHandler.NAMESPACE, "ChainingFilter");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return MetadataFilterChain.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        List<Element> filterElems = XMLHelper.getChildElementsByTagNameNS(element, MetadataNamespaceHandler.NAMESPACE,
                "MetadataFilter");
        builder.addPropertyValue("filters", SpringConfigurationUtils.parseCustomElements(filterElems, parserContext));
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}
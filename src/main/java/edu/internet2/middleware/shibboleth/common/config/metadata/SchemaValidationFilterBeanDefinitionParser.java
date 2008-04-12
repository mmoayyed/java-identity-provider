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

import org.opensaml.saml2.metadata.provider.SchemaValidationFilter;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

/**
 * Bean definition parser for {@link SchemaValidationFilter} beans.
 */
public class SchemaValidationFilterBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(MetadataNamespaceHandler.NAMESPACE, "SchemaValidation");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return SchemaValidationFilter.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        String[] extensions = null;
        
        List<Element> elems = XMLHelper.getChildElementsByTagNameNS(element, MetadataNamespaceHandler.NAMESPACE,
                "ExtensionSchema");
        if (elems != null) {
            extensions = new String[elems.size()];
            for (int i = 0; i < elems.size(); i++) {
                extensions[i] = elems.get(i).getTextContent();
            }
        }
        
        builder.addConstructorArg(extensions);
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}
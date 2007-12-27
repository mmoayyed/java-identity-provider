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

import org.opensaml.saml2.metadata.provider.SignatureValidationFilter;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

/**
 * Bean definition parser for {@link SignatureValidationFilter} beans.
 */
public class SignatureValidationFilterBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(MetadataNamespaceHandler.NAMESPACE, "SignatureValidation");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return SignatureValidationFilter.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        builder.addConstructorArgReference(DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null,
                "trustEngineRef")));

        builder.addPropertyValue("requireSignature", XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(
                null, "requireSignedMetadata")));
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}
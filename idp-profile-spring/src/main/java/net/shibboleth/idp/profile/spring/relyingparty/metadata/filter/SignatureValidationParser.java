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

package net.shibboleth.idp.profile.spring.relyingparty.metadata.filter;

import javax.xml.namespace.QName;

import net.shibboleth.idp.profile.spring.relyingparty.metadata.MetadataNamespaceHandler;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.saml.metadata.resolver.filter.impl.SignatureValidationFilter;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

/**
 * Parser for xsi:type="SignatureValidation".
 */
public class SignatureValidationParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(MetadataNamespaceHandler.NAMESPACE, "SignatureValidation");

    /** {@inheritDoc} */
    @Override protected Class getBeanClass(Element element) {
        return SignatureValidationFilter.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(Element element, BeanDefinitionBuilder builder) {
        builder.addConstructorArgReference(StringSupport.trimOrNull(element.getAttributeNS(null, "trustEngineRef")));

        if (element.hasAttributeNS(null, "requireSignedMetadata")) {
            builder.addPropertyValue("requireSignature", element.getAttributeNS(null, "requireSignedMetadata"));
        }
    }

    /** {@inheritDoc} */
    @Override protected boolean shouldGenerateId() {
        return true;
    }

}

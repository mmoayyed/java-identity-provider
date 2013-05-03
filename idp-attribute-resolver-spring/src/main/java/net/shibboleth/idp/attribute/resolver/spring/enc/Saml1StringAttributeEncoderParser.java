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

package net.shibboleth.idp.attribute.resolver.spring.enc;

import javax.xml.namespace.QName;

import net.shibboleth.idp.saml.impl.attribute.encoding.Saml1StringAttributeEncoder;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring Bean Definition Parser for SAML1 string attribute encoder.
 */
public class Saml1StringAttributeEncoderParser extends BaseAttributeEncoderParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeEncoderNamespaceHandler.NAMESPACE, "SAML1String");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return Saml1StringAttributeEncoder.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        String namespace = "urn:mace:shibboleth:1.0:attributeNamespace:uri";
        if (config.hasAttributeNS(null, "namespace")) {
            namespace = StringSupport.trimOrNull(config.getAttributeNS(null, "namespace"));
        }
        builder.addPropertyValue("namespace", namespace);

        final String attributeName = StringSupport.trimOrNull(config.getAttributeNS(null, "name"));
        if (attributeName == null) {
            throw new BeanCreationException("SAML 1 attribute encoders must contain a name");
        }
    }
}
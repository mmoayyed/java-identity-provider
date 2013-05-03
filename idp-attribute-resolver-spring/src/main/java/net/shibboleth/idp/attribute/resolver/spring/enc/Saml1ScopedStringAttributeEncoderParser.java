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

import net.shibboleth.idp.saml.impl.attribute.encoding.Saml1ScopedStringAttributeEncoder;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring Bean Definition Parser for SAML1 string attribute encoder.
 */
public class Saml1ScopedStringAttributeEncoderParser extends
        BaseScopedAttributeEncoderParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeEncoderNamespaceHandler.NAMESPACE, "SAML1ScopedString");

    /** Local name of namespace attribute. */
    public static final String NAMESPACE_ATTRIBUTE_NAME = "namespace";

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return Saml1ScopedStringAttributeEncoder.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        if (config.hasAttributeNS(null, "scopeType")) {
            builder.addPropertyValue("scopeType", config.getAttributeNS(null, "scopeType"));
        } else {
            builder.addPropertyValue("scopeType", "attribute");
        }

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
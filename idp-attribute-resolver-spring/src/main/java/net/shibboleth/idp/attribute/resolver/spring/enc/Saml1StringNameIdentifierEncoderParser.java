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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.saml.impl.attribute.encoding.Saml1StringSubjectNameIdentifierEncoder;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for {@link SAML1StringNameIdentifierEncoder}s.
 */
public class Saml1StringNameIdentifierEncoderParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(AttributeEncoderNamespaceHandler.NAMESPACE,
            "SAML1StringNameIdentifier");

    /** {@inheritDoc} */
    protected Class<Saml1StringSubjectNameIdentifierEncoder> getBeanClass(@Nullable Element element) {
        return Saml1StringSubjectNameIdentifierEncoder.class;
    }

    /** {@inheritDoc} */
    protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        String namespace = "urn:oasis:names:tc:SAML:1.0:nameid-format:unspecified";
        if (config.hasAttributeNS(null, "nameFormat")) {
            namespace = StringSupport.trimOrNull(config.getAttributeNS(null, "nameFormat"));
        }
        builder.addPropertyValue("nameFormat", namespace);

        builder.addPropertyValue("nameQualifier", config.getAttributeNS(null, "nameQualifier"));
    }
    
    /** {@inheritDoc} */
    public boolean shouldGenerateId() {
        return true;
    }

}
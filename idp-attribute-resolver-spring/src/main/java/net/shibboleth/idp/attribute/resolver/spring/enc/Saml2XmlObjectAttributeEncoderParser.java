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

import net.shibboleth.idp.saml.impl.attribute.encoding.Saml2XmlObjectAttributeEncoder;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Spring Bean Definition Parser for SAML2 XMLObject attribute encoder. */
public class Saml2XmlObjectAttributeEncoderParser extends BaseAttributeEncoderParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeEncoderNamespaceHandler.NAMESPACE, "SAML2XMLObject");

    /** Local name of name format attribute. */
    public static final String NAME_FORMAT_ATTRIBUTE_NAME = "nameFormat";

    /** Local name of friendly name attribute. */
    public static final String FRIENDLY_NAME_ATTRIBUTE_NAME = "friendlyName";

    /** {@inheritDoc} */
    protected Class<Saml2XmlObjectAttributeEncoder> getBeanClass(@Nullable Element element) {
        return Saml2XmlObjectAttributeEncoder.class;
    }

    /** {@inheritDoc} */
    protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        String namespace = "urn:oasis:names:tc:SAML:2.0:attrname-format:uri";
        if (config.hasAttributeNS(null, "nameFormat")) {
            namespace = StringSupport.trimOrNull(config.getAttributeNS(null, "nameFormat"));
        }
        builder.addPropertyValue("namespace", namespace);
        
        builder.addPropertyValue("friendlyName", config.getAttribute(FRIENDLY_NAME_ATTRIBUTE_NAME));

        String attributeName = StringSupport.trimOrNull(config.getAttributeNS(null, "name"));
        if (attributeName == null) {
            throw new BeanCreationException("SAML 2 attribute encoders must contain a name");
        }
    }

}
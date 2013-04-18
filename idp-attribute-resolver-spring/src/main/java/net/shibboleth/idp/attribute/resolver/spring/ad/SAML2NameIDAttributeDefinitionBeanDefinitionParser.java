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

package net.shibboleth.idp.attribute.resolver.spring.ad;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.impl.ad.SAML2NameIDAttributeDefinition;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Spring bean definition parser for SAML 2 NameID attribute definitions. */
public class SAML2NameIDAttributeDefinitionBeanDefinitionParser extends BaseAttributeDefinitionBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "SAML2NameID");

    /** Logger. */
    private final Logger log = LoggerFactory
            .getLogger(SAML1NameIdentifierAttributeDefinitionBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return SAML2NameIDAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        String nameIdFormat = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";
        if (config.hasAttributeNS(null, "nameIdFormat")) {
            nameIdFormat = StringSupport.trimOrNull(config.getAttributeNS(null, "nameIdFormat"));
        }
        builder.addPropertyValue("nameIdFormat", nameIdFormat);

        final String nameIdQualifier = StringSupport.trimOrNull(config.getAttributeNS(null, "nameIdQualifier"));
        builder.addPropertyValue("nameIdQualifier", nameIdQualifier);

        final String nameIdSPQualifier = StringSupport.trimOrNull(config.getAttributeNS(null, "nameIdSPQualifier"));
        builder.addPropertyValue("nameIdSPQualifier", nameIdSPQualifier);

        log.debug("Attribute definition '{}': nameIdFormat '{}', nameIdQualifier '{}', nameIdSPQualifier '{}'",
                new Object[] {getDefinitionId(), nameIdFormat, nameIdQualifier, nameIdSPQualifier,});
    }
}
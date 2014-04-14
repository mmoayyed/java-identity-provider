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

package net.shibboleth.idp.profile.spring.relyingparty.saml;

import java.util.List;

import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser for all classes which extend
 * {@link net.shibboleth.idp.saml.saml2.profile.config.AbstractSAML2ProfileConfiguration} and for elements which inherit
 * from <code>saml:SAML2ProfileConfigutationType</code>.
 */
public abstract class BaseSAML2ProfileConfigurationParser extends BaseSAMLProfileConfigurationParser {

    /** logger. */
    private Logger log = LoggerFactory.getLogger(BaseSAML2ProfileConfigurationParser.class);

    /**
     * Get the list of proxy audiences from the &lt;ProxyAudience&gt; sub-elements.
     * 
     * @param element the element under discussion
     * @return the list of elements (which are subject to property replacement)
     */
    protected List<String> getProxyAudiences(Element element) {
        List<Element> audienceElems =
                ElementSupport.getChildElementsByTagNameNS(element, RelyingPartySAMLNamespaceHandler.NAMESPACE,
                        "ProxyAudience");
        List<String> result = new ManagedList<>(audienceElems.size());
        for (Element audienceElement : audienceElems) {
            final String audience = StringSupport.trimOrNull(audienceElement.getTextContent());
            if (null != audience) {
                result.add(audience);
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        if (element.hasAttributeNS(null, "encryptionOptional")) {
            builder.addPropertyValue("encryptionOptional", element.getAttributeNS(null, "encryptionOptional"));
        }
        
        builder.addPropertyValue("encryptAssertionsPredicate",
                predicateForEncryption(element.getAttributeNS(null, "encryptAssertions"), "conditional"));
        builder.addPropertyValue("encryptNameIDsPredicate",
                predicateForEncryption(element.getAttributeNS(null, "encryptNameIds"), "never"));
        // default encryptAttributesPredicate
        if (element.hasAttributeNS(null, "assertionProxyCount")) {
            builder.addPropertyValue("proxyCount", element.getAttributeNS(null, "assertionProxyCount"));
        }
        if (element.hasAttributeNS(null, "attributeAuthority")) {
            final String attributeAuthority =
                    StringSupport.trimOrNull(element.getAttributeNS(null, "attributeAuthority"));
            if (null != attributeAuthority && !"shibboleth.SAML2AttributeAuthority".equals(attributeAuthority)) {
                log.warn("Non default value for attributeAuthority of '{}' has been ignored", attributeAuthority);
            }
        }
        builder.addPropertyValue("proxyAudiences", getProxyAudiences(element));
    }
}

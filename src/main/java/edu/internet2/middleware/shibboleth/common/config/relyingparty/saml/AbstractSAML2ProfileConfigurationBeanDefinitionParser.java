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

package edu.internet2.middleware.shibboleth.common.config.relyingparty.saml;

import java.util.List;

import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.LazyList;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.relyingparty.provider.CryptoOperationRequirementLevel;

/**
 * Base Spring configuration parser for SAML 2 profile configurations.
 */
public abstract class AbstractSAML2ProfileConfigurationBeanDefinitionParser extends
        AbstractSAMLProfileConfigurationBeanDefinitionParser {

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        List<Element> proxyAudiences = XMLHelper.getChildElementsByTagNameNS(element,
                SAMLRelyingPartyNamespaceHandler.NAMESPACE, "ProxyAudience");
        if (proxyAudiences != null && proxyAudiences.size() > 0) {
            LazyList<String> audiences = new LazyList<String>();
            for (Element proxyAudience : proxyAudiences) {
                audiences.add(DatatypeHelper.safeTrimOrNullString(proxyAudience.getTextContent()));
            }

            builder.addPropertyValue("proxyAudiences", audiences);
        }

        builder.addPropertyReference("attributeAuthority", DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(
                null, "attributeAuthority")));

        if (element.hasAttributeNS(null, "encryptNameIds")) {
            builder.addPropertyValue("encryptNameIds", CryptoOperationRequirementLevel.valueOf(element.getAttributeNS(
                    null, "encryptNameIds")));
        } else {
            builder.addPropertyValue("encryptNameIds", CryptoOperationRequirementLevel.never);
        }

        if (element.hasAttributeNS(null, "encryptAssertions")) {
            builder.addPropertyValue("encryptAssertions", CryptoOperationRequirementLevel.valueOf(element
                    .getAttributeNS(null, "encryptAssertions")));
        } else {
            builder.addPropertyValue("encryptAssertions", CryptoOperationRequirementLevel.conditional);
        }

        if (element.hasAttributeNS(null, "assertionProxyCount")) {
            builder.addPropertyValue("assertionProxyCount", Integer.parseInt(DatatypeHelper
                    .safeTrimOrNullString(element.getAttributeNS(null, "assertionProxyCount"))));
        } else {
            builder.addPropertyValue("assertionProxyCount", 0);
        }
    }
}
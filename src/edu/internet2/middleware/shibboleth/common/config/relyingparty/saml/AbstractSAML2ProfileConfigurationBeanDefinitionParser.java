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

import java.util.ArrayList;
import java.util.List;

import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Base Spring configuration parser for SAML 2 profile configurations.
 */
public abstract class AbstractSAML2ProfileConfigurationBeanDefinitionParser extends
        AbstractSAMLProfileConfigurationBeanDefinitionParser {

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        // TODO encryption credentials

        List<Element> proxyAudiences = XMLHelper.getChildElementsByTagNameNS(element,
                SAMLRelyingPartyNamespaceHandler.NAMESPACE, "ProxyAudience");
        if (proxyAudiences != null && proxyAudiences.size() > 0) {
            ArrayList<String> audiences = new ArrayList<String>();
            for (Element proxyAudience : proxyAudiences) {
                audiences.add(DatatypeHelper.safeTrimOrNullString(proxyAudience.getTextContent()));
            }

            builder.addPropertyValue("proxyAudiences", audiences);
        }

        builder.addPropertyValue("encryptNameIds", XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(
                null, "encryptNameIds")));

        builder.addPropertyValue("encryptAssertions", XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(
                null, "encryptAssertions")));

        builder.addPropertyValue("assertionProxyCount", Integer.parseInt(DatatypeHelper.safeTrimOrNullString(element
                .getAttributeNS(null, "assertionProxyCount"))));
    }
}
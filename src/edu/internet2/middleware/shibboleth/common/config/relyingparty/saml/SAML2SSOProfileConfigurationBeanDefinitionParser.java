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

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring configuration parser for SAML 2 SSO profile configurations.
 */
public class SAML2SSOProfileConfigurationBeanDefinitionParser extends
        AbstractSAML2ProfileConfigurationBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(SAMLRelyingPartyNamespaceHandler.NAMESPACE, "SAML2SSOProfile");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return SAML2SSOProfileConfigurationFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        builder.addPropertyValue("localityAddress", DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null,
                "localityAddress")));

        builder.addPropertyValue("localityDNSName", DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null,
                "localityDNSName")));

        builder.addPropertyValue("includeAttributeStatement", XMLHelper.getAttributeValueAsBoolean(element
                .getAttributeNodeNS(null, "includeAttributeStatement")));
    }
}
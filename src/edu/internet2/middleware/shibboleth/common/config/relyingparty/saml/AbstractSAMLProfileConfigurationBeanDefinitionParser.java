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
import java.util.Map;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Base Spring configuration parser for SAML profile configurations.
 */
public abstract class AbstractSAMLProfileConfigurationBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        builder.setLazyInit(true);
        Map<QName, List<Element>> children = XMLHelper.getChildElements(element);

        List<Element> audienceElems = children.get(new QName(SAMLRelyingPartyNamespaceHandler.NAMESPACE, "Audience"));
        if (audienceElems != null && audienceElems.size() > 0) {
            ArrayList<String> audiences = new ArrayList<String>();
            for (Element audienceElem : audienceElems) {
                audiences.add(DatatypeHelper.safeTrimOrNullString(audienceElem.getTextContent()));
            }
            builder.addPropertyValue("audiences", audiences);
        }

        if (element.hasAttributeNS(null, "signingCredentialRef")) {
            builder.addPropertyReference("signingCredential", DatatypeHelper.safeTrimOrNullString(element
                    .getAttributeNS(null, "signingCredentialRef")));
        }

        builder.addPropertyValue("assertionLifetime", Long.parseLong(DatatypeHelper.safeTrimOrNullString(element
                .getAttributeNS(null, "assertionLifetime"))));

        String artifactType = DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null, "outboundArtifactType"));
        if (artifactType != null) {
            byte[] artifactTypeBytes = DatatypeHelper.intToByteArray(Integer.parseInt(artifactType));
            byte[] trimmedArtifactTypeBytes = { artifactTypeBytes[2], artifactTypeBytes[3] };
            builder.addPropertyValue("outboundArtifactType", trimmedArtifactTypeBytes);
        }

        builder.addPropertyValue("signRequests", XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null,
                "signRequests")));

        builder.addPropertyValue("signResponses", XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null,
                "signResponses")));

        builder.addPropertyValue("signAssertions", XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(
                null, "signAssertions")));
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}
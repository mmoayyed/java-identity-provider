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

package edu.internet2.middleware.shibboleth.common.config.security.saml;

import javax.xml.namespace.QName;

import org.opensaml.common.binding.security.MessageReplayRule;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for message replay rules.
 */
public class MessageReplayRuleBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(SAMLSecurityNamespaceHandler.NAMESPACE, "Replay");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return MessageReplayRule.class;
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        builder.addConstructorArgReference(DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null,
                "replayCacheRef")));

        if (element.hasAttributeNS(null, "required")) {
            builder.addPropertyValue("requiredRule", XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(
                    null, "required")));
        } else {
            builder.addPropertyValue("requiredRule", true);
        }
        
    }
}
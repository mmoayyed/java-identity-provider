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

package edu.internet2.middleware.shibboleth.common.config.relyingparty;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;
import edu.internet2.middleware.shibboleth.common.config.metadata.MetadataNamespaceHandler;

/**
 * Spring bean definition parser for relying party group configurations.
 */
public class RelyingPartyGroupBeanDefinitionParser extends AbstractBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(RelyingPartyNamespaceHandler.NAMESPACE, "RelyingPartyGroup");

    /** {@inheritDoc} */
    protected AbstractBeanDefinition parseInternal(Element config, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(RelyingPartyGroup.class);

        Map<QName, List<Element>> configChildren = XMLHelper.getChildElements(config);
        List<Element> children;

        children = configChildren.get(new QName(RelyingPartyNamespaceHandler.NAMESPACE, "AnonymousRelyingParty"));
        if (children != null && children.size() > 0) {
            builder.addPropertyValue("anonymousRP", SpringConfigurationUtils.parseCustomElement(children.get(0),
                    parserContext));
        }

        children = configChildren.get(new QName(RelyingPartyNamespaceHandler.NAMESPACE, "DefaultRelyingParty"));
        if (children != null && children.size() > 0) {
            builder.addPropertyValue("defaultRP", SpringConfigurationUtils.parseCustomElement(children.get(0),
                    parserContext));
        }

        children = configChildren.get(new QName(RelyingPartyNamespaceHandler.NAMESPACE, "RelyingParty"));
        if (children != null && children.size() > 0) {
            builder.addPropertyValue("relyingParties", SpringConfigurationUtils.parseCustomElements(children,
                    parserContext));
        }

        children = configChildren.get(new QName(RelyingPartyNamespaceHandler.NAMESPACE, "RelyingParty"));
        builder.addPropertyValue("relyingParties", SpringConfigurationUtils
                .parseCustomElements(children, parserContext));

        children = configChildren.get(new QName(MetadataNamespaceHandler.NAMESPACE, "MetadataProvider"));
        if (children != null && children.size() > 0) {
            builder.addPropertyValue("metadataProvider", SpringConfigurationUtils.parseCustomElement(children.get(0),
                    parserContext));
        }

        children = configChildren.get(new QName(RelyingPartyNamespaceHandler.NAMESPACE, "Credential"));
        builder.addPropertyValue("credentials", SpringConfigurationUtils.parseCustomElements(children, parserContext));

        return builder.getBeanDefinition();
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}
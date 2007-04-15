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
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Base bean definition parser for relying party manager configurations.
 */
public abstract class AbstractRelyingPartyManagerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        Map<QName, List<Element>> children = XMLHelper.getChildElements(element);

        List<Element> anonymousRelyingParty = children
                .get(UnidentifiedRelyingPartyBeanDefinitionParser.ANON_RP_ELEMENT_NAME);
        if (anonymousRelyingParty != null && anonymousRelyingParty.size() > 0) {
            builder.addPropertyValue("anonymousRelyingParty", SpringConfigurationUtils.parseCustomElement(
                    anonymousRelyingParty.get(0), parserContext));
        }

        List<Element> defaultRelyingParty = children
                .get(UnidentifiedRelyingPartyBeanDefinitionParser.DEFAULT_RP_ELEMENT_NAME);
        if (defaultRelyingParty != null && defaultRelyingParty.size() > 0) {
            builder.addPropertyValue("defaultRelyingParty", SpringConfigurationUtils.parseCustomElement(
                    defaultRelyingParty.get(0), parserContext));
        }

        List<Element> relyingParties = children.get(IdentifiedRelyingPartyBeanDefinitionParser.RP_ELEMENT_NAME);
        if (relyingParties != null && relyingParties.size() > 0) {
            builder.addPropertyValue("relyingParties", SpringConfigurationUtils.parseCustomElements(relyingParties,
                    parserContext));
        }
    }
}
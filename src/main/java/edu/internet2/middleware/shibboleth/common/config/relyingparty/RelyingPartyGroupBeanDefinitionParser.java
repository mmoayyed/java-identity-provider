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
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;
import edu.internet2.middleware.shibboleth.common.config.metadata.MetadataNamespaceHandler;
import edu.internet2.middleware.shibboleth.common.config.security.SecurityNamespaceHandler;

/**
 * Spring bean definition parser for relying party group configurations.
 */
public class RelyingPartyGroupBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(RelyingPartyNamespaceHandler.NAMESPACE, "RelyingPartyGroup");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return RelyingPartyGroup.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        Map<QName, List<Element>> configChildren = XMLHelper.getChildElements(config);

        List<Element> mds = configChildren.get(new QName(MetadataNamespaceHandler.NAMESPACE, "MetadataProvider"));
        if (mds != null && mds.size() > 0) {
            Element mdConfigElem = mds.get(0);
            SpringConfigurationUtils.parseCustomElement(mdConfigElem, parserContext);
            builder.addPropertyValue("metadataProvider", new RuntimeBeanReference(mdConfigElem.getAttributeNS(null,
                    "id")));
        }

        parseRelyingPartyConfiguration(configChildren, builder, parserContext);

        parseSecurityConfiguration(configChildren, builder, parserContext);
    }

    /**
     * Parses the relying party related configuration elements.
     * 
     * @param configChildren relying party group children
     * @param builder bean definition builder
     * @param parserContext current parsing context
     */
    protected void parseRelyingPartyConfiguration(Map<QName, List<Element>> configChildren,
            BeanDefinitionBuilder builder, ParserContext parserContext) {
        List<Element> anonRP = configChildren.get(RelyingPartyConfigurationBeanDefinitionParser.ANON_RP_ELEMENT_NAME);
        if (anonRP != null && anonRP.size() > 0) {
            builder.addPropertyValue("anonymousRP", SpringConfigurationUtils.parseInnerCustomElement(anonRP.get(0),
                    parserContext));
        }

        List<Element> defaultRP = configChildren
                .get(RelyingPartyConfigurationBeanDefinitionParser.DEFAULT_RP_ELEMENT_NAME);
        builder.addPropertyValue("defaultRP", SpringConfigurationUtils.parseInnerCustomElement(defaultRP.get(0),
                parserContext));

        List<Element> rps = configChildren.get(RelyingPartyConfigurationBeanDefinitionParser.RP_ELEMENT_NAME);
        builder.addPropertyValue("relyingParties", SpringConfigurationUtils
                .parseInnerCustomElements(rps, parserContext));
    }

    /**
     * Parses the security related configuration elements.
     * 
     * @param configChildren relying party group children
     * @param builder bean definition builder
     * @param parserContext current parsing context
     */
    protected void parseSecurityConfiguration(Map<QName, List<Element>> configChildren, BeanDefinitionBuilder builder,
            ParserContext parserContext) {

        List<Element> creds = configChildren.get(new QName(SecurityNamespaceHandler.NAMESPACE, "Credential"));
        builder.addPropertyValue("credentials", SpringConfigurationUtils
                        .parseInnerCustomElements(creds, parserContext));

        List<Element> engines = configChildren.get(new QName(SecurityNamespaceHandler.NAMESPACE, "TrustEngine"));
        builder.addPropertyValue("trustEngines", SpringConfigurationUtils.parseInnerCustomElements(engines,
                parserContext));

        List<Element> secPols = configChildren.get(new QName(SecurityNamespaceHandler.NAMESPACE, "SecurityPolicy"));
        builder.addPropertyValue("securityPolicies", SpringConfigurationUtils.parseInnerCustomElements(secPols,
                parserContext));
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}
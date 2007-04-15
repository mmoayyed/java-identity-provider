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

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Spring bean definition parser for unidentified relying party configurations.
 */
public class UnidentifiedRelyingPartyBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(RelyingPartyNamespaceHandler.NAMESPACE, "UnidentifiedRelyingParty");

    /** Name of the anonymous relying party configuration element. */
    public static final QName ANON_RP_ELEMENT_NAME = new QName(RelyingPartyNamespaceHandler.NAMESPACE,
            "AnonymousRelyingParty");

    /** Name of the default relying party configuration element. */
    public static final QName DEFAULT_RP_ELEMENT_NAME = new QName(RelyingPartyNamespaceHandler.NAMESPACE,
            "DefaultRelyingParty");

    /** Class logger. */
    private static Logger log = Logger.getLogger(UnidentifiedRelyingPartyBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return UnidentifiedRelyingPartyFactoryBean.class;
    }
    
    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        processProvider(builder, element);
        processDefaultSigningCredential(builder, element, parserContext);
        processProfileConfigurations(builder, element, parserContext);
    }

    /**
     * Processes provider configuration attribute.
     * 
     * @param factory bean factory accepting provider id
     * @param configElement configuration element
     */
    protected void processProvider(BeanDefinitionBuilder factory, Element configElement) {
        String provider = DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null, "provider"));
        if (log.isDebugEnabled()) {
            log.debug("Relying party configuration: provider " + provider);
        }
        factory.addPropertyValue("providerId", provider);
    }

    /**
     * Process default signing credential or credential reference.
     * 
     * @param factory bean factory accepting credential
     * @param configElement configuration element
     * @param parserContext current parsing context
     */
    protected void processDefaultSigningCredential(BeanDefinitionBuilder factory, Element configElement,
            ParserContext parserContext) {
        List<Element> anonRP = XMLHelper.getChildElementsByTagNameNS(configElement,
                RelyingPartyNamespaceHandler.NAMESPACE, "DefaultSigningCredential");
        if (anonRP != null || anonRP.size() > 0) {
            if (log.isDebugEnabled()) {
                log.debug("Relying party configuration: setting default signing credential");
            }
            factory.addPropertyValue("defaultSigningCredential", SpringConfigurationUtils.parseCustomElement(anonRP
                    .get(0), parserContext));
        }

        List<Element> anonRPRef = XMLHelper.getChildElementsByTagNameNS(configElement,
                RelyingPartyNamespaceHandler.NAMESPACE, "DefaultSigningCredentialRef");
        if (anonRPRef != null || anonRPRef.size() > 0) {
            if (log.isDebugEnabled()) {
                log.debug("Relying party configuration: setting default signing credential reference");
            }
            factory.addPropertyReference("defaultSigningCredential", anonRPRef.get(0).getTextContent());
        }
    }
    
    /**
     * Processes profile configurations.
     * 
      * @param factory bean factory accepting profile configurations
     * @param configElement configuration element
     * @param parserContext current parsing context
     */
    protected void processProfileConfigurations(BeanDefinitionBuilder factory, Element configElement,
            ParserContext parserContext) {
        List<Element> profileConfigs = XMLHelper.getChildElementsByTagNameNS(configElement,
                RelyingPartyNamespaceHandler.NAMESPACE, "ProfileConfiguration");
        if (profileConfigs != null || profileConfigs.size() > 0) {
            if (log.isDebugEnabled()) {
                log.debug("Relying party configuration: setting default signing credential");
            }
            factory.addPropertyValue("profileConfigurations", SpringConfigurationUtils.parseCustomElements(
                    profileConfigs, parserContext));
        }
    }
}
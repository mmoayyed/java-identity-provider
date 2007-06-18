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
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;
import edu.internet2.middleware.shibboleth.common.relyingparty.provider.SAMLMDRelyingPartyConfigurationManager;

/**
 * Bean definition parser for relying party elements.
 */
public class RelyingPartyConfigurationBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

    /** Schema type name. */
    public static final QName URP_TYPE_NAME = new QName(RelyingPartyNamespaceHandler.NAMESPACE,
            "UnidentifiedRelyingParty");

    /** Schema type name. */
    public static final QName RP_TYPE_NAME = new QName(RelyingPartyNamespaceHandler.NAMESPACE, "IdentifiedRelyingParty");

    /** Name of the anonymous relying party configuration element. */
    public static final QName ANON_RP_ELEMENT_NAME = new QName(RelyingPartyNamespaceHandler.NAMESPACE,
            "AnonymousRelyingParty");

    /** Name of the default relying party configuration element. */
    public static final QName DEFAULT_RP_ELEMENT_NAME = new QName(RelyingPartyNamespaceHandler.NAMESPACE,
            "DefaultRelyingParty");

    /** Name of the relying party configuration element. */
    public static final QName RP_ELEMENT_NAME = new QName(RelyingPartyNamespaceHandler.NAMESPACE, "RelyingParty");

    /** Class logger. */
    private static Logger log = Logger.getLogger(RelyingPartyConfigurationBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return RelyingPartyFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        String rpId = getRelyingPartyId(config);
        if (log.isDebugEnabled()) {
            log.debug("Relying party configuration: relying party id " + rpId);
        }
        builder.addPropertyValue("relyingPartyId", rpId);

        String provider = DatatypeHelper.safeTrimOrNullString(config.getAttributeNS(null, "provider"));
        if (log.isDebugEnabled()) {
            log.debug("Relying party configuration: provider " + provider);
        }
        builder.addPropertyValue("providerId", provider);

        String authnMethod = DatatypeHelper.safeTrimOrNullString(config.getAttributeNS(null,
                "defaultAuthenticationMethod"));
        if (log.isDebugEnabled()) {
            log.debug("Relying party configuration: default authentication method " + authnMethod);
        }
        builder.addPropertyValue("defaultAuthenticationMethod", authnMethod);

        if (config.hasAttributeNS(null, "defaultSigningCredentialRef")) {
            builder.addPropertyReference("defaultSigningCredential", config.getAttributeNS(null,
                    "defaultSigningCredentialRef"));
        }

        List<Element> profileConfigs = XMLHelper.getChildElementsByTagNameNS(config,
                RelyingPartyNamespaceHandler.NAMESPACE, "ProfileConfiguration");
        if (profileConfigs != null && profileConfigs.size() > 0) {
            if (log.isDebugEnabled()) {
                log.debug("Relying party configuration: setting default signing credential");
            }
            builder.addPropertyValue("profileConfigurations", SpringConfigurationUtils.parseCustomElements(
                    profileConfigs, parserContext));
        }
    }

    /**
     * Gets the ID of the relying party.
     * 
     * @param config relying party configuration element
     * 
     * @return ID of the relying party
     */
    protected String getRelyingPartyId(Element config) {
        String id = DatatypeHelper.safeTrimOrNullString(config.getAttributeNS(null, "id"));
        if (id == null) {
            if (XMLHelper.getNodeQName(config).equals(ANON_RP_ELEMENT_NAME)) {
                id = SAMLMDRelyingPartyConfigurationManager.ANONYMOUS_RP_NAME;
            } else if (XMLHelper.getNodeQName(config).equals(DEFAULT_RP_ELEMENT_NAME)) {
                id = SAMLMDRelyingPartyConfigurationManager.DEFAULT_RP_NAME;
            }
        }

        return id;
    }

    /** {@inheritDoc} */
    protected String resolveId(Element arg0, AbstractBeanDefinition arg1, ParserContext arg2) {
        return getRelyingPartyId(arg0);
    }
}
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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.naming.Context;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.LdapDataConnector.SEARCH_SCOPE;

/**
 * Spring bean definition parser for configuring an LDAP data connector.
 */
public class LdapDataConnectorBeanDefinitionParser extends BaseDataConnectorBeanDefinitionParser {

    /** LDAP data connector type name. */
    public static final QName TYPE_NAME = new QName(LdapDataConnectorNamespaceHandler.NAMESPACE, "LDAPDirectory");

    /** Class logger. */
    private static Logger log = Logger.getLogger(LdapDataConnectorBeanDefinitionParser.class);

    /** FilterTemplate element name. */
    private static final QName FILTER_TEMPLATE_ELEMENT_NAME = new QName(LdapDataConnectorNamespaceHandler.NAMESPACE,
            "FilterTemplate");

    /** ReturnAttributes element name. */
    private static final QName RETURN_ATTRIBUTES_ELEMENT_NAME = new QName(LdapDataConnectorNamespaceHandler.NAMESPACE,
            "ReturnAttributes");

    /** LDAPProperty element name. */
    private static final QName LDAP_PROPERTY_ELEMENT_NAME = new QName(LdapDataConnectorNamespaceHandler.NAMESPACE,
            "LDAPProperty");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return LdapDataConnectorFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(String pluginId, Element pluginConfig, Map<QName, List<Element>> pluginConfigChildren,
            BeanDefinitionBuilder pluginBuilder, ParserContext parserContext) {
        super.doParse(pluginId, pluginConfig, pluginConfigChildren, pluginBuilder, parserContext);

        String ldapURL = DatatypeHelper.safeTrimOrNullString(pluginConfig.getAttributeNS(null, "ldapUrl"));
        if (log.isDebugEnabled()) {
            log.debug("Data connector " + pluginId + " LDAP URL: " + ldapURL);
        }
        pluginBuilder.addPropertyValue("ldapUrl", ldapURL);

        String principal = DatatypeHelper.safeTrimOrNullString(pluginConfig.getAttributeNS(null, "principal"));
        pluginBuilder.addPropertyValue("principal", principal);

        String credential = DatatypeHelper.safeTrimOrNullString(pluginConfig
                .getAttributeNS(null, "principalCredential"));
        pluginBuilder.addPropertyValue("principalCredential", credential);

        String filterTemplate = DatatypeHelper.safeTrimOrNullString(pluginConfigChildren.get(
                FILTER_TEMPLATE_ELEMENT_NAME).get(0).getTextContent());
        if (log.isDebugEnabled()) {
            log.debug("Data connector " + pluginId + " LDAP filter template: " + filterTemplate);
        }
        pluginBuilder.addPropertyValue("filterTemplate", filterTemplate);

        SEARCH_SCOPE searchScope = SEARCH_SCOPE.valueOf(pluginConfig.getAttributeNS(null, "searchScope"));
        pluginBuilder.addPropertyValue("searchScope", searchScope);

        String[] returnAttributes = processReturnAttributes(pluginConfigChildren.get(RETURN_ATTRIBUTES_ELEMENT_NAME));
        pluginBuilder.addPropertyValue("returnAttributes", returnAttributes);

        Map<String, String> ldapProperties = processLDAPProperties(pluginId, pluginConfig, pluginConfigChildren
                .get(LDAP_PROPERTY_ELEMENT_NAME));
        pluginBuilder.addPropertyValue("ldapProperties", ldapProperties);

        boolean useStartTLS = XMLHelper
                .getAttributeValueAsBoolean(pluginConfig.getAttributeNodeNS(null, "useStartTLS"));
        pluginBuilder.addPropertyValue("useStartTLS", useStartTLS);

        int poolInitialSize = Integer.parseInt(pluginConfig.getAttributeNS(null, "poolInitialSize"));
        pluginBuilder.addPropertyValue("poolInitialSize", poolInitialSize);

        int poolMaxIdleSize = Integer.parseInt(pluginConfig.getAttributeNS(null, "poolMaxIdleSize"));
        pluginBuilder.addPropertyValue("poolMaxIdleSize", poolMaxIdleSize);

        int searchTimeLimit = Integer.parseInt(pluginConfig.getAttributeNS(null, "searchTimeLimit"));
        pluginBuilder.addPropertyValue("searchTimeLimit", searchTimeLimit);

        int maxResultSize = Integer.parseInt(pluginConfig.getAttributeNS(null, "maxResultSize"));
        pluginBuilder.addPropertyValue("maxResultSize", maxResultSize);

        boolean cacheResults = XMLHelper.getAttributeValueAsBoolean(pluginConfig.getAttributeNodeNS(null,
                "cacheResults"));
        pluginBuilder.addPropertyValue("cacheResults", cacheResults);

        boolean mergeResults = XMLHelper.getAttributeValueAsBoolean(pluginConfig.getAttributeNodeNS(null,
                "mergeResults"));
        pluginBuilder.addPropertyValue("mergeResults", mergeResults);

        boolean noResultsIsError = XMLHelper.getAttributeValueAsBoolean(pluginConfig.getAttributeNodeNS(null,
                "noResultIsError"));
        pluginBuilder.addPropertyValue("noResultsIsError", noResultsIsError);

        String templateEngineRef = DatatypeHelper.safeTrimOrNullString(pluginConfig.getAttributeNS(null,
                "templateEngine"));
        pluginBuilder.addPropertyReference("templateEngine", templateEngineRef);
    }

    /**
     * Processes the return attributes provided in the configuration.
     * 
     * @param returnAttributes return attributes provided in the configuration
     * 
     * @return return attributes provided in the configuration
     */
    protected String[] processReturnAttributes(List<Element> returnAttributes) {
        if (returnAttributes == null || returnAttributes.size() == 0) {
            return null;
        }

        StringTokenizer attributeTokens = new StringTokenizer(returnAttributes.get(0).getTextContent(), " ");
        String[] attributes = new String[attributeTokens.countTokens()];
        for (int i = 0; attributeTokens.hasMoreTokens(); i++) {
            attributes[i] = DatatypeHelper.safeTrimOrNullString(attributeTokens.nextToken());
        }

        return attributes;
    }

    /**
     * Processes the LDAP properties provided in the configuration.
     * 
     * @param pluginConfigElem root configurtion element for this plugin
     * @param propertyElems LDAP properties provided in the configuration
     * 
     * @return LDAP properties provided in the configuration
     */
    protected Map<String, String> processLDAPProperties(String pluginId, Element pluginConfigElem,
            List<Element> propertyElems) {
        HashMap<String, String> properties = new HashMap<String, String>();

        String propName;
        String propValue;
        if (propertyElems != null) {
            for (Element propertyElem : propertyElems) {
                propName = DatatypeHelper.safeTrimOrNullString(propertyElem.getAttributeNS(null, "name"));
                propValue = DatatypeHelper.safeTrimOrNullString(propertyElem.getAttributeNS(null, "value"));
                properties.put(propName, propValue);
            }
        }

        return properties;
    }
}
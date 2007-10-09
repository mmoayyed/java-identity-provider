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

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.LdapDataConnector.SEARCH_SCOPE;

/**
 * Spring bean definition parser for configuring an LDAP data connector.
 */
public class LdapDataConnectorBeanDefinitionParser extends BaseDataConnectorBeanDefinitionParser {

    /** LDAP data connector type name. */
    public static final QName TYPE_NAME = new QName(DataConnectorNamespaceHandler.NAMESPACE, "LDAPDirectory");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(LdapDataConnectorBeanDefinitionParser.class);

    /** FilterTemplate element name. */

    /** ReturnAttributes element name. */

    /** LDAPProperty element name. */

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return LdapDataConnectorFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(String pluginId, Element pluginConfig, Map<QName, List<Element>> pluginConfigChildren,
            BeanDefinitionBuilder pluginBuilder, ParserContext parserContext) {
        super.doParse(pluginId, pluginConfig, pluginConfigChildren, pluginBuilder, parserContext);

        String ldapURL = pluginConfig.getAttributeNS(null, "ldapUrl");
        log.debug("Data connector {} LDAP URL: {}", pluginId, ldapURL);
        pluginBuilder.addPropertyValue("ldapUrl", ldapURL);

        String baseDN = pluginConfig.getAttributeNS(null, "baseDN");
        log.debug("Data connector {} base DN: {}", pluginId, baseDN);
        pluginBuilder.addPropertyValue("baseDN", baseDN);

        String principal = pluginConfig.getAttributeNS(null, "principal");
        log.debug("Data connector {} principal: {}", pluginId, principal);
        pluginBuilder.addPropertyValue("principal", principal);

        String credential = pluginConfig.getAttributeNS(null, "principalCredential");
        pluginBuilder.addPropertyValue("principalCredential", credential);

        String filterTemplate = pluginConfigChildren.get(
                new QName(DataConnectorNamespaceHandler.NAMESPACE, "FilterTemplate")).get(0).getTextContent();
        log.debug("Data connector {} LDAP filter template: {}", pluginId, filterTemplate);
        pluginBuilder.addPropertyValue("filterTemplate", filterTemplate);

        SEARCH_SCOPE searchScope = SEARCH_SCOPE.valueOf(pluginConfig.getAttributeNS(null, "searchScope"));
        log.debug("Data connector {} search scope: {}", pluginId, searchScope);
        pluginBuilder.addPropertyValue("searchScope", searchScope);

        String[] returnAttributes = processReturnAttributes(pluginConfigChildren.get(new QName(
                DataConnectorNamespaceHandler.NAMESPACE, "ReturnAttributes")));
        log.debug("Data connector {} return attributes: {}", pluginId, returnAttributes);
        pluginBuilder.addPropertyValue("returnAttributes", returnAttributes);

        Map<String, String> ldapProperties = processLDAPProperties(pluginConfigChildren.get(new QName(
                DataConnectorNamespaceHandler.NAMESPACE, "LDAPProperty")));
        log.debug("Data connector {} LDAP properties: {}", pluginId, ldapProperties);
        pluginBuilder.addPropertyValue("ldapProperties", ldapProperties);

        boolean useStartTLS = XMLHelper
                .getAttributeValueAsBoolean(pluginConfig.getAttributeNodeNS(null, "useStartTLS"));
        log.debug("Data connector {} use startTLS: {}", pluginId, useStartTLS);
        pluginBuilder.addPropertyValue("useStartTLS", useStartTLS);

        int poolInitialSize = Integer.parseInt(pluginConfig.getAttributeNS(null, "poolInitialSize"));
        log.debug("Data connector {} initial connection pool size: {}", pluginId, poolInitialSize);
        pluginBuilder.addPropertyValue("poolInitialSize", poolInitialSize);

        int poolMaxIdleSize = Integer.parseInt(pluginConfig.getAttributeNS(null, "poolMaxIdleSize"));
        log.debug("Data connector {} maximum idle connection pool size: {}", pluginId, poolMaxIdleSize);
        pluginBuilder.addPropertyValue("poolMaxIdleSize", poolMaxIdleSize);

        int searchTimeLimit = Integer.parseInt(pluginConfig.getAttributeNS(null, "searchTimeLimit"));
        log.debug("Data connector {} search timeout: {}ms", pluginId, searchTimeLimit);
        pluginBuilder.addPropertyValue("searchTimeLimit", searchTimeLimit);

        int maxResultSize = Integer.parseInt(pluginConfig.getAttributeNS(null, "maxResultSize"));
        log.debug("Data connector {} max search result size: {}", pluginId, maxResultSize);
        pluginBuilder.addPropertyValue("maxResultSize", maxResultSize);

        boolean cacheResults = XMLHelper.getAttributeValueAsBoolean(pluginConfig.getAttributeNodeNS(null,
                "cacheResults"));
        log.debug("Data connector {} cache results: {}", pluginId, cacheResults);
        pluginBuilder.addPropertyValue("cacheResults", cacheResults);

        boolean mergeResults = XMLHelper.getAttributeValueAsBoolean(pluginConfig.getAttributeNodeNS(null,
                "mergeResults"));
        log.debug("Data connector{} merge results: {}", pluginId, mergeResults);
        pluginBuilder.addPropertyValue("mergeResults", mergeResults);

        boolean noResultsIsError = XMLHelper.getAttributeValueAsBoolean(pluginConfig.getAttributeNodeNS(null,
                "noResultIsError"));
        log.debug("Data connector {} no results is error: {}", pluginId, noResultsIsError);
        pluginBuilder.addPropertyValue("noResultsIsError", noResultsIsError);

        String templateEngineRef = pluginConfig.getAttributeNS(null, "templateEngine");
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
     * @param propertyElems LDAP properties provided in the configuration
     * 
     * @return LDAP properties provided in the configuration
     */
    protected Map<String, String> processLDAPProperties(List<Element> propertyElems) {
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
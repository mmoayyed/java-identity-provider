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

package edu.internet2.middleware.shibboleth.common.config.resolver.dataConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

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

    /** FilterTemplate element name. */
    private static final QName FILTER_TEMPLATE_ELEMENT_NAME = new QName(LdapDataConnectorNamespaceHandler.NAMESPACE,
            "FilterTemplate");

    /** SearchScope element name. */
    private static final QName SEARCH_SCOPE_ELEMENT_NAME = new QName(LdapDataConnectorNamespaceHandler.NAMESPACE,
            "SearchScope");

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
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        Map<QName, List<Element>> children = XMLHelper.getChildElements(element);

        String filterTemplate = DatatypeHelper.safeTrimOrNullString(children.get(FILTER_TEMPLATE_ELEMENT_NAME).get(0)
                .getTextContent());
        builder.addPropertyValue("filterTemplate", filterTemplate);

        SEARCH_SCOPE searchScope = SEARCH_SCOPE.SUBTREE;
        List<Element> searchScopes = children.get(SEARCH_SCOPE_ELEMENT_NAME);
        if (searchScopes != null && searchScopes.size() > 0) {
            searchScope = SEARCH_SCOPE.valueOf(searchScopes.get(0).getTextContent());
            builder.addPropertyValue("searchScope", searchScope);
        }

        String[] returnAttributes = processReturnAttributes(children.get(RETURN_ATTRIBUTES_ELEMENT_NAME));
        builder.addPropertyValue("returnAttributes", returnAttributes);

        Map<String, String> ldapProperties = processLDAPProperties(children.get(LDAP_PROPERTY_ELEMENT_NAME));
        builder.addPropertyValue("ldapProperties", ldapProperties);

        boolean useStartTLS = XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null, "useStartTLS"));
        builder.addPropertyValue("useStartTLS", useStartTLS);

        int poolInitialSize = Integer.parseInt(element.getAttributeNS(null, "poolInitialSize"));
        builder.addPropertyValue("poolInitialSize", poolInitialSize);

        int poolMaxIdleSize = Integer.parseInt(element.getAttributeNS(null, "poolMaxIdleSize"));
        builder.addPropertyValue("poolMaxIdleSize", poolMaxIdleSize);

        int searchTimeLimit = Integer.parseInt(element.getAttributeNS(null, "searchTimeLimit"));
        builder.addPropertyValue("searchTimeLimit", searchTimeLimit);

        int maxResultSize = Integer.parseInt(element.getAttributeNS(null, "maxResultSize"));
        builder.addPropertyValue("maxResultSize", maxResultSize);

        boolean cacheResults = XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null, "cacheResults"));
        builder.addPropertyValue("cacheResults", cacheResults);

        boolean mergeResults = XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null, "mergeResults"));
        builder.addPropertyValue("mergeResults", mergeResults);

        boolean noResultsIsError = XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null,
                "noResultIsError"));
        builder.addPropertyValue("noResultsIsError", noResultsIsError);
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
        if (propertyElems == null || propertyElems.size() == 0) {
            return null;
        }

        HashMap<String, String> properties = new HashMap<String, String>();
        String propName;
        String propValue;
        for (Element propertyElem : propertyElems) {
            propName = DatatypeHelper.safeTrimOrNullString(propertyElem.getAttributeNS(null, "name"));
            propValue = DatatypeHelper.safeTrimOrNullString(propertyElem.getAttributeNS(null, "value"));
            properties.put(propName, propValue);
        }

        return properties;
    }
}
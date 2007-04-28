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

package edu.internet2.middleware.shibboleth.common.config.attribute.filtering;

import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Base class for Spring bean definition parsers within the filter engine configuration. This base class is responsible
 * for generating an ID for the Spring bean that is unique within all the policy components loaded.
 */
public abstract class BaseFilterBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Generator of unique IDs. */
    private static SecureRandomIdentifierGenerator idGen = new SecureRandomIdentifierGenerator();

    /** {@inheritDoc} */
    protected String resolveId(Element configElement, AbstractBeanDefinition beanDefinition, ParserContext parserContext) {
        return getQualifiedId(configElement, configElement.getLocalName(), configElement.getAttributeNS(null, "id"));
    }

    /**
     * Generates an ID for a filter engine component. If the given localId is null a random one will be generated.
     * 
     * @param configElement component configuration element
     * @param componentNamespace namespace for the component
     * @param localId local id or null
     * 
     * @return unique ID for the componenent
     */
    protected String getQualifiedId(Element configElement, String componentNamespace, String localId) {
        Element afpgElement = configElement.getOwnerDocument().getDocumentElement();
        String policyGroupId = DatatypeHelper.safeTrimOrNullString(afpgElement.getAttributeNS(null, "id"));

        StringBuilder qualifiedId = new StringBuilder();
        qualifiedId.append("/");
        qualifiedId.append(AttributeFilterPolicyGroupBeanDefinitionParser.ELEMENT_NAME.getLocalPart());
        qualifiedId.append(":");
        qualifiedId.append(policyGroupId);
        if(!DatatypeHelper.isEmpty(componentNamespace)){
            qualifiedId.append("/");
            qualifiedId.append(componentNamespace);
            qualifiedId.append(":");
    
            if (DatatypeHelper.isEmpty(localId)) {
                qualifiedId.append(idGen.generateIdentifier());
            } else {
                qualifiedId.append(localId);
            }
        }

        return qualifiedId.toString();
    }

    /**
     * Gets the absolute refrence given a possibly relative reference.
     * 
     * @param configElement component configuration element
     * @param componentNamespace namespace for the component
     * @param reference reference to convert into absolute form
     * 
     * @return absolute form of the reference
     */
    protected String getAbsoluteReference(Element configElement, String componentNamespace, String reference) {
        if (reference.startsWith("/")) {
            return reference;
        } else {
            return getQualifiedId(configElement, componentNamespace, reference);
        }
    }
}
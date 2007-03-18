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

package edu.internet2.middleware.shibboleth.common.config.filter;

import java.util.List;

import org.apache.log4j.Logger;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Utilities for filter engine bean definition parsers.
 */
public class FilterEngineBeanDefinitionParserUtil {
    
    /** Class logger. */
    private static Logger log = Logger.getLogger(FilterEngineBeanDefinitionParserUtil.class);

    /** Generator of unique IDs. */
    private static SecureRandomIdentifierGenerator idGen = new SecureRandomIdentifierGenerator();

    /** Constructor. */
    protected FilterEngineBeanDefinitionParserUtil() {

    }

    /**
     * Processes child elements with references.
     * 
     * @param builderProperty the builder property to set
     * @param builder the bean definition builder
     * @param children the children to process
     * @param context the parser context
     */
    public static void processChildElements(String builderProperty, BeanDefinitionBuilder builder,
            List<Element> children, ParserContext context) {
        if (children == null || children.size() == 0) {
            return;
        }

        if(log.isDebugEnabled()){
            log.debug("Processing configuration for bean property " + builderProperty);
        }
        
        String reference;
        ManagedList builderPropertyValue = new ManagedList();
        for (Element child : children) {
            if (child.hasAttributeNS(null, "ref")) {
                reference = DatatypeHelper.safeTrimOrNullString(child.getAttributeNS(null, "ref"));
                if (!reference.startsWith("/")) {
                    reference = FilterEngineBeanDefinitionParserUtil.getQualifiedId(child, child.getLocalName(),
                            reference);
                }
                if(log.isDebugEnabled()){
                    log.debug("Processing " + child.getLocalName() + " reference " + reference);
                }
                builderPropertyValue.add(new RuntimeBeanReference(reference));
            } else {
                builderPropertyValue.add(SpringConfigurationUtils.parseCustomElement(child, context));
            }
        }

        builder.addPropertyValue(builderProperty, builderPropertyValue);
    }

    /**
     * Generates an ID for a filter engine component. If the given localId is null a random one will be generated.
     * 
     * @param element component configuration element
     * @param beanNamespace namespace for the component
     * @param localId local id or null
     * 
     * @return unique ID for the componenent
     */
    public static String getQualifiedId(Element element, String beanNamespace, String localId) {
        Element afpgElement = element.getOwnerDocument().getDocumentElement();
        String policyGroupId = DatatypeHelper.safeTrimOrNullString(afpgElement.getAttributeNS(null, "id"));

        StringBuilder qualifiedId = new StringBuilder();
        qualifiedId.append("/");
        qualifiedId.append(AttributeFilterPolicyGroupBeanDefinitionParser.ELEMENT_NAME.getLocalPart());
        qualifiedId.append(":");
        qualifiedId.append(policyGroupId);
        qualifiedId.append("/");
        qualifiedId.append(beanNamespace);
        qualifiedId.append(":");

        if (DatatypeHelper.isEmpty(localId)) {
            qualifiedId.append(idGen.generateIdentifier());
        } else {
            qualifiedId.append(localId);
        }

        return qualifiedId.toString();
    }
}

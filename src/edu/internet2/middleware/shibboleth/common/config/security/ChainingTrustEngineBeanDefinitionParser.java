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

package edu.internet2.middleware.shibboleth.common.config.security;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/** Spring bean definition parser for {urn:mace:shibboleth:2.0:security}Chaining elements. */
public class ChainingTrustEngineBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
    
    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(SecurityNamespaceHandler.NAMESPACE, "Chaining");
    
    /** TrustEngine element name. */
    private static final QName TRUST_ENGINE_NAME = new QName(SecurityNamespaceHandler.NAMESPACE, "TrustEngine");
    
    /** TrustEngineRef element name. */
    private static final QName TRUST_ENGINE_REF_NAME = new QName(SecurityNamespaceHandler.NAMESPACE, "TrustEngineRef");
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ChainingTrustEngineBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return ChainingTrustEngineFactoryBean.class;
    }
    
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        log.info("Parsing configuration for {} trust engine with id: {}", XMLHelper.getXSIType(element)
                .getLocalPart(), element.getAttributeNS(null, "id"));
        
        ManagedList managedChain = new ManagedList();
        
        Element child = XMLHelper.getFirstChildElement(element);
        while (child != null) {
            QName childName = XMLHelper.getNodeQName(child);
            if (TRUST_ENGINE_NAME.equals(childName)) {
                log.debug("Parsing chain trust engine member {}", element.getAttributeNS(null, "id"));
                managedChain.add(SpringConfigurationUtils.parseCustomElement(child, parserContext));
            } else if (TRUST_ENGINE_REF_NAME.equals(childName)) {
                log.debug("Parsing chain trust engine member reference {}", element.getAttributeNS(null, "ref") );
                managedChain.add(SpringConfigurationUtils.parseCustomElementReference(child, "ref", parserContext));
            } else {
                log.error("Unsupported child element of chaining trust engine '{}' encountered with name: {}", 
                        element.getAttributeNS(null, "id"), childName);
                throw new FatalBeanException("Unsupported child element of chaining trust engine encountered");
            }
            
            child = XMLHelper.getNextSiblingElement(child);
        }
        
        builder.addPropertyValue("chain", managedChain);
    }
    
    /** {@inheritDoc} */
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        return DatatypeHelper.safeTrim(element.getAttributeNS(null, "id"));
    }
}
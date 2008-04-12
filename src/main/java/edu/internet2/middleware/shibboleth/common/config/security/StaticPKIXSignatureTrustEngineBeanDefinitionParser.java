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
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/** Spring bean definition parser for {urn:mace:shibboleth:2.0:security}StaticPKIXSignature elements. */
public class StaticPKIXSignatureTrustEngineBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
    
    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(SecurityNamespaceHandler.NAMESPACE, "StaticPKIXSignature");
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(StaticPKIXSignatureTrustEngineBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return StaticPKIXSignatureTrustEngineFactoryBean.class;
    }
    
    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        log.info("Parsing configuration for {} trust engine with id: {}", XMLHelper.getXSIType(element)
                .getLocalPart(), element.getAttributeNS(null, "id"));
        
        NodeList childElems = element.getElementsByTagNameNS(SecurityNamespaceHandler.NAMESPACE, "ValidationInfo");
        if(childElems != null){
            ManagedList pkixInfo = 
                SpringConfigurationUtils.parseCustomElements(childElems, parserContext);        
            builder.addPropertyValue("PKIXInfo", pkixInfo);
        }
    }
    
    /** {@inheritDoc} */
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        return DatatypeHelper.safeTrim(element.getAttributeNS(null, "id"));
    }
}
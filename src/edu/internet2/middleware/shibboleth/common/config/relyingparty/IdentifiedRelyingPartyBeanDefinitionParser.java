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

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for unidentified relying party configurations.
 */
public class IdentifiedRelyingPartyBeanDefinitionParser extends UnidentifiedRelyingPartyBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(RelyingPartyNamespaceHandler.NAMESPACE, "IdentifiedRelyingParty");

    /** Name of the relying party configuration element. */
    public static final QName RP_ELEMENT_NAME = new QName(RelyingPartyNamespaceHandler.NAMESPACE, "RelyingParty");

    /** Class logger. */
    private static Logger log = Logger.getLogger(IdentifiedRelyingPartyBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return IdentifiedRelyingPartyFactoryBean.class;
    }
    
    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);
        
        String id = DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null, "id"));
        if (log.isDebugEnabled()) {
            log.debug("Relying party configuration: relying party id " + id);
        }
        builder.addPropertyValue("relyingPartyId", id);
    }
    
    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}
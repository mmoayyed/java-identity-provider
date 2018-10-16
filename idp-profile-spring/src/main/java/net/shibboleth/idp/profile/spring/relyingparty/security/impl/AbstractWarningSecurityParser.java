/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.profile.spring.relyingparty.security.impl;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataProviderParser;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;

/**
 * Base class to issue a deprecation warning on activation.
 * @deprecated remove all super classes in V4. 
 */
@Deprecated
public class AbstractWarningSecurityParser extends AbstractSingleBeanDefinitionParser {

    /** Is this element ultimately parented by a MetadataFilter Element?
     * @param element what to inspect
     * @return if it is.
     */
    private boolean isDescendantOfSignatureFilter(final Element element) {
        final Node parent = element.getParentNode();
        if ((null == parent)||!(parent instanceof Element)) {
            return false;
        }
        if (AbstractMetadataProviderParser.SECURITY_NAMESPACE.equals(parent.getNamespaceURI())) {
            return isDescendantOfSignatureFilter((Element) parent);
        }
        final QName filterQname = AbstractMetadataProviderParser.METADATA_FILTER_ELEMENT_NAME;
        return filterQname.getNamespaceURI().equals(parent.getNamespaceURI())&&
                filterQname.getLocalPart().equals(parent.getLocalName());
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doParse(final Element element, final BeanDefinitionBuilder builder) {
        
        if (!isDescendantOfSignatureFilter(element)){
            DeprecationSupport.warnOnce(ObjectType.ELEMENT, 
                    element.getPrefix() +":" + element.getLocalName(), null, null);
        }
        super.doParse(element, builder);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doParse(final Element element, final ParserContext parserContext, 
            final BeanDefinitionBuilder builder) {
        if (!isDescendantOfSignatureFilter(element)){
            DeprecationSupport.warnOnce(ObjectType.ELEMENT, element.getPrefix() +":" + element.getLocalName(), 
                parserContext.getReaderContext().getResource().getDescription(), null);
        }
        super.doParse(element, parserContext, builder);
    }
}

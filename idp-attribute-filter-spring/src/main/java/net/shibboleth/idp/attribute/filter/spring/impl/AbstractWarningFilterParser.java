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

package net.shibboleth.idp.attribute.filter.spring.impl;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.filter.spring.BaseFilterParser;
import net.shibboleth.idp.attribute.filter.spring.basic.impl.AttributeFilterBasicNamespaceHandler;
import net.shibboleth.idp.attribute.filter.spring.saml.impl.AttributeFilterSAMLNamespaceHandler;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * A special case version of {@link BaseFilterParser} which warns if the non legacy name is used.
 * 
 * Initially it will warn once per namespace.
 */
public abstract class AbstractWarningFilterParser extends BaseFilterParser {

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element element, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {

        final QName suppliedQname = DOMTypeSupport.getXSIType(element);
        if (AttributeFilterSAMLNamespaceHandler.NAMESPACE.equals(suppliedQname.getNamespaceURI())
                || AttributeFilterBasicNamespaceHandler.NAMESPACE.equals(suppliedQname.getNamespaceURI())) {
            DeprecationSupport.warnOnce(ObjectType.XSITYPE, suppliedQname.toString(),
                    parserContext.getReaderContext().getResource().getDescription(), getAFPName().toString());
        }

        super.doParse(element, parserContext, builder);
    }

    /**
     * Helper function to assist rewrite from old to new prefix.
     * 
     * @return the "new" type.
     */
    protected abstract QName getAFPName();

}
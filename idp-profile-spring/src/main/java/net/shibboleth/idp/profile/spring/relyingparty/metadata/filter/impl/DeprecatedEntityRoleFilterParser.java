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

package net.shibboleth.idp.profile.spring.relyingparty.metadata.filter.impl;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataProviderParser;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;


/**
 * Parser for a &lt;EntityRoleWhiteListt&gt; filter.
 * 
 * @deprecated
 */
@Deprecated(forRemoval=true, since="4.1.0")
public class DeprecatedEntityRoleFilterParser extends EntityRoleFilterParser {

    /** Deprecated Element name. */
    @Nonnull public static final QName TYPE_NAME = new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE,
            "EntityRoleWhiteList");

    /** {@inheritDoc} */
    @Override protected void doParse(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {
        
        DeprecationSupport.warn(ObjectType.XSITYPE, TYPE_NAME.getLocalPart(),
                parserContext.getReaderContext().getResource().getDescription(),
                EntityRoleFilterParser.TYPE_NAME.getLocalPart());
        super.doParse(element, parserContext, builder);
    }

}
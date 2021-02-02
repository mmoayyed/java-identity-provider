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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import org.opensaml.saml.metadata.resolver.filter.impl.EntityRoleFilter;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.ext.spring.util.AbstractCustomBeanDefinitionParser;
import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataProviderParser;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

/**
 * Parser for a &lt;EntityRolet&gt; filter.
 */
public class EntityRoleFilterParser extends AbstractCustomBeanDefinitionParser {

    /** Element name. */
    @Nonnull public static final QName TYPE_NAME = new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE,
            "EntityRole");

    /** {@inheritDoc} */
    @Override protected Class<?> getBeanClass(final Element element) {
        return EntityRoleFilter.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {
        
        final List<QName> retainedRoles = new ArrayList<>();
        final List<Element> retainedRoleElems =
                ElementSupport.getChildElementsByTagNameNS(element, AbstractMetadataProviderParser.METADATA_NAMESPACE,
                        "RetainedRole");
        if (retainedRoleElems != null) {
            for (final Element retainedRoleElem : retainedRoleElems) {
                retainedRoles.add(ElementSupport.getElementContentAsQName(retainedRoleElem));
            }
        }
        builder.addConstructorArgValue(retainedRoles);

        if (element.hasAttributeNS(null, "removeRolelessEntityDescriptors")) {
            builder.addPropertyValue("removeRolelessEntityDescriptors",
                    SpringSupport.getStringValueAsBoolean(
                            element.getAttributeNS(null, "removeRolelessEntityDescriptors")));
        }

        if (element.hasAttributeNS(null, "removeEmptyEntitiesDescriptors")) {
            builder.addPropertyValue("removeEmptyEntitiesDescriptors",
                    SpringSupport.getStringValueAsBoolean(
                            element.getAttributeNS(null, "removeEmptyEntitiesDescriptors")));
        }
    }

    /** {@inheritDoc} */
    @Override protected boolean shouldGenerateId() {
        return true;
    }
}

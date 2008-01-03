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

package edu.internet2.middleware.shibboleth.common.config.metadata;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.opensaml.saml2.metadata.provider.EntityRoleFilter;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for {@link EntityRoleFilter}.
 */
public class EntityRoleFilterBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(MetadataNamespaceHandler.NAMESPACE, "EntityRoleWhiteList");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return EntityRoleFilter.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        ArrayList<QName> retainedRoles = new ArrayList<QName>();
        List<Element> retainedRoleElems = XMLHelper.getChildElementsByTagNameNS(element,
                MetadataNamespaceHandler.NAMESPACE, "RetainedRole");
        if (retainedRoleElems != null) {
            for (Element retainedRoleElem : retainedRoleElems) {
                retainedRoles.add(XMLHelper.getElementContentAsQName(retainedRoleElem));
            }
        }
        builder.addConstructorArg(retainedRoles);

        if (element.hasAttributeNS(null, "removeRolelessEntityDescriptors")) {
            builder.addPropertyValue("removeRolelessEntityDescriptors", XMLHelper.getAttributeValueAsBoolean(element
                    .getAttributeNodeNS(null, "removeRolelessEntityDescriptors")));
        }

        if (element.hasAttributeNS(null, "removeEmptyEntitiesDescriptors")) {
            builder.addPropertyValue("removeEmptyEntitiesDescriptors", XMLHelper.getAttributeValueAsBoolean(element
                    .getAttributeNodeNS(null, "removeEmptyEntitiesDescriptors")));
        }
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}
/*
 * Copyright 2007 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.common.config.resource;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.resource.SVNResource;

/** Bean definition parser for {@link SVNResource}s. */
public class SVNResourceBeanDefinitionParser extends AbstractResourceBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(ResourceNamespaceHandler.NAMESPACE, "SVNResource");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return SVNResource.class;
    }

    /** {@inheritDoc} */
    protected String resolveId(Element configElement, AbstractBeanDefinition beanDefinition, ParserContext parserContext) {
        return SVNResource.class.getName() + ":"
                + DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null, "remoteResourceURI"));
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);
        builder.addConstructorArgValue(DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null,
                "remoteResourceURI")));
        builder.addConstructorArgValue(DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null,
                "workingCopyPath")));
        builder.addConstructorArgValue(Long.parseLong(element.getAttributeNS(null, "revision")));

        if (element.hasAttributeNS(null, "username")) {
            builder.addConstructorArgValue(DatatypeHelper
                    .safeTrimOrNullString(element.getAttributeNS(null, "username")));
        }

        if (element.hasAttributeNS(null, "password")) {
            builder.addConstructorArgValue(DatatypeHelper
                    .safeTrimOrNullString(element.getAttributeNS(null, "password")));
        }
    }
}
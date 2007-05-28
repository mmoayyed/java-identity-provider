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

package edu.internet2.middleware.shibboleth.common.config.resource;

import javax.xml.namespace.QName;

import org.opensaml.util.resource.FilesystemResource;
import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

/**
 * Bean definition parser for {@link FilesystemResource}s.
 */
public class FilesystemResourceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(ResourceNamespaceHandler.NAMESPACE, "FilesystemResource");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return FilesystemResource.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element configElement, BeanDefinitionBuilder builder) {
        builder.addConstructorArg(DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null, "file")));
    }
    
    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}
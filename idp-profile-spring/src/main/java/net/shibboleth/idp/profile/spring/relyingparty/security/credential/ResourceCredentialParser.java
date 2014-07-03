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

package net.shibboleth.idp.profile.spring.relyingparty.security.credential;

import javax.xml.namespace.QName;

import net.shibboleth.idp.profile.spring.relyingparty.security.SecurityNamespaceHandler;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

/**
 * 'Parser' for unsuported Credential types. Allows for a better failure semantics.
 */
public class ResourceCredentialParser extends AbstractSingleBeanDefinitionParser {

    /** Type for Basic credentials. */
    public static final QName BASIC_RESOURCE_TYPE = new QName(SecurityNamespaceHandler.NAMESPACE,
            "BasicResourceBacked");

    /** Type for X509 credentials. */
    public static final QName X509_RESOURCE_TYPE = new QName(SecurityNamespaceHandler.NAMESPACE,
            "X509ResourceBacked");

    /** {@inheritDoc} */
    @Override protected Class<?> getBeanClass(Element element) {
        throw new BeanCreationException("Resource backed credentials are not supported");
    }
}

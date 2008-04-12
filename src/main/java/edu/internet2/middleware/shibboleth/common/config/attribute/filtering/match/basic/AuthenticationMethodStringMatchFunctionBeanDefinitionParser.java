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

package edu.internet2.middleware.shibboleth.common.config.attribute.filtering.match.basic;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.basic.AuthenticationMethodStringMatchFunctor;

/**
 * Bean definition parser for {@link AuthenticationMethodStringMatchFunctor}s.
 */
public class AuthenticationMethodStringMatchFunctionBeanDefinitionParser extends
        AbstractStringMatchFunctorBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(BasicMatchFunctorNamespaceHandler.NAMESPACE,
            "AuthenticationMethodString");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return AuthenticationMethodStringMatchFunctor.class;
    }
}
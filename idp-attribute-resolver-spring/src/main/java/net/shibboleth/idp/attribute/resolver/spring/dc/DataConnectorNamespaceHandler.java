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

package net.shibboleth.idp.attribute.resolver.spring.dc;

import net.shibboleth.idp.attribute.resolver.spring.dc.ldap.LdapDataConnectorBeanDefinitionParser;
import net.shibboleth.idp.spring.BaseSpringNamespaceHandler;

/** Namespace handler for the Shibboleth static data connector namespace. */
public class DataConnectorNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:resolver:dc";

    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(StaticDataConnectorBeanDefinitionParser.TYPE_NAME,
                new StaticDataConnectorBeanDefinitionParser());
        // TODO
        //registerBeanDefinitionParser(ComputedIDDataConnectorBeanDefinitionParser.TYPE_NAME,
          //      new ComputedIDDataConnectorBeanDefinitionParser());
        //registerBeanDefinitionParser(StoredIDDataConnectorBeanDefinitionParser.TYPE_NAME,
          //      new StoredIDDataConnectorBeanDefinitionParser());
        //registerBeanDefinitionParser(RDBMSDataConnectorBeanDefinitionParser.TYPE_NAME,
          //      new RDBMSDataConnectorBeanDefinitionParser());
        registerBeanDefinitionParser(LdapDataConnectorBeanDefinitionParser.TYPE_NAME,
                new LdapDataConnectorBeanDefinitionParser());
    }
}

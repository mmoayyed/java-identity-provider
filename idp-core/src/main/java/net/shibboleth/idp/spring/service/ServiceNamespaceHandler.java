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

package net.shibboleth.idp.spring.service;

import net.shibboleth.idp.spring.BaseSpringNamespaceHandler;

// TODO incomplete port from v2
/** Namespace handler for a {@link Service}. */
public class ServiceNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:services";

    /** {@inheritDoc} */
    public void init() {
        ServicesBeanDefinitionParser parser = new ServicesBeanDefinitionParser();
        registerBeanDefinitionParser(ServicesBeanDefinitionParser.ELEMENT_NAME, parser);
        registerBeanDefinitionParser(ServicesBeanDefinitionParser.SCHEMA_TYPE, parser);

        // TODO remove
        // registerBeanDefinitionParser(ServletContextAttributeExporterBeanDefinitionParser.TYPE_NAME,
        // new ServletContextAttributeExporterBeanDefinitionParser());
    }
}
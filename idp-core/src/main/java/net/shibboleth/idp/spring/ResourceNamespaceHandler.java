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

package net.shibboleth.idp.spring;

// TODO incomplete port from v2
/** Namespace handler for resources. */
public class ResourceNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace URI. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:resource";

    /** {@inheritDoc} */
    public void init() {

        registerBeanDefinitionParser(FilesystemResourceBeanDefinitionParser.SCHEMA_TYPE,
                new FilesystemResourceBeanDefinitionParser());

        registerBeanDefinitionParser(ClasspathResourceBeanDefinitionParser.SCHEMA_TYPE,
                new ClasspathResourceBeanDefinitionParser());

        // registerBeanDefinitionParser(FilesystemResourceBeanDefinitionParser.SCHEMA_TYPE,
        // new FilesystemResourceBeanDefinitionParser());

        // registerBeanDefinitionParser(HttpResourceBeanDefinitionParser.SCHEMA_TYPE,
        // new HttpResourceBeanDefinitionParser());

        // registerBeanDefinitionParser(FileBackedHttpResourceBeanDefinitionParser.SCHEMA_TYPE,
        // new FileBackedHttpResourceBeanDefinitionParser());

        // registerBeanDefinitionParser(SVNResourceBeanDefinitionParser.SCHEMA_TYPE,
        // new SVNResourceBeanDefinitionParser());

        // registerBeanDefinitionParser(PropertyReplacementResourceFilterBeanDefinitionParser.SCHEMA_TYPE,
        // new PropertyReplacementResourceFilterBeanDefinitionParser());

        // registerBeanDefinitionParser(ChainingResourceFilterBeanDefinitionParser.SCHEMA_TYPE,
        // new ChainingResourceFilterBeanDefinitionParser());

    }

}
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

package net.shibboleth.idp.profile.spring.relyingparty.metadata;

import javax.xml.namespace.QName;

import net.shibboleth.idp.profile.spring.relyingparty.metadata.filter.ChainingParser;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.filter.RequiredValidUntilParser;
import net.shibboleth.idp.profile.spring.resource.ClasspathResourceParser;
import net.shibboleth.idp.profile.spring.resource.SVNResourceParser;
import net.shibboleth.idp.spring.BaseSpringNamespaceHandler;

// TODO incomplete
/** Namespace handler for <code>urn:mace:shibboleth:2.0:metadata</code>. */
public class MetadataNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:metadata";

    /** Metadata provider element name. */
    public static final QName METADATA_ELEMENT_NAME = new QName(NAMESPACE, "MetadataProvider");

    /** Metadata filter Element name. */
    public static final QName METADATA_FILTER_ELEMENT_NAME = new QName(NAMESPACE, "MetadataFilter");

    /** {@inheritDoc} */
    @Override public void init() {
        // Profile Configuration
        registerBeanDefinitionParser(ChainingMetadataProviderParser.ELEMENT_NAME, new ChainingMetadataProviderParser());
        registerBeanDefinitionParser(InlineMetadataProviderParser.ELEMENT_NAME, new InlineMetadataProviderParser());
        registerBeanDefinitionParser(FilesystemMetadataProviderParser.ELEMENT_NAME,
                new FilesystemMetadataProviderParser());
        registerBeanDefinitionParser(HTTPMetadataProviderParser.ELEMENT_NAME, new HTTPMetadataProviderParser());
        registerBeanDefinitionParser(FileBackedHTTPMetadataProviderParser.ELEMENT_NAME,
                new FileBackedHTTPMetadataProviderParser());
        registerBeanDefinitionParser(ResourceBackedMetadataProviderParser.ELEMENT_NAME,
                new ResourceBackedMetadataProviderParser());
        
        // Resources
        registerBeanDefinitionParser(ClasspathResourceParser.ELEMENT_NAME, new ClasspathResourceParser());
        registerBeanDefinitionParser(SVNResourceParser.ELEMENT_NAME, new SVNResourceParser());

        // Filters
        registerBeanDefinitionParser(RequiredValidUntilParser.ELEMENT_NAME, new RequiredValidUntilParser());
        registerBeanDefinitionParser(ChainingParser.ELEMENT_NAME, new ChainingParser());
    }
}
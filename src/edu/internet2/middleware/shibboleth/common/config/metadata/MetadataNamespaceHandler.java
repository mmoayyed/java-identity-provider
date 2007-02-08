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

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 * Spring namespace handler for the Shibboleth metadata namespace.
 */
public class MetadataNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:metadata";
    
    /** Local tag name for MetadataProvider tags. */
    public static final String METADATA_PROVIDER_ELEMENT_LOCAL_NAME = "MetadataProvider";
    
    /** Local tag name for MetadataFilter tags. */
    public static final String METADATA_FILTER_ELEMENT_LOCAL_NAME = "MetadataFilter";
    
    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(ChainingMetadataProviderBeanDefinitionParser.TYPE_NAME,
                new ChainingMetadataProviderBeanDefinitionParser());
        
        registerBeanDefinitionParser(InlineMetadataProviderBeanDefinitionParser.TYPE_NAME,
                new InlineMetadataProviderBeanDefinitionParser());
        
        registerBeanDefinitionParser(FileBackedURLMetadataProviderBeanDefinitionParser.TYPE_NAME,
                new FileBackedURLMetadataProviderBeanDefinitionParser());
        
        registerBeanDefinitionParser(URLMetadataProviderBeanDefinitionParser.TYPE_NAME,
                new URLMetadataProviderBeanDefinitionParser());
        
        registerBeanDefinitionParser(FilesystemMetadataProviderBeanDefinitionParser.TYPE_NAME,
                new FilesystemMetadataProviderBeanDefinitionParser());
    }
}
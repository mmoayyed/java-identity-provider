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

package net.shibboleth.idp.saml.metadata.impl;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.MetadataNodeProcessor;
import org.opensaml.saml.metadata.resolver.filter.impl.NodeProcessingMetadataFilter;
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver;
import org.testng.annotations.BeforeClass;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

public abstract class BaseNodeProcessorTest extends XMLObjectBaseTestCase {
    
    protected MetadataResolver resolver;
    
    @BeforeClass
    public void getMetadataResolver() throws URISyntaxException, ComponentInitializationException, ResolverException {
        final URL mdURL = BaseNodeProcessorTest.class
                .getResource("/net/shibboleth/idp/saml/impl/metadata/NodeProcessor-metadata.xml");
        final File mdFile = new File(mdURL.toURI());

        final List<MetadataNodeProcessor> processors = List.of(getProcessor());
        
        final NodeProcessingMetadataFilter metadataFilter = new NodeProcessingMetadataFilter();
        metadataFilter.setNodeProcessors(processors);
        metadataFilter.initialize();
        
        final FilesystemMetadataResolver fileResolver = new FilesystemMetadataResolver(mdFile);
        fileResolver.setParserPool(parserPool);
        fileResolver.setMetadataFilter(metadataFilter);
        fileResolver.setId("test");
        fileResolver.initialize();
        resolver = fileResolver;
    }

    protected abstract MetadataNodeProcessor getProcessor(); 
}

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

package net.shibboleth.idp.ui.saml;

import java.io.IOException;
import java.util.Timer;

import net.shibboleth.ext.spring.resource.ResourceHelper;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.metadata.resolver.impl.ResourceBackedMetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.annotations.BeforeClass;

public abstract class AbstractUIComponentTest extends XMLObjectBaseTestCase {
    
    private ResourceBackedMetadataResolver resolver;

    @BeforeClass public void setup() throws IOException, ComponentInitializationException {
        Resource metadata = new ClassPathResource("/net/shibboleth/idp/ui/example-metadata.xml");
        
        resolver = new ResourceBackedMetadataResolver(new Timer(), ResourceHelper.of(metadata));
        resolver.setParserPool(parserPool);
        resolver.setMaxRefreshDelay(500000);
        resolver.initialize();
    }
    
    protected EntityDescriptor get(String what) throws ResolverException {
        final EntityIdCriterion criterion = new EntityIdCriterion(what);
        return resolver.resolveSingle(new CriteriaSet(criterion));
    }
    
}

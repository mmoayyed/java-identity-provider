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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.MetadataNodeProcessor;
import org.opensaml.saml.metadata.resolver.filter.impl.NodeProcessingMetadataFilter;
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.saml.metadata.ScopesContainer;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

public final class ScopesNodeProcessorTest extends XMLObjectBaseTestCase {
    
    private MetadataResolver resolver;
    
    @BeforeClass
    public void getMetadataResolver() throws URISyntaxException, ComponentInitializationException, ResolverException {
        final URL mdURL = ScopesNodeProcessorTest.class
                .getResource("/net/shibboleth/idp/saml/impl/metadata/Scopes-NodeProcessor-metadata.xml");
        final File mdFile = new File(mdURL.toURI());

        final List<MetadataNodeProcessor> processors = List.of(new ScopesNodeProcessor());
        
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
    
    @Test
    public void noScopes() throws ResolverException {
        
        final EntityDescriptor noScopes  = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion("https://noscopes.example.org")));
        assertTrue(noScopes.getObjectMetadata().get(ScopesContainer.class).isEmpty());
        final AttributeAuthorityDescriptor aaNoScope = noScopes.getAttributeAuthorityDescriptor("urn:oasis:names:tc:SAML:2.0:protocol");
        assertTrue(aaNoScope.getObjectMetadata().get(ScopesContainer.class).isEmpty());
        final IDPSSODescriptor idpSSONoScope = noScopes.getIDPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol");
        assertTrue(idpSSONoScope.getObjectMetadata().get(ScopesContainer.class).isEmpty());
    }
    
    @Test 
    public void scopes() throws ResolverException {
        final EntityDescriptor entity  = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion("https://scopes.example.org")));

        final List<ScopesContainer> entityList = entity.getObjectMetadata().get(ScopesContainer.class);
        assertEquals(entityList.size(),1);
        final ScopesContainer entityContainer = entityList.get(0);
        /* 
            <shibmd:Scope>entityScope</shibmd:Scope>
            <shibmd:Scope>entityScope2</shibmd:Scope>
         */
        assertFalse(entityContainer.matchesScope("flibby"));
        assertFalse(entityContainer.matchesScope("entityScope1"));
        assertFalse(entityContainer.matchesScope("2entityScope2"));
        assertTrue(entityContainer.matchesScope("entityScope"));
        assertTrue(entityContainer.matchesScope("entityScope2"));
        
        final IDPSSODescriptor idpSSO = entity.getIDPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol");
        final List<ScopesContainer> idpSSOList = idpSSO.getObjectMetadata().get(ScopesContainer.class);
        assertEquals(idpSSOList.size(),1);
        final ScopesContainer idpSSOContainer = idpSSOList.get(0);
        /*
            <shibmd:Scope regexp="true">^.*IDPSSO.*reg.*Scope</shibmd:Scope>
            <shibmd:Scope regexp="false">IDPSSOScope2</shibmd:Scope>
         */
        assertFalse(idpSSOContainer.matchesScope("flibby"));
        assertFalse(idpSSOContainer.matchesScope("FFFIDPSSOPREregSSScoped"));
        assertTrue(idpSSOContainer.matchesScope("FFFIDPSSOPREregSSScope"));
        assertTrue(idpSSOContainer.matchesScope("IDPSSOScope2"));
        
        final AttributeAuthorityDescriptor aa = entity.getAttributeAuthorityDescriptor("urn:oasis:names:tc:SAML:2.0:protocol");
        final List<ScopesContainer> aaList = aa.getObjectMetadata().get(ScopesContainer.class);
        assertEquals(aaList.size(),1);
        final ScopesContainer aaContainer = aaList.get(0);
        /*
            <shibmd:Scope regexp="false">AAScope1</shibmd:Scope>
            <shibmd:Scope regexp="true">^.*AASCOPE2.*</shibmd:Scope>
         */
        assertFalse(aaContainer.matchesScope("flibby"));
        assertTrue(aaContainer.matchesScope("AAScope1"));
        assertTrue(aaContainer.matchesScope("AASCOPE2"));
        assertTrue(aaContainer.matchesScope("flibbyAASCOPE2flibby"));
        
    }

}

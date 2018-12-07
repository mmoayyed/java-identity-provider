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

import java.util.Iterator;

import net.shibboleth.utilities.java.support.repository.RepositorySupport;

import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.FileBackedHTTPMetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.HTTPMetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.ResourceBackedMetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.mock.env.MockPropertySource;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ResourceMetadataParserTest extends AbstractMetadataParserTest {
    
    private static final String PROP_MDURL = "metadataURL";
    
    private static final String REPO_IDP = "java-identity-provider";

    private static final String ENTITY_XML = "idp-profile-spring/src/test/resources/net/shibboleth/idp/profile/spring/relyingparty/metadata/entity.xml";
    
    private static final String ENTITIES_XML = "idp-profile-spring/src/test/resources/net/shibboleth/idp/profile/spring/relyingparty/metadata/entities.xml";
    
    @Test public void fileEntity() throws Exception {

        final FilesystemMetadataResolver resolver = getBean(FilesystemMetadataResolver.class, "resourceFileEntity.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "resourceFileEntity");
   
        final Iterator<EntityDescriptor> entities = resolver.resolve(criteriaFor(IDP_ID)).iterator();
        Assert.assertTrue(resolver.isFailFastInitialization());
        Assert.assertTrue(resolver.isRequireValidMetadata());
        
        Assert.assertEquals(entities.next().getEntityID(), IDP_ID);
        Assert.assertFalse(entities.hasNext());

        Assert.assertEquals(resolver.getRefreshDelayFactor(), 0.75, 0.001);
        Assert.assertSame(resolver.getParserPool(), parserPool);
        
        Assert.assertNull(resolver.resolveSingle(criteriaFor(SP_ID)));
    }

    @Test public void fileEntities() throws Exception {

        final FilesystemMetadataResolver resolver = getBean(FilesystemMetadataResolver.class, "resourceFileEntities.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "resourceFileEntities");
        Assert.assertEquals(resolver.getMaxRefreshDelay(), 1000*60*55);
        Assert.assertEquals(resolver.getMinRefreshDelay(), 1000*60*15);
        Assert.assertEquals(resolver.getRefreshDelayFactor(), 0.5, 0.001);
        Assert.assertNotSame(resolver.getParserPool(), parserPool);
   
        final Iterator<EntityDescriptor> entities = resolver.resolve(criteriaFor(IDP_ID)).iterator();
        Assert.assertTrue(resolver.isFailFastInitialization());
        Assert.assertTrue(resolver.isRequireValidMetadata());
        
        Assert.assertEquals(entities.next().getEntityID(), IDP_ID);
        Assert.assertFalse(entities.hasNext());

        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(SP_ID)));
    }
    
    @Test public void classpathEntity() throws Exception {

        final MetadataResolver resolver = getBean(ResourceBackedMetadataResolver.class, "resourceClasspathEntity.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "resourceClasspathEntity");
   
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        Assert.assertNull(resolver.resolveSingle(criteriaFor(SP_ID)));
    }

    @Test public void classpathEntities() throws Exception {

        final MetadataResolver resolver = getBean(ResourceBackedMetadataResolver.class, "resourceClasspathEntities.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "resourceClasspathEntities");

        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(SP_ID)));
    }

    @Test public void httpEntity() throws Exception {
        final MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPResourceURL(REPO_IDP, ENTITY_XML, false));

        final MetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "resourceHTTPEntity.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "resourceHTTPEntity");
   
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        Assert.assertNull(resolver.resolveSingle(criteriaFor(SP_ID)));
    }

    @Test public void httpEntities() throws Exception {
        final MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPResourceURL(REPO_IDP, ENTITIES_XML, false));

        final MetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "resourceHTTPEntities.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "resourceHTTPEntities");

        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(SP_ID)));
    }
    
    @Test public void fileHttpEntity() throws Exception {
        final MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPResourceURL(REPO_IDP, ENTITY_XML, false));

        final MetadataResolver resolver = getBean(FileBackedHTTPMetadataResolver.class, propSource, "resourceFileBackedHTTPEntity.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "resourceFileBackedHTTPEntity");
   
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        Assert.assertNull(resolver.resolveSingle(criteriaFor(SP_ID)));
    }

    @Test public void fileHttpEntities() throws Exception {
        final MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPResourceURL(REPO_IDP, ENTITIES_XML, false));

        final MetadataResolver resolver = getBean(FileBackedHTTPMetadataResolver.class, propSource, "resourceFileBackedHTTPEntities.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "resourceFileBackedHTTPEntities");

        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(SP_ID)));
    }

    @Test public void ref() throws Exception {

        final MetadataResolver resolver = getBean(ResourceBackedMetadataResolver.class, "resourceRef.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "resourceRefEntity");
   
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        Assert.assertNull(resolver.resolveSingle(criteriaFor(SP_ID)));
    }


    @Test(expectedExceptions={BeanCreationException.class,}, enabled=false) public void svnParams() throws Exception {
        getBean(MetadataResolver.class, "svnParams.xml", "beans.xml");
    }
}

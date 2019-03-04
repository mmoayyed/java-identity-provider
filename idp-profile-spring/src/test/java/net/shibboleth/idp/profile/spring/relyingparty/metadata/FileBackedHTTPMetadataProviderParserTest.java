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

import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.FileBackedHTTPMetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.HTTPMetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.mock.env.MockPropertySource;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.repository.RepositorySupport;

public class FileBackedHTTPMetadataProviderParserTest extends AbstractMetadataParserTest {
    
    private static final String PROP_MDURL = "metadataURL";
    
    private static final String REPO_IDP = "java-identity-provider";

    private static final String ENTITY_XML = "idp-profile-spring/src/test/resources/net/shibboleth/idp/profile/spring/relyingparty/metadata/entity.xml";
    
    private static final String ENTITIES_XML = "idp-profile-spring/src/test/resources/net/shibboleth/idp/profile/spring/relyingparty/metadata/entities.xml";
        
    @Test public void entity() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL,
                RepositorySupport.buildHTTPResourceURL(REPO_IDP, ENTITY_XML, false));

        FileBackedHTTPMetadataResolver resolver = getBean(FileBackedHTTPMetadataResolver.class, propSource, "fileBackedHTTPEntity.xml", "beans.xml");

        Assert.assertEquals(resolver.getId(), "fileBackedHTTPEntity");

        final Iterator<EntityDescriptor> entities = resolver.resolve(criteriaFor(IDP_ID)).iterator();
        Assert.assertTrue(resolver.isFailFastInitialization());
        Assert.assertTrue(resolver.isRequireValidMetadata());

        Assert.assertEquals(entities.next().getEntityID(), IDP_ID);
        Assert.assertFalse(entities.hasNext());

        Assert.assertEquals(resolver.getRefreshDelayFactor(), 0.75, 0.001);
        Assert.assertEquals(resolver.getExpirationWarningThreshold(), 1000*60*60*12);
        Assert.assertSame(resolver.getParserPool(), parserPool);

        Assert.assertEquals(resolver.isInitializeFromBackupFile(), false);
        Assert.assertEquals(resolver.getBackupFileInitNextRefreshDelay(), 10*1000);

        Assert.assertNull(resolver.resolveSingle(criteriaFor(SP_ID)));
    }

    @Test public void entities() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL,
                RepositorySupport.buildHTTPResourceURL(REPO_IDP, ENTITIES_XML, false));

        MetadataResolver resolver = getBean(FileBackedHTTPMetadataResolver.class, propSource, "fileBackedHTTPEntities.xml", "beans.xml");

        Assert.assertEquals(resolver.getId(), "fileBackedHTTPEntities");

        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(SP_ID)));
    }

    @Test(enabled=false) public void httpsNoTrustEngine() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPSResourceURL(REPO_IDP, ENTITY_XML));

        HTTPMetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "https-noTrustEngine.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "HTTPSEntity");
        
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
    }
    
    @Test public void httpsTrustEngineExplicitKey() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPSResourceURL(REPO_IDP, ENTITY_XML));

        HTTPMetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "https-trustEngine-explicitKey.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "HTTPSEntity");
        
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
    }
    
    @Test(expectedExceptions=BeanCreationException.class) public void httpsTrustEngineInvalidKey() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPSResourceURL(REPO_IDP, ENTITY_XML));

        HTTPMetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "https-trustEngine-invalidKey.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "HTTPSEntity");
        
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
    } 
    
    @Test public void httpsTrustEngineValidPKIX() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPSResourceURL(REPO_IDP, ENTITY_XML));

        HTTPMetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "https-trustEngine-validPKIX.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "HTTPSEntity");
        
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
    }
    
    @Test public void httpsTrustEngineValidPKIXExplicitTrustedName() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPSResourceURL(REPO_IDP, ENTITY_XML));

        HTTPMetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "https-trustEngine-validPKIX-explicitTrustedName.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "HTTPSEntity");
        
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
    }
    
    @Test(expectedExceptions=BeanCreationException.class) public void httpsTrustEngineInvalidPKIX() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPSResourceURL(REPO_IDP, ENTITY_XML));

        HTTPMetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "https-trustEngine-invalidPKIX.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "HTTPSEntity");
        
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
    } 
    
    /**Test the proxy parameters.  This will throw an exception because we do not
     * have a proxy to test against.  It is here to allow hand walking of the code during
     * development and as a placeholder against when we get a proxy gost.
     * @throws Exception
     */
    @Test(expectedExceptions={BeanCreationException.class,}, enabled=false) public void proxy() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPResourceURL(REPO_IDP, ENTITY_XML, false));

        getBean(HTTPMetadataResolver.class, propSource, "HTTPProxy.xml", "beans.xml");
    }

    @Test public void httpClient() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPResourceURL(REPO_IDP, ENTITIES_XML, false));

        HTTPMetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "http-entities-client.xml", "beans.xml", "httpClient.xml");
        
        Assert.assertEquals(resolver.getId(), "HTTPEntities");
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(SP_ID)));
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        Assert.assertNotSame(resolver.getParserPool(), parserPool);
        
    }
    
    @Test public void httpCachingNone() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPResourceURL(REPO_IDP, ENTITY_XML, false));

        HTTPMetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "http-entity-httpCaching-none.xml", "beans.xml", "httpClient.xml");
        
        Assert.assertEquals(resolver.getId(), "HTTPEntity");
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        
    }
    
    @Test public void httpCachingMemory() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPResourceURL(REPO_IDP, ENTITY_XML, false));

        HTTPMetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "http-entity-httpCaching-memory.xml", "beans.xml", "httpClient.xml");
        
        Assert.assertEquals(resolver.getId(), "HTTPEntity");
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        
    }
    
    @Test public void httpCachingFile() throws Exception {
        System.out.println(System.getProperty("java.io.tmpdir"));
        
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPResourceURL(REPO_IDP, ENTITY_XML, false));

        HTTPMetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "http-entity-httpCaching-file.xml", "beans.xml", "httpClient.xml");
        
        Assert.assertEquals(resolver.getId(), "HTTPEntity");
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        
    }
    
    @Test public void timeouts() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPSResourceURL(REPO_IDP, ENTITY_XML));

        HTTPMetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "https-timeouts.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "HTTPSEntityWithTimeouts");
        
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
    }
    
    @Test public void clientSecurityParameters() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPResourceURL(REPO_IDP, ENTITY_XML, false));

        HTTPMetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "https-client-security-params.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "HTTPSEntityWithClientSecurityParams");
        
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
    }
    
    @Test public void indexes() throws Exception {
        MockPropertySource propSource = singletonPropertySource(PROP_MDURL, 
                RepositorySupport.buildHTTPSResourceURL(REPO_IDP, ENTITY_XML));

        HTTPMetadataResolver resolver = getBean(HTTPMetadataResolver.class, propSource, "https-indexes.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "HTTPSEntityWithIndexes");
        
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        
        Assert.assertNotNull(resolver.getIndexes());
        Assert.assertEquals(resolver.getIndexes().size(), 3);
    }
    
}

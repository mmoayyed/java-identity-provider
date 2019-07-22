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

import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;

import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.saml.metadata.RelyingPartyMetadataProvider;

public class FilesystemMetadataParserTest extends AbstractMetadataParserTest {
    
    @Test public void entity() throws Exception {

        FilesystemMetadataResolver resolver = getBean(FilesystemMetadataResolver.class, "fileEntity.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "fileEntity");
   
        final Iterator<EntityDescriptor> entities = resolver.resolve(criteriaFor(IDP_ID)).iterator();
        Assert.assertTrue(resolver.isFailFastInitialization());
        Assert.assertTrue(resolver.isRequireValidMetadata());
        
        Assert.assertEquals(entities.next().getEntityID(), IDP_ID);
        Assert.assertFalse(entities.hasNext());

        Assert.assertEquals(resolver.getRefreshDelayFactor(), 0.75, 0.001);
        Assert.assertSame(resolver.getParserPool(), parserPool);
        
        Assert.assertNull(resolver.resolveSingle(criteriaFor(SP_ID)));
    }

    @Test public void entities() throws Exception {

        FilesystemMetadataResolver resolver = getBean(FilesystemMetadataResolver.class, "fileEntities.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "fileEntities");
        Assert.assertEquals(resolver.getMaxRefreshDelay(), Duration.ofMinutes(55));
        Assert.assertEquals(resolver.getMinRefreshDelay(), Duration.ofMinutes(15));
        Assert.assertEquals(resolver.getRefreshDelayFactor(), 0.5, 0.001);
        Assert.assertEquals(resolver.getExpirationWarningThreshold(), Duration.ofHours(12));
        Assert.assertNotSame(resolver.getParserPool(), parserPool);
   
        final Iterator<EntityDescriptor> entities = resolver.resolve(criteriaFor(IDP_ID)).iterator();
        Assert.assertTrue(resolver.isFailFastInitialization());
        Assert.assertTrue(resolver.isRequireValidMetadata());
        
        Assert.assertEquals(entities.next().getEntityID(), IDP_ID);
        Assert.assertFalse(entities.hasNext());

        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(SP_ID)));
    }
    
    @Test public void predicatesDefaults() throws IOException {
        FilesystemMetadataResolver resolver = getBean(FilesystemMetadataResolver.class, "filePredicatesDefaults.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "filePredicatesDefaults");
        
        Assert.assertFalse(resolver.isSatisfyAnyPredicates());
        Assert.assertTrue(resolver.isUseDefaultPredicateRegistry());
        Assert.assertNotNull(resolver.getCriterionPredicateRegistry());
        Assert.assertFalse(resolver.isResolveViaPredicatesOnly());
    }
    
    @Test public void predicatesNoDefaultRegistry() throws IOException {
        FilesystemMetadataResolver resolver = getBean(FilesystemMetadataResolver.class, "filePredicatesNoDefaultRegistry.xml", "beans.xml");
        
        Assert.assertEquals(resolver.getId(), "filePredicatesNoDefaultRegistry");
        
        Assert.assertFalse(resolver.isSatisfyAnyPredicates());
        Assert.assertFalse(resolver.isUseDefaultPredicateRegistry());
        Assert.assertNull(resolver.getCriterionPredicateRegistry());
        Assert.assertFalse(resolver.isResolveViaPredicatesOnly());
    }
    
    @Test public void predicatesOptions() throws IOException {
        ApplicationContext appContext = getApplicationContext("filesystemResolverContext",
                "filePredicatesOptions.xml", "beans.xml");
        
        RelyingPartyMetadataProvider rpProvider = 
                appContext.getBean("filePredicatesOptions", RelyingPartyMetadataProvider.class);
        FilesystemMetadataResolver resolver = 
                FilesystemMetadataResolver.class.cast(rpProvider.getEmbeddedResolver());
        
        Assert.assertEquals(resolver.getId(), "filePredicatesOptions");
        
        Assert.assertTrue(resolver.isSatisfyAnyPredicates());
        Assert.assertTrue(resolver.isUseDefaultPredicateRegistry());
        Assert.assertNotNull(resolver.getCriterionPredicateRegistry());
        Assert.assertSame(resolver.getCriterionPredicateRegistry(), appContext.getBean("metadata.CriterionPredicateRegistry"));
        Assert.assertTrue(resolver.isResolveViaPredicatesOnly());
    }
    
    @Test(expectedExceptions = {BeanCreationException.class}) public void badRVPO() throws IOException {
        ApplicationContext appContext = getApplicationContext("filesystemResolverContext",
                "fileBadRVPO.xml", "beans.xml");
        
        RelyingPartyMetadataProvider rpProvider = 
                appContext.getBean("BadRVPO", RelyingPartyMetadataProvider.class);
        FilesystemMetadataResolver resolver = 
                FilesystemMetadataResolver.class.cast(rpProvider.getEmbeddedResolver());
        
        Assert.assertEquals(resolver.getId(), "BadRVPO");
        
        Assert.assertTrue(resolver.isSatisfyAnyPredicates());
        Assert.assertTrue(resolver.isUseDefaultPredicateRegistry());
        Assert.assertNotNull(resolver.getCriterionPredicateRegistry());
        Assert.assertSame(resolver.getCriterionPredicateRegistry(), appContext.getBean("metadata.CriterionPredicateRegistry"));
        Assert.assertFalse(resolver.isResolveViaPredicatesOnly());
    }

}

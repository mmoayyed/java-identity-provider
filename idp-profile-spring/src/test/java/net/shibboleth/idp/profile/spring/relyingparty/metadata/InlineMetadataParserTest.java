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

import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.saml.metadata.resolver.impl.DOMMetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class InlineMetadataParserTest extends AbstractMetadataParserTest {
    
    @Test public void entity() throws ResolverException {
        DOMMetadataResolver resolver = getBean(DOMMetadataResolver.class, true, "inLineEntity.xml");
        
        Assert.assertEquals(resolver.getId(), "inLineEntity");
   
        final Iterator<EntityDescriptor> entities = resolver.resolve(criteriaFor(IDP_ID)).iterator();
        Assert.assertTrue(resolver.isFailFastInitialization());
        Assert.assertTrue(resolver.isRequireValidMetadata());
        
        Assert.assertEquals(entities.next().getEntityID(), IDP_ID);
        Assert.assertFalse(entities.hasNext());
        
    }
    
    @Test public void maintainExpired() throws ResolverException {
        DOMMetadataResolver resolver = getBean(DOMMetadataResolver.class, false, "inLineMaintainExpired.xml");
        
        Assert.assertEquals(resolver.getId(), "maintainExpired");
   
        final Iterator<EntityDescriptor> entities = resolver.resolve(criteriaFor(IDP_ID)).iterator();
        Assert.assertTrue(resolver.isFailFastInitialization());
        Assert.assertFalse(resolver.isRequireValidMetadata());
        
        Assert.assertEquals(entities.next().getEntityID(), IDP_ID);
        Assert.assertFalse(entities.hasNext());
        
    }
    
    @Test(expectedExceptions={BeanDefinitionStoreException.class,}) public void maintainExpiredBoth() throws ResolverException {
        getBean(DOMMetadataResolver.class, false, "inLineMaintainExpiredBoth.xml");  
    }

       
    @Test public void entities() throws ResolverException {
        DOMMetadataResolver resolver = getBean(DOMMetadataResolver.class, true, "inLineEntities.xml");
        
        Assert.assertEquals(resolver.getId(), "inLineEntities");
        
        Assert.assertFalse(resolver.isFailFastInitialization());
        Assert.assertFalse(resolver.isRequireValidMetadata());
           
        Assert.assertNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(SP_ID)));
        
    }

}

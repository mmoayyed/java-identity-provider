/*
 * Copyright 2009 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.consent.components;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.idp.consent.BaseTest;
import edu.internet2.middleware.shibboleth.idp.consent.StaticTestDataProvider;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentContext;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.mock.BaseAttribute;
import edu.internet2.middleware.shibboleth.idp.consent.mock.IdPContext;
import edu.internet2.middleware.shibboleth.idp.consent.persistence.Storage;

/**
 * Tests UserConsentContextBuilder.
 */

@Test(dataProviderClass = StaticTestDataProvider.class)
public class UserConsentContextBuilderTest extends BaseTest {

    @Autowired
    private UserConsentContextBuilder userConsentContextBuilder; 
    
    @Resource(name="mapStorage")
    private Storage storage;
    
    @Test(dataProvider = "idpContext")
    public void uniqueIdNotInAtrributeSet(IdPContext idpContext) {
        String uniqueIdAttributeId  = null;
        Map<String, BaseAttribute<String>> attributes = idpContext.getReleasedAttributes();
        for (String id: attributes.keySet()) {
            if (id.equals(userConsentContextBuilder.getUniqueIdAttribute())) {          
                uniqueIdAttributeId = id;
        	}
        }
        
        attributes.remove(uniqueIdAttributeId);
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(idpContext);
        assertEquals(idpContext.getPrincipalName(), userConsentContext.getPrincipal().getUniqueId());
    }
    
    
    @Test(dataProvider = "idpContext")
    public void firstAccessOfPrincipal(IdPContext idpContext) {
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(idpContext);

        Principal principal = storage.readPrincipal(userConsentContext.getPrincipal().getUniqueId());
        assertEquals(userConsentContext.getPrincipal(), principal);
    }
    
    @Test(dataProvider = "idpContext")
    public void firstAccessToRelyingParty(IdPContext idpContext) {
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(idpContext);
        
        RelyingParty relyingParty = storage.readRelyingParty(userConsentContext.getRelyingParty().getEntityId());
        assertEquals(userConsentContext.getRelyingParty(), relyingParty);
    }
    
    @Test(dataProvider = "idpContextAndUniqueIdAndAgreedTermsOfUses")
    public void furtherAccessFromPrincipalCheckTermsOfUse(IdPContext idpContext, String uniqueId, DateTime date, Collection<AgreedTermsOfUse> agreedTermsOfUses) {
        Principal principal = storage.createPrincipal(uniqueId, date);
        RelyingParty relyingParty = storage.createRelyingParty(idpContext.getEntityID());
        persistTermsOfUses(principal, agreedTermsOfUses);

        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(idpContext);
        
        assertEquals(principal, userConsentContext.getPrincipal());
        assertEquals(relyingParty, userConsentContext.getRelyingParty());
        
        assertTrue(CollectionUtils.isEqualCollection(agreedTermsOfUses, userConsentContext.getPrincipal().getAgreedTermsOfUses()));
    }
    
    @Test(dataProvider = "idpContextAndUniqueIdAndAttributeReleaseConsents")
    public void furtherAccessFromPrincipalCheckAttributeReleaseConsent(IdPContext idpContext, String uniqueId, DateTime date, Collection<AttributeReleaseConsent> attributeReleaseConsents) {
        Principal principal = storage.createPrincipal(uniqueId, date);
        RelyingParty relyingParty = storage.createRelyingParty(idpContext.getEntityID());
        persistAttributeReleaseConsents(principal, relyingParty, attributeReleaseConsents);
        
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(idpContext);
        
        assertEquals(principal, userConsentContext.getPrincipal());
        assertEquals(relyingParty, userConsentContext.getRelyingParty());

        assertTrue(CollectionUtils.isEqualCollection(attributeReleaseConsents, userConsentContext.getPrincipal().getAttributeReleaseConsents(relyingParty)));
    }  
    
    @Test(dataProvider = "idpContext")
    public void furtherAccessToRelyingParty(IdPContext idpContext) {
        RelyingParty relyingParty = storage.createRelyingParty(idpContext.getEntityID()); 
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(idpContext);
     
        assertEquals(relyingParty.getEntityId(), userConsentContext.getRelyingParty().getEntityId());
    }
    
    private void persistTermsOfUses(Principal principal, Collection<AgreedTermsOfUse> agreedTermsOfUses) {
        for (AgreedTermsOfUse agreedTermsOfUse : agreedTermsOfUses) {
            assertNotNull(storage.createAgreedTermsOfUse(principal, agreedTermsOfUse.getTermsOfUse(), agreedTermsOfUse.getAgreeDate()));
        }
    }
    
    private void persistAttributeReleaseConsents(Principal principal, RelyingParty relyingParty, Collection<AttributeReleaseConsent> attributeReleaseConsents) {
        for (AttributeReleaseConsent attributeReleaseConsent: attributeReleaseConsents) { 
            assertNotNull(storage.createAttributeReleaseConsent(principal, relyingParty, attributeReleaseConsent.getAttribute(), attributeReleaseConsent.getReleaseDate()));
        }
    }
      
}

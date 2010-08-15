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
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.idp.consent.BaseTest;
import edu.internet2.middleware.shibboleth.idp.consent.ProfileContext;
import edu.internet2.middleware.shibboleth.idp.consent.StaticTestDataProvider;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentContext;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.persistence.Storage;

/**
 * Tests UserConsentContextBuilder.
 */

@Test(dataProviderClass = StaticTestDataProvider.class)
public class UserConsentContextBuilderTest extends BaseTest {

    @Resource(name="userConsentContextBuilder")
    private UserConsentContextBuilder userConsentContextBuilder; 
    
    @Resource(name="storage")
    private Storage storage;
    
    @Test(dataProvider = "profileContext")
    public void uniqueIdNotInAtrributeSet(ProfileContext profileContext) {
        String uniqueIdAttributeId  = null;
        Map<String, BaseAttribute> attributes = profileContext.getReleasedAttributes();
        for (String id: attributes.keySet()) {
            if (id.equals(userConsentContextBuilder.getUniqueIdAttribute())) {          
                uniqueIdAttributeId = id;
        	}
        }
        
        attributes.remove(uniqueIdAttributeId);
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(profileContext);
        assertEquals(profileContext.getPrincipalName(), userConsentContext.getPrincipal().getUniqueId());
    }
    
    
    @Test(dataProvider = "profileContext")
    public void firstAccessOfPrincipal(ProfileContext profileContext) {
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(profileContext);

        Principal principal = storage.readPrincipal(userConsentContext.getPrincipal().getUniqueId());
        assertEquals(userConsentContext.getPrincipal(), principal);
    }
    
    @Test(dataProvider = "profileContext")
    public void firstAccessToRelyingParty(ProfileContext profileContext) {
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(profileContext);
        
        RelyingParty relyingParty = storage.readRelyingParty(userConsentContext.getRelyingParty().getEntityId());
        assertEquals(userConsentContext.getRelyingParty(), relyingParty);
    }
    
    @Test(dataProvider = "profileContextAndUniqueIdAndAgreedTermsOfUses")
    public void furtherAccessFromPrincipalCheckTermsOfUse(ProfileContext profileContext, String uniqueId, DateTime date, Collection<AgreedTermsOfUse> agreedTermsOfUses) {
        Principal principal = storage.createPrincipal(uniqueId, date);
        RelyingParty relyingParty = storage.createRelyingParty(profileContext.getEntityID());
        persistTermsOfUses(principal, agreedTermsOfUses);

        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(profileContext);
        
        assertEquals(principal, userConsentContext.getPrincipal());
        assertEquals(relyingParty, userConsentContext.getRelyingParty());
        
        assertTrue(CollectionUtils.isEqualCollection(agreedTermsOfUses, userConsentContext.getPrincipal().getAgreedTermsOfUses()));
    }
    
    @Test(dataProvider = "profileContextAndUniqueIdAndAttributeReleaseConsents")
    public void furtherAccessFromPrincipalCheckAttributeReleaseConsent(ProfileContext profileContext, String uniqueId, DateTime date, Collection<AttributeReleaseConsent> attributeReleaseConsents) {
        Principal principal = storage.createPrincipal(uniqueId, date);
        RelyingParty relyingParty = storage.createRelyingParty(profileContext.getEntityID());
        persistAttributeReleaseConsents(principal, relyingParty, attributeReleaseConsents);
        
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(profileContext);
        
        assertEquals(principal, userConsentContext.getPrincipal());
        assertEquals(relyingParty, userConsentContext.getRelyingParty());

        assertTrue(CollectionUtils.isEqualCollection(attributeReleaseConsents, userConsentContext.getPrincipal().getAttributeReleaseConsents(relyingParty)));
    }  
    
    @Test(dataProvider = "profileContext")
    public void furtherAccessToRelyingParty(ProfileContext profileContext) {
        RelyingParty relyingParty = storage.createRelyingParty(profileContext.getEntityID()); 
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(profileContext);
     
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

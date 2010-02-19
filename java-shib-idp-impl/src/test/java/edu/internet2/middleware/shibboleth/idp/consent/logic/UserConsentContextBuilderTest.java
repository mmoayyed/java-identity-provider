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

package edu.internet2.middleware.shibboleth.idp.consent.logic;

import static org.testng.AssertJUnit.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.List;

import edu.internet2.middleware.shibboleth.idp.consent.StaticTestDataProvider;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.entities.TermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.mock.ProfileContext;
import edu.internet2.middleware.shibboleth.idp.consent.persistence.BaseJDBCTest;
import edu.internet2.middleware.shibboleth.idp.consent.persistence.Storage;

/**
 * Tests UserConsentContextBuilder.
 */

@Test(dependsOnGroups = { "jdbc.initialization" }, dataProviderClass = StaticTestDataProvider.class)
public class UserConsentContextBuilderTest extends BaseJDBCTest {

    private final Logger logger = LoggerFactory.getLogger(UserConsentContextBuilderTest.class);

    @Autowired
    private UserConsentContextBuilder userConsentContextBuilder; 
    @Autowired
    private Storage storage;
    
    @Test(dataProvider = "profileContext")
    public void uniqueIdNotInAtrributeSet(ProfileContext profileContext) {
        logger.info("start");
        Attribute uniqueIdAttribute = new Attribute();
        uniqueIdAttribute.setId(userConsentContextBuilder.getUniqueIdAttribute());
        
        Collection<Attribute> attributes = profileContext.getReleasedAttributes();
        attributes.remove(uniqueIdAttribute);
        
        try {
            userConsentContextBuilder.buildUserConsentContext(profileContext);
            fail("UserConsentException expected");
        } catch (UserConsentException e) {}
        logger.info("stop");
    }
    
    
    @Test(dataProvider = "profileContext")
    public void firstAccessOfPrincipal(ProfileContext profileContext) {
        logger.info("start");
        UserConsentContext userConsentContext = null;
        try {
            userConsentContext = userConsentContextBuilder.buildUserConsentContext(profileContext);
        } catch (UserConsentException e) {
            fail(e.getMessage());
        }
        
        Principal principal = storage.readPrincipal(userConsentContext.getPrincipal());
        assertEquals(userConsentContext.getPrincipal(), principal);
    }
    
    @Test(dataProvider = "profileContext")
    public void firstAccessToRelyingParty(ProfileContext profileContext) {
        logger.info("start");
        UserConsentContext userConsentContext = null;
        try {
            userConsentContext = userConsentContextBuilder.buildUserConsentContext(profileContext);
        } catch (UserConsentException e) {
            fail(e.getMessage());
        }
        
        RelyingParty relyingParty = storage.readRelyingParty(userConsentContext.getRelyingParty());
        assertEquals(userConsentContext.getRelyingParty(), relyingParty);
        logger.info("stop");
    }
    
    @Test(dataProvider = "profileContextAndPrincipalAndAttributeReleaseConsents")
    public void furtherAccessFromPrincipal(ProfileContext profileContext, Principal principal, Collection<AttributeReleaseConsent> attributeReleaseConsents) {
        logger.info("start");
        persistData(principal, profileContext.getRelyingParty(), attributeReleaseConsents);
  
        UserConsentContext userConsentContext = null;
        try {
            userConsentContext = userConsentContextBuilder.buildUserConsentContext(profileContext);
        } catch (UserConsentException e) {
            fail(e.getMessage());
        }
        
        logger.debug("Principal stored {}", principal);
        logger.debug("Principal retrie {}", userConsentContext.getPrincipal());
        
        assertEquals(principal, userConsentContext.getPrincipal());
        
        logger.info("stop");
        //fail("not finished");
    }   
    
    
    @Test(dataProvider = "profileContext")
    public void furtherAccessToRelyingParty(ProfileContext profileContext) {
        logger.info("start");
        long id = storage.createRelyingParty(profileContext.getRelyingParty());      
        UserConsentContext userConsentContext = null;
        try {
            userConsentContext = userConsentContextBuilder.buildUserConsentContext(profileContext);
        } catch (UserConsentException e) {
            fail(e.getMessage());
        }      
        assertEquals(id, userConsentContext.getRelyingParty().getId());
        logger.info("stop");
    }    
    
    private void persistData(Principal principal, RelyingParty relyingParty, Collection<AttributeReleaseConsent> attributeReleaseConsents) {
        assertTrue(0 < storage.createRelyingParty(relyingParty));
        assertTrue(0 < storage.createPrincipal(principal));
        
        for (AgreedTermsOfUse agreedTermsOfUse : principal.getAgreedTermsOfUses()) {
            assertTrue(0 < storage.createAgreedTermsOfUse(principal, agreedTermsOfUse.getTermsOfUse(), agreedTermsOfUse.getAgreeDate()));
        }
        
        for (AttributeReleaseConsent attributeReleaseConsent: attributeReleaseConsents) {
            assertTrue(0 < storage.createAttributeReleaseConsent(principal, relyingParty, attributeReleaseConsent.getAttribute(), attributeReleaseConsent.getReleaseDate()));
        }
     }
    
    
}

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

package edu.internet2.middleware.shibboleth.idp.consent.persistence;

import static org.testng.AssertJUnit.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.idp.consent.components.TermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;

/**
 * Tests JDBC storage using the Spring JDBC framework.
 */

@Test(dependsOnGroups = {"jdbc.initialization"})
public class JDBCStorageTest extends BaseJDBCTest {

    private final Logger logger = LoggerFactory.getLogger(JDBCStorageTest.class);
     
    @Test(dataProvider = "crudPrincipalTest")
    public void crudPrincipal(final String uniqueId, final DateTime firstAccess, final DateTime lastAccess) {
        logger.info("start");
                
        // Find unavailable
        assertFalse(storage.containsPrincipal(uniqueId));
        
        // Create        
        assertNotNull(storage.createPrincipal(uniqueId, firstAccess));
 
        // Find
        assertTrue(storage.containsPrincipal(uniqueId));
        
        // Read
        Principal principal = storage.readPrincipal(uniqueId); 
        assertEquals(uniqueId, principal.getUniqueId());
        assertEquals(firstAccess, principal.getFirstAccess());
        assertEquals(firstAccess, principal.getLastAccess());
        assertFalse(principal.hasGlobalConsent()); 
                
        // Update
        principal.setGlobalConsent(true);
        principal.setLastAccess(lastAccess);
        assertTrue(storage.updatePrincipal(principal));
        
        Principal updatedPrincipal = storage.readPrincipal(uniqueId); 
        assertEquals(principal, updatedPrincipal);
        assertEquals(principal.getLastAccess(), updatedPrincipal.getLastAccess());
        assertTrue(updatedPrincipal.hasGlobalConsent());
        
        try {
            assertTrue(storage.deletePrincipal(principal));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
        
        logger.info("stop");
    }

    @Test(dataProvider = "crudRelyingPartyTest")
    public void crudRelyingParty(final String entityId) {
        logger.info("start");
        
        // Find unavailable
        assertFalse(storage.containsRelyingParty(entityId));
        
        // Create
        assertNotNull(storage.createRelyingParty(entityId));

        // Find
        assertTrue(storage.containsRelyingParty(entityId));

        // Read
        RelyingParty relyingParty = storage.readRelyingParty(entityId);
        assertEquals(entityId, relyingParty.getEntityId());

        // Update
        try {
            assertTrue(storage.updateRelyingParty(relyingParty));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}

        // Delete
        try {
            assertTrue(storage.deleteRelyingParty(relyingParty));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
        
        logger.info("stop");
    }
    
    @Test(dataProvider = "crudAgreedTermsOfUseTest")
    public void crudAgreedTermsOfUse(final String uniqueId, final DateTime accessDate, final TermsOfUse termsOfUse, final DateTime agreeDate) {

        logger.info("start");
       
        // Preparation
        Principal principal = persistPrincipal(uniqueId, accessDate);
        
        // Create
        assertNotNull(storage.createAgreedTermsOfUse(principal, termsOfUse, agreeDate));
        
        // Read
        Collection<AgreedTermsOfUse> agreedTermsOfUses = storage.readAgreedTermsOfUses(principal);
        assertEquals(1, agreedTermsOfUses.size());
       
        AgreedTermsOfUse agreedTermsOfUse = storage.readAgreedTermsOfUse(principal, termsOfUse);     
        assertEquals(termsOfUse, agreedTermsOfUse.getTermsOfUse());
        assertEquals(agreeDate, agreedTermsOfUse.getAgreeDate());
        
        // Update
        DateTime updatedAgreeDate = agreeDate.plusDays(1);
        assertTrue(storage.updateAgreedTermsOfUse(principal, termsOfUse, updatedAgreeDate));
        
        AgreedTermsOfUse updatedAgreedTermsOfUse = storage.readAgreedTermsOfUse(principal, termsOfUse);  
        assertEquals(termsOfUse, updatedAgreedTermsOfUse.getTermsOfUse());
        assertEquals(updatedAgreeDate, updatedAgreedTermsOfUse.getAgreeDate());
          
        // Delete
        try {
            assertEquals(1,storage.deleteAgreedTermsOfUses(principal));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
        try {
            assertTrue(storage.deleteAgreedTermsOfUse(principal, termsOfUse));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}

        logger.info("stop");
    }

    @Test(dataProvider = "crudAttributeReleaseConsentTest")
    public void crudAttributeReleaseConsent(final String uniqueId, final DateTime accessDate, final String entityId, final Attribute attribute, final DateTime releaseDate) {
        logger.info("start");
        
        // Preparation
        Principal principal = persistPrincipal(uniqueId, accessDate);
        RelyingParty relyingParty = persistRelyingParty(entityId);
        
        // Create
        assertNotNull(storage.createAttributeReleaseConsent(principal, relyingParty, attribute, releaseDate));
        
        // Read
        Collection<AttributeReleaseConsent> attributeReleaseConsents;
        try {
            attributeReleaseConsents = storage.readAttributeReleaseConsents(principal);
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
        
        attributeReleaseConsents = storage.readAttributeReleaseConsents(principal, relyingParty);
        assertEquals(1, attributeReleaseConsents.size());
        
        AttributeReleaseConsent attributeReleaseConsent = storage.readAttributeReleaseConsent(principal, relyingParty, attribute);
        assertEquals(attribute, attributeReleaseConsent.getAttribute());
        assertEquals(releaseDate, attributeReleaseConsent.getReleaseDate());

        // Update
        Collection<String> values = new ArrayList<String>();
        values.add("New value");
        Attribute updatedAttribute = new Attribute(attributeReleaseConsent.getAttribute().getId(), values);
        DateTime updatedReleaseDate = releaseDate.plusDays(1);
        
        assertTrue(storage.updateAttributeReleaseConsent(principal, relyingParty, updatedAttribute, updatedReleaseDate));
        
        AttributeReleaseConsent updatedAttributeReleaseConsent = storage.readAttributeReleaseConsent(principal, relyingParty, attribute);
        assertEquals(updatedAttribute, updatedAttributeReleaseConsent.getAttribute());
        assertEquals(updatedReleaseDate, updatedAttributeReleaseConsent.getReleaseDate());
                        
        // Delete
        try {
            assertEquals(1, storage.deleteAttributeReleaseConsents(principal));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
              
        assertEquals(1, storage.deleteAttributeReleaseConsents(principal, relyingParty));
        attributeReleaseConsents = storage.readAttributeReleaseConsents(principal, relyingParty);
        assertTrue(attributeReleaseConsents.isEmpty());
           
        try {
            assertTrue(storage.deleteAttributeReleaseConsent(principal, relyingParty, attribute));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
        
        logger.info("stop");
    }
}

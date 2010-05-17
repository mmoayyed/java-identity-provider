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
        long id = storage.findPrincipal(uniqueId);
        assertEquals(0, id);
        
        // Create        
        Principal principal = storage.createPrincipal(uniqueId, firstAccess);
        assertTrue(principal.getId() > 0);
 
        // Find
        id = storage.findPrincipal(uniqueId);
        assertTrue(id > 0);
        
        // Read
        principal = storage.readPrincipal(id);
        assertEquals(id, principal.getId());  
        assertEquals(uniqueId, principal.getUniqueId());
        assertEquals(firstAccess, principal.getFirstAccess());
        assertEquals(firstAccess, principal.getLastAccess());
        assertFalse(principal.hasGlobalConsent()); 
                
        // Update
        principal.setGlobalConsent(true);
        principal.setLastAccess(lastAccess);
        Principal updatedPrincipal = storage.updatePrincipal(principal);
        
        assertEquals(principal, updatedPrincipal);
        assertEquals(principal.getLastAccess(), updatedPrincipal.getLastAccess());
        assertTrue(updatedPrincipal.hasGlobalConsent());
        
        try {
            assertEquals(1, storage.deletePrincipal(principal));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
        
        logger.info("stop");
    }

    @Test(dataProvider = "crudRelyingPartyTest")
    public void crudRelyingParty(final String entityId) {
        logger.info("start");
        
        // Find unavailable
        long id = storage.findRelyingParty(entityId);
        assertEquals(0, id);
        
        // Create
        RelyingParty relyingParty = storage.createRelyingParty(entityId);
        assertTrue(relyingParty.getId() > 0);

        // Find
        id = storage.findRelyingParty(entityId);
        assertTrue(id > 0);

        // Read
        relyingParty = storage.readRelyingParty(id);
        assertEquals(id, relyingParty.getId());  
        assertEquals(entityId, relyingParty.getEntityId());

        // Update
        try {
            storage.updateRelyingParty(relyingParty);
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}

        // Delete
        try {
            assertEquals(1, storage.deleteRelyingParty(relyingParty));
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
        List<AgreedTermsOfUse> agreedTermsOfUses = storage.readAgreedTermsOfUses(principal);
        assertEquals(1, agreedTermsOfUses.size());
       
        AgreedTermsOfUse agreedTermsOfUse = storage.readAgreedTermsOfUse(principal, termsOfUse);     
        assertEquals(termsOfUse, agreedTermsOfUse.getTermsOfUse());
        assertEquals(agreeDate, agreedTermsOfUse.getAgreeDate());
        
        // Update
        DateTime updatedAgreeDate = agreeDate.plusDays(1);
        agreedTermsOfUse = storage.updateAgreedTermsOfUse(principal, termsOfUse, updatedAgreeDate);
        assertEquals(termsOfUse, agreedTermsOfUse.getTermsOfUse());
        assertEquals(updatedAgreeDate, agreedTermsOfUse.getAgreeDate());
          
        // Delete
        try {
            assertEquals(1, storage.deleteAgreedTermsOfUses(principal));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
        try {
            assertEquals(1, storage.deleteAgreedTermsOfUse(principal, termsOfUse));
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
        List<AttributeReleaseConsent> attributeReleaseConsents;
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
        
        attributeReleaseConsent = storage.updateAttributeReleaseConsent(principal, relyingParty, updatedAttribute, updatedReleaseDate);
        assertEquals(updatedAttribute, attributeReleaseConsent.getAttribute());
        assertEquals(updatedReleaseDate, attributeReleaseConsent.getReleaseDate());
                        
        // Delete
        try {
            assertEquals(1, storage.deleteAttributeReleaseConsents(principal));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
              
        assertEquals(1, storage.deleteAttributeReleaseConsents(principal, relyingParty));
        attributeReleaseConsents = storage.readAttributeReleaseConsents(principal, relyingParty);
        assertTrue(attributeReleaseConsents.isEmpty());
           
        try {
            assertEquals(1, storage.deleteAttributeReleaseConsent(principal, relyingParty, attribute));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
        
        logger.info("stop");
    }
}

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
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.idp.consent.StaticTestDataProvider;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.entities.TermsOfUse;

/**
 * Tests JDBC storage using the Spring JDBC framework.
 */

@Test(dependsOnGroups = {"jdbc.initialization"}, dataProviderClass = StaticTestDataProvider.class)
public class JDBCStorageTest extends BaseJDBCTest {

    private final Logger logger = LoggerFactory.getLogger(JDBCStorageTest.class);

    @Autowired
    private Storage storage;
     
    @Test(dataProvider = "crudPrincipalTest")
    public void crudPrincipal(final Principal principal) {
        logger.info("start");
        
        long id;
        
        // Find unavailable
        id = storage.findPrincipal(principal);
        assertEquals(0, id);
        
        // Create        
        id = storage.createPrincipal(principal);
        assertEquals(principal.getId(), id);

        // Find
        id = storage.findPrincipal(principal);
        assertTrue(id > 0);
        assertEquals(id, principal.getId());

        // Read
        Principal principal2 = new Principal();
        principal2.setId(id);
        principal2 = storage.readPrincipal(principal2);
        assertEquals(principal, principal2);
        assertEquals(principal.getFirstAccess(), principal2.getFirstAccess());
        assertEquals(principal.getLastAccess(), principal2.getLastAccess());
        assertEquals(principal.hasGlobalConsent(), principal2.hasGlobalConsent());
        
        // Update
        principal2.setLastAccess(new Date(principal2.getLastAccess().getTime() + 60 * 1000));
        principal2.setGlobalConsent(!principal2.hasGlobalConsent());
        assertEquals(1, storage.updatePrincipal(principal2));
        
        Principal principal3 = new Principal();
        principal3.setId(id);       
        principal3 = storage.readPrincipal(principal3);
        assertEquals(principal2, principal3);
        assertEquals(principal2.getFirstAccess(), principal3.getFirstAccess());
        assertEquals(principal2.getLastAccess(), principal3.getLastAccess());
        assertEquals(principal2.hasGlobalConsent(), principal3.hasGlobalConsent());

        try {
            assertEquals(1, storage.deletePrincipal(principal3));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
        
        logger.info("stop");
    }

    @Test(dataProvider = "crudRelyingPartyTest")
    public void crudRelyingParty(final RelyingParty relyingParty) {
        logger.info("start");

        long id;
        
        // Find unavailable
        id = storage.findRelyingParty(relyingParty);
        assertEquals(0, id);
        
        // Create
        id = storage.createRelyingParty(relyingParty);
        assertEquals(relyingParty.getId(), id);

        // Find
        id = storage.findRelyingParty(relyingParty);
        assertTrue(id > 0);
        assertEquals(id, relyingParty.getId());

        // Read
        RelyingParty relyingParty2 = new RelyingParty();
        relyingParty2.setId(id);
        relyingParty2 = storage.readRelyingParty(relyingParty2);
        assertEquals(relyingParty, relyingParty2);

        // Update
        try {
            storage.updateRelyingParty(relyingParty2);
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}

        // Delete
        try {
            assertEquals(1, storage.deleteRelyingParty(relyingParty2));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
        
        logger.info("stop");
    }
    
    @Test(dataProvider = "crudAgreedTermsOfUseTest")
    public void crudAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse, final Date agreeDate) {

        logger.info("start");
        
        long id;
        // Preparation
        id = storage.createPrincipal(principal);
        assertEquals(principal.getId(), id);
        
        // Create
        assertEquals(1, storage.createAgreedTermsOfUse(principal, termsOfUse, agreeDate));
        
        // Read
        List<AgreedTermsOfUse> agreedTermsOfUses = storage.readAgreedTermsOfUses(principal);
        assertEquals(1, agreedTermsOfUses.size());
        
        AgreedTermsOfUse agreedTermsOfUse;
        agreedTermsOfUse = storage.readAgreedTermsOfUse(principal, termsOfUse);     
        assertEquals(termsOfUse, agreedTermsOfUse.getTermsOfUse());
        assertEquals(agreeDate, agreedTermsOfUse.getAgreeDate());
        
        // Update
        Date date = new Date(agreedTermsOfUse.getAgreeDate().getTime() + 60 * 1000);
        assertEquals(1, storage.updateAgreedTermsOfUse(principal, termsOfUse, date));
        agreedTermsOfUse = storage.readAgreedTermsOfUse(principal, termsOfUse);
        assertEquals(termsOfUse, agreedTermsOfUse.getTermsOfUse());
        assertEquals(date, agreedTermsOfUse.getAgreeDate());
          
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
    public void crudAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty, final Attribute attribute, final Date releaseDate) {
        logger.info("start");
        
        long id;
        // Preparation
        id = storage.createPrincipal(principal);
        assertEquals(principal.getId(), id);
        id = storage.createRelyingParty(relyingParty);
        assertEquals(relyingParty.getId(), id);
        
        // Create
        assertEquals(1, storage.createAttributeReleaseConsent(principal, relyingParty, attribute, releaseDate));
        
        // Read
        List<AttributeReleaseConsent> attributeReleaseConsents;
        AttributeReleaseConsent attributeReleaseConsent;
        try {
            attributeReleaseConsents = storage.readAttributeReleaseConsents(principal);
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}

        attributeReleaseConsents = storage.readAttributeReleaseConsents(principal, relyingParty);
        assertEquals(1, attributeReleaseConsents.size());
        attributeReleaseConsent = attributeReleaseConsents.get(0);
        
        assertEquals(attribute, attributeReleaseConsent.getAttribute());
        assertEquals(releaseDate, attributeReleaseConsent.getReleaseDate());
        
        try {
            attributeReleaseConsent = storage.readAttributeReleaseConsent(principal, relyingParty, attribute);
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
        
        // Update
        Date date = new Date(attributeReleaseConsent.getReleaseDate().getTime() + 60 * 1000);
        Attribute attributeChanged = new Attribute();
        attributeChanged.setId(attributeReleaseConsent.getAttribute().getId());
        attributeChanged.addValue("New value");  
        assertEquals(1, storage.updateAttributeReleaseConsent(principal, relyingParty, attributeChanged, date));
        attributeReleaseConsents = storage.readAttributeReleaseConsents(principal, relyingParty);
        assertEquals(1, attributeReleaseConsents.size());
        attributeReleaseConsent = attributeReleaseConsents.get(0);
        
        assertEquals(attributeChanged, attributeReleaseConsent.getAttribute());
        assertEquals(date, attributeReleaseConsent.getReleaseDate());
                
        // Delete
        assertEquals(1, storage.deleteAttributeReleaseConsents(principal));
        attributeReleaseConsents = storage.readAttributeReleaseConsents(principal, relyingParty);
        assertTrue(attributeReleaseConsents.isEmpty());
        
        assertEquals(0, storage.deleteAttributeReleaseConsents(principal, relyingParty));
        assertEquals(1, storage.createAttributeReleaseConsent(principal, relyingParty, attribute, releaseDate));
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

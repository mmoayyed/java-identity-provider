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
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.testng.annotations.Parameters;
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

@ContextConfiguration("/edu/internet2/middleware/shibboleth/idp/consent/test-context.xml")
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = true)
@Test(dataProviderClass = StaticTestDataProvider.class)
public class JDBCStorageTest extends AbstractTransactionalTestNGSpringContextTests {

    private final Logger logger = LoggerFactory.getLogger(JDBCStorageTest.class);

    @Autowired
    private Storage storage;

    @Test
    @Rollback(false)
    @Parameters( { "initFile" })
    public void initialization(String initFile) {
        logger.info("initializationTest started");
        super.executeSqlScript(initFile, false);
        logger.info("initializationTest stopped");
    }
     
    @Test(dependsOnMethods = {"initialization"}, dataProvider = "crudPrincipalTest")
    public void crudPrincipal(final Principal principal) {
        logger.info("crudPrincipalTest started");
        // Create        
        assertEquals(1, storage.createPrincipal(principal));

        // Find
        long id = storage.findPrincipalId(principal.getUniqueId());
        assertTrue(id > 0);
        assertEquals(id, principal.getId());

        // Read
        Principal principalFromDB = storage.readPrincipal(id);
        assertEquals(id, principalFromDB.getId());
        assertEquals(principal.getUniqueId(), principalFromDB.getUniqueId());
        assertEquals(principal.getFirstAccess(), principalFromDB.getFirstAccess());
        assertEquals(principal.getLastAccess(), principalFromDB.getLastAccess());
        assertEquals(principal.hasGlobalConsent(), principalFromDB.hasGlobalConsent());

        // Update
        Date date = new Date(principalFromDB.getLastAccess().getTime() + 60 * 1000);
        principalFromDB.setLastAccess(date);
        principalFromDB.setGlobalConsent(!principalFromDB.hasGlobalConsent());
        assertEquals(1, storage.updatePrincipal(principalFromDB));
        Principal principalFromDBupdated = storage.readPrincipal(principalFromDB.getId());
        assertEquals(date, principalFromDBupdated.getLastAccess()) ;
        assertEquals(principalFromDB.hasGlobalConsent(), principalFromDBupdated.hasGlobalConsent());

        try {
            assertEquals(1, storage.deletePrincipal(principalFromDBupdated));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
        
        logger.info("crudPrincipalTest stopped");
    }

    @Test(dependsOnMethods = {"initialization"}, dataProvider = "crudRelyingPartyTest")
    public void crudRelyingParty(final RelyingParty relyingParty) {
        logger.info("crudRelyingPartyTest started");
        
        // Create
        assertEquals(1,storage.createRelyingParty(relyingParty));

        // Find
        long id = storage.findRelyingPartyId(relyingParty.getEntityId());
        assertTrue(id > 0);
        assertEquals(id, relyingParty.getId());

        // Read
        RelyingParty relyingPartyFromDB = storage.readRelyingParty(id);
        assertEquals(id, relyingPartyFromDB.getId());
        assertEquals(relyingParty.getEntityId(), relyingPartyFromDB.getEntityId());

        // Update
        try {
            storage.updateRelyingParty(relyingPartyFromDB);
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}

        // Delete
        try {
            assertEquals(1, storage.deleteRelyingParty(relyingPartyFromDB));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
        
        logger.info("crudRelyingPartyTest stopped");
    }
    
    @Test(dependsOnMethods = {"initialization"}, dataProvider = "crudAgreedTermsOfUseTest")
    public void crudAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse, final Date agreeDate) {

        logger.info("crudAgreedTermsOfUseTest started");
        
        // Preparation
        assertEquals(1, storage.createPrincipal(principal));
        
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
        
        logger.info("crudAgreedTermsOfUseTest stopped");
    }

    @Test(dependsOnMethods = {"initialization"}, dataProvider = "crudAttributeReleaseConsentTest")
    public void crudAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty, final Attribute attribute, final Date releaseDate) {
        logger.info("crudAttributeReleaseConsentTest started");
        
        // Preparation
        assertEquals(1, storage.createPrincipal(principal));
        assertEquals(1, storage.createRelyingParty(relyingParty));
        
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
        
        // TODO
        assertEquals(0, storage.deleteAttributeReleaseConsents(principal, relyingParty));
        assertEquals(1, storage.createAttributeReleaseConsent(principal, relyingParty, attribute, releaseDate));
        assertEquals(1, storage.deleteAttributeReleaseConsents(principal, relyingParty));
        attributeReleaseConsents = storage.readAttributeReleaseConsents(principal, relyingParty);
        assertTrue(attributeReleaseConsents.isEmpty());
        
        try {
            assertEquals(1, storage.deleteAttributeReleaseConsent(principal, relyingParty, attribute));
            fail("UnsupportedOperation is supported");
        } catch (UnsupportedOperationException e) {}
        
        logger.info("crudAttributeReleaseConsentTest stopped");
    }
}

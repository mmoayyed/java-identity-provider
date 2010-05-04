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

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collection;

import org.joda.time.DateTime;
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
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;

/**
 * Tests JDBC storage using the Spring JDBC framework.
 */

@ContextConfiguration("/edu/internet2/middleware/shibboleth/idp/consent/test-context.xml")
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback=true)
@Test(dataProviderClass = StaticTestDataProvider.class)
public class BaseJDBCTest extends AbstractTransactionalTestNGSpringContextTests {

    @Autowired
    protected Storage storage;
	
    private final Logger logger = LoggerFactory.getLogger(BaseJDBCTest.class);

    @Test(groups = {"jdbc.initialization"})
    @Parameters({ "jdbcInitFile" })
    @Rollback(false)
    protected void initialization(final String initFile) {
        logger.info("start");
        super.executeSqlScript(initFile, false);
        logger.info("stop");
    }
    
    protected Principal persistPrincipal(final String uniqueId, final DateTime accessDate) {
    	Principal principal = storage.createPrincipal(uniqueId, accessDate);
    	assertNotNull(principal);
    	return principal;
    }
    
    protected Principal persistPrincipal(final String uniqueId, final DateTime firstAccess, final DateTime lastAccess, final boolean globalConsent) {
    	Principal principal = storage.createPrincipal(uniqueId, firstAccess);
    	assertNotNull(principal);
    	principal.setLastAccess(lastAccess);
    	principal.setGlobalConsent(globalConsent);
    	principal = storage.updatePrincipal(principal);
    	assertNotNull(principal);
    	return principal;
    }
    
    protected RelyingParty persistRelyingParty(final String entityId) {
    	RelyingParty relyingParty = storage.createRelyingParty(entityId);
    	assertNotNull(relyingParty);
    	return relyingParty;
    }
    
    protected void persistTermsOfUses(Principal principal, Collection<AgreedTermsOfUse> agreedTermsOfUses) {
        for (AgreedTermsOfUse agreedTermsOfUse : agreedTermsOfUses) {
            assertNotNull(storage.createAgreedTermsOfUse(principal, agreedTermsOfUse.getTermsOfUse(), agreedTermsOfUse.getAgreeDate()));
        }
    }
    
    protected void persistAttributeReleaseConsents(Principal principal, RelyingParty relyingParty, Collection<AttributeReleaseConsent> attributeReleaseConsents) {
    	for (AttributeReleaseConsent attributeReleaseConsent: attributeReleaseConsents) { 
    		assertNotNull(storage.createAttributeReleaseConsent(principal, relyingParty, attributeReleaseConsent.getAttribute(), attributeReleaseConsent.getReleaseDate()));
    	}
	}
}

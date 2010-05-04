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

import static org.testng.AssertJUnit.*;

import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;


import java.util.Collection;
import java.util.List;

import edu.internet2.middleware.shibboleth.idp.consent.StaticTestDataProvider;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentContext;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentException;
import edu.internet2.middleware.shibboleth.idp.consent.components.TermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.components.UserConsentContextBuilder;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.mock.IdPMock;
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
    
    @Test(dataProvider = "dummyIdP")
    public void uniqueIdNotInAtrributeSet(IdPMock dummyIdP) {
        logger.info("start");
        Attribute uniqueIdAttribute  = null;
        Collection<Attribute> attributes = dummyIdP.getReleasedAttributes();
        for (Attribute attribute: attributes) {
        	if (attribute.getId().equals(userConsentContextBuilder.getUniqueIdAttribute())) {
        		uniqueIdAttribute = attribute;
        	}
        }
        
        attributes.remove(uniqueIdAttribute);
        
        try {
        	userConsentContextBuilder.setIdPMock(dummyIdP);
            userConsentContextBuilder.buildUserConsentContext();
            fail("UserConsentException expected");
        } catch (UserConsentException e) {}
        logger.info("stop");
    }
    
    
    @Test(dataProvider = "dummyIdP")
    public void firstAccessOfPrincipal(IdPMock dummyIdP) {
        logger.info("start");
        UserConsentContext userConsentContext = null;
        try {
        	userConsentContextBuilder.setIdPMock(dummyIdP);
            userConsentContext = userConsentContextBuilder.buildUserConsentContext();
        } catch (UserConsentException e) {
            fail(e.getMessage());
        }
        
        Principal principal = storage.readPrincipal(userConsentContext.getPrincipal().getId());
        assertEquals(userConsentContext.getPrincipal(), principal);
    }
    
    @Test(dataProvider = "dummyIdP")
    public void firstAccessToRelyingParty(IdPMock dummyIdP) {
        logger.info("start");
        UserConsentContext userConsentContext = null;
        try {
        	userConsentContextBuilder.setIdPMock(dummyIdP);
            userConsentContext = userConsentContextBuilder.buildUserConsentContext();
        } catch (UserConsentException e) {
            fail(e.getMessage());
        }
        
        RelyingParty relyingParty = storage.readRelyingParty(userConsentContext.getRelyingParty().getId());
        assertEquals(userConsentContext.getRelyingParty(), relyingParty);
        logger.info("stop");
    }
    
    @Test(dataProvider = "dummyIdPAndUniqueIdAndAttributeReleaseConsents")
    public void furtherAccessFromPrincipal(IdPMock dummyIdP, String uniqueId, DateTime date, Collection<AgreedTermsOfUse> agreedTermsOfUses, Collection<AttributeReleaseConsent> attributeReleaseConsents) {
        logger.info("start");
        Principal principal = persistPrincipal(uniqueId, date);
        RelyingParty relyingParty = persistRelyingParty(dummyIdP.getEntityID());
        persistTermsOfUses(principal, agreedTermsOfUses);
        //persistAttributeReleaseConsents(principal, relyingParty, attributeReleaseConsents);
        
        UserConsentContext userConsentContext = null;
        try {
        	userConsentContextBuilder.setIdPMock(dummyIdP);
            userConsentContext = userConsentContextBuilder.buildUserConsentContext();
        } catch (UserConsentException e) {
            fail(e.getMessage());
        }
        
        assertEquals(principal, userConsentContext.getPrincipal());
        assertEquals(relyingParty, userConsentContext.getRelyingParty());
        
        logger.debug("{}", agreedTermsOfUses);
        logger.debug("{}",userConsentContext.getPrincipal().getAgreedTermsOfUses());
        
        assertTrue(CollectionUtils.isEqualCollection(agreedTermsOfUses, userConsentContext.getPrincipal().getAgreedTermsOfUses()));
        //assertTrue(CollectionUtils.isEqualCollection(attributeReleaseConsents, userConsentContext.getPrincipal().getAttributeReleaseConsents(relyingParty)));

        logger.info("stop");
    }   
    
    @Test(dataProvider = "dummyIdP")
    public void furtherAccessToRelyingParty(IdPMock dummyIdP) {
        logger.info("start");
        RelyingParty relyingParty = storage.createRelyingParty(dummyIdP.getEntityID());      
        UserConsentContext userConsentContext = null;
        try {
        	userConsentContextBuilder.setIdPMock(dummyIdP);
            userConsentContext = userConsentContextBuilder.buildUserConsentContext();
        } catch (UserConsentException e) {
            fail(e.getMessage());
        }      
        assertEquals(relyingParty.getId(), userConsentContext.getRelyingParty().getId());
        logger.info("stop");
    }    
      
}

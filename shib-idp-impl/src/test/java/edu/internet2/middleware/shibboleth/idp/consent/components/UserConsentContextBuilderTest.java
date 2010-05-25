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
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.idp.consent.StaticTestDataProvider;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentContext;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentException;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.mock.BaseAttribute;
import edu.internet2.middleware.shibboleth.idp.consent.mock.IdPContext;
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
    
    @Test(dataProvider = "idpContext")
    public void uniqueIdNotInAtrributeSet(IdPContext idpContext) {
        logger.info("start");
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
        logger.info("stop");
    }
    
    
    @Test(dataProvider = "idpContext")
    public void firstAccessOfPrincipal(IdPContext idpContext) {
        logger.info("start");
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(idpContext);

        Principal principal = storage.readPrincipal(userConsentContext.getPrincipal().getUniqueId());
        assertEquals(userConsentContext.getPrincipal(), principal);
    }
    
    @Test(dataProvider = "idpContext")
    public void firstAccessToRelyingParty(IdPContext idpContext) {
        logger.info("start");
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(idpContext);
        
        RelyingParty relyingParty = storage.readRelyingParty(userConsentContext.getRelyingParty().getEntityId());
        assertEquals(userConsentContext.getRelyingParty(), relyingParty);
        logger.info("stop");
    }
    
    @Test(dataProvider = "idpContextAndUniqueIdAndAgreedTermsOfUses")
    public void furtherAccessFromPrincipalCheckTermsOfUse(IdPContext idpContext, String uniqueId, DateTime date, Collection<AgreedTermsOfUse> agreedTermsOfUses) {
        logger.info("start");
        Principal principal = persistPrincipal(uniqueId, date);
        RelyingParty relyingParty = persistRelyingParty(idpContext.getEntityID());
        persistTermsOfUses(principal, agreedTermsOfUses);

        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(idpContext);
        
        assertEquals(principal, userConsentContext.getPrincipal());
        assertEquals(relyingParty, userConsentContext.getRelyingParty());
        
        assertTrue(CollectionUtils.isEqualCollection(agreedTermsOfUses, userConsentContext.getPrincipal().getAgreedTermsOfUses()));

        logger.info("stop");
    }
    
    @Test(dataProvider = "idpContextAndUniqueIdAndAttributeReleaseConsents")
    public void furtherAccessFromPrincipalCheckAttributeReleaseConsent(IdPContext idpContext, String uniqueId, DateTime date, Collection<AttributeReleaseConsent> attributeReleaseConsents) {
        logger.info("start");
        Principal principal = persistPrincipal(uniqueId, date);
        RelyingParty relyingParty = persistRelyingParty(idpContext.getEntityID());
        persistAttributeReleaseConsents(principal, relyingParty, attributeReleaseConsents);
        
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(idpContext);
        
        assertEquals(principal, userConsentContext.getPrincipal());
        assertEquals(relyingParty, userConsentContext.getRelyingParty());

        assertTrue(CollectionUtils.isEqualCollection(attributeReleaseConsents, userConsentContext.getPrincipal().getAttributeReleaseConsents(relyingParty)));

        logger.info("stop");
    }  
    
    @Test(dataProvider = "idpContext")
    public void furtherAccessToRelyingParty(IdPContext idpContext) {
        logger.info("start");
        RelyingParty relyingParty = persistRelyingParty(idpContext.getEntityID()); 
        UserConsentContext userConsentContext = userConsentContextBuilder.buildUserConsentContext(idpContext);
     
        assertEquals(relyingParty.getEntityId(), userConsentContext.getRelyingParty().getEntityId());
        logger.info("stop");
    }
      
}

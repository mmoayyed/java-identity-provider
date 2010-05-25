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

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.idp.consent.BaseTest;
import edu.internet2.middleware.shibboleth.idp.consent.components.RelyingPartyBlacklist;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;

/**
 * Tests RelyingPartyBlackList.
 */

@Test
public class RelyingPartyBlackListTest extends BaseTest {

    private final Logger logger = LoggerFactory.getLogger(RelyingPartyBlackListTest.class);

    @Autowired
    private RelyingPartyBlacklist relyingPartyBlacklist;
    
    
    @Test()
    public void relyingPartyInBlacklist() {
        RelyingParty relyingParty1 = new RelyingParty("https://sp.example1.org/shibboleth");
        assertTrue(relyingPartyBlacklist.contains(relyingParty1));
        RelyingParty relyingParty2 = new RelyingParty("https://sp.example2.org/shibboleth");
        assertTrue(relyingPartyBlacklist.contains(relyingParty2));
        RelyingParty relyingParty3 = new RelyingParty("https://sp.example3.org/shibboleth");
        assertTrue(relyingPartyBlacklist.contains(relyingParty3));
        RelyingParty relyingParty4 = new RelyingParty("https://xx.example3.org/shibboleth");
        assertTrue(relyingPartyBlacklist.contains(relyingParty4));
    }
    
    @Test()
    public void relyingPartyNotInBlacklist() {
        RelyingParty relyingParty1 = new RelyingParty("https://xx.example1.org/shibboleth");
        assertFalse(relyingPartyBlacklist.contains(relyingParty1));
        RelyingParty relyingParty2 = new RelyingParty("https://sp.example4.org/shibboleth");
        assertFalse(relyingPartyBlacklist.contains(relyingParty2));
    }
    
}

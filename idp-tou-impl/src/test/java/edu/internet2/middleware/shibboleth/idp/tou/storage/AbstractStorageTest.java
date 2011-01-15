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

package edu.internet2.middleware.shibboleth.idp.tou.storage;


import static org.testng.AssertJUnit.*;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.idp.tou.TestData;
import edu.internet2.middleware.shibboleth.idp.tou.ToUAcceptance;


@ContextConfiguration("classpath:/tou-test-context.xml")
@Test(dataProviderClass = TestData.class)
public abstract class AbstractStorageTest extends AbstractTransactionalTestNGSpringContextTests {
    
    protected final Logger logger = LoggerFactory.getLogger("Test");
 
    protected Storage storage;
    
    protected void setStorage(Storage storage) {
        this.storage = storage;
    }
    
    @BeforeTest
    public abstract void initialization();
       
    @Test(dataProvider = "userIdVersionFingerprintDate")
    public void crudToUAcceptance(final String userId, String version, String fingerprint, DateTime date) {
        assertFalse(storage.containsToUAcceptance(userId, version));
        assertNull(storage.readToUAcceptance(userId, version));
        
        ToUAcceptance touAcceptance = new ToUAcceptance(version, fingerprint, date);
        storage.createToUAcceptance(userId, touAcceptance);
        assertTrue(storage.containsToUAcceptance(userId, version));

        touAcceptance = storage.readToUAcceptance(userId, version);
        
        assertEquals(version, touAcceptance.getVersion());
        assertEquals(fingerprint, touAcceptance.getFingerprint());
        assertEquals(date, touAcceptance.getAcceptanceDate());
        
        touAcceptance = new ToUAcceptance(version, fingerprint.substring(1), date.plusMonths(1));
        storage.updateToUAcceptance(userId, touAcceptance);
        
        touAcceptance = storage.readToUAcceptance(userId, version);        
        assertEquals(version, touAcceptance.getVersion());
        assertEquals(fingerprint.substring(1), touAcceptance.getFingerprint());
        assertEquals(date.plusMonths(1), touAcceptance.getAcceptanceDate());       
    }
}

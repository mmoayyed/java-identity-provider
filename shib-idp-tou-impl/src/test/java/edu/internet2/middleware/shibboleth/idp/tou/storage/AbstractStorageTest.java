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


import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import javax.annotation.Resource;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.idp.tou.TestData;
import edu.internet2.middleware.shibboleth.idp.tou.ToU;


@ContextConfiguration("classpath:/tou-test-context.xml")
@Test(dataProviderClass = TestData.class)
public abstract class AbstractStorageTest extends AbstractTransactionalTestNGSpringContextTests {
    
    protected final Logger logger = LoggerFactory.getLogger("Test");
 
    @Resource(name="tou")
    private ToU tou;
    
    protected Storage storage;
    
    protected void setStorage(Storage storage) {
        this.storage = storage;
    }
    
    @BeforeTest
    public abstract void initialization();
       
    @Test(dataProvider = "createAcceptedToU")
    public void createAcceptedToU(final String userId, final ToU otherToU, final DateTime acceptanceDate, final DateTime otherAcceptanceDate) {
        assertFalse(storage.containsAcceptedToU(userId, tou));
        assertFalse(storage.containsAcceptedToU(userId, otherToU));
        
        storage.createAcceptedToU(userId, tou, acceptanceDate);
        storage.createAcceptedToU(userId, otherToU, acceptanceDate);
        assertTrue(storage.containsAcceptedToU(userId, tou));
        assertTrue(storage.containsAcceptedToU(userId, otherToU));

        storage.createAcceptedToU(userId, tou, otherAcceptanceDate);
        assertTrue(storage.containsAcceptedToU(userId, tou));
    }
    
    @Test(dataProvider = "containsAcceptedToU")
    public void containsAcceptedToU(final String userId, final ToU otherToU, final DateTime acceptanceDate) {    
        assertFalse(storage.containsAcceptedToU(userId, otherToU));
        
        storage.createAcceptedToU(userId, otherToU, acceptanceDate);
        assertTrue(storage.containsAcceptedToU(userId, otherToU));
        assertFalse(storage.containsAcceptedToU("unknown-user", otherToU));
        
        assertFalse(storage.containsAcceptedToU(userId, tou));
    }  
}

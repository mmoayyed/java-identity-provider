/*
 * Licensed to the University Corporation for Advanced Internet Development, Inc.
 * under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.consent.storage;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collection;

import net.shibboleth.idp.attribute.consent.AttributeRelease;
import net.shibboleth.idp.attribute.consent.TestData;
import net.shibboleth.idp.attribute.consent.User;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@ContextConfiguration("classpath:/consent-test-context.xml")
@Test(dataProviderClass = TestData.class)
public abstract class AbstractStorageTest extends AbstractTransactionalTestNGSpringContextTests {

    protected final Logger logger = LoggerFactory.getLogger("Test");

    protected Storage storage;

    protected void setStorage(Storage storage) {
        this.storage = storage;
    }

    @BeforeTest
    public abstract void initialization();

    @Test(dataProvider = "userIdGlobalConsent")
    public void crudUser(String userId, boolean globalConsent) {
        assertFalse(storage.containsUser(userId));
        assertNull(storage.readUser(userId));

        User user = new User(userId, globalConsent);
        storage.createUser(user);
        assertTrue(storage.containsUser(userId));

        user = storage.readUser(userId);
        assertEquals(userId, user.getId());
        assertEquals(globalConsent, user.hasGlobalConsent());

        user.setGlobalConsent(!globalConsent);
        storage.updateUser(user);
        user = storage.readUser(userId);
        assertEquals(userId, user.getId());
        assertEquals(!globalConsent, user.hasGlobalConsent());
    }

    @Test(dataProvider = "userRelyingPartyIdAttributeIdHashDate")
    public void crudAttributeRelease(final User user, final String relyingPartyId, final String attributeId,
            final String hash, final DateTime date) {
        AttributeRelease attributeRelease = new AttributeRelease(attributeId, hash, date);
        assertFalse(storage.containsAttributeRelease(user.getId(), relyingPartyId, attributeId));
        assertTrue(storage.readAttributeReleases(user.getId(), relyingPartyId).isEmpty());

        storage.createUser(user);
        assertFalse(storage.containsAttributeRelease(user.getId(), relyingPartyId, attributeId));
        assertTrue(storage.readAttributeReleases(user.getId(), relyingPartyId).isEmpty());

        storage.createAttributeRelease(user.getId(), relyingPartyId, attributeRelease);
        assertTrue(storage.containsAttributeRelease(user.getId(), relyingPartyId, attributeId));

        Collection<AttributeRelease> attributeReleases = storage.readAttributeReleases(user.getId(), relyingPartyId);
        assertEquals(1, attributeReleases.size());

        attributeRelease = attributeReleases.iterator().next();
        assertEquals(attributeId, attributeRelease.getAttributeId());
        assertEquals(hash, attributeRelease.getValuesHash());
        assertEquals(date, attributeRelease.getDate());

        attributeRelease = new AttributeRelease(attributeId, hash.substring(1), date.plusMonths(1));
        storage.updateAttributeRelease(user.getId(), relyingPartyId, attributeRelease);

        attributeReleases = storage.readAttributeReleases(user.getId(), relyingPartyId);
        assertEquals(1, attributeReleases.size());

        attributeRelease = attributeReleases.iterator().next();
        assertEquals(attributeId, attributeRelease.getAttributeId());
        assertEquals(hash.substring(1), attributeRelease.getValuesHash());
        assertEquals(date.plusMonths(1), attributeRelease.getDate());
    }
}

/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
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

package net.shibboleth.idp.tou;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import org.joda.time.DateTime;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

/**
 * Tests ToUAcceptance.
 */

@ContextConfiguration("classpath:/tou-test-context.xml")
@Test(dataProviderClass = TestData.class)
public class ToUAcceptanceTest extends AbstractTestNGSpringContextTests {

    @javax.annotation.Resource(name = "tou")
    private ToU tou;

    @Test(dataProvider = "date")
    public void createToUAcceptance(final DateTime date) {
        final ToUAcceptance touAcceptance = ToUAcceptance.createToUAcceptance(tou, date);
        assertEquals(tou.getVersion(), touAcceptance.getVersion());
        final String fingerprint = ToUHelper.getToUFingerprint(tou);
        assertEquals(fingerprint, touAcceptance.getFingerprint());
        assertEquals(date, touAcceptance.getAcceptanceDate());
    }

    public void emptyToUAcceptance() {
        final ToUAcceptance touAcceptance = ToUAcceptance.emptyToUAcceptance();
        assertEquals("", touAcceptance.getVersion());
        assertEquals("", touAcceptance.getFingerprint());
        assertNull(touAcceptance.getAcceptanceDate());
    }

    @Test(dataProvider = "touAcceptance")
    public void contains(final ToUAcceptance otherToUAcceptance) {
        final ToUAcceptance touAcceptance = ToUAcceptance.createToUAcceptance(tou, new DateTime());
        final ToUAcceptance emptyToUAcceptance = ToUAcceptance.emptyToUAcceptance();

        assertTrue(touAcceptance.contains(tou));
        assertFalse(otherToUAcceptance.contains(tou));
        assertFalse(emptyToUAcceptance.contains(tou));
    }

}

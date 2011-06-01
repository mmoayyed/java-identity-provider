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

package net.shibboleth.idp.attribute.consent;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Resource;

import net.shibboleth.idp.attribute.Attribute;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

/**
 * Tests ConsentHelper.
 */

@ContextConfiguration("classpath:/consent-test-context.xml")
@Test(dataProviderClass = TestData.class)
public class ConsentHelperTest extends AbstractTestNGSpringContextTests {

    @Resource(name = "consent.config.attributeSortOrder")
    private List<String> attributeSortOrder;

    @Resource(name = "consent.config.attributeBlacklist")
    private Set<String> attributeBlacklist;

    @Resource(name = "consent.config.relyingPartyWhiteBlackList")
    private Set<String> relyingPartyWhiteBlackList;

    @Resource(name = "consent.config.userIdAttribute")
    private String userIdAttribute;

    @Test(dataProvider = "attributesAttributesWithUserIdAttribute")
    public void findUserId(Collection<Attribute<?>> attributesExludingUserId,
            Collection<Attribute<?>> attributesIncludingUserId) {
        String userId = ConsentHelper.findUserId(userIdAttribute, attributesIncludingUserId);
        assertNotNull("userId-value", userId);

        userId = ConsentHelper.findUserId(userIdAttribute, attributesExludingUserId);
        assertNull(userId);
    }

    public void skipRelyingParty() {

        assertTrue(ConsentHelper.skipRelyingParty(relyingPartyWhiteBlackList, true,
                "https://sp.example1.org/shibboleth"));
        assertTrue(ConsentHelper.skipRelyingParty(relyingPartyWhiteBlackList, true,
                "https://sp.example2.org/shibboleth"));
        assertTrue(ConsentHelper.skipRelyingParty(relyingPartyWhiteBlackList, true,
                "https://sp.example3.org/shibboleth"));
        assertTrue(ConsentHelper.skipRelyingParty(relyingPartyWhiteBlackList, true,
                "https://xx.example3.org/shibboleth"));

        assertFalse(ConsentHelper.skipRelyingParty(relyingPartyWhiteBlackList, true,
                "https://xx.example1.org/shibboleth"));
        assertFalse(ConsentHelper.skipRelyingParty(relyingPartyWhiteBlackList, true,
                "https://sp.example4.org/shibboleth"));

        assertFalse(ConsentHelper.skipRelyingParty(relyingPartyWhiteBlackList, false,
                "https://sp.example1.org/shibboleth"));
        assertFalse(ConsentHelper.skipRelyingParty(relyingPartyWhiteBlackList, false,
                "https://sp.example2.org/shibboleth"));
        assertFalse(ConsentHelper.skipRelyingParty(relyingPartyWhiteBlackList, false,
                "https://sp.example3.org/shibboleth"));
        assertFalse(ConsentHelper.skipRelyingParty(relyingPartyWhiteBlackList, false,
                "https://xx.example3.org/shibboleth"));

        assertTrue(ConsentHelper.skipRelyingParty(relyingPartyWhiteBlackList, false,
                "https://xx.example1.org/shibboleth"));
        assertTrue(ConsentHelper.skipRelyingParty(relyingPartyWhiteBlackList, false,
                "https://sp.example4.org/shibboleth"));

    }

    @Test(dataProvider = "numberedAttributes")
    public void removeBlacklistedAttributes(Collection<Attribute<?>> allAttributes) {
        Collection<Attribute<?>> attributes =
                ConsentHelper.removeBlacklistedAttributes(attributeBlacklist, allAttributes);
        for (Attribute<?> attribute : attributes) {
            if (attributeBlacklist.contains(attribute.getId())) {
                fail("Blacklisted attribute found");
            }
        }
    }

    @Test(dataProvider = "numberedAttributes")
    public void sortAttributes(Collection<Attribute<?>> unsortedAttributes) {

        SortedSet<Attribute<?>> attributes = ConsentHelper.sortAttributes(attributeSortOrder, unsortedAttributes);

        int pos = 0;
        boolean onlyUnlisted = false;
        for (Attribute attribute : attributes) {
            int index = attributeSortOrder.indexOf(attribute.getId());
            if (index >= 0) {
                assertFalse(onlyUnlisted);
                assertTrue(index >= pos++);
            } else {
                onlyUnlisted = true;
            }
        }

    }

}
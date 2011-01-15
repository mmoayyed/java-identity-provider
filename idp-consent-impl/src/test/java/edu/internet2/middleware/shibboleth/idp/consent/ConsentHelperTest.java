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

package edu.internet2.middleware.shibboleth.idp.consent;

import static org.testng.AssertJUnit.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Resource;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.idp.attribute.Attribute;

/**
 * Tests ConsentHelper.
 */

@ContextConfiguration("classpath:/consent-test-context.xml")
@Test(dataProviderClass = TestData.class)
public class ConsentHelperTest extends AbstractTestNGSpringContextTests {
    
    @Resource(name="consent.config.attributeSortOrder")
    private List<String> attributeSortOrder;
    
    @Resource(name="consent.config.attributeBlacklist")
    private Set<String> attributeBlacklist;

    @Resource(name="consent.config.relyingPartyBlacklist")
    private Set<String> relyingPartyBlacklist;
    
    @Resource(name="consent.config.userIdAttribute")
    private String userIdAttribute;
    
    @Test(dataProvider = "attributesAttributesWithUserIdAttribute")
    public void findUserId(Collection<Attribute<?>> attributesExludingUserId, Collection<Attribute<?>> attributesIncludingUserId) {
        String userId = ConsentHelper.findUserId(userIdAttribute, attributesIncludingUserId);
        assertNotNull("userId-value", userId);
        
        userId = ConsentHelper.findUserId(userIdAttribute, attributesExludingUserId);
        assertNull(userId);     
    }
    
    public void isRelyingPartyBlacklisted() {
        assertTrue(ConsentHelper.isRelyingPartyBlacklisted(relyingPartyBlacklist, "https://sp.example1.org/shibboleth"));
        assertTrue(ConsentHelper.isRelyingPartyBlacklisted(relyingPartyBlacklist, "https://sp.example2.org/shibboleth"));
        assertTrue(ConsentHelper.isRelyingPartyBlacklisted(relyingPartyBlacklist, "https://sp.example3.org/shibboleth"));
        assertTrue(ConsentHelper.isRelyingPartyBlacklisted(relyingPartyBlacklist, "https://xx.example3.org/shibboleth"));
        
        assertFalse(ConsentHelper.isRelyingPartyBlacklisted(relyingPartyBlacklist, "https://xx.example1.org/shibboleth"));
        assertFalse(ConsentHelper.isRelyingPartyBlacklisted(relyingPartyBlacklist, "https://sp.example4.org/shibboleth"));
    }
   
    @Test(dataProvider = "numberedAttributes")
    public void removeBlacklistedAttributes(Collection<Attribute<?>> allAttributes) {
        Collection<Attribute<?>> attributes = ConsentHelper.removeBlacklistedAttributes(attributeBlacklist, allAttributes);
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
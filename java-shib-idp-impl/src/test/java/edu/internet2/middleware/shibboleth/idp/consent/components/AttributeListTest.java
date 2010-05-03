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

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.idp.consent.BaseTest;
import edu.internet2.middleware.shibboleth.idp.consent.components.AttributeList;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;

/**
 * Tests AttributeList.
 */

@Test
public class AttributeListTest extends BaseTest {

    private final Logger logger = LoggerFactory.getLogger(AttributeListTest.class);

    @Autowired
    private AttributeList attributeList;
        
    @Test(dataProvider = "attributeList")
    public void removeBlacklisted(Collection<Attribute> attributes) {
        attributes = attributeList.removeBlacklisted(attributes);
                
        for (Attribute attribute : attributes) {
            for (String blacklistedAttributeId : attributeList.getBlacklistedAttributeIds()) {
                if (attribute.getId().equals(blacklistedAttributeId)) {
                    fail("Blacklisted attribute found");
                }
            }
        }
        
    }
    
    @Test(dataProvider = "attributeList")
    public void sortAttributes(Collection<Attribute> attributes) {
        
        logger.debug("Before blacklisted attributes:");
        for (Attribute attribute : attributes) {
            logger.debug("{}", attribute.getId());
        }
        
        attributes = attributeList.removeBlacklisted(attributes);
        
        logger.debug("After blacklisted attributes:");
        for (Attribute attribute : attributes) {
            logger.debug("{}", attribute.getId());
        }
        
        logger.debug("Before sorting:");
        for (Attribute attribute : attributes) {
            logger.debug("{}", attribute.getId());
        }
        
        Collection<Attribute> attributesSorted = attributeList.sortAttributes(attributes);
        
        logger.debug("After sorting:");
        for (Attribute attribute : attributesSorted) {
            logger.debug("{}", attribute.getId());
        }
               
        List<String> orderedAttributeIds = attributeList.getOrderedAttributeIds();
        int pos = 0;
        boolean onlyUnlisted = false;
        for (Attribute attribute : attributesSorted) {
            int index = orderedAttributeIds.indexOf(attribute.getId());
            if (index >= 0) {
                assertFalse(onlyUnlisted);
                assertTrue(index >= pos++);
            } else {
                onlyUnlisted = true;
            }
        }
    }
    
    
}

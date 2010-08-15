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
import static org.testng.AssertJUnit.fail;

import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.idp.consent.BaseTest;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;

/**
 * Tests AttributeList.
 */

@Test
public class AttributeListTest extends BaseTest {

    @Resource(name="attributeListConfiguration")
    private AttributeListConfiguration attributeListConfiguration;
        
    @Test(dataProvider = "attributeList")
    public void removeBlacklisted(Collection<Attribute> rawAttributes) {
        
        Collection<Attribute> attributes = new AttributeList(attributeListConfiguration, rawAttributes);
                
        for (Attribute rawAttribute : rawAttributes) {
            if (attributeListConfiguration.isBlacklisted(rawAttribute)
                    && attributes.contains(rawAttribute)) {
                fail("Blacklisted attribute found"); 
            }
        }
        
    }

    @Test(dataProvider = "attributeList")
    public void sortAttributes(Collection<Attribute> rawAttributes) {     
        Collection<Attribute> attributes = new AttributeList(attributeListConfiguration, rawAttributes);
        
        List<String> sortedAttributeIds = attributeListConfiguration.getSortedAttributeIds();
        int pos = 0;
        boolean onlyUnlisted = false;
        for (Attribute attribute : attributes) {
            int index = sortedAttributeIds.indexOf(attribute.getId());
            if (index >= 0) {
                assertFalse(onlyUnlisted);
                assertTrue(index >= pos++);
            } else {
                onlyUnlisted = true;
            }
        }
       
    }
      
}

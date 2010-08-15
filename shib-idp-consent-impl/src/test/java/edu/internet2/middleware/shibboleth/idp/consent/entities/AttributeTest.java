/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.consent.entities;

import java.util.Arrays;

import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import edu.internet2.middleware.shibboleth.idp.consent.BaseTest;

/**
 *
 */

@Test
public class AttributeTest extends BaseTest {
    
    Attribute attribute = new Attribute("id", "hash");
    Attribute attributeT1 = new Attribute("id", "hash");
    Attribute attributeT2 = new Attribute("other-id", "hash");
    Attribute attributeT3 = new Attribute("id", "other-hash");
    
    public void equals() {
        assertEquals(attribute, attributeT1);
        assertFalse(attribute.equals(attributeT2)); 
        assertEquals(attribute, attributeT3);
    }
    
    public void equalsHash() {        
        assertTrue(attribute.equalsValuesHash(attributeT1));
        assertFalse(attribute.equalsValuesHash(attributeT2));
        assertFalse(attribute.equalsValuesHash(attributeT3));
    }
    
    public void equalsValue() {
        String[] values = new String[] {"value1", "value2"};
        String[] othervalues = new String[] {"other-value1", "other-value2"};

        attribute = new Attribute("id", Arrays.asList(values));
        attributeT1 = new Attribute("id", Arrays.asList(values));
        attributeT2 = new Attribute("other-id", Arrays.asList(values));
        attributeT3 = new Attribute("id", Arrays.asList(othervalues));
    
        assertTrue(attribute.equalsValuesHash(attributeT1));
        assertFalse(attribute.equalsValuesHash(attributeT2));
        assertFalse(attribute.equalsValuesHash(attributeT3));
    }
    
}
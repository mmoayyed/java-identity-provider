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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.util.Collection;

import net.shibboleth.idp.attribute.Attribute;

import org.joda.time.DateTime;
import org.testng.annotations.Test;

/**
 *
 */

@Test(dataProviderClass = TestData.class)
public class AttributeReleaseTest {

    @Test(dataProvider = "attributesDate")
    public void createAttributeReleases(Collection<Attribute<?>> attributes, DateTime date) {
        Collection<AttributeRelease> attributeReleases = AttributeRelease.createAttributeReleases(attributes, date);

        assertEquals(attributes.size(), attributeReleases.size());

        for (AttributeRelease attributeRelease : attributeReleases) {
            Attribute<?> attribute = checkAttribute(attributes, attributeRelease.getAttributeId());
            assertEquals(attribute.getId(), attributeRelease.getAttributeId());
            String valueHash = ConsentHelper.hashAttributeValues(attribute);
            assertEquals(valueHash, attributeRelease.getValuesHash());
            assertEquals(date, attributeRelease.getDate());
        }
    }

    @Test(dataProvider = "attributesDateAttribute")
    public void contains(Collection<Attribute<?>> attributes, DateTime date, Attribute otherAttribute) {
        Collection<AttributeRelease> attributeReleases = AttributeRelease.createAttributeReleases(attributes, date);

        for (AttributeRelease attributeRelease : attributeReleases) {
            Attribute<?> attribute = checkAttribute(attributes, attributeRelease.getAttributeId());
            assertTrue(attributeRelease.contains(attribute));
            assertFalse(attributeRelease.contains(otherAttribute));
        }
    }

    private static Attribute<?> checkAttribute(Collection<Attribute<?>> attributes, String attributeId) {
        assertTrue(!attributes.isEmpty());
        for (Attribute<?> attribute : attributes) {
            if (attribute.getId().equals(attributeId)) {
                attributes.remove(attribute);
                return attribute;
            }
        }
        fail();
        return null;
    }
}
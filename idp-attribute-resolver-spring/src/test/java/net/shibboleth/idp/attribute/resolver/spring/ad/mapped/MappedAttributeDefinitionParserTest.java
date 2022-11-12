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

package net.shibboleth.idp.attribute.resolver.spring.ad.mapped;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.resolver.ad.mapped.impl.MappedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.ad.mapped.impl.MappedAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.testing.BaseAttributeDefinitionParserTest;

/**
 * Test for {@link MappedAttributeDefinitionParser}.
 */
@SuppressWarnings("javadoc")
public class MappedAttributeDefinitionParserTest extends BaseAttributeDefinitionParserTest {

    private MappedAttributeDefinition getDefinition(final String fileName) {
        return getAttributeDefn("mapped/" + fileName, MappedAttributeDefinition.class);
    }

    @Test public void multiDefault() {
        final MappedAttributeDefinition defn = getDefinition("resolver/multiDefault.xml");

        assertTrue(defn.isPassThru());
        assertEquals(defn.getValueMaps().size(), 2);
        assertEquals(defn.getDefaultAttributeValue().getValue(), "foobar");
    }

    @Test public void defaultCase() {
        final MappedAttributeDefinition defn = getDefinition("resolver/mapped.xml");

        assertTrue(defn.isPassThru());
        assertEquals(defn.getValueMaps().size(), 2);
        assertEquals(defn.getDefaultAttributeValue().getValue(), "foobar");
    }
    
    @Test(expectedExceptions = {BeanCreationException.class}) public void emptyPassThru() {
        final MappedAttributeDefinition defn = getDefinition("resolver/empty.xml");

        assertFalse(defn.isPassThru());
        assertEquals(defn.getValueMaps().size(), 2);
        assertEquals(defn.getDefaultAttributeValue().getValue(), "foobar");
    }


    @Test public void noDefault() {
        final MappedAttributeDefinition defn = getDefinition("resolver/mappedNoDefault.xml");

        assertTrue(defn.isDependencyOnly());
        assertTrue(defn.isPreRequested());
        assertFalse(defn.isPassThru());
        assertEquals(defn.getValueMaps().size(), 1);
        assertNull(defn.getDefaultAttributeValue());
    }

    @Test public void noValues() {

        try {
            getDefinition("resolver/mappedNoValueMap.xml");
            fail();
        } catch (final BeanDefinitionStoreException e) {
            // OK
        }
    }
}

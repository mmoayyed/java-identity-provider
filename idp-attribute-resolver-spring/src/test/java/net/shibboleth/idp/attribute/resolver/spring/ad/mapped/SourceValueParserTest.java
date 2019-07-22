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
import org.springframework.context.support.GenericApplicationContext;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.resolver.ad.mapped.impl.MappedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ad.mapped.impl.SourceValue;
import net.shibboleth.idp.attribute.resolver.ad.mapped.impl.ValueMap;
import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.idp.attribute.resolver.spring.ad.mapped.impl.SourceValueParser;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

/**
 * Test for {@link SourceValueParser}.
 */
public class SourceValueParserTest extends BaseAttributeDefinitionParserTest {

    private SourceValue getSourceValue(String fileName) {

        GenericApplicationContext context = new GenericApplicationContext();
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + SourceValueParserTest.class);
        final MappedAttributeDefinition defn = getBean(ATTRIBUTE_FILE_PATH + "mapped/" + fileName,
                MappedAttributeDefinition.class, context);

        final ValueMap vm = defn.getValueMaps().iterator().next();

        return vm.getSourceValues().iterator().next();
    }

    @Test public void simple() {
        SourceValue value = getSourceValue("resolver/sourceValue.xml");

        assertTrue(value.isCaseSensitive());
        assertFalse(value.isPartialMatch());
        try {
            assertNull(value.getValue());
            fail();
        } catch (ConstraintViolationException e) {

        }
    }

    @Test public void values1() {
        SourceValue value = getSourceValue("resolver/sourceValueAttributes1.xml");

        assertFalse(value.isCaseSensitive());
        assertTrue(value.isPartialMatch());
        assertEquals(value.getValue(), "sourceValueAttributes1");
    }

    @Test public void values2() {
        SourceValue value = getSourceValue("resolver/sourceValueAttributes2.xml");

        assertTrue(value.isCaseSensitive());
        assertFalse(value.isPartialMatch());
        try {
            assertEquals(value.getValue(), "sourceValueAttributes2");
            fail();
        } catch (ConstraintViolationException e) {

        }
    }
    
    @Test(expectedExceptions = {BeanCreationException.class}) public void emptyCase() {
        SourceValue value = getSourceValue("resolver/sourceValueEmptyCase.xml");

        assertTrue(value.isCaseSensitive());
        assertTrue(value.isPartialMatch());
        assertEquals(value.getValue(), "sourceValueAttributes1");
    }

    @Test(expectedExceptions = {BeanCreationException.class}) public void emptyPartial() {
        SourceValue value = getSourceValue("resolver/sourceValueEmptyPartial.xml");

        assertFalse(value.isCaseSensitive());
        assertFalse(value.isPartialMatch());
    }

    
    @SuppressWarnings("deprecation")
    @Test public void deprecated() {
        SourceValue value = getSourceValue("resolver/sourceValueDeprecated.xml");

        assertFalse(value.isCaseSensitive());
        assertTrue(value.isIgnoreCase());
        assertTrue(value.isPartialMatch());
        assertEquals(value.getValue(), "sourceValueAttributes1");
    }

    @Test public void both() {
        SourceValue value = getSourceValue("resolver/sourceValueBoth.xml");

        assertTrue(value.isCaseSensitive());
        assertFalse(value.isPartialMatch());
    }


}

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

package net.shibboleth.idp.attribute.resolver.spring.ad;

import static org.testng.Assert.*;

import static org.testng.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.resolver.ad.impl.DateTimeAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.DateTimeAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.testing.BaseAttributeDefinitionParserTest;

/**
 * Test for {@link DateTimeAttributeDefinitionParser}.
 */
@SuppressWarnings("javadoc")
public class DateTimeAttributeDefinitionParserTest extends BaseAttributeDefinitionParserTest {

    @Test public void defaults() {
        DateTimeAttributeDefinition attrDef = getAttributeDefn("resolver/datetime.xml", DateTimeAttributeDefinition.class);

        assertEquals(attrDef.getId(), "datetime");
        assertTrue(attrDef.isEpochInSeconds());
        assertFalse(attrDef.isIgnoreConversionErrors());
        assertNull(attrDef.getDateTimeFormatter());
    }

    @Test public void custom() {
        DateTimeAttributeDefinition attrDef = getAttributeDefn("resolver/datetimeCustom.xml", DateTimeAttributeDefinition.class);

        assertEquals(attrDef.getId(), "datetime");
        assertFalse(attrDef.isEpochInSeconds());
        assertTrue(attrDef.isIgnoreConversionErrors());
        
        final DateTimeFormatter formatter = attrDef.getDateTimeFormatter();
        assertNotNull(formatter);
        assertEquals(formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochSecond(100), ZoneId.of("UTC"))), "1970");
    }

}
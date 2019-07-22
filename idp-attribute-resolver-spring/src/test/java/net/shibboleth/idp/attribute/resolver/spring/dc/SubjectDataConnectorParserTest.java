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

package net.shibboleth.idp.attribute.resolver.spring.dc;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.springframework.beans.factory.BeanCreationException;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.resolver.dc.impl.SubjectDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;

/**
 * test for {@link SubjectDataConnectorParser}
 */
public class SubjectDataConnectorParserTest extends BaseAttributeDefinitionParserTest {
    
    @Test public void simple() {
        final SubjectDataConnector connector = getDataConnector("resolver/subjectAttributes.xml", SubjectDataConnector.class);

        assertFalse(connector.isExportAllAttributes());
        assertEquals(connector.getExportAttributes().size(), 2);
        assertTrue(connector.getExportAttributes().contains("foo"));
        assertTrue(connector.getExportAttributes().contains("bar"));
        assertTrue(connector.isNoResultIsError());
    }
    
    @Test(expectedExceptions = {BeanCreationException.class}) public void emptyNoResultIsError() {
        final SubjectDataConnector connector = getDataConnector("resolver/subjectAttributesNull.xml", SubjectDataConnector.class);

        assertFalse(connector.isExportAllAttributes());
        assertEquals(connector.getExportAttributes().size(), 2);
        assertTrue(connector.getExportAttributes().contains("foo"));
        assertTrue(connector.getExportAttributes().contains("bar"));
        assertFalse(connector.isNoResultIsError());
    }
}
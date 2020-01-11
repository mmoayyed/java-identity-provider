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

import java.util.List;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.dc.impl.StaticDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.StaticDataConnectorParser;

/**
 * test for {@link StaticDataConnectorParser}
 */
@SuppressWarnings("javadoc")
public class StaticDataConnectorParserTest extends BaseAttributeDefinitionParserTest {
    
    @Test public void simple() {
        final StaticDataConnector connector = getDataConnector("resolver/staticAttributes.xml", StaticDataConnector.class);

        assertFalse(connector.isExportAllAttributes());
        assertEquals(connector.getExportAttributes().size(), 2);
        assertTrue(connector.getExportAttributes().contains("foo"));
        assertTrue(connector.getExportAttributes().contains("bar"));

        assertEquals(connector.getAttributes().keySet().size(), 2);
        final IdPAttribute epe = connector.getAttributes().get("eduPersonEntitlement");
        List<IdPAttributeValue> values = epe.getValues();
        assertEquals(values.size(), 2);
        assertTrue(values.contains(new StringAttributeValue("urn:example.org:entitlement:entitlement1")));
        assertTrue(values.contains(new StringAttributeValue("urn:mace:dir:entitlement:common-lib-terms")));
        
        values = connector.getAttributes().get("staticEpA").getValues();
        assertEquals(values.size(), 1);
        assertTrue(values.contains(new StringAttributeValue("member")));
    }

    @Test public void hybrid() {
        final StaticDataConnector connector = getDataConnector("resolver/staticAttributesHybrid.xml", StaticDataConnector.class);
        assertTrue(connector.isExportAllAttributes());
        
        assertEquals(connector.getAttributes().keySet().size(), 2);
        final IdPAttribute epe = connector.getAttributes().get("eduPersonEntitlement");
        List<IdPAttributeValue> values = epe.getValues();
        assertEquals(values.size(), 2);
        assertTrue(values.contains(new StringAttributeValue("urn:example.org:entitlement:entitlement1")));
        assertTrue(values.contains(new StringAttributeValue("urn:mace:dir:entitlement:common-lib-terms")));
        
        values = connector.getAttributes().get("staticEpA").getValues();
        assertEquals(values.size(), 1);
        assertTrue(values.contains(new StringAttributeValue("member")));
    }


    @Test public void nativesimple() {
        final StaticDataConnector connector = getDataConnector("staticAttributesNative.xml", StaticDataConnector.class);
        
        assertFalse(connector.isExportAllAttributes());
        assertTrue(connector.getExportAttributes().isEmpty());
        assertEquals(connector.getAttributes().keySet().size(), 2);
        final IdPAttribute epe = connector.getAttributes().get("eduPersonEntitlement");
        List<IdPAttributeValue> values = epe.getValues();
        assertEquals(values.size(), 2);
        assertTrue(values.contains(new StringAttributeValue("urn:example.org:entitlement:entitlement1")));
        assertTrue(values.contains(new StringAttributeValue("urn:mace:dir:entitlement:common-lib-terms")));
        
        values = connector.getAttributes().get("staticEpA").getValues();
        assertEquals(values.size(), 2);
        assertTrue(values.contains(new StringAttributeValue("member")));
    }
}

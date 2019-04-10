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

package net.shibboleth.idp.attribute.resolver.ad.mapped.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Collections;
import java.util.Set;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolverDataConnectorDependency;
import net.shibboleth.idp.attribute.resolver.ResolverTestSupport;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;

/** Test the mapped attribute type. */
public class MappedAttributeTest {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "mapped";

    @Test public void instantiation() throws ComponentInitializationException, ResolutionException {
        final MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);

        assertFalse(definition.isPassThru());

        try {
            definition.initialize();
            fail("Initialized without dependencies and value mappings");
        } catch (final ComponentInitializationException e) {
            // expected this
        }

        final Set<ResolverDataConnectorDependency> dependencySet = new LazySet<>();
        dependencySet.add(TestSources.makeDataConnectorDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR));
        definition.setDataConnectorDependencies(dependencySet);

        try {
            definition.initialize();
            fail("Initialized without value mappings");
        } catch (final ComponentInitializationException e) {
            // expected this
        }

        definition.setValueMaps(Collections.singleton(substringValueMapping("foo", false, "foo")));

        definition.initialize();

        definition.destroy();
        try {
            definition.initialize();
            fail("init a torn down mapper?");
        } catch (final DestroyedComponentException e) {
            // expected this
        }

        try {
            definition.resolve(new AttributeResolutionContext());
            fail("resolve a torn down mapper?");
        } catch (final DestroyedComponentException e) {
            // expected this
        }

    }

    @Test public void noAttributeValues() throws Exception {
        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1",
                        ResolverTestSupport.buildAttribute(ResolverTestSupport.EPE_ATTRIB_ID,
                                ResolverTestSupport.EPE1_VALUES), ResolverTestSupport.buildAttribute(
                                ResolverTestSupport.EPA_ATTRIB_ID, ResolverTestSupport.EPA1_VALUES)));

        final MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);
        definition.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("connector1",
                "NoSuchAttribute")));
        definition.setValueMaps(Collections.singleton(substringValueMapping("foo", false, "foo")));
        definition.initialize();

        final IdPAttribute result = definition.resolve(resolutionContext);
        assertEquals(result.getId(), TEST_ATTRIBUTE_NAME);
        assertTrue(result.getValues().isEmpty());
    }

    @Test public void noAttributeValuesDefault() throws Exception {
        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1",
                        ResolverTestSupport.buildAttribute(ResolverTestSupport.EPE_ATTRIB_ID,
                                ResolverTestSupport.EPE1_VALUES), ResolverTestSupport.buildAttribute(
                                ResolverTestSupport.EPA_ATTRIB_ID, ResolverTestSupport.EPA1_VALUES)));

        final MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);
        definition.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("connector1",
                "NoSuchAttribute")));
        definition.setValueMaps(Collections.singleton(substringValueMapping("foo", false, "foo")));
        definition.setDefaultValue("");
        assertNull(definition.getDefaultAttributeValue());
        assertNull(definition.getDefaultValue());
        definition.setDefaultValue("default");
        assertEquals(definition.getDefaultValue(), "default");
        definition.initialize();

        final IdPAttribute result = definition.resolve(resolutionContext);
        assertEquals(result.getId(), TEST_ATTRIBUTE_NAME);
        assertFalse(result.getValues().isEmpty());
        assertTrue(result.getValues().contains(new StringAttributeValue("default")));
    }

    @Test public void invalidValueType() throws ComponentInitializationException {
        final IdPAttribute attr = new IdPAttribute(ResolverTestSupport.EPA_ATTRIB_ID);
        attr.setValues(Collections.singletonList(new ByteAttributeValue(new byte[] {1, 2, 3})));

        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1", attr));

        final MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);
        definition.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("connector1",
                ResolverTestSupport.EPA_ATTRIB_ID)));
        definition.setValueMaps(Collections.singleton(substringValueMapping("student", false, "student")));
        definition.initialize();

        try {
            definition.resolve(resolutionContext);
            fail("invalid types");
        } catch (final ResolutionException e) {
            //
        }

    }

    @Test public void emptyAttributeValues() throws Exception {
        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1",
                        ResolverTestSupport.buildAttribute(ResolverTestSupport.EPE_ATTRIB_ID,
                                ResolverTestSupport.EPE1_VALUES), ResolverTestSupport.buildAttribute(
                                ResolverTestSupport.EPA_ATTRIB_ID, (String) null, "")));

        final MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);
        definition.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("connector1",
                ResolverTestSupport.EPA_ATTRIB_ID)));
        assertTrue(definition.getValueMaps().isEmpty());
        definition.setValueMaps(Collections.singleton(substringValueMapping("student", false, "student")));
        assertEquals(definition.getValueMaps().size(), 1);
        definition.initialize();

        final IdPAttribute result = definition.resolve(resolutionContext);
        assertEquals(result.getId(), TEST_ATTRIBUTE_NAME);
        // mapped attribute definition should return no values for empty and null
        assertTrue(result.getValues().isEmpty());
    }

    @Test public void validValueType() throws Exception {
        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1",
                        ResolverTestSupport.buildAttribute(ResolverTestSupport.EPE_ATTRIB_ID,
                                ResolverTestSupport.EPE1_VALUES), ResolverTestSupport.buildAttribute(
                                ResolverTestSupport.EPA_ATTRIB_ID, ResolverTestSupport.EPA3_VALUES)));

        final MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);
        definition.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("connector1",
                ResolverTestSupport.EPA_ATTRIB_ID)));
        assertTrue(definition.getValueMaps().isEmpty());
        definition.setValueMaps(Collections.singleton(substringValueMapping("student", false, "student")));
        assertEquals(definition.getValueMaps().size(), 1);
        definition.initialize();

        final IdPAttribute result = definition.resolve(resolutionContext);
        assertEquals(result.getId(), TEST_ATTRIBUTE_NAME);
        assertFalse(result.getValues().isEmpty());
        assertEquals(result.getValues().size(), 2);
        assertTrue(result.getValues().get(0).equals(new StringAttributeValue("student")));
        assertTrue(result.getValues().get(1).equals(new StringAttributeValue("student")));
    }

    @Test public void defaultCase() throws Exception {
        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1",
                        ResolverTestSupport.buildAttribute(ResolverTestSupport.EPE_ATTRIB_ID,
                                ResolverTestSupport.EPE1_VALUES), ResolverTestSupport.buildAttribute(
                                ResolverTestSupport.EPA_ATTRIB_ID, ResolverTestSupport.EPA3_VALUES)));

        final MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);
        definition.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("connector1",
                ResolverTestSupport.EPA_ATTRIB_ID)));
        assertTrue(definition.getValueMaps().isEmpty());
        definition.setValueMaps(Collections.singleton(substringValueMapping("elephant", false, "banana")));
        definition.setDefaultValue("default");
        assertEquals(definition.getDefaultAttributeValue().getValue(), "default");
        assertFalse(definition.isPassThru());
        definition.initialize();

        final IdPAttribute result = definition.resolve(resolutionContext);
        assertEquals(result.getId(), TEST_ATTRIBUTE_NAME);
        assertFalse(result.getValues().isEmpty());
        assertEquals(result.getValues().size(), 3);
        assertTrue(result.getValues().get(0).equals(new StringAttributeValue("default")));
        assertTrue(result.getValues().get(1).equals(new StringAttributeValue("default")));
        assertTrue(result.getValues().get(2).equals(new StringAttributeValue("default")));
    }

    @Test public void passThrough() throws Exception {
        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1",
                        ResolverTestSupport.buildAttribute(ResolverTestSupport.EPE_ATTRIB_ID,
                                ResolverTestSupport.EPE1_VALUES), ResolverTestSupport.buildAttribute(
                                ResolverTestSupport.EPA_ATTRIB_ID, ResolverTestSupport.EPA3_VALUES)));

        final MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);
        definition.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("connector1",
                ResolverTestSupport.EPA_ATTRIB_ID)));
        assertTrue(definition.getValueMaps().isEmpty());
        definition.setValueMaps(Collections.singleton(substringValueMapping("elephant", false, "banana")));
        definition.setDefaultValue("default");
        assertEquals(definition.getDefaultAttributeValue().getValue(), "default");
        definition.setPassThru(true);
        definition.initialize();

        final IdPAttribute result = definition.resolve(resolutionContext);
        assertEquals(result.getId(), TEST_ATTRIBUTE_NAME);
        assertFalse(result.getValues().isEmpty());
        assertEquals(result.getValues().size(), ResolverTestSupport.EPA3_VALUES.length);
        for (final String val : ResolverTestSupport.EPA3_VALUES) {
            assertTrue(result.getValues().contains(new StringAttributeValue(val)));
        }
    }

    protected ValueMap substringValueMapping(final String targetValue, final boolean caseInsensitive, final String returnValue) throws ComponentInitializationException {
        final ValueMap retVal = new ValueMap();
        retVal.setReturnValue(returnValue);
        retVal.setSourceValues(Collections.singleton(SourceValueTest.newSourceValue(returnValue, caseInsensitive, true)));
        return retVal;
    }
    
    @Test public void IdP1389() throws Exception {
        final SourceValue source = new SourceValue();
        source.setValue(".*(.*)");
        source.setPartialMatch(false);
        source.initialize();

        final ValueMap valueMap = new ValueMap();
        valueMap.setReturnValue("$1");
        valueMap.setSourceValues(Collections.singleton(source));
        
        final MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);
        definition.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("connector1",
                ResolverTestSupport.EPA_ATTRIB_ID)));
        
        definition.setValueMaps(Collections.singleton(valueMap));
        definition.initialize();

        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1",
                        ResolverTestSupport.buildAttribute(ResolverTestSupport.EPE_ATTRIB_ID,
                                ResolverTestSupport.EPE1_VALUES), ResolverTestSupport.buildAttribute(
                                ResolverTestSupport.EPA_ATTRIB_ID, "Val", "val")));
        
        definition.resolve(resolutionContext);
    }
    
}

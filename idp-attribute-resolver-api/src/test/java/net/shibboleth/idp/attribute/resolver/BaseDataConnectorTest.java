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

package net.shibboleth.idp.attribute.resolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.shibboleth.idp.attribute.Attribute;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@link BaseDataConnector}. This test does not test any methods inherited from
 * {@link BaseResolverPlugin}, those are covered in {@link BaseResolverPluginTest}.
 */
public class BaseDataConnectorTest {

    /** Test instantiation and post-instantiation state. */
    @Test
    public void testInstantiation() {
        MockBaseDataConnector connector = new MockBaseDataConnector("foo", Collections.EMPTY_MAP);

        Assert.assertNull(connector.getFailoverDataConnectorId());
    }

    /** Test getting/setting dependency ID. */
    @Test
    public void testFailoverDependencyId() {
        MockBaseDataConnector connector = new MockBaseDataConnector("foo", Collections.EMPTY_MAP);

        connector.setFailoverDataConnectorId(" foo ");
        Assert.assertEquals(connector.getFailoverDataConnectorId(), "foo");

        connector.setFailoverDataConnectorId("");
        Assert.assertEquals(connector.getFailoverDataConnectorId(), null);

        connector.setFailoverDataConnectorId(null);
        Assert.assertEquals(connector.getFailoverDataConnectorId(), null);
    }

    /** Test the resolution of the data connector. */
    @Test
    public void testResolve() throws Exception {
        AttributeResolutionContext context = new AttributeResolutionContext(null);

        MockBaseDataConnector connector = new MockBaseDataConnector("foo", (Map<String, Attribute<?>>)null);
        Assert.assertNull(connector.resolve(context));

        HashMap<String, Attribute<?>> values = new HashMap<String, Attribute<?>>();
        connector = new MockBaseDataConnector("foo", values);
        Assert.assertNotNull(connector.resolve(context));

        Attribute<?> attribute = new Attribute<String>("foo");
        values.put(attribute.getId(), attribute);

        connector = new MockBaseDataConnector("foo", values);
        Map<String, Attribute<?>> result = connector.resolve(context);
        Assert.assertTrue(result.containsKey(attribute.getId()));
        Assert.assertEquals(result.get(attribute.getId()), attribute);
    }
    
    /**
     * This class implements the minimal level of functionality and is meant only as a means of testing the abstract
     * {@link BaseDataConnector}.
     */
    private static final class MockBaseDataConnector extends BaseDataConnector {

        /** Static values returned for {@link #resolve(AttributeResolutionContext)}. */
        private Map<String, Attribute<?>> staticValues;

        /**
         * Constructor.
         * 
         * @param id id of the data connector
         * @param values values returned for {@link #resolve(AttributeResolutionContext)}
         */
        public MockBaseDataConnector(final String id, final Map<String, Attribute<?>> values) {
            super(id);
            staticValues = values;
        }

        /** {@inheritDoc} */
        protected Map<String, Attribute<?>> doDataConnectorResolve(AttributeResolutionContext resolutionContext)
                throws AttributeResolutionException {
            return staticValues;
        }
    }
}
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

import com.google.common.base.Optional;

/**
 * Unit test for {@link BaseDataConnector}. This test does not test any methods inherited from
 * {@link BaseResolverPlugin}, those are covered in {@link BaseResolverPluginTest}.
 */
public class BaseDataConnectorTest {

    /** Test instantiation and post-instantiation state. */
    @Test public void instantiation() {
        MockBaseDataConnector connector = new MockBaseDataConnector("foo", Collections.EMPTY_MAP);

        Assert.assertFalse(connector.getFailoverDataConnectorId().isPresent());
    }

    /** Test getting/setting dependency ID. */
    @Test public void failoverDependencyId() {
        MockBaseDataConnector connector = new MockBaseDataConnector("foo", Collections.EMPTY_MAP);

        connector.setFailoverDataConnectorId(" foo ");
        Assert.assertEquals(connector.getFailoverDataConnectorId().get(), "foo");

        connector.setFailoverDataConnectorId("");
        Assert.assertEquals(connector.getFailoverDataConnectorId(), Optional.absent());

        connector.setFailoverDataConnectorId(null);
        Assert.assertEquals(connector.getFailoverDataConnectorId(), Optional.absent());
    }

    /** Test the resolution of the data connector. */
    @Test public void resolve() throws Exception {
        AttributeResolutionContext context = new AttributeResolutionContext();

        MockBaseDataConnector connector = new MockBaseDataConnector("foo", (Map<String, Attribute>) null);
        connector.initialize();
        Assert.assertEquals(connector.resolve(context), Optional.absent());

        HashMap<String, Attribute> values = new HashMap<String, Attribute>();
        connector = new MockBaseDataConnector("foo", values);
        connector.initialize();
        Assert.assertNotNull(connector.resolve(context));

        Attribute attribute = new Attribute("foo");
        values.put(attribute.getId(), attribute);

        connector = new MockBaseDataConnector("foo", values);
        connector.initialize();
        Optional<Map<String, Attribute>> result = connector.resolve(context);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.get().containsKey(attribute.getId()));
        Assert.assertEquals(result.get().get(attribute.getId()), attribute);
    }

    /**
     * This class implements the minimal level of functionality and is meant only as a means of testing the abstract
     * {@link BaseDataConnector}.
     */
    private static final class MockBaseDataConnector extends BaseDataConnector {

        /** Static values returned for {@link #resolve(AttributeResolutionContext)}. */
        private Optional<Map<String, Attribute>> staticValues;

        /**
         * Constructor.
         * 
         * @param id id of the data connector
         * @param values values returned for {@link #resolve(AttributeResolutionContext)}
         */
        public MockBaseDataConnector(final String id, final Map<String, Attribute> values) {
            setId(id);
            staticValues = Optional.<Map<String, Attribute>> fromNullable(values);
        }

        /** {@inheritDoc} */
        protected Optional<Map<String, Attribute>> doDataConnectorResolve(
                AttributeResolutionContext resolutionContext) throws ResolutionException {
            return staticValues;
        }
    }
}
/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
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

package net.shibboleth.idp.attribute.resolver;

import java.util.Map;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;


/** A data connector that just returns a static collection of attributes. */
@ThreadSafe
public class MockDataConnector extends BaseDataConnector {

    /** Static collection of values returned by this connector. */
    private final Map<String, Attribute<?>> values;

    /**
     * Constructor.
     * 
     * @param id unique ID for this data connector
     * @param connectorValues static collection of values returned by this connector
     */
    public MockDataConnector(String id, Map<String, Attribute<?>> connectorValues) {
        super(id);
        values = connectorValues;
    }

    /** {@inheritDoc} */
    protected Map<String, Attribute<?>> doDataConnectorResolve(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        return values;
    }
}
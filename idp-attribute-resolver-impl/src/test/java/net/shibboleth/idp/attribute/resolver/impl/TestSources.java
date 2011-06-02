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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Map;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;

import org.opensaml.util.collections.LazyMap;
import org.opensaml.util.collections.LazySet;

/** Basic data sources for testing the attribute generators. */
public final class TestSources {
    /** The name we use in this test for the static connector. */
    protected static final String STATIC_CONNECTOR_NAME = "staticCon";

    /** The name we use in this test for the static attribute. */
    protected static final String STATIC_ATTRIBUTE_NAME = "staticAtt";

    /** The name of the attribute we use as source. */
    protected static final String DEPENDS_ON_ATTRIBUTE_NAME = "at1";

    /** A value from both providers. */
    protected static final String COMMON_ATTRIBUTE_VALUE = "at1-Data";

    /** A value from the connector. */
    protected static final String CONNECTOR_ATTRIBUTE_VALUE = "at1-Connector";

    /** A value from the attribute. */
    protected static final String ATTRIBUTE_ATTRIBUTE_VALUE = "at1-Attribute";

    /** Regexp. for CONNECTOR_ATTRIBUTE_VALUE (for map & regexp testing). */
    protected static final String CONNECTOR_ATTRIBUTE_VALUE_REGEXP = "at1-(.+)or";
    
    /** Regexp result. for CONNECTOR_ATTRIBUTE_VALUE (for regexp testing). */
    protected static final String CONNECTOR_ATTRIBUTE_VALUE_REGEXP_RESULT = "Connect";
    

    /** Constructor. */
    private TestSources() {
    }

    /**
     * Create a static connector with known attributes and values.
     * 
     * @return The connector
     */
    protected static BaseDataConnector populatedStaticConnectior() {
        Map<String, Attribute<?>> values;
        Attribute<String> attr;
        Set<String> valuesSet;

        values = new LazyMap<String, Attribute<?>>();
        valuesSet = new LazySet<String>();

        valuesSet.add(COMMON_ATTRIBUTE_VALUE);
        valuesSet.add(CONNECTOR_ATTRIBUTE_VALUE);
        attr = new Attribute<String>(DEPENDS_ON_ATTRIBUTE_NAME);
        attr.setValues(valuesSet);
        values.put(DEPENDS_ON_ATTRIBUTE_NAME, attr);

        attr = new Attribute<String>("at2");
        valuesSet = new LazySet<String>();
        valuesSet.add("at2-Val1");
        valuesSet.add("at2-Val2");

        attr.setValues(valuesSet);
        values.put("at2", attr);

        return new StaticDataConnector(STATIC_CONNECTOR_NAME, values);
    }

    /**
     * Create a static attribute with known values.
     * 
     * @return the attribute definition
     */
    protected static BaseAttributeDefinition populatedStaticAttribute() {
        Attribute<String> attr;
        Set<String> valuesSet;

        valuesSet = new LazySet<String>();

        valuesSet.add(COMMON_ATTRIBUTE_VALUE);
        valuesSet.add(ATTRIBUTE_ATTRIBUTE_VALUE);
        attr = new Attribute<String>(DEPENDS_ON_ATTRIBUTE_NAME);
        attr.setValues(valuesSet);
        return new StaticAttributeDefinition(STATIC_ATTRIBUTE_NAME, attr);
    }

}

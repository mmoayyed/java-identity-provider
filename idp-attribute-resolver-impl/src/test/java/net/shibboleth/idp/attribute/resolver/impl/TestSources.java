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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Map;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;

import org.opensaml.util.collections.LazyMap;
import org.opensaml.util.collections.LazySet;
import org.opensaml.util.component.ComponentInitializationException;

/** Basic data sources for testing the attribute generators. */
public final class TestSources {
    /** The name we use in this test for the static connector. */
    protected static final String STATIC_CONNECTOR_NAME = "staticCon";

    /** The name we use in this test for the static attribute. */
    protected static final String STATIC_ATTRIBUTE_NAME = "staticAtt";

    /** The name of the attribute we use as source. */
    protected static final String DEPENDS_ON_ATTRIBUTE_NAME = "at1";
    
    /** The name of another attribute we use as source. */
    protected static final String DEPENDS_ON_SECOND_ATTRIBUTE_NAME = "at2";

    /** Another attributes values. */
    protected static final String[] SECOND_ATTRIBUTE_VALUES = {"at2-Val1", "at2-Val2"};

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
    
    /** Principal name for Principal method tests */
    protected static final String TEST_PRINCIPAL = "PrincipalName";

    /** Relying party name for Principal method tests */
    protected static final String TEST_RELYING_PARTY = "RP1";

    /** Authenitcation method for Principal method tests */
    protected static final String TEST_AUTHN_METHOD = "AuthNmEthod";


    /** Constructor. */
    private TestSources() {
    }

    /**
     * Create a static connector with known attributes and values.
     * 
     * @return The connector
     * @throws ComponentInitializationException if we cannot initialized (unlikely)
     */
    protected static BaseDataConnector populatedStaticConnectior() throws ComponentInitializationException {
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

        attr = new Attribute<String>(DEPENDS_ON_SECOND_ATTRIBUTE_NAME);
        valuesSet = new LazySet<String>();
        valuesSet.add(SECOND_ATTRIBUTE_VALUES[0]);
        valuesSet.add(SECOND_ATTRIBUTE_VALUES[1]);

        attr.setValues(valuesSet);
        values.put("at2", attr);
        
        StaticDataConnector connector = new StaticDataConnector();
        connector.setId(STATIC_CONNECTOR_NAME);
        connector.setValues(values);
        connector.initialize();

        return connector;
    }

    /**
     * Create a static attribute with known values.
     * 
     * @return the attribute definition
     * @throws ComponentInitializationException if we cannot initialized (unlikely)
     */
    protected static BaseAttributeDefinition populatedStaticAttribute() throws ComponentInitializationException {
        Attribute<String> attr;
        Set<String> valuesSet;

        valuesSet = new LazySet<String>();

        valuesSet.add(COMMON_ATTRIBUTE_VALUE);
        valuesSet.add(ATTRIBUTE_ATTRIBUTE_VALUE);
        attr = new Attribute<String>(DEPENDS_ON_ATTRIBUTE_NAME);
        attr.setValues(valuesSet);
        
        StaticAttributeDefinition definition = new StaticAttributeDefinition();
        definition.setId(STATIC_ATTRIBUTE_NAME);
        definition.setAttribute(attr);
        definition.initialize();
        return definition;
    }

}

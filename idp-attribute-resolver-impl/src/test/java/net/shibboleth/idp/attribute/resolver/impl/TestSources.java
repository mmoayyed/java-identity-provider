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

import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.attribute.resolver.impl.ad.SAML2NameIDAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.impl.ad.StaticAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.impl.dc.StaticDataConnector;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/** Basic data sources for testing the attribute generators. */
public final class TestSources {
    /** The name we use in this test for the static connector. */
    public static final String STATIC_CONNECTOR_NAME = "staticCon";

    /** The name we use in this test for the static attribute. */
    public static final String STATIC_ATTRIBUTE_NAME = "staticAtt";

    /** The name of the attribute we use as source. */
    public static final String DEPENDS_ON_ATTRIBUTE_NAME_ATTR = "at1";
    public static final String DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR = "ac1";
    
    /** The name of another attribute we use as source. */
    public static final String DEPENDS_ON_SECOND_ATTRIBUTE_NAME = "at2";

    /** Another attributes values. */
    public static final String[] SECOND_ATTRIBUTE_VALUE_STRINGS = {"at2-Val1", "at2-Val2"};
    public static final StringAttributeValue[] SECOND_ATTRIBUTE_VALUE_RESULTS = {new StringAttributeValue(SECOND_ATTRIBUTE_VALUE_STRINGS[0]),new StringAttributeValue(SECOND_ATTRIBUTE_VALUE_STRINGS[0]),};

    /** A value from both providers. */
    public static final String COMMON_ATTRIBUTE_VALUE_STRING = "at1-Data";
    public static final StringAttributeValue COMMON_ATTRIBUTE_VALUE_RESULT = new StringAttributeValue(COMMON_ATTRIBUTE_VALUE_STRING);

    /** A value from the connector. */
    public static final String CONNECTOR_ATTRIBUTE_VALUE_STRING = "at1-Connector";
    public static final StringAttributeValue CONNECTOR_ATTRIBUTE_VALUE_RESULT = new StringAttributeValue(CONNECTOR_ATTRIBUTE_VALUE_STRING);

    /** A value from the attribute. */
    public static final String ATTRIBUTE_ATTRIBUTE_VALUE_STRING = "at1-Attribute";
    public static final StringAttributeValue ATTRIBUTE_ATTRIBUTE_VALUE_RESULT = new StringAttributeValue(ATTRIBUTE_ATTRIBUTE_VALUE_STRING);

    /** Regexp. for CONNECTOR_ATTRIBUTE_VALUE (for map & regexp testing). */
    
    public static final String CONNECTOR_ATTRIBUTE_VALUE_REGEXP = "at1-(.+)or";
    public static final Pattern CONNECTOR_ATTRIBUTE_VALUE_REGEXP_PATTERN = Pattern.compile(CONNECTOR_ATTRIBUTE_VALUE_REGEXP);
    public static final StringAttributeValue CONNECTOR_ATTRIBUTE_VALUE_REGEXP_RESULT = new StringAttributeValue("Connect");
    
    /** Principal name for Principal method tests */
    public static final String TEST_PRINCIPAL = "PrincipalName";

    /** Relying party name for Principal method tests */
    public static final String TEST_RELYING_PARTY = "RP1";

    /** Authentication method for Principal method tests */
    public static final String TEST_AUTHN_METHOD = "AuthNmEthod";

    public static final String IDP_ENTITY_ID = "https://idp.example.org/idp";

    public static final String PRINCIPAL_ID = "PETER_THE_PRINCIPAL";

    public static final String SP_ENTITY_ID = "https://sp.example.org/sp";


    /** Constructor. */
    private TestSources() {
    }

    /**
     * Create a static connector with known attributes and values.
     * 
     * @return The connector
     * @throws ComponentInitializationException if we cannot initialized (unlikely)
     */
    public static DataConnector populatedStaticConnector() throws ComponentInitializationException {
        IdPAttribute attr;
        Set<IdPAttribute> attributeSet;
        Set<IdPAttributeValue<?>> valuesSet;

        valuesSet = new LazySet<>();
        attributeSet = new LazySet<IdPAttribute>();

        valuesSet.add(new StringAttributeValue(COMMON_ATTRIBUTE_VALUE_STRING));
        valuesSet.add(new StringAttributeValue(CONNECTOR_ATTRIBUTE_VALUE_STRING));
        attr = new IdPAttribute(DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR);
        attr.setValues(valuesSet);
        attributeSet.add(attr);

        attr = new IdPAttribute(DEPENDS_ON_SECOND_ATTRIBUTE_NAME);
        valuesSet = new LazySet<>();
        valuesSet.add(new StringAttributeValue(SECOND_ATTRIBUTE_VALUE_STRINGS[0]));
        valuesSet.add(new StringAttributeValue(SECOND_ATTRIBUTE_VALUE_STRINGS[1]));
        attr.setValues(valuesSet);
        attributeSet.add(attr);
        
        StaticDataConnector connector = new StaticDataConnector();
        connector.setId(STATIC_CONNECTOR_NAME);
        connector.setValues(attributeSet);
        connector.initialize();

        return connector;
    }

    /**
     * Create a static attribute with known values.
     * 
     * @return the attribute definition
     * @throws ComponentInitializationException if we cannot initialized (unlikely)
     */
    public static AttributeDefinition populatedStaticAttribute() throws ComponentInitializationException {
        return populatedStaticAttribute(STATIC_ATTRIBUTE_NAME, DEPENDS_ON_ATTRIBUTE_NAME_ATTR, 2);
    }

    public static AttributeDefinition populatedStaticAttribute(String definitionName, String attributeName, int attributeCount) throws ComponentInitializationException {
        IdPAttribute attr;
        Set<IdPAttributeValue<?>> valuesSet;

        valuesSet = new LazySet<>();

        if (attributeCount > 0) {
            valuesSet.add(new StringAttributeValue(COMMON_ATTRIBUTE_VALUE_STRING));
        }
        if (attributeCount > 1) {
            valuesSet.add(new StringAttributeValue(ATTRIBUTE_ATTRIBUTE_VALUE_STRING));
        }
        for (int i = 2; i < attributeCount; i++) {
            valuesSet.add(new StringAttributeValue(ATTRIBUTE_ATTRIBUTE_VALUE_STRING + i));
        }
        attr = new IdPAttribute(attributeName);
        attr.setValues(valuesSet);
        
        StaticAttributeDefinition definition = new StaticAttributeDefinition();
        definition.setId(definitionName);
        definition.setValue(attr);
        definition.initialize();
        return definition;
    }
    
    public static AttributeDefinition nonStringAttributeDefiniton(String name) {
        final SAML2NameIDAttributeDefinition defn = new SAML2NameIDAttributeDefinition();
        defn.setId(name);

        // Set the dependency on the data connector
        ResolverPluginDependency depend = new ResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME);
        depend.setDependencyAttributeId(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);
        defn.setDependencies(Collections.singleton(depend));
        return defn;
    }

    public static AttributeResolutionContext createResolutionContext(String principal, String issuerID, String recipientId) {
        AttributeResolutionContext retVal = new AttributeResolutionContext();
        
        retVal.setAttributeIssuerID(issuerID);
        retVal.setAttributeRecipientID(recipientId);
        retVal.setPrincipal(principal);
        
        retVal.getSubcontext(AttributeResolverWorkContext.class, true);
        return retVal;
    }
    
    public static ResolverPluginDependency makeResolverPluginDependency(String pluginId, String attributeId) {
        ResolverPluginDependency retVal = new ResolverPluginDependency(pluginId);
        retVal.setDependencyAttributeId(attributeId);
        return retVal;
    }
}

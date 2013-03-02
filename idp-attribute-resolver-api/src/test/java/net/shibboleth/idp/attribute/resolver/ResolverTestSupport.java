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

import java.util.Arrays;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 *
 */
public class ResolverTestSupport {

    public static final String EPA_ATTRIB_ID = "urn:oid:1.3.6.1.4.1.5923.1.1.1.1";

    public static final String[] EPA1_VALUES = new String[] {"student", "member"};

    public static final String[] EPA2_VALUES = new String[] {"staff", "member"};

    public static final String[] EPA3_VALUES = new String[] {"student", "part-time-student", "library-walk-in"};

    public static final String EPE_ATTRIB_ID = "urn:oid:1.3.6.1.4.1.5923.1.1.1.7";

    public static final String[] EPE1_VALUES = new String[] {"http://example.org/entitlement1",
            "http://example.org/entitlement2"};

    public static final String[] EPE2_VALUES = new String[] {"urn:example.org:entitlement1"};

    public static final String[] EPE3_VALUES = new String[] {"urn:example.org:entitlement2"};

    public static AttributeResolutionContext buildResolutionContext(BaseResolverPlugin... plugins) {
        AttributeResolutionContext resolutionContext = new AttributeResolutionContext();

        MockStaticAttributeDefinition definition;
        MockStaticDataConnector connector;
        try {
            for (BaseResolverPlugin plugin : plugins) {
                if (plugin instanceof MockStaticAttributeDefinition) {
                    definition = (MockStaticAttributeDefinition) plugin;
                    resolutionContext.recordAttributeDefinitionResolution(definition,
                            definition.resolve(resolutionContext));
                }

                if (plugin instanceof MockStaticDataConnector) {
                    connector = (MockStaticDataConnector) plugin;
                    resolutionContext.recordDataConnectorResolution(connector, connector.resolve(resolutionContext));
                }
            }
        } catch (ResolutionException e) {
            // this can't happen here
            e.printStackTrace();
        }

        return resolutionContext;
    }

    public static Attribute buildAttribute(String attributeId, String... values) {
        Attribute attribute = new Attribute(attributeId);
        for (String value : values) {
            attribute.getValues().add(new StringAttributeValue(value));
        }

        return attribute;
    }

    public static MockStaticAttributeDefinition buildAttributeDefinition(String attributeId, String... values) {
        Attribute attribute = buildAttribute(attributeId, values);

        try {
            MockStaticAttributeDefinition definition = new MockStaticAttributeDefinition();
            definition.setId(attributeId);
            definition.setValue(attribute);
            definition.initialize();
            return definition;
        } catch (ComponentInitializationException e) {
            // this can't happen here
            e.printStackTrace();
            return null;
        }
    }

    public static MockStaticDataConnector buildDataConnector(String connectorId, Attribute... attributes) {

        try {
            MockStaticDataConnector connector = new MockStaticDataConnector();
            connector.setId(connectorId);
            connector.setValues(Arrays.asList(attributes));
            connector.initialize();

            return connector;
        } catch (ComponentInitializationException e) {
            // this can't happen here
            e.printStackTrace();
            return null;
        }
    }
}
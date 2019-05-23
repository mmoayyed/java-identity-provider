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

package net.shibboleth.idp.attribute.filter.policyrule.saml.impl;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.idp.attribute.filter.matcher.impl.DataSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * test for {@link AttributeRequesterEntityAttributeExactPolicyRule}.
 */
public class AttributeRequesterEntityAttributeExactPolicyRuleTest extends BaseMetadataTests {

    private AttributeRequesterEntityAttributeExactPolicyRule getMatcher() throws ComponentInitializationException {
        return getMatcher("urn:example.org:policies", "urn:example.org:policy:1234", null, false);
    }

    private AttributeRequesterEntityAttributeExactPolicyRule getMatcher(String attributeName, String attributeValue,
            String attributeNameFormat, boolean ignoreUnmapped) throws ComponentInitializationException {
        AttributeRequesterEntityAttributeExactPolicyRule matcher = new AttributeRequesterEntityAttributeExactPolicyRule();
        matcher.setId("matcher");
        matcher.setAttributeName(attributeName);
        matcher.setValue(attributeValue);
        matcher.setNameFormat(attributeNameFormat);
        matcher.setIgnoreUnmappedEntityAttributes(ignoreUnmapped);
        matcher.initialize();
        return matcher;
    }

    @Test public void testValue() throws ComponentInitializationException {

        AttributeRequesterEntityAttributeExactPolicyRule matcher = getMatcher();
        assertEquals(matcher.matches(reqMetadataContext(idpEntity, "Principal")), Tristate.TRUE);

        assertEquals(matcher.matches(reqMetadataContext(jiraEntity, "Principal")), Tristate.FALSE);
    }

    @Test public void testFormat() throws ComponentInitializationException {

        AttributeRequesterEntityAttributeExactPolicyRule matcher =
                getMatcher("urn:example.org:entitlements", "urn:example.org:entitlements:1234", null, true);
        assertEquals(matcher.getValue(), "urn:example.org:entitlements:1234");
        assertEquals(matcher.matches(reqMetadataContext(idpEntity, "Principal")), Tristate.TRUE);

        assertEquals(matcher.matches(reqMetadataContext(wikiEntity, "Principal")), Tristate.FALSE);

        matcher = getMatcher("urn:example.org:entitlements", "urn:example.org:entitlements:1234", "foo", true);
        assertEquals(matcher.matches(reqMetadataContext(idpEntity, "Principal")), Tristate.FALSE);

        assertEquals(matcher.matches(reqMetadataContext(jiraEntity, "Principal")), Tristate.FALSE);

        matcher =
                getMatcher("urn:example.org:entitlements", "urn:example.org:entitlements:1234",
                        "urn:oasis:names:tc:SAML:2.0:attrname-format:uri", true);
        assertEquals(matcher.matches(reqMetadataContext(idpEntity, "Principal")), Tristate.TRUE);

        assertEquals(matcher.matches(reqMetadataContext(jiraEntity, "Principal")), Tristate.FALSE);
    }

    @Test public void testNoMatch() throws ComponentInitializationException {

        AttributeRequesterEntityAttributeExactPolicyRule matcher =
                getMatcher("urn:example.org:policies", "urn:example.org:policy:1235", null, false);
        assertEquals(matcher.matches(reqMetadataContext(idpEntity, "Principal")), Tristate.FALSE);
        assertEquals(matcher.matches(reqMetadataContext(jiraEntity, "Principal")), Tristate.FALSE);

        matcher = getMatcher("urn:example.org:policiess", "urn:example.org:policy:1234", null, false);
        assertEquals(matcher.matches(reqMetadataContext(idpEntity, "Principal")), Tristate.FALSE);
        assertEquals(matcher.matches(reqMetadataContext(noneEntity, "Principal")), Tristate.FALSE);
    }

    @Test public void testSplitAttribute() throws ComponentInitializationException {

        AttributeRequesterEntityAttributeExactPolicyRule matcher =
                getMatcher("urn:example.org:policies", "urn:example.org:policy:1234", null, false);
        assertEquals(matcher.matches(reqMetadataContext(idpEntity, "Principal")), Tristate.TRUE);
        assertEquals(matcher.matches(reqMetadataContext(jiraEntity, "Principal")), Tristate.FALSE);

        matcher = getMatcher("urn:example.org:policies", "urn:example.org:policy:5678", null, false);
        assertEquals(matcher.matches(reqMetadataContext(idpEntity, "Principal")), Tristate.TRUE);
        assertEquals(matcher.matches(reqMetadataContext(noneEntity, "Principal")), Tristate.FALSE);
    }

    @Test public void testUnpopulated()
            throws ComponentInitializationException {
        assertEquals(getMatcher().matches(DataSources.unPopulatedFilterContext()), Tristate.FALSE);
    }

    @Test public void testNoMetadata()
            throws ComponentInitializationException {
        assertEquals(getMatcher().matches(reqMetadataContext(null, "Principal")), Tristate.FALSE);
    }
}

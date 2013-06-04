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

package net.shibboleth.idp.attribute.filter.impl.saml;

import junit.framework.Assert;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.MatcherException;
import net.shibboleth.idp.attribute.filter.impl.matcher.DataSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.annotations.Test;

/**
 * test for {@link AttributeRequesterEntityAttributeExactMatcher}.
 */
public class AttributeRequesterEntityAttributeExactMatcherTest extends BaseMetadataTests {

    private AttributeRequesterEntityAttributeExactMatcher getMatcher() throws ComponentInitializationException {
        return getMatcher("urn:example.org:policies", "urn:example.org:policy:1234", null);
    }

    private AttributeRequesterEntityAttributeExactMatcher getMatcher(String attributeName, String attributeValue,
            String attributeNameFormat) throws ComponentInitializationException {
        AttributeRequesterEntityAttributeExactMatcher matcher = new AttributeRequesterEntityAttributeExactMatcher();
        matcher.setId("matcher");
        matcher.setAttributeName(attributeName);
        matcher.setValue(attributeValue);
        matcher.setNameFormat(attributeNameFormat);
        matcher.initialize();
        return matcher;
    }

    @Test public void testValue() throws AttributeFilterException, ComponentInitializationException {

        AttributeRequesterEntityAttributeExactMatcher matcher = getMatcher();
        Assert.assertTrue(matcher.matches(metadataContext(null, idpEntity, "Principal")));

        Assert.assertFalse(matcher.matches(metadataContext(null, jiraEntity, "Principal")));

    }

    @Test public void testFormat() throws AttributeFilterException, ComponentInitializationException {

        AttributeRequesterEntityAttributeExactMatcher matcher =
                getMatcher("urn:example.org:entitlements", "urn:example.org:entitlements:1234", null);
        Assert.assertEquals(matcher.getValue(), "urn:example.org:entitlements:1234");
        Assert.assertTrue(matcher.matches(metadataContext(null, idpEntity, "Principal")));

        Assert.assertFalse(matcher.matches(metadataContext(null, wikiEntity, "Principal")));

        matcher = getMatcher("urn:example.org:entitlements", "urn:example.org:entitlements:1234", "foo");
        Assert.assertFalse(matcher.matches(metadataContext(null, idpEntity, "Principal")));

        Assert.assertFalse(matcher.matches(metadataContext(null, jiraEntity, "Principal")));

        matcher =
                getMatcher("urn:example.org:entitlements", "urn:example.org:entitlements:1234",
                        "urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
        Assert.assertTrue(matcher.matches(metadataContext(null, idpEntity, "Principal")));

        Assert.assertFalse(matcher.matches(metadataContext(null, jiraEntity, "Principal")));
    }

    @Test public void testNoMatch() throws AttributeFilterException, ComponentInitializationException {

        AttributeRequesterEntityAttributeExactMatcher matcher =
                getMatcher("urn:example.org:policies", "urn:example.org:policy:1235", null);
        Assert.assertFalse(matcher.matches(metadataContext(null, idpEntity, "Principal")));
        Assert.assertFalse(matcher.matches(metadataContext(null, jiraEntity, "Principal")));

        matcher = getMatcher("urn:example.org:policiess", "urn:example.org:policy:1234", null);
        Assert.assertFalse(matcher.matches(metadataContext(null, idpEntity, "Principal")));
        Assert.assertFalse(matcher.matches(metadataContext(null, noneEntity, "Principal")));
    }

    @Test(expectedExceptions = {MatcherException.class}) public void testUnpopulated()
            throws ComponentInitializationException, AttributeFilterException {
        getMatcher().matches(DataSources.unPopulatedFilterContext());
    }

    @Test(expectedExceptions = {MatcherException.class}) public void testNoMetadata()
            throws ComponentInitializationException, AttributeFilterException {
        getMatcher().matches(metadataContext(null, null, "Principal"));
    }
}

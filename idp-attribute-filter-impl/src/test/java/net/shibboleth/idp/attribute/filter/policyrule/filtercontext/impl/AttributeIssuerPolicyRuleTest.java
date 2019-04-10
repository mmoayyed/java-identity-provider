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

package net.shibboleth.idp.attribute.filter.policyrule.filtercontext.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.idp.attribute.filter.matcher.impl.DataSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;

/**
 * Tests for {@link AttributeIssuerPolicyRule}.
 */
public class AttributeIssuerPolicyRuleTest {

    private AttributeIssuerPolicyRule getMatcher() throws ComponentInitializationException {
        return getMatcher(true);
    }

    private AttributeIssuerPolicyRule getMatcher(final boolean caseSensitive) throws ComponentInitializationException {
        final AttributeIssuerPolicyRule matcher = new AttributeIssuerPolicyRule();
        matcher.setMatchString("issuer");
        matcher.setCaseSensitive(caseSensitive);
        matcher.setId("Test");
        matcher.initialize();
        return matcher;
    }

    @Test public void testNull() throws ComponentInitializationException {

        try {
            new AttributeIssuerPolicyRule().matches(null);
            fail();
        } catch (UninitializedComponentException ex) {
            // OK
        }
    }

    @Test public void testUnpopulated()
            throws ComponentInitializationException {
        assertEquals(getMatcher().matches(DataSources.unPopulatedFilterContext()), Tristate.FAIL);
    }
    
    @SuppressWarnings("deprecation")
    @Test public void testDefault() {
        final AttributeIssuerPolicyRule matcher = new AttributeIssuerPolicyRule();
        assertFalse(matcher.isIgnoreCase());
        assertTrue(matcher.isCaseSensitive());
    }

    @Test public void testNoIssuer()
            throws ComponentInitializationException {
        assertEquals(getMatcher().matches(DataSources.populatedFilterContext(null, null, null)), Tristate.FAIL);
    }

    @Test public void testCaseSensitive() throws ComponentInitializationException {

        final AttributeIssuerPolicyRule matcher = getMatcher();
        assertTrue(matcher.isCaseSensitive());

        assertEquals(matcher.matches(DataSources.populatedFilterContext(null, "wibble", null)), Tristate.FALSE);
        assertEquals(matcher.matches(DataSources.populatedFilterContext(null, "ISSUER", null)), Tristate.FALSE);
        assertEquals(matcher.matches(DataSources.populatedFilterContext(null, "issuer", null)), Tristate.TRUE);
    }

    @Test public void testCaseInsensitive() throws ComponentInitializationException {

        final AttributeIssuerPolicyRule matcher = getMatcher(false);

        assertEquals(matcher.matches(DataSources.populatedFilterContext(null, "wibble", null)), Tristate.FALSE);
        assertEquals(matcher.matches(DataSources.populatedFilterContext(null, "ISSUER", null)), Tristate.TRUE);
        assertEquals(matcher.matches(DataSources.populatedFilterContext(null, "issuer", null)), Tristate.TRUE);
    }
}

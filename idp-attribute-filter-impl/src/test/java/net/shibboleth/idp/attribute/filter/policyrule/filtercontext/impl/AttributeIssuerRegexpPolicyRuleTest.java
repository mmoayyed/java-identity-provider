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
import static org.testng.Assert.fail;

import java.util.regex.Pattern;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.idp.attribute.filter.matcher.impl.DataSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;

/**
 * Tests for {@link AttributeIssuerRegexpPolicyRule}.
 */
public class AttributeIssuerRegexpPolicyRuleTest {

    private AttributeIssuerRegexpPolicyRule getMatcher(boolean caseSensitive) throws ComponentInitializationException {
        final AttributeIssuerRegexpPolicyRule matcher = new AttributeIssuerRegexpPolicyRule();
        final int flags;
        if (caseSensitive) {
            flags = 0;
        } else {
            flags = Pattern.CASE_INSENSITIVE;
        }
        matcher.setPattern(Pattern.compile("^issu.*", flags));
        matcher.setId("Test");
        matcher.initialize();
        return matcher;
    }

    @Test public void testAll() throws ComponentInitializationException {

        try {
            new AttributeIssuerRegexpPolicyRule().matches(null);
            fail();
        } catch (UninitializedComponentException ex) {
            // OK
        }
        AttributeIssuerRegexpPolicyRule matcher = getMatcher(true);

        assertEquals(matcher.matches(DataSources.populatedFilterContext(null, "wibble", null)), Tristate.FALSE);
        assertEquals(matcher.matches(DataSources.populatedFilterContext(null, "ISSUER", null)), Tristate.FALSE);
        assertEquals(matcher.matches(DataSources.populatedFilterContext(null, "issuer", null)), Tristate.TRUE);
        
        matcher = getMatcher(false);

        assertEquals(matcher.matches(DataSources.populatedFilterContext(null, "wibble", null)), Tristate.FALSE);
        assertEquals(matcher.matches(DataSources.populatedFilterContext(null, "ISSUER", null)), Tristate.TRUE);
        assertEquals(matcher.matches(DataSources.populatedFilterContext(null, "issuer", null)), Tristate.TRUE);
    }

    @Test public void testUnpopulated()
            throws ComponentInitializationException {
        assertEquals(getMatcher(true).matches(DataSources.unPopulatedFilterContext()), Tristate.FAIL);;
    }

    @Test  public void testNoIssuer()
            throws ComponentInitializationException{
        assertEquals(getMatcher(false).matches(DataSources.populatedFilterContext(null, null, null)), Tristate.FAIL);;
    }

}

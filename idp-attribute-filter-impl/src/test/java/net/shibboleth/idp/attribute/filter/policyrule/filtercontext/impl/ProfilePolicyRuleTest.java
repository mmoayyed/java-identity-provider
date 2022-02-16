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

import org.opensaml.messaging.context.navigate.ParentContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.matcher.impl.DataSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;


/**
 * Tests for {@link ProfilePolicyRule}.
 */
@SuppressWarnings("javadoc")
public class ProfilePolicyRuleTest {
    
    private ProfilePolicyRule getMatcher(final boolean caseSensitive) throws ComponentInitializationException {
        ProfilePolicyRule matcher = new ProfilePolicyRule();
        matcher.setMatchString("https://shibboleth.net/profile");
        matcher.setCaseSensitive(caseSensitive);
        matcher.setId("Test");
        matcher.setProfileContextStrategy(new ParentContextLookup<>(ProfileRequestContext.class));
        matcher.initialize();
        return matcher;
    }
    
    @Test public void testNull() throws ComponentInitializationException {

        try {
            new ProfilePolicyRule().matches(null);
            fail();
        } catch (UninitializedComponentException ex) {
            // OK
        }       
    }
    
    @Test public void testUnpopulated() throws ComponentInitializationException {
        final ProfilePolicyRule matcher = getMatcher(true);
        assertEquals(matcher.matches(DataSources.unPopulatedFilterContext()), Tristate.FALSE);
    }

    @Test public void testNoProfile() throws ComponentInitializationException {
        final ProfilePolicyRule matcher = getMatcher(true);
        final AttributeFilterContext afc = DataSources.populatedFilterContext(null, null, null);
        
        assertEquals(matcher.matches(afc), Tristate.FALSE);
    }

    @Test public void testCaseSensitive() throws ComponentInitializationException {

        final ProfilePolicyRule matcher = getMatcher(true);
        final AttributeFilterContext afc = DataSources.populatedFilterContext(null, null, null);
        
        ((ProfileRequestContext) afc.getParent()).setProfileId("https://shibboleth.net/Profile");
        assertEquals(matcher.matches(afc), Tristate.FALSE);
        
        ((ProfileRequestContext) afc.getParent()).setProfileId("https://shibboleth.net/profile");
        assertEquals(matcher.matches(afc), Tristate.TRUE);
    }

    
    @Test public void testCaseInsensitive() throws ComponentInitializationException {

        final ProfilePolicyRule matcher = getMatcher(false);
        final AttributeFilterContext afc = DataSources.populatedFilterContext(null, null, null);
        
        ((ProfileRequestContext) afc.getParent()).setProfileId("https://shibboleth.net/Profile");
        assertEquals(matcher.matches(afc), Tristate.TRUE);
        
        ((ProfileRequestContext) afc.getParent()).setProfileId("https://shibboleth.net/profile");
        assertEquals(matcher.matches(afc), Tristate.TRUE);
    }

}
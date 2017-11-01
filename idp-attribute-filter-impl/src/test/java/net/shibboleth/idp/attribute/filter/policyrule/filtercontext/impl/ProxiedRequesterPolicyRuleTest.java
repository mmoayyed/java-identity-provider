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

import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.matcher.impl.DataSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;

import java.util.Arrays;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProxiedRequesterContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link ProxiedRequesterPolicyRule}.
 */
public class ProxiedRequesterPolicyRuleTest {

    private ProxiedRequesterPolicyRule getMatcher(boolean caseSensitive) throws ComponentInitializationException {
        final ProxiedRequesterPolicyRule matcher = new ProxiedRequesterPolicyRule();
        matcher.setMatchString("requester");
        matcher.setIgnoreCase(!caseSensitive);
        matcher.setId("Test");
        matcher.initialize();
        return matcher;

    }

    private ProxiedRequesterPolicyRule getMatcher() throws ComponentInitializationException {
        return getMatcher(true);
    }

    @Test public void testNull() throws ComponentInitializationException {

        try {
            new ProxiedRequesterPolicyRule().matches(null);
            Assert.fail();
        } catch (UninitializedComponentException ex) {
            // OK
        }
    }

    @Test public void testUnpopulated()
            throws ComponentInitializationException {
        Assert.assertEquals(getMatcher().matches(DataSources.unPopulatedFilterContext()), Tristate.FALSE);
    }

    @Test public void testNoProxies()
            throws ComponentInitializationException {
        Assert.assertEquals(getMatcher().matches(DataSources.populatedFilterContext(null, null, "foo")), Tristate.FALSE);
    }

    @Test public void testCaseSensitive() throws ComponentInitializationException {

        final ProxiedRequesterPolicyRule matcher = getMatcher();
        
        final AttributeFilterContext ctx = DataSources.populatedFilterContext(null, null, "wibble");
        ctx.setProxiedRequesterContextLookupStrategy(
                new ChildContextLookup<AttributeFilterContext,ProxiedRequesterContext>(ProxiedRequesterContext.class));
        ctx.getSubcontext(ProxiedRequesterContext.class, true).getRequesters().addAll(Arrays.asList("foo", "bar"));

        Assert.assertEquals(matcher.matches(ctx), Tristate.FALSE);
        
        ctx.getSubcontext(ProxiedRequesterContext.class).getRequesters().add("requester");
        Assert.assertEquals(matcher.matches(ctx), Tristate.TRUE);
    }

    @Test public void testCaseInsensitive() throws ComponentInitializationException {

        final ProxiedRequesterPolicyRule matcher = getMatcher(false);
        
        final AttributeFilterContext ctx = DataSources.populatedFilterContext(null, null, "wibble");
        ctx.setProxiedRequesterContextLookupStrategy(
                new ChildContextLookup<AttributeFilterContext,ProxiedRequesterContext>(ProxiedRequesterContext.class));
        ctx.getSubcontext(ProxiedRequesterContext.class, true).getRequesters().addAll(Arrays.asList("foo", "bar"));

        Assert.assertEquals(matcher.matches(ctx), Tristate.FALSE);
        
        ctx.getSubcontext(ProxiedRequesterContext.class).getRequesters().add("REQUESTER");
        Assert.assertEquals(matcher.matches(ctx), Tristate.TRUE);
    }
    
}
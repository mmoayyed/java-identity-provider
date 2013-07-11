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

package net.shibboleth.idp.attribute.filter.impl.policyrule.filtercontext;

import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.idp.attribute.filter.impl.matcher.DataSources;
import net.shibboleth.idp.attribute.filter.impl.policyrule.filtercontext.AuthenticationMethodPolicyRule;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;

import org.opensaml.messaging.context.BaseContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link AuthenticationMethodPolicyRule}.
 */
public class AuthenticationMethodPolicyRuleTest {

    protected static String METHOD = "AuthnMethod";
    
    private AuthenticationMethodPolicyRule getMatcher() throws ComponentInitializationException {
        return getMatcher(true);
    }
    
    protected static AttributeFilterContext filterContextWithAuthn(String authn) {
        BaseContext parent = new BaseContext() {};
        
        AttributeRecipientContext recipientContext = new AttributeRecipientContext();
        AttributeResolutionContext resolutionContext = new AttributeResolutionContext();
        
        recipientContext.setPrincipalAuthenticationMethod(authn);
        resolutionContext.addSubcontext(recipientContext);

        parent.addSubcontext(resolutionContext);
        return parent.getSubcontext(AttributeFilterContext.class, true);
    }
    

    private AuthenticationMethodPolicyRule getMatcher(boolean caseSensitive) throws ComponentInitializationException {
        final AuthenticationMethodPolicyRule matcher = new AuthenticationMethodPolicyRule();
        matcher.setMatchString(METHOD);
        matcher.setCaseSensitive(caseSensitive);
        matcher.setId("Test");
        matcher.initialize();
        return matcher;
    }

    @Test public void testNull() throws ComponentInitializationException, AttributeFilterException {

        try {
            new AuthenticationMethodPolicyRule().matches(null);
            Assert.fail();
        } catch (UninitializedComponentException ex) {
            // OK
        }
    }

    @Test public void testUnpopulated()
            throws ComponentInitializationException, AttributeFilterException {
        Assert.assertEquals(getMatcher().matches(DataSources.unPopulatedFilterContext()), Tristate.FAIL);
    }

    @Test public void testNoIssuer()
            throws ComponentInitializationException, AttributeFilterException {
        Assert.assertEquals(getMatcher().matches(filterContextWithAuthn(null)), Tristate.FAIL);
    }

    @Test public void testCaseSensitive() throws ComponentInitializationException, AttributeFilterException {

        final AuthenticationMethodPolicyRule matcher = getMatcher();

        Assert.assertEquals(matcher.matches(filterContextWithAuthn(METHOD+METHOD)), Tristate.FALSE);
        Assert.assertEquals(matcher.matches(filterContextWithAuthn(METHOD.toLowerCase())), Tristate.FALSE);
        Assert.assertEquals(matcher.matches(filterContextWithAuthn(METHOD)), Tristate.TRUE);
    }

    @Test public void testCaseInsensitive() throws ComponentInitializationException, AttributeFilterException {

        final AuthenticationMethodPolicyRule matcher = getMatcher(false);

        Assert.assertEquals(matcher.matches(filterContextWithAuthn(METHOD+METHOD)), Tristate.FALSE);
        Assert.assertEquals(matcher.matches(filterContextWithAuthn(METHOD.toLowerCase())), Tristate.TRUE);
        Assert.assertEquals(matcher.matches(filterContextWithAuthn(METHOD)), Tristate.TRUE);
    }
}
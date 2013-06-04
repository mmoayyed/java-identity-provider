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

package net.shibboleth.idp.attribute.filter.impl.filtercontext;

import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.MatcherException;
import net.shibboleth.idp.attribute.filter.impl.matcher.DataSources;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;

import org.opensaml.messaging.context.BaseContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link AuthenticationMethodRegexpMatcher}.
 */
public class AuthenticationMethodRegexpMatcherTest {

    protected static String METHOD = "AuthnMethod";
    
    protected static AttributeFilterContext filterContextWithAuthn(String authn) {
        BaseContext parent = new BaseContext() {};
        
        AttributeRecipientContext recipientContext = new AttributeRecipientContext();
        AttributeResolutionContext resolutionContext = new AttributeResolutionContext();
        
        recipientContext.setPrincipalAuthenticationMethod(authn);
        resolutionContext.addSubcontext(recipientContext);

        parent.addSubcontext(resolutionContext);
        return parent.getSubcontext(AttributeFilterContext.class, true);
    }
    

    private AuthenticationMethodRegexpMatcher getMatcher() throws ComponentInitializationException {
        final AuthenticationMethodRegexpMatcher matcher = new AuthenticationMethodRegexpMatcher();
        matcher.setRegularExpression("^Authn.*");
        matcher.setId("Test");
        matcher.initialize();
        return matcher;
    }

    @Test public void testNull() throws ComponentInitializationException {

        try {
            new AuthenticationMethodRegexpMatcher().doCompare(null);
            Assert.fail();
        } catch (UninitializedComponentException ex) {
            // OK
        }
    }

    @Test(expectedExceptions = {MatcherException.class}) public void testUnpopulated()
            throws ComponentInitializationException, AttributeFilterException {
        getMatcher().doCompare(DataSources.unPopulatedFilterContext());
    }

    @Test(expectedExceptions = {MatcherException.class}) public void testNoIssuer()
            throws ComponentInitializationException, AttributeFilterException {
        getMatcher().doCompare(filterContextWithAuthn(null));
    }

    @Test public void testRegexp() throws ComponentInitializationException, AttributeFilterException {

        final AuthenticationMethodRegexpMatcher matcher = getMatcher();

        Assert.assertFalse(matcher.matches(filterContextWithAuthn("foo")));
        Assert.assertTrue(matcher.doCompare(filterContextWithAuthn(METHOD)));
    }

}
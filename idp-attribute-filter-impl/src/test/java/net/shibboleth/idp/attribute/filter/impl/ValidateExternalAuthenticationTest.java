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

package net.shibboleth.idp.attribute.filter.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import net.shibboleth.ext.spring.service.MockApplicationContext;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.idp.attribute.filter.AttributeFilterPolicy;
import net.shibboleth.idp.attribute.filter.AttributeRule;
import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.idp.attribute.filter.impl.AttributeFilterImpl;
import net.shibboleth.idp.attribute.filter.policyrule.filtercontext.impl.AttributeIssuerPolicyRule;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.ExternalAuthenticationContext;
import net.shibboleth.idp.authn.impl.BaseAuthenticationContextTest;
import net.shibboleth.idp.authn.impl.ExternalAuthenticationImpl;
import net.shibboleth.idp.authn.impl.ValidateExternalAuthentication;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.service.MockReloadableService;
import net.shibboleth.utilities.java.support.service.ReloadableService;

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link ValidateExternalAuthentication} unit test using attributes. */
public class ValidateExternalAuthenticationTest extends BaseAuthenticationContextTest {
    
    private ValidateExternalAuthentication action;
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        prc.getSubcontext(AuthenticationContext.class).setAttemptedFlow(authenticationFlows.get(0));

        action = new ValidateExternalAuthentication(getFilterService());
        action.setHttpServletRequest((HttpServletRequest) src.getExternalContext().getNativeRequest());
        action.initialize();
    }

    @Test public void testPrincipalName() {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        final ExternalAuthenticationContext eac = (ExternalAuthenticationContext) ac.addSubcontext(
                new ExternalAuthenticationContext(new ExternalAuthenticationImpl()), true);
        eac.setPrincipalName("jdoe");
        
        final IdPAttribute mail = new IdPAttribute("mail");
        mail.setValues(Collections.singletonList(StringAttributeValue.valueOf("jdoe@example.org")));
        eac.getSubcontext(AttributeContext.class, true).setIdPAttributes(Collections.singletonList(mail));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertFalse(ac.getAuthenticationResult().isPreviousResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(
                UsernamePrincipal.class).iterator().next().getName(), "jdoe");
        Assert.assertTrue(ac.getAuthenticationResult().getSubject().getPrincipals(IdPAttributePrincipal.class).isEmpty());
    }
    
    @Test public void testAuthnAuthorities() {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        final ExternalAuthenticationContext eac = (ExternalAuthenticationContext) ac.addSubcontext(
                new ExternalAuthenticationContext(new ExternalAuthenticationImpl()), true);
        eac.setPrincipalName("jdoe");
        eac.getAuthenticatingAuthorities().addAll(Arrays.asList("foo", "bar", "baz"));

        final IdPAttribute mail = new IdPAttribute("mail");
        mail.setValues(Collections.singletonList(StringAttributeValue.valueOf("jdoe@example.org")));
        eac.getSubcontext(AttributeContext.class, true).setIdPAttributes(Collections.singletonList(mail));

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(ac.getAuthenticationResult());
        final Set<IdPAttributePrincipal> prin =
                ac.getAuthenticationResult().getSubject().getPrincipals(IdPAttributePrincipal.class);
        Assert.assertEquals(prin.size(), 1);
        
        final IdPAttribute copy = prin.iterator().next().getAttribute();
        Assert.assertEquals(copy.getId(), "mail");
        Assert.assertEquals(copy.getValues().size(), 1);
        Assert.assertEquals(copy.getValues().get(0).getNativeValue(), "jdoe@example.org");
    }
    
    private ReloadableService<AttributeFilter> getFilterService() throws ComponentInitializationException {
        
        final AttributeRule rule = new AttributeRule();
        rule.setId("mailRule");
        rule.setAttributeId("mail");
        rule.setMatcher(Matcher.MATCHES_ALL);
        rule.setIsDenyRule(false);
        rule.initialize();
        
        final AttributeIssuerPolicyRule policyRule = new AttributeIssuerPolicyRule();
        policyRule.setId("issuerRule");
        policyRule.setMatchString("foo");
        policyRule.initialize();
        
        final AttributeFilterPolicy policy = new AttributeFilterPolicy("mailPolicy",
                policyRule, Collections.singletonList(rule));
        policy.initialize();
        
        final AttributeFilterImpl filter = new AttributeFilterImpl("test", Collections.singletonList(policy));
        filter.setApplicationContext(new MockApplicationContext());
        filter.initialize();
        
        return new MockReloadableService<>(filter);
    }

}
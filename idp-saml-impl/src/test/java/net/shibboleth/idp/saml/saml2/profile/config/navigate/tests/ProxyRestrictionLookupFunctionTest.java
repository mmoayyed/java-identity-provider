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

package net.shibboleth.idp.saml.saml2.profile.config.navigate.tests;

import java.util.Collections;
import java.util.Set;

import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.saml2.profile.config.impl.BrowserSSOProfileConfiguration;
import net.shibboleth.idp.saml.saml2.profile.config.navigate.ProxyRestrictionLookupFunction;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.component.ComponentInitializationException;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit test for {@link ProxyRestrictionLookupFunction}. */
@SuppressWarnings("javadoc")
public class ProxyRestrictionLookupFunctionTest extends OpenSAMLInitBaseTestCase {
    
    private ProfileRequestContext prc;
    private SubjectContext sc;
    private BrowserSSOProfileConfiguration config;
    private ProxyRestrictionLookupFunction fn;
    private Pair<Integer,Set<String>> result;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        prc = new RequestContextBuilder()
                .setRelyingPartyProfileConfigurations(Collections.singletonList(new BrowserSSOProfileConfiguration()))
                .buildProfileRequestContext();
        config = (BrowserSSOProfileConfiguration) prc.getSubcontext(RelyingPartyContext.class).getConfiguration().getProfileConfiguration(
                prc, BrowserSSOProfileConfiguration.PROFILE_ID);
        prc.getSubcontext(RelyingPartyContext.class).setProfileConfig(config);
        sc = prc.getOrCreateSubcontext(SubjectContext.class);
        sc.getAuthenticationResults().put("test1", new AuthenticationResult("test1", new Subject()));
        sc.getAuthenticationResults().put("test2", new AuthenticationResult("test2", new Subject()));
        fn = new ProxyRestrictionLookupFunction();
    }
    
    @Test
    public void testNoPrincipals() {
        result = fn.apply(prc);
        Assert.assertNull(result.getFirst());
        Assert.assertTrue(result.getSecond().isEmpty());
    }

    @Test
    public void testOneEmptyPrincipal() {
        final ProxyAuthenticationPrincipal proxy = new ProxyAuthenticationPrincipal();
        sc.getAuthenticationResults().get("test2").getSubject().getPrincipals().add(proxy);
        
        result = fn.apply(prc);
        Assert.assertNull(result.getFirst());
        Assert.assertTrue(result.getSecond().isEmpty());
    }

    @Test
    public void testOneCount() {
        final ProxyAuthenticationPrincipal proxy = new ProxyAuthenticationPrincipal();
        proxy.setProxyCount(10);
        sc.getAuthenticationResults().get("test2").getSubject().getPrincipals().add(proxy);
        
        result = fn.apply(prc);
        Assert.assertEquals(result.getFirst(), Integer.valueOf(9));
        Assert.assertTrue(result.getSecond().isEmpty());
        
        proxy.setProxyCount(1);
        result = fn.apply(prc);
        Assert.assertEquals(result.getFirst(), Integer.valueOf(0));

        proxy.setProxyCount(0);
        result = fn.apply(prc);
        Assert.assertEquals(result.getFirst(), Integer.valueOf(0));
    }

    @Test
    public void testTwoCounts() {
        final ProxyAuthenticationPrincipal proxy1 = new ProxyAuthenticationPrincipal();
        proxy1.setProxyCount(10);
        sc.getAuthenticationResults().get("test1").getSubject().getPrincipals().add(proxy1);
        
        final ProxyAuthenticationPrincipal proxy2 = new ProxyAuthenticationPrincipal();
        proxy1.setProxyCount(5);
        sc.getAuthenticationResults().get("test2").getSubject().getPrincipals().add(proxy2);
        
        result = fn.apply(prc);
        Assert.assertEquals(result.getFirst(), Integer.valueOf(4));
        Assert.assertTrue(result.getSecond().isEmpty());
        
        proxy1.setProxyCount(1);
        proxy2.setProxyCount(1);
        result = fn.apply(prc);
        Assert.assertEquals(result.getFirst(), Integer.valueOf(0));

        proxy1.setProxyCount(0);
        proxy1.setProxyCount(5);
        result = fn.apply(prc);
        Assert.assertEquals(result.getFirst(), Integer.valueOf(0));
    }
    
    @Test
    public void testOneAudienceSet() {
        final ProxyAuthenticationPrincipal proxy = new ProxyAuthenticationPrincipal();
        proxy.setProxyCount(10);
        proxy.getAudiences().addAll(CollectionSupport.setOf("foo", "bar"));
        sc.getAuthenticationResults().get("test2").getSubject().getPrincipals().add(proxy);
        
        result = fn.apply(prc);
        Assert.assertEquals(result.getFirst(), Integer.valueOf(9));
        Assert.assertEquals(result.getSecond(), CollectionSupport.setOf("foo", "bar"));
    }

    @Test
    public void testTwoAudienceSets() {
        final ProxyAuthenticationPrincipal proxy1 = new ProxyAuthenticationPrincipal();
        proxy1.setProxyCount(10);
        proxy1.getAudiences().addAll(CollectionSupport.setOf("foo", "bar"));
        sc.getAuthenticationResults().get("test1").getSubject().getPrincipals().add(proxy1);

        final ProxyAuthenticationPrincipal proxy2 = new ProxyAuthenticationPrincipal();
        proxy2.getAudiences().addAll(CollectionSupport.setOf("foo", "bar"));
        sc.getAuthenticationResults().get("test2").getSubject().getPrincipals().add(proxy2);
        
        result = fn.apply(prc);
        Assert.assertEquals(result.getFirst(), Integer.valueOf(9));
        Assert.assertEquals(result.getSecond(), CollectionSupport.setOf("foo", "bar"));
        
        proxy1.getAudiences().clear();
        proxy1.getAudiences().add("bar");
        result = fn.apply(prc);
        Assert.assertEquals(result.getSecond(), CollectionSupport.singleton("bar"));
        
        proxy2.getAudiences().clear();
        proxy2.getAudiences().add("foo");
        result = fn.apply(prc);
        Assert.assertTrue(result.getSecond().isEmpty());
    }

    @Test
    public void testConfigOnly() {
        config.setProxyCount(5);
        config.setProxyAudiences(CollectionSupport.setOf("foo", "bar"));
        prc.removeSubcontext(sc);
        
        result = fn.apply(prc);
        Assert.assertEquals(result.getFirst(), Integer.valueOf(5));
        Assert.assertEquals(result.getSecond(), CollectionSupport.setOf("foo", "bar"));
    }

    @Test
    public void testJointCount() {
        config.setProxyCount(5);

        final ProxyAuthenticationPrincipal proxy1 = new ProxyAuthenticationPrincipal();
        proxy1.setProxyCount(10);
        sc.getAuthenticationResults().get("test1").getSubject().getPrincipals().add(proxy1);
        
        final ProxyAuthenticationPrincipal proxy2 = new ProxyAuthenticationPrincipal();
        proxy1.setProxyCount(5);
        sc.getAuthenticationResults().get("test2").getSubject().getPrincipals().add(proxy2);
        
        result = fn.apply(prc);
        Assert.assertEquals(result.getFirst(), Integer.valueOf(4));
        Assert.assertTrue(result.getSecond().isEmpty());
        
        config.setProxyCount(1);
        proxy1.setProxyCount(1);
        result = fn.apply(prc);
        Assert.assertEquals(result.getFirst(), Integer.valueOf(0));

        config.setProxyCount(0);
        proxy1.setProxyCount(3);
        result = fn.apply(prc);
        Assert.assertEquals(result.getFirst(), Integer.valueOf(0));
    }

    @Test
    public void testJointAudiences() {
        config.setProxyAudiences(CollectionSupport.setOf("foo", "bar"));

        final ProxyAuthenticationPrincipal proxy1 = new ProxyAuthenticationPrincipal();
        proxy1.getAudiences().addAll(CollectionSupport.setOf("foo", "bar"));
        sc.getAuthenticationResults().get("test1").getSubject().getPrincipals().add(proxy1);

        final ProxyAuthenticationPrincipal proxy2 = new ProxyAuthenticationPrincipal();
        proxy2.getAudiences().addAll(CollectionSupport.setOf("foo", "bar"));
        sc.getAuthenticationResults().get("test2").getSubject().getPrincipals().add(proxy2);
        
        result = fn.apply(prc);
        Assert.assertNull(result.getFirst());
        Assert.assertEquals(result.getSecond(), CollectionSupport.setOf("foo", "bar"));
        
        config.setProxyAudiences(CollectionSupport.setOf("foo", "baz"));
        result = fn.apply(prc);
        Assert.assertEquals(result.getSecond(), CollectionSupport.singleton("foo"));
        
        proxy2.getAudiences().clear();
        proxy2.getAudiences().addAll(CollectionSupport.setOf("foo", "bar", "baz"));
        result = fn.apply(prc);
        Assert.assertEquals(result.getSecond(), Set.of("foo"));
        
        proxy1.getAudiences().clear();
        proxy1.getAudiences().add("bar");
        result = fn.apply(prc);
        Assert.assertTrue(result.getSecond().isEmpty());
        Assert.assertEquals(result.getFirst(), Integer.valueOf(0));
    }

}
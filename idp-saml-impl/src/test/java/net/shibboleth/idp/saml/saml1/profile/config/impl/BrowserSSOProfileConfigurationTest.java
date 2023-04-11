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

package net.shibboleth.idp.saml.saml1.profile.config.impl;

import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;
import net.shibboleth.saml.profile.config.BasicSAMLArtifactConfiguration;
import net.shibboleth.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.shared.logic.ConstraintViolationException;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.logic.PredicateSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.*;

/** Unit test for {@link BrowserSSOProfileConfiguration}. */
@SuppressWarnings({"javadoc", "null"})
public class BrowserSSOProfileConfigurationTest {

    @Test
    public void testProfileId() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertEquals(config.getId(), BrowserSSOProfileConfiguration.PROFILE_ID);
    }

    @Test public void testSignAssertions() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        config.setSignAssertions(false);
        Assert.assertFalse(config.isSignAssertions(null));
    }

    @Test public void testAssertionLifetime() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertTrue(config.getAssertionLifetime(null).toMillis() > 0);

        config.setAssertionLifetime(Duration.ofMillis(100));
        Assert.assertEquals(config.getAssertionLifetime(null), Duration.ofMillis(100));

        try {
            config.setAssertionLifetime(Duration.ZERO);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }

        try {
            config.setAssertionLifetime(Duration.ofMillis(-100));
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }
    }

    @Test public void testIndirectAssertionLifetime() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        config.setAssertionLifetimeLookupStrategy(FunctionSupport.constant(Duration.ofMillis(500)));
        Assert.assertEquals(config.getAssertionLifetime(null), Duration.ofMillis(500));

        config.setAssertionLifetimeLookupStrategy(FunctionSupport.constant(null));
        try {
            config.getAssertionLifetime(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }
    }

    @Test public void testIncludeNotBefore() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertTrue(config.isIncludeConditionsNotBefore(null));

        config.setIncludeConditionsNotBefore(false);
        Assert.assertFalse(config.isIncludeConditionsNotBefore(null));
    }

    @Test public void testIndirectIncludeNotBefore() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        config.setIncludeConditionsNotBeforePredicate(PredicateSupport.alwaysFalse());
        Assert.assertFalse(config.isIncludeConditionsNotBefore(null));
    }

    @Test public void testAdditionalAudiencesForAssertion() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertNotNull(config.getAssertionAudiences(null));
        Assert.assertTrue(config.getAssertionAudiences(null).isEmpty());

        config.setAssertionAudiences(Arrays.asList("", null, " foo"));

        final Set<String> audiences = config.getAssertionAudiences(null);
        Assert.assertNotNull(audiences);
        Assert.assertEquals(audiences.size(), 1);
        Assert.assertTrue(audiences.contains("foo"));

        try {
            audiences.add("bar");
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            // expected this
        }

        config.setAssertionAudiences(null);
        Assert.assertNotNull(config.getAssertionAudiences(null));
        Assert.assertTrue(config.getAssertionAudiences(null).isEmpty());
    }

    @Test public void testIndirectAudiencesForAssertion() {
final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        final Set<String> audiences = new HashSet<>();
        audiences.add("foo");
        audiences.add("bar");
        config.setAssertionAudiencesLookupStrategy(FunctionSupport.constant(audiences));
        Assert.assertEquals(config.getAssertionAudiences(null), audiences);
    }
    
    @Test
    public void testResolveAttributes(){
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertTrue(config.isResolveAttributes(null));
        
        config.setResolveAttributes(false);
        Assert.assertFalse(config.isResolveAttributes(null));
    }

    @Test
    public void testIndirectResolveAttributes(){
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        config.setResolveAttributesPredicate(PredicateSupport.alwaysFalse());
        Assert.assertFalse(config.isResolveAttributes(null));
    }

    @Test
    public void testIncludeAttributeStatement(){
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertFalse(config.isIncludeAttributeStatement(null));

        config.setIncludeAttributeStatement(true);
        Assert.assertTrue(config.isIncludeAttributeStatement(null));
    }

    @Test
    public void testIndirectIncludeAttributeStatement(){
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        config.setIncludeAttributeStatementPredicate(PredicateSupport.alwaysTrue());
        Assert.assertTrue(config.isIncludeAttributeStatement(null));
    }

    @Test
    public void testArtifactConfiguration() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertNull(config.getArtifactConfiguration(null));

        final SAMLArtifactConfiguration artifactConfiguration = new BasicSAMLArtifactConfiguration();
        config.setArtifactConfiguration(artifactConfiguration);

        Assert.assertSame(config.getArtifactConfiguration(null), artifactConfiguration);
    }

    @Test
    public void testIndirectArtifactConfiguration() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        final SAMLArtifactConfiguration artifactConfiguration = new BasicSAMLArtifactConfiguration();
        config.setArtifactConfigurationLookupStrategy(
                FunctionSupport.<ProfileRequestContext,SAMLArtifactConfiguration>constant(artifactConfiguration));

        Assert.assertSame(config.getArtifactConfiguration(null), artifactConfiguration);
    }

    @Test
    public void testDefaultAuthenticationMethods() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertTrue(config.getDefaultAuthenticationMethods(null).isEmpty());

        final List<AuthenticationMethodPrincipal> principals = new ArrayList<>();
        principals.add(new AuthenticationMethodPrincipal("foo"));
        principals.add(new AuthenticationMethodPrincipal("bar"));

        config.setDefaultAuthenticationMethods(principals);
        Assert.assertEquals(config.getDefaultAuthenticationMethods(null), principals);
    }

    @Test
    public void testIndirectDefaultAuthenticationMethods() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        final List<AuthenticationMethodPrincipal> principals = new ArrayList<>();
        principals.add(new AuthenticationMethodPrincipal("foo"));
        principals.add(new AuthenticationMethodPrincipal("bar"));

        config.setDefaultAuthenticationMethodsLookupStrategy(FunctionSupport.constant(principals));
        Assert.assertEquals(config.getDefaultAuthenticationMethods(null), principals);
    }

    @Test
    public void testAuthenticationFlows() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertTrue(config.getAuthenticationFlows(null).isEmpty());

        final Set<String> flows = new HashSet<>();
        flows.add("foo");
        flows.add("bar");

        config.setAuthenticationFlows(flows);
        Assert.assertEquals(config.getAuthenticationFlows(null), flows);
    }

    @Test
    public void testIndirectAuthenticationFlows() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        final Set<String> flows = new HashSet<>();
        flows.add("foo");
        flows.add("bar");

        config.setAuthenticationFlowsLookupStrategy(FunctionSupport.constant(flows));
        Assert.assertEquals(config.getAuthenticationFlows(null), flows);
    }

    @Test
    public void testPostAuthenticationFlows() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertTrue(config.getPostAuthenticationFlows(null).isEmpty());

        final List<String> flows = new ArrayList<>();
        flows.add("foo");
        flows.add("bar");

        config.setPostAuthenticationFlows(flows);
        Assert.assertEquals(config.getPostAuthenticationFlows(null), flows);
    }

    @Test
    public void testIndirectPostAuthenticationFlows() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        final List<String> flows = new ArrayList<>();
        flows.add("foo");
        flows.add("bar");

        config.setPostAuthenticationFlowsLookupStrategy(FunctionSupport.constant(flows));
        Assert.assertEquals(config.getPostAuthenticationFlows(null), flows);
    }

    @Test
    public void testNameIDFormatPrecedence() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertTrue(config.getNameIDFormatPrecedence(null).isEmpty());

        final List<String> formats = new ArrayList<>();
        formats.add("foo");
        formats.add("bar");

        config.setNameIDFormatPrecedence(formats);
        Assert.assertEquals(config.getNameIDFormatPrecedence(null), formats);
    }

    @Test
    public void testIndirectNameIDFormatPrecedence() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        final List<String> formats = new ArrayList<>();
        formats.add("foo");
        formats.add("bar");

        config.setNameIDFormatPrecedenceLookupStrategy(FunctionSupport.constant(formats));
        Assert.assertEquals(config.getNameIDFormatPrecedence(null), formats);
    }

}
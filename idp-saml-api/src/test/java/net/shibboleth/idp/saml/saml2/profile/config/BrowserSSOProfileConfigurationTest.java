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

package net.shibboleth.idp.saml.saml2.profile.config;

import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.profile.config.BasicSAMLArtifactConfiguration;
import net.shibboleth.idp.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/** Unit test for {@link BrowserSSOProfileConfiguration}. */
public class BrowserSSOProfileConfigurationTest {

    @Test
    public void testProfileId() {
        Assert.assertEquals(BrowserSSOProfileConfiguration.PROFILE_ID, "http://shibboleth.net/ns/profiles/saml2/sso/browser");

        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertEquals(config.getId(), BrowserSSOProfileConfiguration.PROFILE_ID);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testResolveAttributes(){
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertTrue(config.resolveAttributes());
        
        config.setResolveAttributes(false);
        Assert.assertFalse(config.resolveAttributes());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testIndirectResolveAttributes(){
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        config.setResolveAttributesPredicate(Predicates.<ProfileRequestContext>alwaysFalse());
        Assert.assertFalse(config.resolveAttributes());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testIncludeAttributeStatement(){
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertTrue(config.includeAttributeStatement());

        config.setIncludeAttributeStatement(false);
        Assert.assertFalse(config.includeAttributeStatement());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testIndirectIncludeAttributeStatement(){
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        config.setIncludeAttributeStatementPredicate(Predicates.<ProfileRequestContext>alwaysFalse());
        Assert.assertFalse(config.includeAttributeStatement());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSkipEndpointValidationWhenSigned() {
        BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertFalse(config.skipEndpointValidationWhenSigned());

        config.setSkipEndpointValidationWhenSigned(true);
        Assert.assertTrue(config.skipEndpointValidationWhenSigned());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testIndirectEndpointValidationWhenSigned(){
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        config.setSkipEndpointValidationWhenSignedPredicate(Predicates.<ProfileRequestContext>alwaysTrue());
        Assert.assertTrue(config.skipEndpointValidationWhenSigned());
    }
    
    @Test
    public void testMaximumSPSessionLifeTime() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertEquals(config.getMaximumSPSessionLifetime(), 0);

        config.setMaximumSPSessionLifetime(1000);
        Assert.assertEquals(config.getMaximumSPSessionLifetime(), 1000);
    }
    
    @Test
    public void testIndirectMaximumSPSessionLifeTime() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        config.setMaximumSPSessionLifetimeLookupStrategy(FunctionSupport.<ProfileRequestContext,Long>constant(1000L));
        Assert.assertEquals(config.getMaximumSPSessionLifetime(), 1000);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testAllowingDelegation() {
        // Note: testing the deprecated boolean value variant
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertFalse(config.isAllowingDelegation());
        Assert.assertFalse(config.getAllowingDelegation());
        
        config.setAllowingDelegation(false);
        Assert.assertFalse(config.isAllowingDelegation());
        Assert.assertFalse(config.getAllowingDelegation());
        Assert.assertEquals(config.getAllowingDelegation(), Boolean.FALSE);

        config.setAllowingDelegation(true);
        Assert.assertTrue(config.isAllowingDelegation());
        Assert.assertTrue(config.getAllowingDelegation());
        Assert.assertEquals(config.getAllowingDelegation(), Boolean.TRUE);
    }
    
    @Test
    public void testAllowDelegation() {
        // Note: testing the newer predicate variant
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertNotNull(config.getAllowDelegation());
        
        final Predicate<ProfileRequestContext> predicate = Predicates.alwaysTrue();
        config.setAllowDelegation(predicate);
        Assert.assertSame(config.getAllowDelegation(), predicate);
        
        try {
            config.setAllowDelegation(null);
            Assert.fail("Null predicate should not have been allowed");
        } catch (ConstraintViolationException e) {
            // expected, do nothing 
        }
    }
    
    @Test
    public void testMaximumTokenDelegationChainLength(){
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertEquals(config.getMaximumTokenDelegationChainLength(), 1);
        
        config.setMaximumTokenDelegationChainLength(10);
        Assert.assertEquals(config.getMaximumTokenDelegationChainLength(), 10);
    }
    
    @Test
    public void testIndirectMaximumTokenDelegationChainLength(){
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        
        config.setMaximumTokenDelegationChainLengthLookupStrategy(FunctionSupport.<ProfileRequestContext,Long>constant(10L));
        Assert.assertEquals(config.getMaximumTokenDelegationChainLength(), 10);
    }

    @Test
    public void testArtifactConfiguration() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertNull(config.getArtifactConfiguration());

        final SAMLArtifactConfiguration artifactConfiguration = new BasicSAMLArtifactConfiguration();
        config.setArtifactConfiguration(artifactConfiguration);

        Assert.assertSame(config.getArtifactConfiguration(), artifactConfiguration);
    }

    @Test
    public void testIndirectArtifactConfiguration() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        final SAMLArtifactConfiguration artifactConfiguration = new BasicSAMLArtifactConfiguration();
        config.setArtifactConfigurationLookupStrategy(
                FunctionSupport.<ProfileRequestContext,SAMLArtifactConfiguration>constant(artifactConfiguration));

        Assert.assertSame(config.getArtifactConfiguration(), artifactConfiguration);
    }

    @Test
    public void testDefaultAuthenticationMethods() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertTrue(config.getDefaultAuthenticationMethods().isEmpty());

        final List<AuthnContextClassRefPrincipal> principals = new ArrayList<>();
        principals.add(new AuthnContextClassRefPrincipal("foo"));
        principals.add(new AuthnContextClassRefPrincipal("bar"));

        config.setDefaultAuthenticationMethods(principals);
        Assert.assertEquals(config.getDefaultAuthenticationMethods(), principals);
    }

    @Test
    public void testIndirectDefaultAuthenticationMethods() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        final List<AuthnContextClassRefPrincipal> principals = new ArrayList<>();
        principals.add(new AuthnContextClassRefPrincipal("foo"));
        principals.add(new AuthnContextClassRefPrincipal("bar"));

        config.setDefaultAuthenticationMethodsLookupStrategy(
                FunctionSupport.<ProfileRequestContext,Collection<AuthnContextClassRefPrincipal>>constant(principals));
        Assert.assertEquals(config.getDefaultAuthenticationMethods(), principals);
    }

    @Test
    public void testAuthenticationFlows() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertTrue(config.getAuthenticationFlows().isEmpty());

        final Set<String> flows = new HashSet<>();
        flows.add("foo");
        flows.add("bar");

        config.setAuthenticationFlows(flows);
        Assert.assertEquals(config.getAuthenticationFlows(), flows);
    }

    @Test
    public void testIndirectAuthenticationFlows() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        final Set<String> flows = new HashSet<>();
        flows.add("foo");
        flows.add("bar");

        config.setAuthenticationFlowsLookupStrategy(
                FunctionSupport.<ProfileRequestContext,Set<String>>constant(flows));
        Assert.assertEquals(config.getAuthenticationFlows(), flows);
    }

    @Test
    public void testPostAuthenticationFlows() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertTrue(config.getPostAuthenticationFlows().isEmpty());

        final List<String> flows = new ArrayList<>();
        flows.add("foo");
        flows.add("bar");

        config.setPostAuthenticationFlows(flows);
        Assert.assertEquals(config.getPostAuthenticationFlows(), flows);
    }

    @Test
    public void testIndirectPostAuthenticationFlows() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        final List<String> flows = new ArrayList<>();
        flows.add("foo");
        flows.add("bar");

        config.setPostAuthenticationFlowsLookupStrategy(
                FunctionSupport.<ProfileRequestContext,Collection<String>>constant(flows));
        Assert.assertEquals(config.getPostAuthenticationFlows(), flows);
    }

    @Test
    public void testNameIDFormatPrecedence() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        Assert.assertTrue(config.getNameIDFormatPrecedence().isEmpty());

        final List<String> formats = new ArrayList<>();
        formats.add("foo");
        formats.add("bar");

        config.setNameIDFormatPrecedence(formats);
        Assert.assertEquals(config.getNameIDFormatPrecedence(), formats);
    }

    @Test
    public void testIndirectNameIDFormatPrecedence() {
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();

        final List<String> formats = new ArrayList<>();
        formats.add("foo");
        formats.add("bar");

        config.setNameIDFormatPrecedenceLookupStrategy(
                FunctionSupport.<ProfileRequestContext,Collection<String>>constant(formats));
        Assert.assertEquals(config.getNameIDFormatPrecedence(), formats);
    }
}
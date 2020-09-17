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

package net.shibboleth.idp.saml.saml2.profile.config.navigate;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthnContextDeclRefPrincipal;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit test for {@link ProxyAwareDefaultAuthenticationMethodsLookupFunction}. */
public class ProxyAwareDefaultAuthenticationMethodsLookupFunctionTest extends OpenSAMLInitBaseTestCase {
    
    private ProfileRequestContext prc1,prc2;
    private AuthenticationContext ac;
    private RequestedPrincipalContext rpc;
    private ProxyAwareDefaultAuthenticationMethodsLookupFunction fn;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        prc1 = new RequestContextBuilder().buildProfileRequestContext();
        ac = prc1.getSubcontext(AuthenticationContext.class, true);
        prc2 = ac.getSubcontext(ProfileRequestContext.class, true);
        rpc = ac.getSubcontext(RequestedPrincipalContext.class, true);
        fn = new ProxyAwareDefaultAuthenticationMethodsLookupFunction();
    }
    
    @Test
    public void testEmptyTree() {
        final Collection<AuthnContextClassRefPrincipal> principals = fn.apply(prc1);
        Assert.assertTrue(principals.isEmpty());
    }
    
    @Test
    public void testNonProxy() {
        rpc.setRequestedPrincipals(List.of(
                new AuthnContextClassRefPrincipal("foo"),
                new AuthnContextClassRefPrincipal("bar")));
        
        final Collection<AuthnContextClassRefPrincipal> principals = fn.apply(prc1);
        Assert.assertTrue(principals.isEmpty());
    }
    
    @Test
    public void testPassthrough() {
        rpc.setRequestedPrincipals(List.of(
                new AuthnContextClassRefPrincipal("foo"),
                new AuthnContextClassRefPrincipal("bar")));
        
        final Collection<AuthnContextClassRefPrincipal> principals = fn.apply(prc2);
        Assert.assertEquals(principals, rpc.getRequestedPrincipals());
    }

    @Test
    public void testMapped() {
        rpc.setRequestedPrincipals(List.of(
                new AuthnContextClassRefPrincipal("foo"),
                new AuthnContextClassRefPrincipal("bar"),
                new AuthenticationMethodPrincipal("baz")));
        
        final Map<Principal,Collection<Principal>> mappings = new HashMap<>();
        mappings.put(new AuthnContextClassRefPrincipal("foo"), Collections.emptyList());
        mappings.put(new AuthenticationMethodPrincipal("baz"),
                List.of(new AuthnContextClassRefPrincipal("frobnitz"),
                        new AuthnContextDeclRefPrincipal("grue"),
                        new AuthnContextClassRefPrincipal("zorkmid")));
        
        fn.setMappings(mappings);
        
        final Collection<AuthnContextClassRefPrincipal> principals = fn.apply(prc2);
        Assert.assertEquals(principals.stream().map(p -> p.getName()).collect(Collectors.toUnmodifiableList()),
                List.of("bar", "frobnitz", "zorkmid"));
    }

}
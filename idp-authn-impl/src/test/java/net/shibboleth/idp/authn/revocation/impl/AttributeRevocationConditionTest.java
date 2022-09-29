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

package net.shibboleth.idp.authn.revocation.impl;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceableComponent;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link AttributeRevocationCondition} unit test. */
public class AttributeRevocationConditionTest extends BaseAuthenticationContextTest {
    
    private Collection<Instant> revocationsToResolve;
    
    private AttributeRevocationCondition condition; 

    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        condition = new AttributeRevocationCondition();
        condition.setPrincipalNameLookupStrategy(FunctionSupport.constant("jdoe"));
        condition.setAttributeResolver(new MockResolver());
        condition.setAttributeId("revocation");
        condition.initialize();
        
        authenticationFlows.get(1).setRevocationCondition(condition);
    }
    
    @AfterMethod
    public void tearDown() {
        condition.destroy();
    }
    
    
    @Test public void testNotRevoked() {
        final AuthenticationResult active = authenticationFlows.get(1).newAuthenticationResult(new Subject());
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active));

        Assert.assertTrue(active.test(prc));
    }
    
    @Test public void testRevoked() {
        final AuthenticationResult active = authenticationFlows.get(1).newAuthenticationResult(new Subject());
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active));

        revocationsToResolve = Collections.singletonList(Instant.now().plusSeconds(3600));
        
        Assert.assertFalse(active.test(prc));
    }

    @Test public void testPastRevoked() {
        final AuthenticationResult active = authenticationFlows.get(1).newAuthenticationResult(new Subject());
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active));

        revocationsToResolve = Collections.singletonList(Instant.now().minusSeconds(3600));
        
        Assert.assertTrue(active.test(prc));
    }

    /**
     * Mock attribute source.
     */
    private class MockResolver implements ReloadableService<AttributeResolver> {

        /** {@inheritDoc} */
        public boolean isInitialized() {
            return true;
        }

        /** {@inheritDoc} */
        public void initialize() throws ComponentInitializationException {            
        }

        /** {@inheritDoc} */
        public Instant getLastSuccessfulReloadInstant() {
            return null;
        }

        /** {@inheritDoc} */
        public Instant getLastReloadAttemptInstant() {
            return null;
        }

        /** {@inheritDoc} */
        public Throwable getReloadFailureCause() {
            return null;
        }

        /** {@inheritDoc} */
        public void reload() {
        }

        /** {@inheritDoc} */
        public ServiceableComponent<AttributeResolver> getServiceableComponent() {
            return new ServiceableComponent<AttributeResolver>() {

                public AttributeResolver getComponent() {
                    return new AttributeResolver() {

                        public String getId() {
                            return "test";
                        }

                        public void resolveAttributes(AttributeResolutionContext resolutionContext)
                                throws ResolutionException {
                            if ("jdoe".equals(resolutionContext.getPrincipal()) && revocationsToResolve != null) {
                                final IdPAttribute attr = new IdPAttribute("revocation");
                                attr.setValues(
                                        revocationsToResolve.stream()
                                            .map(i -> StringAttributeValue.valueOf(Long.toString(i.getEpochSecond())))
                                            .collect(Collectors.toUnmodifiableList())
                                        );
                                resolutionContext.setResolvedIdPAttributes(Collections.singletonList(attr));
                            }
                        }
                    };
                }

                public void close() {
                }

            };
        }
        
    }
    
}

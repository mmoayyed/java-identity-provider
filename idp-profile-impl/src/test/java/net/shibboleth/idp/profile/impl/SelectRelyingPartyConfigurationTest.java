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

package net.shibboleth.idp.profile.impl;

import java.time.Instant;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.security.config.SecurityConfiguration;
import org.opensaml.security.credential.Credential;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.profile.relyingparty.RelyingPartyConfigurationResolver;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.AbstractIdentifiedInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceableComponent;

/** {@link SelectRelyingPartyConfiguration} unit test. */
public class SelectRelyingPartyConfigurationTest {

    /**
     * Test action with no resolver.
     * 
     * @throws ComponentInitializationException
     */
    @Test(expectedExceptions = ComponentInitializationException.class) public void testNoResolver()
            throws ComponentInitializationException {
        final SelectRelyingPartyConfiguration action = new SelectRelyingPartyConfiguration();
        action.initialize();
    }

    /**
     * Test that the action errors out properly if there is no relying party context.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoRelyingPartyContext() throws Exception {
        final RequestContext src = new RequestContextBuilder().buildRequestContext();
        final ProfileRequestContext prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        assert prc != null;
        prc.removeSubcontext(RelyingPartyContext.class);

        final SelectRelyingPartyConfiguration action = new SelectRelyingPartyConfiguration();
        action.setRelyingPartyConfigurationResolver(new MockResolver(null, null));
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertEvent(event, IdPEventIds.INVALID_RELYING_PARTY_CTX);
    }

    /**
     * Test that the action errors out properly if there is no relying party configuration.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoRelyingPartyConfiguration() throws Exception {
        final RequestContext src = new RequestContextBuilder().buildRequestContext();
        final ProfileRequestContext prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        assert prc != null;
        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx != null;
        rpCtx.setConfiguration(null);

        final SelectRelyingPartyConfiguration action = new SelectRelyingPartyConfiguration();
        action.setRelyingPartyConfigurationResolver(new MockResolver(null, null));
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertEvent(event, IdPEventIds.INVALID_RELYING_PARTY_CONFIG);
    }

    /**
     * Test that the action errors out properly if the relying party configuration can not be resolved.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testUnableToResolveRelyingPartyConfiguration() throws Exception {
        final RequestContext src = new RequestContextBuilder().buildRequestContext();
        final ProfileRequestContext prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        assert prc != null;
        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx != null;
        rpCtx.setConfiguration(null);

        final var config = new net.shibboleth.profile.relyingparty.RelyingPartyConfiguration();
        config.setId("foo");
        config.setResponderId("http://idp.example.org");
        config.setDetailedErrors(true);
        config.initialize();

        final SelectRelyingPartyConfiguration action = new SelectRelyingPartyConfiguration();
        action.setRelyingPartyConfigurationResolver(new MockResolver(config, new ResolverException()));
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertEvent(event, IdPEventIds.INVALID_RELYING_PARTY_CONFIG);
    }

    /**
     * Test that the action resolves the relying party and proceeds properly.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testResolveRelyingPartyConfiguration() throws Exception {
        final RequestContext src = new RequestContextBuilder().buildRequestContext();
        final ProfileRequestContext prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        assert prc != null;
        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx != null;
        rpCtx.setConfiguration(null);

        final var config = new net.shibboleth.profile.relyingparty.RelyingPartyConfiguration();
        config.setId("foo");
        config.setResponderId("http://idp.example.org");
        config.setDetailedErrors(true);
        config.initialize();

        final SelectRelyingPartyConfiguration action = new SelectRelyingPartyConfiguration();
        action.setRelyingPartyConfigurationResolver(new MockResolver(config, null));
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        
        final RelyingPartyConfiguration resolvedConfig = (RelyingPartyConfiguration) rpCtx.getConfiguration();
        assert resolvedConfig != null;
        Assert.assertEquals(resolvedConfig.getId(), config.getId());
        Assert.assertEquals(resolvedConfig.getIssuer(prc), config.getIssuer(prc));
        Assert.assertEquals(resolvedConfig.getProfileConfigurations(prc), config.getProfileConfigurations(prc));
    }

    /**
     * A resolver that returns a relying party configuration or throws an exception. */
    private class MockResolver extends AbstractIdentifiedInitializableComponent
                implements ReloadableService<RelyingPartyConfigurationResolver>,
                    ServiceableComponent<RelyingPartyConfigurationResolver>, RelyingPartyConfigurationResolver {

        /** The relying party configuration to be returned. */
        @Nullable RelyingPartyConfiguration configuration;

        /** Exception thrown by resolution attempts. */
        private ResolverException exception;

        /**
         * Constructor.
         * 
         * @param relyingPartyConfiguration the relying party configuration to be returned
         * @param resolverException exception thrown on failed resolution
         *            
         * @throws ComponentInitializationException 
         */
        public MockResolver(@Nullable RelyingPartyConfiguration relyingPartyConfiguration,
                @Nullable final ResolverException resolverException) throws ComponentInitializationException {
            configuration = relyingPartyConfiguration;
            exception = resolverException;
            setId("mock");
            initialize();
        }

        /** {@inheritDoc} */
        @Override public @Nonnull Iterable<RelyingPartyConfiguration> resolve(final @Nullable CriteriaSet criteria)
                throws ResolverException {
            if (exception != null) {
                throw exception;
            }
            assert configuration != null;
            return CollectionSupport.singleton(configuration);
        }

        /** {@inheritDoc} */
        @Override public RelyingPartyConfiguration resolveSingle(final @Nullable CriteriaSet criteria)
                throws ResolverException {
            if (exception != null) {
                throw exception;
            }
            return configuration;
        }

        /** {@inheritDoc} */
        @Override public SecurityConfiguration getDefaultSecurityConfiguration(@Nonnull String profileId) {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public @Nonnull Collection<Credential> getSigningCredentials() {
            return CollectionSupport.emptyList();
        }

        /** {@inheritDoc} */
        @Override
        public @Nonnull Collection<Credential> getEncryptionCredentials() {
            return CollectionSupport.emptyList();
        }

        /** {@inheritDoc} */
        @Override
        public Instant getLastSuccessfulReloadInstant() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Instant getLastReloadAttemptInstant() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Throwable getReloadFailureCause() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public void reload() {
            
        }

        /** {@inheritDoc} */
        @Override
        public @Nonnull ServiceableComponent<RelyingPartyConfigurationResolver> getServiceableComponent() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public @Nonnull RelyingPartyConfigurationResolver getComponent() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public void close() {
        }
    }

}
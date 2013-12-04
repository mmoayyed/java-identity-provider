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

import java.util.Collections;

import javax.annotation.Nullable;

import net.shibboleth.idp.profile.EventIds;

import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.resolver.Resolver;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link SelectRelyingPartyConfiguration} unit test. */
public class SelectRelyingPartyConfigurationTest {

    /** Test that the action errors out properly if there is no relying party context. */
    @Test public void testNoRelyingPartyContext() throws Exception {
        ProfileRequestContext profileCtx = new ProfileRequestContext();

        Resolver<RelyingPartyConfiguration, ProfileRequestContext> resolver = new MockResolver(null, null);

        SelectRelyingPartyConfiguration action = new SelectRelyingPartyConfiguration(resolver);
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);

        ActionTestingSupport.assertEvent(profileCtx, EventIds.INVALID_RELYING_PARTY_CTX);
    }

    /** Test that the action errors out properly if there is no relying party configuration. */
    @Test public void testNoRelyingPartyConfiguration() throws Exception {
        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        Resolver<RelyingPartyConfiguration, ProfileRequestContext> resolver = new MockResolver(null, null);

        SelectRelyingPartyConfiguration action = new SelectRelyingPartyConfiguration(resolver);
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);

        ActionTestingSupport.assertEvent(profileCtx, EventIds.INVALID_RELYING_PARTY_CONFIG);
    }

    /** Test that the action errors out properly if the relying party configuration can not be resolved. */
    @Test public void testUnableToResolveRelyingPartyConfiguration() throws Exception {
        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        RelyingPartyConfiguration config =
                new RelyingPartyConfiguration("foo", "http://idp.example.org", Collections.EMPTY_LIST);

        Resolver<RelyingPartyConfiguration, ProfileRequestContext> resolver =
                new MockResolver(config, new ResolverException());

        SelectRelyingPartyConfiguration action = new SelectRelyingPartyConfiguration(resolver);
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);

        ActionTestingSupport.assertEvent(profileCtx, EventIds.INVALID_RELYING_PARTY_CONFIG);
    }

    /** Test that the action resolves the relying party and proceeds properly. */
    @Test public void testResolveRelyingPartyConfiguration() throws Exception {
        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        RelyingPartyConfiguration config =
                new RelyingPartyConfiguration("foo", "http://idp.example.org", Collections.EMPTY_LIST);

        Resolver<RelyingPartyConfiguration, ProfileRequestContext> resolver = new MockResolver(config, null);

        SelectRelyingPartyConfiguration action = new SelectRelyingPartyConfiguration(resolver);
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);

        ActionTestingSupport.assertProceedEvent(profileCtx);

        RelyingPartyConfiguration resolvedConfig =
                profileCtx.getSubcontext(RelyingPartyContext.class).getConfiguration();
        Assert.assertEquals(resolvedConfig.getId(), config.getId());
        Assert.assertEquals(resolvedConfig.getResponderEntityId(), config.getResponderEntityId());
        Assert.assertEquals(resolvedConfig.getProfileConfigurations(), config.getProfileConfigurations());
    }

    /** A resolver that returns a relying party configuration or throws an exception. */
    private class MockResolver extends AbstractIdentifiableInitializableComponent implements
            Resolver<RelyingPartyConfiguration, ProfileRequestContext> {

        /** The relying party configuration to be returned. */
        private RelyingPartyConfiguration configuration;

        /** Exception thrown by {@link #resolve(ProfileRequestContext)} and {@link #resolveSingle(ProfileRequestContext)} */
        private ResolverException exception;

        /**
         * Constructor.
         * 
         * @param relyingPartyConfiguration the relying party configuration to be returned
         * @param resolverException exception thrown by {@link #resolve(ProfileRequestContext)} and
         *            {@link #resolveSingle(ProfileRequestContext)}
         */
        public MockResolver(@Nullable final RelyingPartyConfiguration relyingPartyConfiguration,
                @Nullable final ResolverException resolverException) {
            configuration = relyingPartyConfiguration;
            exception = resolverException;
        }

        /** {@inheritDoc} */
        public Iterable<RelyingPartyConfiguration> resolve(final ProfileRequestContext context)
                throws ResolverException {
            if (exception != null) {
                throw exception;
            }
            return Collections.singleton(configuration);
        }

        /** {@inheritDoc} */
        public RelyingPartyConfiguration resolveSingle(final ProfileRequestContext context) throws ResolverException {
            if (exception != null) {
                throw exception;
            }
            return configuration;
        }
    }
}

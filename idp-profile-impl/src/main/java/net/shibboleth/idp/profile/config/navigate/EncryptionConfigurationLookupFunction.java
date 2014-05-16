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

package net.shibboleth.idp.profile.config.navigate;

import java.util.List;

import javax.annotation.Nullable;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.config.SecurityConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.relyingparty.RelyingPartyConfigurationResolver;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.xmlsec.EncryptionConfiguration;
import org.opensaml.xmlsec.SecurityConfigurationSupport;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * A function that returns a {@link EncryptionConfiguration} list using injected lookup strategies.
 * 
 * <p>If a specific setting is unavailable, a null value is returned.</p>
 */
public class EncryptionConfigurationLookupFunction
        implements Function<ProfileRequestContext,List<EncryptionConfiguration>> {

    /** A resolver for default security configurations. */
    @Nullable private RelyingPartyConfigurationResolver rpResolver;
    
    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nullable private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Constructor. */
    public EncryptionConfigurationLookupFunction() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
    }

    /**
     * Set the resolver for default security configurations.
     * 
     * @param resolver the resolver to use
     */
    public void setRelyingPartyConfigurationResolver(@Nullable final RelyingPartyConfigurationResolver resolver) {
        rpResolver = resolver;
    }
    
    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nullable final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        relyingPartyContextLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public List<EncryptionConfiguration> apply(@Nullable final ProfileRequestContext input) {
        
        final List<EncryptionConfiguration> configs = Lists.newArrayList();
        
        configs.add(SecurityConfigurationSupport.getGlobalEncryptionConfiguration());
        
        // Check for a per-profile default (relying party independent) config.
        if (input != null && rpResolver != null) {
            final SecurityConfiguration defaultConfig =
                    rpResolver.getDefaultSecurityConfiguration(input.getProfileId());
            if (defaultConfig != null && defaultConfig.getEncryptionConfiguration() != null) {
                configs.add(defaultConfig.getEncryptionConfiguration());
            }
        }

        if (input != null && relyingPartyContextLookupStrategy != null) {
            final RelyingPartyContext rpc = relyingPartyContextLookupStrategy.apply(input);
            if (rpc != null) {
                final ProfileConfiguration pc = rpc.getProfileConfig();
                if (pc != null && pc.getSecurityConfiguration() != null) {
                    configs.add(pc.getSecurityConfiguration().getEncryptionConfiguration());
                }
            }
        }
        
        return configs;
    }

}
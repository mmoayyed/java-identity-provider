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

package net.shibboleth.idp.profile.config.navigate.messaging;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.security.httpclient.HttpClientSecurityConfiguration;
import org.opensaml.security.httpclient.HttpClientSecuritySupport;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.config.SecurityConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.messaging.AbstractRelyingPartyLookupFunction;
import net.shibboleth.idp.relyingparty.RelyingPartyConfigurationResolver;

/**
 * A {@link MessageContext} function that returns a {@link HttpClientSecurityConfiguration} list 
 * by way of various lookup strategies.
 * 
 * <p>If a specific setting is unavailable, a null value is returned.</p>
 */
public class HttpClientSecurityConfigurationLookupFunction
        extends AbstractRelyingPartyLookupFunction<List<HttpClientSecurityConfiguration>> {
    
    /** A resolver for default security configurations. */
    @Nullable private RelyingPartyConfigurationResolver rpResolver;

    /**
     * Set the resolver for default security configurations.
     * 
     * @param resolver the resolver to use
     */
    public void setRelyingPartyConfigurationResolver(@Nullable final RelyingPartyConfigurationResolver resolver) {
        rpResolver = resolver;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public List<HttpClientSecurityConfiguration> apply(@Nullable final MessageContext input) {
        
        final List<HttpClientSecurityConfiguration> configs = new ArrayList<>();
        
        final RelyingPartyContext rpc = getRelyingPartyContextLookupStrategy().apply(input);
        if (rpc != null) {
            final ProfileConfiguration pc = rpc.getProfileConfig();
            if (pc != null) {
                final SecurityConfiguration sc =
                        pc.getSecurityConfiguration(this.getProfileRequestContextLookupStrategy().apply(input));
                if (sc != null && sc.getHttpClientSecurityConfiguration() != null) {
                    configs.add(sc.getHttpClientSecurityConfiguration());
                }
            }
            
            // Check for a per-profile default (relying party independent) config.
            if (pc != null) {
                final String id = pc.getId();
                if (id != null && rpResolver != null) {
                    final SecurityConfiguration defaultConfig = rpResolver.getDefaultSecurityConfiguration(id);
                    if (defaultConfig != null && defaultConfig.getHttpClientSecurityConfiguration() != null) {
                        configs.add(defaultConfig.getHttpClientSecurityConfiguration());
                    }
                }
            }
        }

        configs.add(HttpClientSecuritySupport.getGlobalHttpClientSecurityConfiguration());
        
        return configs;
    }

}
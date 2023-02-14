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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.relyingparty.RelyingPartyConfigurationResolver;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceException;
import net.shibboleth.idp.profile.context.navigate.AbstractRelyingPartyLookupFunction;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.security.config.SecurityConfiguration;
import org.opensaml.xmlsec.SignatureValidationConfiguration;
import org.opensaml.xmlsec.config.XMLSecurityConfiguration;
import org.opensaml.xmlsec.SecurityConfigurationSupport;

/**
 * A function that returns a {@link SignatureValidationConfiguration} list by way of various lookup strategies.
 * 
 * <p>If a specific setting is unavailable, a null value is returned.</p>
 */
public class SignatureValidationConfigurationLookupFunction
        extends AbstractRelyingPartyLookupFunction<List<SignatureValidationConfiguration>> {

    /** A resolver for default security configurations. */
    @Nullable private ReloadableService<RelyingPartyConfigurationResolver> rpResolver;
    
    /**
     * Set the resolver for default security configurations.
     * 
     * @param resolver the resolver to use
     */
    public void setRelyingPartyConfigurationResolver(
            @Nullable final ReloadableService<RelyingPartyConfigurationResolver> resolver) {
        rpResolver = resolver;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public List<SignatureValidationConfiguration> apply(@Nullable final ProfileRequestContext input) {
        
        final List<SignatureValidationConfiguration> configs = new ArrayList<>();
        
        final RelyingPartyContext rpc = getRelyingPartyContextLookupStrategy().apply(input);
        if (rpc != null) {
            final ProfileConfiguration pc = rpc.getProfileConfig();
            if (pc != null) {
                final SecurityConfiguration sc = pc.getSecurityConfiguration(input);
                if (sc instanceof XMLSecurityConfiguration xsc && xsc.getSignatureValidationConfiguration() != null) {
                    configs.add(xsc.getSignatureValidationConfiguration());
                }
            }
        }
        
        // Check for a per-profile default (relying party independent) config.
        if (input != null && rpResolver != null) {
            try {
                final SecurityConfiguration defaultConfig =
                        rpResolver.getServiceableComponent().getComponent().getDefaultSecurityConfiguration(
                                input.getProfileId());
                if (defaultConfig instanceof XMLSecurityConfiguration xsc &&
                        xsc.getSignatureValidationConfiguration() != null) {
                    configs.add(xsc.getSignatureValidationConfiguration());
                }
            } catch (final ServiceException e) {
                
            }
        }

        configs.add(SecurityConfigurationSupport.getGlobalSignatureValidationConfiguration());
        
        return configs;
    }

}
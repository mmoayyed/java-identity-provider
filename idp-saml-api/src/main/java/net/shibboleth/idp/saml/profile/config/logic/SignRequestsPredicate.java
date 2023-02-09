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

package net.shibboleth.idp.saml.profile.config.logic;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.logic.AbstractRelyingPartyPredicate;
import net.shibboleth.idp.saml.profile.config.SAMLProfileConfiguration;
import net.shibboleth.idp.saml.profile.context.navigate.SAMLMetadataContextLookupFunction;
import net.shibboleth.shared.logic.Constraint;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;

/**
 * A predicate implementation that forwards to 
 * {@link SAMLProfileConfiguration#isSignRequests(ProfileRequestContext)}
 * or follows {@link IDPSSODescriptor#getWantAuthnRequestsSigned()} if so configured.
 */
public class SignRequestsPredicate extends AbstractRelyingPartyPredicate {
    
    /** Whether to override the result based on the WantAuthnRequestsSigned flag in SAML metadata. */
    private boolean honorMetadata;
    
    /** Lookup strategy for {@link SAMLMetadataContext}. */
    private Function<ProfileRequestContext,SAMLMetadataContext> metadataContextLookupStrategy;
    
    /** Constructor. */
    public SignRequestsPredicate() {
        metadataContextLookupStrategy = new SAMLMetadataContextLookupFunction();
    }
    
    /**
     * Set whether to override the result based on the WantAuthnRequestsSigned flag in SAML metadata.
     * 
     * <p>Defaults to false.</p>
     * 
     * @param flag flag to set
     * 
     * @since 4.0.0
     */
    public void setHonorMetadata(final boolean flag) {
        honorMetadata = flag;
    }

    /**
     * Set lookup strategy for {@link SAMLMetadataContext}.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.0.0
     */
    public void setMetadataContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLMetadataContext> strategy) {
        metadataContextLookupStrategy = Constraint.isNotNull(strategy,
                "SAMLMetadataContext lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input) {

        if (honorMetadata) {
            final SAMLMetadataContext metadataCtx = metadataContextLookupStrategy.apply(input);
            if (metadataCtx != null) {
                final RoleDescriptor role = metadataCtx.getRoleDescriptor();
                if (role instanceof IDPSSODescriptor) {
                    final Boolean flag = ((IDPSSODescriptor) role).getWantAuthnRequestsSigned();
                    if (flag != null && flag.booleanValue()) {
                        return true;
                    }
                }
            }
        }
        
        final RelyingPartyContext rpc = getRelyingPartyContextLookupStrategy().apply(input);
        if (rpc != null) {
            final ProfileConfiguration pc = rpc.getProfileConfig();
            if (pc != null && pc instanceof SAMLProfileConfiguration) {
                return ((SAMLProfileConfiguration) pc).isSignRequests(input);
            }
        }
        
        return false;
    }

}
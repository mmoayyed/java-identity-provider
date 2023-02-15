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

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;

import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.context.logic.AbstractRelyingPartyPredicate;
import net.shibboleth.saml.profile.context.navigate.SAMLMetadataContextLookupFunction;
import net.shibboleth.idp.saml.profile.config.SAMLProfileConfiguration;
import net.shibboleth.shared.logic.Constraint;

/** A predicate implementation that forwards to
 * {@link SAMLProfileConfiguration#isSignAssertions(ProfileRequestContext)}.
 * or follows {@link SPSSODescriptor#getWantAssertionsSigned()} if so configured.*/
public class SignAssertionsPredicate extends AbstractRelyingPartyPredicate {

    /** Whether to override the result based on the WantAssertionsSigned flag in SAML metadata. */
    private boolean honorMetadata;
    
    /** Lookup strategy for {@link SAMLMetadataContext}. */
    private Function<ProfileRequestContext,SAMLMetadataContext> metadataContextLookupStrategy;
    
    /** Constructor. */
    public SignAssertionsPredicate() {
        honorMetadata = true;
        metadataContextLookupStrategy = new SAMLMetadataContextLookupFunction();
    }
    
    /**
     * Set whether to override the result based on the WantAssertionsSigned flag in SAML metadata.
     * 
     * @param flag flag to set
     */
    public void setHonorMetadata(final boolean flag) {
        honorMetadata = flag;
    }
    
    /**
     * Set lookup strategy for {@link SAMLMetadataContext}.
     * 
     * @param strategy lookup strategy
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
                if (role instanceof SPSSODescriptor) {
                    final Boolean flag = ((SPSSODescriptor) role).getWantAssertionsSigned();
                    if (flag != null && flag.booleanValue()) {
                        return true;
                    }
                }
            }
        }
        
        final RelyingPartyContext rpc = getRelyingPartyContext(input);
        if (rpc != null) {
            final ProfileConfiguration pc = rpc.getProfileConfig();
            if (pc instanceof SAMLProfileConfiguration) {
                return ((SAMLProfileConfiguration) pc).isSignAssertions(input);
            }
        }

        return false;
    }

}
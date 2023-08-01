/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.nameid;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameIDPolicy;

import com.google.common.base.Strings;

import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.shared.logic.Constraint;

/**
 * IdP-specific base class for SAML 2.0 NameID generation that extends the OpenSAML base class with support for
 * {@link BrowserSSOProfileConfiguration#getSPNameQualifier(org.opensaml.profile.context.ProfileRequestContext)}.
 * 
 * @since 5.0.0
 */
public class AbstractSAML2NameIDGenerator extends org.opensaml.saml.saml2.profile.AbstractSAML2NameIDGenerator {

    /** Strategy function to lookup RelyingPartyContext. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Strategy used to locate an {@link AuthnRequest} to check. */
    @Nonnull private Function<ProfileRequestContext,AuthnRequest> requestLookupStrategy;

    /**
     * Constructor.
     */
    public AbstractSAML2NameIDGenerator() {
        requestLookupStrategy = new MessageLookup<>(AuthnRequest.class).compose(new InboundMessageContextLookup());
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class); 
    }

    /**
     * Set the strategy used to locate the {@link AuthnRequest} to check for a
     * {@link org.opensaml.saml.saml2.core.NameIDPolicy}.
     * 
     * @param strategy lookup strategy
     */
    public void setRequestLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,AuthnRequest> strategy) {
        checkSetterPreconditions();
    
        requestLookupStrategy = Constraint.isNotNull(strategy, "AuthnRequest lookup strategy cannot be null");
    }

    /**
     * Set the lookup strategy to use to locate the {@link RelyingPartyContext}.
     * 
     * @param strategy lookup function to use
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    @Nullable protected String getEffectiveSPNameQualifier(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        // Override the default behavior if the SP specifies a qualifier in its request,
        // matching the original base class behavior. SP request trumps local configuration.
        final AuthnRequest request = requestLookupStrategy.apply(profileRequestContext);
        if (request != null) {
            final NameIDPolicy policy = request.getNameIDPolicy();
            if (policy != null) {
                final String qual = policy.getSPNameQualifier();
                if (!Strings.isNullOrEmpty(qual)) {
                    return qual;
                }
            }
        }
        
        final RelyingPartyContext rpContext = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpContext != null) {
            if (rpContext.getProfileConfig() instanceof BrowserSSOProfileConfiguration sso) {
                final String qual = sso.getSPNameQualifier(profileRequestContext);
                if (qual != null) {
                    return qual;
                }
            }
        }
        
        return super.getEffectiveSPNameQualifier(profileRequestContext);
    }
    
}
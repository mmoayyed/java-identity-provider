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

package net.shibboleth.idp.saml.profile.config.logic;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.saml.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.context.logic.AbstractRelyingPartyPredicate;
import net.shibboleth.shared.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;
import org.opensaml.saml.common.binding.BindingDescriptor;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;

/**
 * A predicate that evaluates a SSO {@link ProfileRequestContext} and determines whether an attribute statement
 * should be included in the outgoing assertion.
 * 
 * <p>The "includeAttributeStatement" profile configuration flag is the main setting governing this decision,
 * but is overridden to "true" in the case that the outgoing {@link SAMLBindingContext} indicates the outbound
 * binding is an artifact mechanism.</p> 
 */
public class IncludeAttributeStatementPredicate extends AbstractRelyingPartyPredicate {
    
    /** Strategy function for access to {@link SAMLBindingContext} to populate. */
    @Nonnull private Function<ProfileRequestContext,SAMLBindingContext> bindingContextLookupStrategy;
    
    /** Constructor. */
    public IncludeAttributeStatementPredicate() {
        bindingContextLookupStrategy = new ChildContextLookup<>(SAMLBindingContext.class).compose(
                new OutboundMessageContextLookup());
    }

    /**
     * Set lookup strategy for {@link SAMLBindingContext} to examine.
     * 
     * @param strategy  lookup strategy
     */
    public void setBindingContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLBindingContext> strategy) {
        bindingContextLookupStrategy = Constraint.isNotNull(strategy,
                "SAMLBindingContext lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input) {
        
        // Check for an artifact binding.
        if (isArtifactBinding(input)) {
            return true;
        }
        
        final RelyingPartyContext rpc = getRelyingPartyContext(input);
        if (rpc != null) {
            final ProfileConfiguration pc = rpc.getProfileConfig();
            
            if (pc instanceof BrowserSSOProfileConfiguration sso) {
                return sso.isIncludeAttributeStatement(input);
            } else if (pc instanceof net.shibboleth.saml.saml1.profile.config.AttributeQueryProfileConfiguration) {
                return true;
            } else if (pc instanceof net.shibboleth.saml.saml2.profile.config.AttributeQueryProfileConfiguration) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Returns true iff the SAML binding is an artifact variant.
     * 
     * @param profileRequestContext profile request context
     * 
     * @return true iff the SAML binding is an artifact variant
     */
    private boolean isArtifactBinding(@Nullable final ProfileRequestContext profileRequestContext) {
        final SAMLBindingContext bindingCtx = bindingContextLookupStrategy.apply(profileRequestContext);
        if (bindingCtx != null) {
            final BindingDescriptor bd = bindingCtx.getBindingDescriptor();
            if (bd != null) {
                return bd.isArtifact();
            }
        }
        
        return false;
    }
    
}
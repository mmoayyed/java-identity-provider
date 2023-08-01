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

package net.shibboleth.idp.saml.profile.config.impl;

import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.config.AbstractInterceptorAwareProfileConfiguration;
import net.shibboleth.idp.saml.profile.config.SAMLProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.logic.PredicateSupport;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;

/** Base class for SAML profile configurations. */
public abstract class AbstractSAMLProfileConfiguration extends AbstractInterceptorAwareProfileConfiguration implements
        SAMLProfileConfiguration {
    
    /** Predicate used to determine if the generated request should be signed. Default returns false. */
    @Nonnull private Predicate<ProfileRequestContext> signRequestsPredicate;

    /** Predicate used to determine if the generated response should be signed. Default returns false. */
    @Nonnull private Predicate<ProfileRequestContext> signResponsesPredicate;

    /** Lookup strategy for message decorator. */
    @Nonnull private Function<MessageContext,Function<MessageContext,Exception>> messageHandlerLookupStrategy;

    /**
     * Constructor.
     * 
     * @param profileId ID of the communication profile
     */
    public AbstractSAMLProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);

        signRequestsPredicate = PredicateSupport.alwaysFalse();
        signResponsesPredicate = PredicateSupport.alwaysFalse();
        
        messageHandlerLookupStrategy = FunctionSupport.constant(null);
    }

    /** {@inheritDoc} */
    public boolean isSignRequests(@Nullable final ProfileRequestContext profileRequestContext) {
        return signRequestsPredicate.test(profileRequestContext);
    }

    /**
     * Set whether generated requests should be signed.
     * 
     * @param flag flag to set
     */
    public void setSignRequests(final boolean flag) {
        signRequestsPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set the predicate used to determine if generated requests should be signed.
     * 
     * @param predicate predicate used to determine if generated requests should be signed
     * 
     * @since 4.0.0
     */
    public void setSignRequestsPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        signRequestsPredicate = Constraint.isNotNull(predicate, "Condition cannot be null");
    }

    /** {@inheritDoc} */
    public boolean isSignResponses(@Nullable final ProfileRequestContext profileRequestContext) {
        return signResponsesPredicate.test(profileRequestContext);
    }

    /**
     * Set whether generated responses should be signed.
     * 
     * @param flag flag to set
     */
    public void setSignResponses(final boolean flag) {
        signResponsesPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set the predicate used to determine if generated responses should be signed.
     * 
     * @param predicate predicate used to determine if generated responses should be signed
     * 
     * @since 4.0.0
     */
    public void setSignResponsesPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        signResponsesPredicate = Constraint.isNotNull(predicate, "Condition cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable
    public Function<MessageContext,Exception> getMessageHandler(@Nullable final MessageContext messageContext) {
        return messageHandlerLookupStrategy.apply(messageContext);
    }
    
    /**
     * Set a handler for the SAML message.
     * 
     * @param handler message handler
     * 
     * @since 5.0.0
     */
    public void setMessageDecorator(@Nullable final Function<MessageContext,Exception> handler) {
        messageHandlerLookupStrategy = FunctionSupport.constant(handler);
    }
    
    /**
     * Set a lookup strategy for the handler for the SAML message.
     * 
     * @param strategy lookup strategy 
     * 
     * @since 5.0.0
     */
    public void setMessageHandlerLookupStrategy(
            @Nonnull final Function<MessageContext,Function<MessageContext,Exception>> strategy) {
        messageHandlerLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

}
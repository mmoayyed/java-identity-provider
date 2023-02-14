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

package net.shibboleth.idp.profile.relyingparty;

import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.logic.PredicateSupport;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * IdP-specific subclass of base configuration class.
 */
public class RelyingPartyConfiguration extends net.shibboleth.profile.relyingparty.RelyingPartyConfiguration {
    
    /** Lookup function to supply <code>responderId</code> property. */
    @NonnullAfterInit private Function<ProfileRequestContext,String> responderIdLookupStrategy;

    /** Controls whether detailed information about errors should be exposed. */
    @Nonnull private Predicate<ProfileRequestContext> detailedErrorsPredicate;

    /** Constructor. */
    public RelyingPartyConfiguration() {
        detailedErrorsPredicate = PredicateSupport.alwaysFalse();
    }

    /**
     * Get the self-referential ID to use when responding to requests.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return ID to use when responding
     */
    @Nonnull @NotEmpty public String getResponderId(@Nullable final ProfileRequestContext profileRequestContext) {
        return Constraint.isNotEmpty(responderIdLookupStrategy.apply(profileRequestContext),
                "ResponderId cannot be null or empty");
    }

    /**
     * Set the self-referential ID to use when responding to messages.
     * 
     * @param responder ID to use when responding to messages
     */
    public void setResponderId(@Nonnull @NotEmpty final String responder) {
        checkSetterPreconditions();
        final String id =
                Constraint.isNotNull(StringSupport.trimOrNull(responder), "ResponderId cannot be null or empty");
        responderIdLookupStrategy = FunctionSupport.constant(id);
    }

    /**
     * Set a lookup strategy for the <code>responderId</code> property.
     * 
     * @param strategy  lookup strategy
     * 
     * @since 3.4.0
     */
    public void setResponderIdLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        responderIdLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
    /**
     * Get whether detailed information about errors should be exposed.
     * 
     * @param profileRequestContext current profile request context
     *
     * @return true iff it is acceptable to expose detailed error information
     */
    public boolean isDetailedErrors(@Nullable final ProfileRequestContext profileRequestContext) {
        return detailedErrorsPredicate.test(profileRequestContext);
    }

    /**
     * Set whether detailed information about errors should be exposed.
     * 
     * @param flag  flag to set
     */
    public void setDetailedErrors(final boolean flag) {
        checkSetterPreconditions();
        detailedErrorsPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set a condition to determine whether detailed information about errors should be exposed.
     * 
     * @param condition  condition to set
     */
    public void setDetailedErrorsPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        detailedErrorsPredicate = Constraint.isNotNull(condition, "Condition cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
    
        if (responderIdLookupStrategy == null) {
            throw new ComponentInitializationException("ResponderID lookup strategy cannot be null");
        }
    }

}
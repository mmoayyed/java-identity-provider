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

package net.shibboleth.idp.session;

import javax.annotation.Nonnull;

import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.idp.session.context.LogoutPropagationContext;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * A descriptor for a logout propagation flow.
 * 
 * <p>
 * A flow models a sequence of profile actions that performs logout propagation of an {@link SPSession}.
 * Flows may not interact with the client, and must include an activation predicate to indicate their
 * suitability based on the content of the {@link ProfileRequestContext}, particularly the required
 * {@link LogoutPropagationContext} child context.
 * </p>
 */
public class LogoutPropagationFlowDescriptor extends AbstractIdentifiableInitializableComponent
        implements Predicate<ProfileRequestContext> {

    /** Predicate that must be true for this flow to be usable for a given request. */
    @Nonnull private Predicate<ProfileRequestContext> activationCondition;

    /** Constructor. */
    public LogoutPropagationFlowDescriptor() {
        activationCondition = Predicates.alwaysTrue();
    }

    /**
     * Set the activation condition in the form of a {@link Predicate} such that iff the condition evaluates to true
     * should the corresponding flow be allowed/possible.
     * 
     * @param condition predicate that controls activation of the flow
     */
    public void setActivationCondition(@Nonnull final Predicate<ProfileRequestContext> condition) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        activationCondition = Constraint.isNotNull(condition, "Activation condition predicate cannot be null");
    }

    /** {@inheritDoc} */
    @Override public boolean apply(ProfileRequestContext input) {
        return activationCondition.apply(input);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return getId().hashCode();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof LogoutPropagationFlowDescriptor) {
            return getId().equals(((LogoutPropagationFlowDescriptor) obj).getId());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return MoreObjects.toStringHelper(this).add("flowId", getId()).toString();
    }

    /** Default condition that operates based on the underlying {@link SPSession} type. */
    public static class ActivationCondition implements Predicate<ProfileRequestContext> {
        
        /** Lookup strategy for {@link LogoutPropagationContext}. */
        @Nonnull private Function<ProfileRequestContext, LogoutPropagationContext> contextLookupStrategy;
        
        /** Type of session to look for. */
        @Nonnull private Class<? extends SPSession> sessionType;

        /** Constructor. */
        public ActivationCondition() {
            contextLookupStrategy = new ChildContextLookup<>(LogoutPropagationContext.class);
            sessionType = BasicSPSession.class;
        }
        
        /**
         * Set the lookup strategy for the {@link LogoutContext}.
         * 
         * @param strategy lookup strategy
         */
        public void setLogoutContextLookupStrategy(
                @Nonnull final Function<ProfileRequestContext, LogoutPropagationContext> strategy) {
            contextLookupStrategy = Constraint.isNotNull(strategy,
                    "SingleLogoutContext lookup strategy cannot be null");
        }
        
        /**
         * Set the type of {@link SPSession} to look for.
         * 
         * @param type  type of session to look for
         */
        public void setSessionType(@Nonnull final Class<? extends SPSession> type) {
            sessionType = Constraint.isNotNull(type, "SPSession type cannot be null");
        }
        
        /** {@inheritDoc} */
        @Override
        public boolean apply(@Nonnull final ProfileRequestContext input) {
            
            final LogoutPropagationContext logoutPropCtx = contextLookupStrategy.apply(input);
            if (logoutPropCtx != null && logoutPropCtx.getSession() != null) {
                return sessionType.isInstance(logoutPropCtx.getSession());
            }
            
            return false;
        }
        
    }
    
}
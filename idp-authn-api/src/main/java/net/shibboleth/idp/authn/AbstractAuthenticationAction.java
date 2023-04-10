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

package net.shibboleth.idp.authn;

import java.util.function.Function;

import javax.annotation.Nonnull;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.logic.Constraint;

/**
 * A base class for authentication related actions.
 * 
 * In addition to the work performed by {@link AbstractProfileAction}, this action also looks up
 * and makes available the {@link AuthenticationContext}.
 * 
 * Authentication action implementations should override
 * {@link #doExecute(ProfileRequestContext, AuthenticationContext)}
 * 
 * @event {@link AuthnEventIds#INVALID_AUTHN_CTX}
 */
public abstract class AbstractAuthenticationAction
        extends AbstractProfileAction {

    /**
     * Strategy used to extract, and create if necessary, the {@link AuthenticationContext} from the
     * {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext,AuthenticationContext> authnCtxLookupStrategy;
    
    /** AuthenticationContext to operate on. */
    @NonnullBeforeExec private AuthenticationContext authnContext;

    /** Constructor. */
    public AbstractAuthenticationAction() {
        authnCtxLookupStrategy = new ChildContextLookup<>(AuthenticationContext.class);
    }

    /**
     * Set the context lookup strategy.
     * 
     * @param strategy  lookup strategy function for {@link AuthenticationContext}.
     */
    public void setAuthenticationContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,AuthenticationContext> strategy) {
        checkSetterPreconditions();
        
        authnCtxLookupStrategy = Constraint.isNotNull(strategy, "Strategy cannot be null");
    }

    /** null safe getter.
     * @return Returns the authnContext.
     */
    @SuppressWarnings("null")
    @Nonnull private AuthenticationContext getAuthenticationContext() {
        assert isPreExecuteCalled();
        return authnContext;
    }

    /** {@inheritDoc} */
    @Override
    protected final boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (super.doPreExecute(profileRequestContext)) {
            final AuthenticationContext ac = authnContext = authnCtxLookupStrategy.apply(profileRequestContext);
            if (ac  == null) {
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_AUTHN_CTX);
                return false;
            }
    
            return doPreExecute(profileRequestContext, ac);
        }
        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    protected final void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        doExecute(profileRequestContext, getAuthenticationContext());
    }

    /**
     * Performs this authentication action's pre-execute step. Default implementation just returns true.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     * 
     * @return true iff execution should continue
     */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        return true;
    }
    
    /**
     * Performs this authentication action. Default implementation throws an exception.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
    }

}
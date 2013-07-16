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

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;

import com.google.common.base.Function;

/**
 * A base class for authentication related actions.
 * 
 * In addition to the work performed by {@link AbstractProfileAction}, this action also looks up and makes available the
 * {@link AuthenticationContext}.
 * 
 * Authentication action implementations should override
 * {@link #doExecute(ProfileRequestContext, AuthenticationContext)}
 */
public abstract class AbstractAuthenticationAction extends AbstractProfileAction {

    /**
     * Strategy used to extract, and create if necessary, the {@link AuthenticationContext} from the
     * {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext, AuthenticationContext> authnCtxLookupStrategy;

    /** Constructor. */
    public AbstractAuthenticationAction() {
        super();

        authnCtxLookupStrategy =
                new ChildContextLookup<ProfileRequestContext, AuthenticationContext>(
                        AuthenticationContext.class, false);
    }

    /**
     * Constructor.
     * 
     * @param strategy lookup function to locate {@link AuthenticationContext}
     */
    public AbstractAuthenticationAction(
            @Nonnull Function<ProfileRequestContext, AuthenticationContext> strategy) {
        super();

        authnCtxLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy function cannot be null");
    }
    
    /** {@inheritDoc} */
    protected final void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        final AuthenticationContext authenticationContext = authnCtxLookupStrategy.apply(profileRequestContext);
        if (authenticationContext == null) {
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_AUTHN_CTX);
            return;
        }

        doExecute(profileRequestContext, authenticationContext);
    }

    /**
     * Performs this authentication action. Default implementation throws an exception.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     * 
     * @throws AuthenticationException thrown if there is a problem performing the authentication action
     */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {
        throw new UnsupportedOperationException("This action is not implemented");
    }

}
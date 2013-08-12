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

package net.shibboleth.idp.authn.impl;

import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectContext;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Strings;


/**
 * An authentication action that runs after a completed authentication flow (or the reuse
 * of an active result) and transfers information from the {@link AuthenticationContext}
 * to a {@link SubjectContext} child of the {@link ProfileRequestContext} for use by other components.
 * 
 * <p>The context is populated based on the presence of a canonical principal name, and also includes
 * the completed {@link AuthenticationResult} and any other active results found in the context.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != null</pre>
 * @post If AuthenticationContext.getCanonicalPrincipalName() != null,
 * then ProfileRequestContext.getSubcontext(SubjectContext.class, false) != null 
 * @post AuthenticationContext.setCompletionInstant() was called
 */
public class FinalizeAuthentication extends AbstractAuthenticationAction {

    /** {@inheritDoc} */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        if (Strings.isNullOrEmpty(authenticationContext.getCanonicalPrincipalName())) {
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        SubjectContext sc = profileRequestContext.getSubcontext(SubjectContext.class, true);
        sc.setPrincipalName(authenticationContext.getCanonicalPrincipalName());

        Map scResults = sc.getAuthenticationResults();
        scResults.putAll(authenticationContext.getActiveResults());
        
        AuthenticationResult latest = authenticationContext.getAuthenticationResult();
        if (latest != null && !scResults.containsKey(latest.getAuthenticationFlowId())) {
                scResults.put(latest.getAuthenticationFlowId(), latest);
        }
        
        authenticationContext.setCompletionInstant();
    }

}
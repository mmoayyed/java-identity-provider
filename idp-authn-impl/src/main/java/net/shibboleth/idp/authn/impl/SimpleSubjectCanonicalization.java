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

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractSubjectCanonicalizationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Predicate;

/**
 * An action that operates on a {@link SubjectCanonicalizationContext} child of the current
 * {@link ProfileRequestContext}, and transforms the input {@link javax.security.auth.Subject}
 * into a principal name by searching for one and only one {@link UsernamePrincipal} custom principal.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link org.opensaml.profile.action.EventIds#INVALID_PROFILE_CTX}
 * @event {@link AuthnEventIds#SUBJECT_C14N_ERROR}
 * @pre <pre>ProfileRequestContext.getSubcontext(SubjectCanonicalizationContext.class, false) != null</pre>
 * @post <pre>SubjectCanonicalizationContext.getPrincipalName() != null
 *  || SubjectCanonicalizationContext.getException() != null</pre>
 */
public class SimpleSubjectCanonicalization extends AbstractSubjectCanonicalizationAction {
    
    /** The custom Principal to operate on. */
    @Nullable private UsernamePrincipal usernamePrincipal;
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext, 
            @Nonnull final SubjectCanonicalizationContext c14nContext) throws SubjectCanonicalizationException {
        
        final Set<UsernamePrincipal> usernames;
        if (c14nContext.getSubject() != null) {
            usernames = c14nContext.getSubject().getPrincipals(UsernamePrincipal.class);
        } else {
            usernames = null;
        }
        
        if (usernames == null || usernames.isEmpty()) {
            c14nContext.setException(new SubjectCanonicalizationException("No UsernamePrincipals were found"));
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.SUBJECT_C14N_ERROR);
            return false;
        } else if (usernames.size() > 1) {
            c14nContext.setException(new SubjectCanonicalizationException("Multiple UsernamePrincipals were found"));
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.SUBJECT_C14N_ERROR);
            return false;
        }
        
        usernamePrincipal = usernames.iterator().next();
        
        return super.doPreExecute(profileRequestContext, c14nContext);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext, 
            @Nonnull final SubjectCanonicalizationContext c14nContext) throws SubjectCanonicalizationException {
        
        c14nContext.setPrincipalName(applyTransforms(usernamePrincipal.getName()));
    }
     
    /** A predicate that determines if this action can run or not. */
    public static class ActivationCondition implements Predicate<ProfileRequestContext> {

        /** {@inheritDoc} */
        @Override
        public boolean apply(@Nullable final ProfileRequestContext input) {
            
            if (input != null) {
                final SubjectCanonicalizationContext c14nContext =
                        input.getSubcontext(SubjectCanonicalizationContext.class, false);
                if (c14nContext != null || c14nContext.getSubject() != null) {
                    final Set<UsernamePrincipal> usernames =
                            c14nContext.getSubject().getPrincipals(UsernamePrincipal.class);
                    return usernames != null && usernames.size() == 1;
                }
            }
            
            return false;
        }
        
    }

}
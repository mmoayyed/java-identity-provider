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
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.AbstractSubjectCanonicalizationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;

/**
 * An action that operates on a {@link SubjectCanonicalizationContext} child of the current
 * {@link ProfileRequestContext}, and transforms the input {@link javax.security.auth.Subject}
 * into a principal name by searching for one and only one {@link UsernamePrincipal} custom principal.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#INVALID_SUBJECT}
 * @pre <pre>ProfileRequestContext.getSubcontext(SubjectCanonicalizationContext.class, false) != null</pre>
 * @post <pre>SubjectCanonicalizationContext.getPrincipalName() != null
 *  || SubjectCanonicalizationContext.getException() != null</pre>
 */
public class SimpleSubjectCanonicalization extends AbstractSubjectCanonicalizationAction {

    /** Class logger. */
   // @Nonnull private final Logger log = LoggerFactory.getLogger(SelectAuthenticationFlow.class);

    /** Supplies logic for pre-execute test. */
    @Nonnull private final ActivationCondition embeddedPredicate;
    
    /** The custom Principal to operate on. */
    @Nullable private UsernamePrincipal usernamePrincipal;
    
    /** Constructor. */
    public SimpleSubjectCanonicalization() {
        embeddedPredicate = new ActivationCondition();
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext, 
            @Nonnull final SubjectCanonicalizationContext c14nContext) {

        if (embeddedPredicate.apply(profileRequestContext, c14nContext, true)) {
            final Subject c14CtxSubject = c14nContext.getSubject();
            assert c14CtxSubject != null;
            usernamePrincipal = c14CtxSubject.getPrincipals(UsernamePrincipal.class).iterator().next();
            return super.doPreExecute(profileRequestContext, c14nContext);
        }
        
        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext, 
            @Nonnull final SubjectCanonicalizationContext c14nContext) {
        assert usernamePrincipal != null;
        c14nContext.setPrincipalName(applyTransforms(usernamePrincipal.getName()));
    }
     
    /** A predicate that determines if this action can run or not. */
    public static class ActivationCondition implements Predicate<ProfileRequestContext> {

        /** {@inheritDoc} */
        public boolean test(@Nullable final ProfileRequestContext input) {
            
            if (input != null) {
                final SubjectCanonicalizationContext c14nContext =
                        input.getSubcontext(SubjectCanonicalizationContext.class);
                if (c14nContext != null) {
                    return apply(input, c14nContext, false);
                }
            }
            
            return false;
        }

        /**
         * Helper method that runs either as part of the {@link Predicate} or directly from
         * the {@link SimpleSubjectCanonicalization#doPreExecute(ProfileRequestContext, SubjectCanonicalizationContext)}
         * method above.
         * 
         * @param profileRequestContext the current profile request context
         * @param c14nContext   the current c14n context
         * @param duringAction  true iff the method is run from the action above
         * @return true iff the action can operate successfully on the candidate contexts
         */
        public boolean apply(@Nonnull final ProfileRequestContext profileRequestContext,
                @Nonnull final SubjectCanonicalizationContext c14nContext, final boolean duringAction) {

            final Set<UsernamePrincipal> usernames;
            final Subject c14CtxSubject = c14nContext.getSubject();
            if (c14CtxSubject  != null) {
                usernames = c14CtxSubject .getPrincipals(UsernamePrincipal.class);
            } else {
                usernames = null;
            }
            
            if (usernames == null || usernames.isEmpty()) {
                c14nContext.setException(
                        new SubjectCanonicalizationException("No UsernamePrincipals were found"));
                if (duringAction) {
                    ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
                }
                return false;
            } else if (usernames.size() > 1) {
                c14nContext.setException(
                        new SubjectCanonicalizationException("Multiple UsernamePrincipals were found"));
                if (duringAction) {
                    ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
                }
                return false;
            }
            
            return true;
        }
        
    }

}

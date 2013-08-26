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

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.authn.UsernamePrincipal;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;

/**
 * An action that operates on a {@link SubjectCanonicalizationContext} child of the current
 * {@link ProfileRequestContext}, and transforms the input {@link Subject} into a principal name
 * by searching for one and only one {@link UsernamePrincipal} custom principal.
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_SUBJECT_C14N_CTX}
 * @event {@link AuthnEventIds#SUBJECT_C14N_ERROR}
 * @pre <pre>ProfileRequestContext.getSubcontext(SubjectCanonicalizationContext.class, false) != null</pre>
 * @post <pre>SubjectCanonicalizationContext.getPrincipalName() != null
 *  || SubjectCanonicalizationContext.getException() != null</pre>
 */
public class SimpleSubjectCanonicalization extends AbstractProfileAction {

    /**
     * Strategy used to find the {@link SubjectCanonicalizationContext} from the
     * {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext, SubjectCanonicalizationContext> scCtxLookupStrategy;
    
    /** SubjectCanonicalizationContext to operate on. */
    @Nullable private SubjectCanonicalizationContext scContext;
    
    /** Constructor. */
    SimpleSubjectCanonicalization() {
        super();
        
        scCtxLookupStrategy = new ChildContextLookup(SubjectCanonicalizationContext.class, false);
    }

    /**
     * Set the context lookup strategy.
     * 
     * @param strategy  lookup strategy function for {@link SubjectCanonicalizationContext}.
     */
    public void setLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, SubjectCanonicalizationContext> strategy) {
        scCtxLookupStrategy = Constraint.isNotNull(strategy, "Strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        scContext = scCtxLookupStrategy.apply(profileRequestContext);
        if (scContext == null) {
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        
        Set<UsernamePrincipal> usernames = scContext.getSubject().getPrincipals(UsernamePrincipal.class);
        if (usernames == null || usernames.isEmpty()) {
            scContext.setException(new SubjectCanonicalizationException("No UsernamePrincipals were found"));
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.SUBJECT_C14N_ERROR);
        } else if (usernames.size() > 1) {
            scContext.setException(new SubjectCanonicalizationException("Multiple UsernamePrincipals were found"));
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.SUBJECT_C14N_ERROR);
        } else {
            scContext.setPrincipalName(usernames.iterator().next().getName());
        }
    }
    
}
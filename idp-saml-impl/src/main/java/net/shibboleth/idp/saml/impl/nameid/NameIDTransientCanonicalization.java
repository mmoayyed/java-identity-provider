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

package net.shibboleth.idp.saml.impl.nameid;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.saml.authn.principal.NameIDPrincipal;
import net.shibboleth.idp.saml.nameid.NameCanonicalizationException;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.NameID;

/**
 * An action that operates on a {@link SubjectCanonicalizationContext} child of the current
 * {@link ProfileRequestContext}, and transforms the input {@link javax.security.auth.Subject}
 * into a principal name by searching for one and only one {@link NameIDPrincipal} custom principal.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link org.opensaml.profile.action.EventIds#INVALID_PROFILE_CTX}
 * @event {@link AuthnEventIds#SUBJECT_C14N_ERROR}
 * @event {@link AuthnEventIds#RESELECT_FLOW}
 * @pre <pre>ProfileRequestContext.getSubcontext(SubjectCanonicalizationContext.class, false) != null</pre>
 * @post <pre>SubjectCanonicalizationContext.getPrincipalName() != null
 *  || SubjectCanonicalizationContext.getException() != null</pre>
 */
public class NameIDTransientCanonicalization extends AbstractTransientCanonicalization {
    
    /** The custom Principal to operate on. */
    @Nullable private String transientPrincipal;
    
    
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext, 
            @Nonnull final SubjectCanonicalizationContext c14nContext) throws SubjectCanonicalizationException {
        
        final Set<NameIDPrincipal> nameIDs;
        if (c14nContext.getSubject() != null) {
            nameIDs = c14nContext.getSubject().getPrincipals(NameIDPrincipal.class);
        } else {
            nameIDs = null;
        }
        
        if (nameIDs == null || nameIDs.isEmpty()) {
            c14nContext.setException(new SubjectCanonicalizationException("No NameIDPrincipals were found"));
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.RESELECT_FLOW);
            return false;
        } else if (nameIDs.size() > 1) {
            c14nContext.setException(new SubjectCanonicalizationException("Multiple NameIDPrincipals were found"));
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.SUBJECT_C14N_ERROR);
            return false;
        }
        
        final NameID nameId = nameIDs.iterator().next().getNameID();
        
        if (!getFormats().contains(nameId.getFormat())) {
            c14nContext.setException(new SubjectCanonicalizationException("Format does not match found"));
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.RESELECT_FLOW);
            return false;
        }
        
        try {
            transientPrincipal = decode(nameId.getValue(), c14nContext.getRequesterId());
        } catch (NameCanonicalizationException e) {
            c14nContext.setException(e);
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.SUBJECT_C14N_ERROR);
            return false;            
        }
        
        return super.doPreExecute(profileRequestContext, c14nContext);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext, 
            @Nonnull final SubjectCanonicalizationContext c14nContext) throws SubjectCanonicalizationException {
        
        c14nContext.setPrincipalName(transientPrincipal);
    }
     
}

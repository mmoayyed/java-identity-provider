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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.AbstractSubjectCanonicalizationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.CollectionSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;

/**
 * An abstract action which contains the logic to do format matching (which is common among all
 * {@link org.opensaml.saml.saml2.core.NameID} and {@link org.opensaml.saml.saml1.core.NameIdentifier} C14N
 * implementations.
 */
public abstract class AbstractSAMLNameCanonicalization extends AbstractSubjectCanonicalizationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractSAMLNameCanonicalization.class);

    /** Store Set of acceptable formats. */
    @Nonnull @Unmodifiable private Set<String> formats = Collections.EMPTY_SET;

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (formats.isEmpty()) {
            throw new ComponentInitializationException(getLogPrefix() + " list of formats is empty");
        }

        log.debug("{} SAML formats '{}'", getLogPrefix(), getFormats());
    }

    /**
     * Return the set of acceptable formats.
     * 
     * @return Returns the formats.
     */
    @Nonnull public Collection<String> getFormats() {
        return formats;
    }

    /**
     * Sets the acceptable formats.
     * 
     * @param theFormats The formats to set.
     */
    public void setFormats(@Nonnull Collection<String> theFormats) {
        Constraint.isNotNull(theFormats, "Format loist must be non null");
        final Set<String> newFormats = new HashSet(theFormats.size());
        CollectionSupport.addIf(newFormats, theFormats, Predicates.notNull());

        formats = ImmutableSet.copyOf(newFormats);
    }

    /**
     * Check the provided responder against the one from the C14N context. If we are in the action then we log the error
     * into the C14N context and add the appropriate event to the ProfileRequest context
     * 
     * @param responder the format to check
     * @param profileRequestContext the current profile request context
     * @param c14nContext the current c14n context
     * @param duringAction true iff the method is run from the action above
     * @return true if the format matches.
     */
    protected boolean responderMatches(@Nonnull String responder,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final SubjectCanonicalizationContext c14nContext, final boolean duringAction) {

        if (null == responder || responder.equals(c14nContext.getResponderId())) {
            return true;
        }
        if (duringAction) {
            c14nContext.setException(new SubjectCanonicalizationException(
                    "Name Qualifier does not match responder ID"));
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
        }
        return false;
     }
    
    /**
     * Check the provided requester against the one from the C14N context. If we are in the action then we log the error
     * into the C14N context and add the appropriate event to the ProfileRequest context
     * 
     * @param requester the format to check
     * @param profileRequestContext the current profile request context
     * @param c14nContext the current c14n context
     * @param duringAction true iff the method is run from the action above
     * @return true if the format matches.
     */
    protected boolean requesterMatches(@Nonnull String requester,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final SubjectCanonicalizationContext c14nContext, final boolean duringAction) {

        if (null == requester || requester.equals(c14nContext.getRequesterId())) {
            return true;
        }
        if (duringAction) {
            c14nContext.setException(new SubjectCanonicalizationException(
                    "Name Qualifier does not match reqjuester ID"));
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
        }
        return false;
     }


}

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
import net.shibboleth.idp.saml.nameid.NameIdentifierDecoder;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.NameID;

import com.google.common.base.Predicate;

/**
 * An action that operates on a {@link SubjectCanonicalizationContext} child of the current
 * {@link ProfileRequestContext}, and transforms the input {@link javax.security.auth.Subject} into a principal name by
 * searching for one and only one {@link NameIDPrincipal} custom principal. <br/>
 * The precise decide is controlled by the
 *
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#INVALID_SUBJECT}
 * @pre <pre>
 * ProfileRequestContext.getSubcontext(SubjectCanonicalizationContext.class, false) != null
 * </pre>
 * @post <pre>
 * SubjectCanonicalizationContext.getPrincipalName() != null || SubjectCanonicalizationContext.getException() != null
 * </pre>
 */
public class NameIDCanonicalization extends AbstractSAMLNameCanonicalization {

    /** The custom Principal to operate on. */
    @Nullable private String transientPrincipal;

    /** Supplies logic for pre-execute test. */
    @Nonnull private final ActivationCondition embeddedPredicate;

    /** Supplies logic for decoding the {@link NameID#getValue()} into the principal. */
    @NonnullAfterInit private NameIdentifierDecoder decoder;

    /**
     * Constructor.
     */
    public NameIDCanonicalization() {
        embeddedPredicate = new ActivationCondition();
    }

    /**
     * Gets the class responsible for decoding the {@link NameID#getValue()} into the principal.
     *
     * @return Returns the decoder.
     */
    @NonnullAfterInit public NameIdentifierDecoder getDecoder() {
        return decoder;
    }

    /**
     * Sets the class responsible for decoding the {@link NameID#getValue()} into the principal.
     *
     * @param theDecoder the decoder.
     */
    @NonnullAfterInit public void setDecoder(@Nonnull NameIdentifierDecoder theDecoder) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        decoder = Constraint.isNotNull(theDecoder, "Name decoder must not be null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        if (null == decoder) {
            throw new ComponentInitializationException(getLogPrefix() + " decoder not supplied");
        }
        super.doInitialize();
    }

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final SubjectCanonicalizationContext c14nContext) throws SubjectCanonicalizationException {

        if (embeddedPredicate.apply(profileRequestContext, c14nContext, true)) {

            final Set<NameIDPrincipal> nameIDs = c14nContext.getSubject().getPrincipals(NameIDPrincipal.class);
            final NameID nameID = nameIDs.iterator().next().getNameID();

            try {
                transientPrincipal =
                        decoder.decode(nameID.getValue(), c14nContext.getResponderId(), c14nContext.getRequesterId());
            } catch (SubjectCanonicalizationException e) {
                c14nContext.setException(e);
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.SUBJECT_C14N_ERROR);
                return false;
            }
            return super.doPreExecute(profileRequestContext, c14nContext);
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final SubjectCanonicalizationContext c14nContext) throws SubjectCanonicalizationException {

        c14nContext.setPrincipalName(transientPrincipal);
    }

    /** A predicate that determines if this action can run or not. */
    public class ActivationCondition implements Predicate<ProfileRequestContext> {

        /** {@inheritDoc} */
        @Override public boolean apply(@Nullable final ProfileRequestContext input) {

            if (input != null) {
                final SubjectCanonicalizationContext c14nContext =
                        input.getSubcontext(SubjectCanonicalizationContext.class, false);
                if (c14nContext != null) {
                    return apply(input, c14nContext, false);
                }
            }

            return false;
        }

        /**
         * Helper method that runs either as part of the {@link Predicate} or directly from the
         * {@link NameIDCanonicalization#doPreExecute(ProfileRequestContext, SubjectCanonicalizationContext)} method
         * above.
         *
         * @param profileRequestContext the current profile request context
         * @param c14nContext the current c14n context
         * @param duringAction true iff the method is run from the action above
         * @return true iff the action can operate successfully on the candidate contexts
         */
        public boolean apply(@Nullable final ProfileRequestContext profileRequestContext,
                @Nonnull final SubjectCanonicalizationContext c14nContext, final boolean duringAction) {

            final Set<NameIDPrincipal> nameIDs;
            if (c14nContext.getSubject() != null) {
                nameIDs = c14nContext.getSubject().getPrincipals(NameIDPrincipal.class);
            } else {
                nameIDs = null;
            }

            if (duringAction) {
                if (nameIDs == null || nameIDs.isEmpty()) {
                    c14nContext.setException(new SubjectCanonicalizationException("No UsernamePrincipals were found"));
                    ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
                    return false;
                } else if (nameIDs.size() > 1) {
                    c14nContext.setException(new SubjectCanonicalizationException(
                            "Multiple UsernamePrincipals were found"));
                    ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
                    return false;
                }
            } else {
                if (nameIDs == null || nameIDs.size() != 1) {
                    return false;
                }
            }
            final NameID nameID = nameIDs.iterator().next().getNameID();

            if (!getFormats().contains(nameID.getFormat())) {
                if (duringAction) {
                    c14nContext.setException(new SubjectCanonicalizationException("Format does not match found"));
                    ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
                }
                return false;
            }
            return true;
        }

    }
}

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

package net.shibboleth.idp.saml.impl.profile;

import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.idp.saml.authn.principal.NameIDPrincipal;
import net.shibboleth.idp.saml.authn.principal.NameIdentifierPrincipal;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.messaging.context.SAMLSubjectNameIdentifierContext;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Action that extracts a SAML Subject from an inbound message, and prepares a
 * {@link SubjectCanonicalizationContext} to process it into a principal identity.
 * 
 * <p>If the inbound message does not supply a {@link NameIdentifier} or {@link NameID} to
 * process, then nothing is done, and the local event ID {@link #NO_SUBJECT} is signaled.</p>
 * 
 * <p>Otherwise, a custom {@link Principal} of the appropriate type is wrapped around the
 * identifier object and a Java {@link Subject} is prepared for canonicalization.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link #NO_SUBJECT}
 * 
 * @post If "proceed" signaled, then ProfileRequestContext.getSubcontext(SubjectCanonicalizationContext.class) != null
 */
public class ExtractSubjectFromRequest extends AbstractProfileAction {

    /** Local event signaling that canonicalization is unnecessary. */
    @Nonnull @NotEmpty public static final String NO_SUBJECT = "NoSubject";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ExtractSubjectFromRequest.class);
    
    /** Function used to obtain the requester ID. */
    @Nullable private Function<ProfileRequestContext,String> requesterLookupStrategy;

    /** Function used to obtain the responder ID. */
    @Nullable private Function<ProfileRequestContext,String> responderLookupStrategy;
    
    /** SAML 1 or 2 identifier object to wrap for c14n. */
    @Nullable private SAMLObject nameIdentifier;
    
    /** Constructor. */
    public ExtractSubjectFromRequest() {
        requesterLookupStrategy = new RelyingPartyIdLookupFunction();
        responderLookupStrategy = new ResponderIdLookupFunction();
    }
    
    /**
     * Set the strategy used to locate the requester ID for canonicalization.
     * 
     * @param strategy lookup strategy
     */
    public synchronized void setRequesterLookupStrategy(
            @Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        requesterLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to locate the responder ID for canonicalization.
     * 
     * @param strategy lookup strategy
     */
    public synchronized void setResponderLookupStrategy(
            @Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        responderLookupStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        final MessageContext<?> msgCtx = profileRequestContext.getInboundMessageContext();
        if (msgCtx == null || msgCtx.getMessage() == null) {
            log.debug("{} No inbound message", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, NO_SUBJECT);
            return false;
        }

        nameIdentifier = msgCtx.getSubcontext(SAMLSubjectNameIdentifierContext.class, true).getSubjectNameIdentifier();
        if (nameIdentifier == null) {
            log.debug("{} No NameID or NameIdentifier in message", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, NO_SUBJECT);
            return false;
        }
        
        return super.doPreExecute(profileRequestContext);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        Subject subject = null;
        if (nameIdentifier instanceof NameIdentifier) {
            subject = new Subject(false,
                    Collections.singleton(new NameIdentifierPrincipal((NameIdentifier) nameIdentifier)),
                    Collections.emptySet(), Collections.emptySet());
        } else if (nameIdentifier instanceof NameID) {
            subject = new Subject(false,
                    Collections.singleton(new NameIDPrincipal((NameID) nameIdentifier)),
                    Collections.emptySet(), Collections.emptySet());
        }
        
        if (subject == null) {
            log.debug("{} Identifier was not of a supported type, ignoring");
            ActionSupport.buildEvent(profileRequestContext, NO_SUBJECT);
            return;
        }
        
        final SubjectCanonicalizationContext c14n = new SubjectCanonicalizationContext();
        c14n.setSubject(subject);
        if (requesterLookupStrategy != null) {
            c14n.setRequesterId(requesterLookupStrategy.apply(profileRequestContext));
        }
        if (responderLookupStrategy != null) {
            c14n.setResponderId(responderLookupStrategy.apply(profileRequestContext));
        }
        
        profileRequestContext.addSubcontext(c14n, true);
        log.debug("{} Created subject canonicalization context", getLogPrefix());
    }
    
}
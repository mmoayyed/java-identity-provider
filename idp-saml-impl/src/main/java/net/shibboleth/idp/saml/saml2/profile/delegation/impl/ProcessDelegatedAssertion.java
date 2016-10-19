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

package net.shibboleth.idp.saml.saml2.profile.delegation.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLPresenterEntityContext;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.idp.saml.authn.principal.NameIDPrincipal;
import net.shibboleth.utilities.java.support.annotation.Prototype;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;


/**
 * Process the pre-validated inbound {@link Assertion} WS-Security token, and set up the resulting
 * {@link NameID} for subject canonicalization as the effective subject of the request.
 * 
 * <p>
 * A {@link SubjectCanonicalizationContext} is added containing a {@link NameIDPrincipal} with the
 * token's {@link NameID}.
 * </p>
 * 
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @event {@link AuthnEventIds#INVALID_SUBJECT}
 * @pre <pre>assertionTokenStrategy.apply(profileRequestContext).getSubject().getNameID() != null</pre>
 * @post <pre>profileRequestContext.getSubcontext(SubjectCanonicalizationContext.class) != null</pre>
 */
@Prototype
public class ProcessDelegatedAssertion extends AbstractProfileAction {
    
    /** Logger. */
    private Logger log = LoggerFactory.getLogger(ProcessDelegatedAssertion.class);
    

    /** Function used to obtain the requester ID, for purposes of Subject c14n. */
    @Nullable private Function<ProfileRequestContext,String> requesterLookupStrategy;

    /** Function used to obtain the responder ID, for purposes of Subject c14n. */
    @Nullable private Function<ProfileRequestContext,String> responderLookupStrategy;
    
    /** Function used to resolve the assertion token to process. */
    @Nonnull private Function<ProfileRequestContext, Assertion> assertionTokenStrategy;
    
    /** The SAML 2 Assertion token being processed. */
    private Assertion assertion;
    
    /** The SAML 2 NameID representing the authenticated user. */
    private NameID nameID;
    
    /**
     * Constructor.
     */
    public ProcessDelegatedAssertion() {
        requesterLookupStrategy = new DefaultC14NRequesterLookupFunction();
        responderLookupStrategy = new ResponderIdLookupFunction();
        assertionTokenStrategy = new DelegatedAssertionLookupStrategy();
    }
    
    /**
     * Set the strategy used to locate the inbound assertion token to process.
     * 
     * @param strategy lookup strategy
     */
    public void setAssertionTokenStrategy(
            @Nonnull final Function<ProfileRequestContext,Assertion> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        assertionTokenStrategy = Constraint.isNotNull(strategy, "Assertion token strategy may not be null");
    }
    
    /**
     * Set the strategy used to locate the requester ID for subject canonicalization.
     * 
     * @param strategy lookup strategy
     */
    public void setRequesterLookupStrategy(
            @Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        requesterLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to locate the responder ID for subject canonicalization.
     * 
     * @param strategy lookup strategy
     */
    public void setResponderLookupStrategy(
            @Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        responderLookupStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        assertion = assertionTokenStrategy.apply(profileRequestContext);
        
        if (assertion == null) {
            log.warn("{} No valid SAML 2 Assertion available within the request context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return false;
        }
        
        final org.opensaml.saml.saml2.core.Subject samlSubject = assertion.getSubject();
        if (samlSubject == null || samlSubject.getNameID() == null) {
            log.warn("{} SAML 2 Assertion does not contain either a Subject or a NameID", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
            return false;
        }
        
        nameID = samlSubject.getNameID();
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (log.isDebugEnabled()) {
            try {
                log.debug("{} Authenticated user based on inbound SAML 2 Assertion token with NameID: {}", 
                        getLogPrefix(), SerializeSupport.nodeToString(XMLObjectSupport.marshall(nameID)));
            } catch (final MarshallingException e) {
                log.debug("{} Could not marshall SAML 2 NameID for logging purposes", getLogPrefix(), e);
            }
        }
        
        // Set up Subject c14n context for call to c14n subflow.
        final Subject subject = new Subject();
        subject.getPrincipals().add(new NameIDPrincipal(nameID));
        
        final SubjectCanonicalizationContext c14n = new SubjectCanonicalizationContext();
        c14n.setSubject(subject);
        
        String requesterEntityID = null;
        if (requesterLookupStrategy != null) {
            requesterEntityID = requesterLookupStrategy.apply(profileRequestContext);
        }
        if (requesterEntityID != null) {
            log.debug("Resolved effective SAML requester entityID for Subject c14n: {}", requesterEntityID);
            c14n.setRequesterId(requesterEntityID);
        } else {
            log.warn("Unable to determine effective SAML requester for c14n, Subject c14n may fail, " 
                    + "depending on NameID type");
        }
        
        if (responderLookupStrategy != null) {
            c14n.setResponderId(responderLookupStrategy.apply(profileRequestContext));
        }
        profileRequestContext.addSubcontext(c14n, true);
    }
    
    /**
     * Default strategy for resolving the requester entityID for SAML subject c14n.
     */
    public class DefaultC14NRequesterLookupFunction implements Function<ProfileRequestContext, String> {

        /** {@inheritDoc} */
        public String apply(final ProfileRequestContext input) {
            // First attempt to resolve SPNameQualifier from delegated Assertion's Subject NameID, if present
            if (nameID.getSPNameQualifier() != null) {
                log.debug("Saw delegated Assertion Subject NameID SPNameQualifier: {}", nameID.getSPNameQualifier());
                return nameID.getSPNameQualifier();
            }
            
            // Second attempt to resolve entityID of entity in SAML presenter role
            if (input != null && input.getInboundMessageContext() != null) {
                final SAMLPresenterEntityContext presenterContext = 
                        input.getInboundMessageContext().getSubcontext(SAMLPresenterEntityContext.class);
                if (presenterContext != null) {
                    log.debug("Saw SAML presenter entityID: {}", presenterContext.getEntityId());
                    return presenterContext.getEntityId();
                } else {
                    return null;
                }
            }
            
            return null;
        }
        
    }
    
}

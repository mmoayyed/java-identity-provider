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

package net.shibboleth.idp.saml.impl.profile.saml2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.profile.SAMLEventIds;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.EncryptedID;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.ManageNameIDRequest;
import org.opensaml.saml.saml2.core.ManageNameIDResponse;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDMappingRequest;
import org.opensaml.saml.saml2.core.NameIDMappingResponse;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectQuery;
import org.opensaml.xmlsec.DecryptionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * Action to decrypt an {@link EncryptedID} element and replace it with the decrypted {@link NameID}
 * in situ.
 * 
 * <p>All of the built-in SAML message types that may include an {@link EncryptedID} are potentially
 * handled, but the actual message to handle is obtained via strategy function, by default an inbound
 * message.</p> 
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CTX}
 * @event {@link IdPEventIds#INVALID_PROFILE_CONFIG}
 * @event {@link SAMLEventIds#DECRYPT_NAMEID_FAILED}
 */
public class DecryptNameID extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DecryptNameID.class);
    
    /** Are decryption failures a fatal condition? */
    private boolean errorFatal;

    /** Strategy used to locate the {@link RelyingPartyContext}. */
    @Nonnull private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Strategy used to locate the SAML message to operate on. */
    @Nonnull private Function<ProfileRequestContext, Object> messageLookupStrategy;
    
    /** Configuration supporting decryption. */
    @Nullable private DecryptionConfiguration decryptionConfig;
    
    /** Message to operate on. */
    @Nullable private Object message;
    
    /** Constructor. */
    public DecryptNameID() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class, false);
        messageLookupStrategy = Functions.compose(new MessageLookup(), new InboundMessageContextLookup());
    }
    
    /**
     * Set whether decryption failure should be treated as an error or ignored.
     * 
     * @param flag  true iff decryption failure should be fatal
     */
    public synchronized void setErrorFatal(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        errorFatal = flag;
    }
    
    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public synchronized void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /**
     * Set the strategy used to locate the {@link SAMLObect} to operate on.
     * 
     * @param strategy strategy used to locate the {@link SAMLObject} to operate on
     */
    public synchronized void setResponseLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, Object> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        messageLookupStrategy = Constraint.isNotNull(strategy, "Message lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        message = messageLookupStrategy.apply(profileRequestContext);
        if (message == null) {
            log.debug("{} No message was returned by lookup strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        } else if (!(message instanceof SAMLObject)) {
            log.debug("{} Message was not a SAML construct, nothing to do", getLogPrefix());
            return false;
        }
        
        final RelyingPartyContext rpCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpCtx == null) {
            log.debug("{} No relying party context located in current profile request context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        } else if (rpCtx.getProfileConfig() == null
                || rpCtx.getProfileConfig().getSecurityConfiguration() == null) {
            log.debug("{} No profile configuration located in relying party context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        
        decryptionConfig = rpCtx.getProfileConfig().getSecurityConfiguration().getDecryptionConfiguration();
        if (decryptionConfig == null) {
            log.debug("{} No DecryptionConfiguration available in security configuration", getLogPrefix());
            if (errorFatal) {
                ActionSupport.buildEvent(profileRequestContext, SAMLEventIds.DECRYPT_NAMEID_FAILED);
            }
            return false;
        }
        
        return super.doPreExecute(profileRequestContext);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        
        boolean result = true;
        
        if (message instanceof AuthnRequest) {
            result = processSubject(((AuthnRequest) message).getSubject());
        } else if (message instanceof SubjectQuery) {
            result = processSubject(((SubjectQuery) message).getSubject());
        } else if (message instanceof Response) {
            
        } else if (message instanceof LogoutRequest) {
            
        } else if (message instanceof LogoutResponse) {
            
        } else if (message instanceof ManageNameIDRequest) {
            
        } else if (message instanceof ManageNameIDResponse) {
            
        } else if (message instanceof NameIDMappingRequest) {
            
        } else if (message instanceof NameIDMappingResponse) {
            
        } else if (message instanceof Assertion) {
            
        } else {
            log.debug("{} Message was of unrecognized type {}, nothing to do", getLogPrefix(),
                    message.getClass().getName());
            return;
        }
        
        if (!result && errorFatal) {
            ActionSupport.buildEvent(profileRequestContext, SAMLEventIds.DECRYPT_NAMEID_FAILED);
        }
    }
    
    /**
     * Decrypt an {@link EncryptedID} and return the result.
     * 
     * @param encID the encrypted object
     * @return the decrypted name, or null
     */
    @Nullable private NameID processEncryptedID(@Nonnull final EncryptedID encID) {
        return null;
    }

    /**
     * Decrypt any {@link EncryptedID} found in a subject and replace it with the result.
     * 
     * @param subject   subject to operate on
     * @return  true iff no decryption failures occurred
     */
    private boolean processSubject(@Nullable final Subject subject) {
        
        if (subject != null) {
            if (subject.getEncryptedID() != null) {
                log.debug("{} Decrypting EncryptedID in Subject", getLogPrefix());
                final NameID decrypted = processEncryptedID(subject.getEncryptedID());
                if (decrypted != null) {
                    subject.setNameID(decrypted);
                    subject.setEncryptedID(null);
                } else {
                    return false;
                }
            }
            
            for (final SubjectConfirmation sc : subject.getSubjectConfirmations()) {
                if (sc.getEncryptedID() != null) {
                    log.debug("{} Decrypting EncryptedID in SubjectConfirmation", getLogPrefix());
                    final NameID decrypted = processEncryptedID(subject.getEncryptedID());
                    if (decrypted != null) {
                        sc.setNameID(decrypted);
                        sc.setEncryptedID(null);
                    } else {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }

}
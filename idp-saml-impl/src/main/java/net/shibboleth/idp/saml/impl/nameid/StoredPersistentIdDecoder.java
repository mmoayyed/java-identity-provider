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

import java.io.IOException;

import javax.annotation.Nonnull;

import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.saml.nameid.NameDecoderException;
import net.shibboleth.idp.saml.nameid.NameIDDecoder;
import net.shibboleth.idp.saml.nameid.PersistentIdEntry;
import net.shibboleth.idp.saml.nameid.PersistentIdStore;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * An abstract action which contains the logic to decode SAML persistent IDs that are managed with a store.
 * This reverses the work done by {@link StoredPersistentIdGenerationStrategy}.
 */
public class StoredPersistentIdDecoder extends AbstractIdentifiableInitializableComponent implements NameIDDecoder {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StoredPersistentIdDecoder.class);
    
    /** Data store for IDs. */
    @NonnullAfterInit private PersistentIdStore idStore;
    
    /**
     * Get the data store.
     * 
     * @return the data store
     */
    @NonnullAfterInit public PersistentIdStore getPersistentIdStore() {
        return idStore;
    }

    /**
     * Set the data store.
     * 
     * @param store the data store
     */
    public void setPersistentIdStore(@Nonnull final PersistentIdStore store) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        idStore = Constraint.isNotNull(store, "PersistentIdStore cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
    
        if (null == idStore) {
            throw new ComponentInitializationException("PersistentIdStore cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty public String decode(@Nonnull final SubjectCanonicalizationContext c14nContext,
            @Nonnull final NameID nameID) throws NameDecoderException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        if (nameID.getValue() == null) {
            throw new NameDecoderException("Persistent NameID was empty");
        }

        String recipientID = nameID.getSPNameQualifier();
        if (recipientID == null) {
            recipientID = c14nContext.getRequesterId();
            if (recipientID == null) {
                throw new NameDecoderException("SPNameQualifier and requester ID were null");
            }
        }
        
        String issuerID = nameID.getNameQualifier();
        if (issuerID == null) {
            issuerID = c14nContext.getResponderId();
            if (issuerID == null) {
                throw new NameDecoderException("NameQualifier and responder ID were null");
            }
        }
        
        final PersistentIdEntry entry;
        try {
            entry = idStore.getActiveEntry(nameID.getValue());
            if (entry == null) {
                log.info("No entry found for persistent ID {}", nameID.getValue());
                return null;
            }
        } catch (final IOException e) {
            log.error("I/O error looking up persistent ID", e);
            return null;
        }
        
        if (!recipientID.equals(entry.getRecipientEntityId())) {
            log.warn("{} Persistent identifier issued to {} but requested by {}",
                    entry.getRecipientEntityId(), recipientID);
            throw new NameDecoderException("Misuse of identifier by an improper relying party");
        } else if (!issuerID.equals(entry.getIssuerEntityId())) {
            log.warn("{} Persistent identifier issued by {} but requested from {}",
                    entry.getIssuerEntityId(), issuerID);
            throw new NameDecoderException("Misuse of identifier issued by somebody else");
        }
        
        return entry.getPrincipalName();
    }

}
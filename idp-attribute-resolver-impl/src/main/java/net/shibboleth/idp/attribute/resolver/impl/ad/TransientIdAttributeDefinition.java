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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.persistence.PersistenceManager;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.security.RandomIdentifierGenerationStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * An attribute definition that generates random identifiers useful for transient subject IDs.
 * 
 * Information about the created IDs are stored within a provided {@link PersistenceManager} in the form of
 * {@link TransientIdEntry}s. Each entry is mapped under two keys; the generated ID and a key derived from the tuple
 * (outbound message issuer, inbound message issuer, principal name).
 */
public class TransientIdAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(TransientIdAttributeDefinition.class);

    /** Store used to map tokens to principals. */
    // TODO This needs to be changed when Persistence is finalized.
    private PersistenceManager<TransientIdEntry> idStore;

    /** Generator of random, hex-encoded, tokens. */
    private IdentifierGenerationStrategy idGenerator;

    /** Size, in bytes, of the token. */
    private int idSize;

    /** Length, in milliseconds, tokens are valid. */
    private long idLifetime;

    /**
     * Constructor. Sets the defaults where required.
     */
    public TransientIdAttributeDefinition() {
        idSize = 16;
        idLifetime = 1000 * 60 * 60 * 4;
    }

    /**
     * Gets the ID store we are using.
     * 
     * @return the ID store we are using.
     */
    public PersistenceManager<TransientIdEntry> getIdStore() {
        return idStore;
    }

    /**
     * Sets the ID store we should use.
     * 
     * @param store the store to use.
     */
    public void setIdStore(PersistenceManager<TransientIdEntry> store) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        idStore = store;
    }

    /**
     * Gets the size, in bytes, of the id.
     * 
     * @return size, in bytes, of the id
     */
    public int getIdSize() {
        return idSize;
    }

    /**
     * Sets the size, in bytes, of the id.
     * 
     * @param size size, in bytes, of the id
     */
    public void setIdSize(int size) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        idSize = size;
    }

    /**
     * Gets the time, in milliseconds, ids are valid.
     * 
     * @return time, in milliseconds, ids are valid
     */
    public long getIdLifetime() {
        return idLifetime;
    }

    /**
     * Sets the time, in milliseconds, ids are valid.
     * 
     * @param lifetime time, in milliseconds, ids are valid
     */
    public void setIdLiftetime(long lifetime) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        idLifetime = lifetime;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        idGenerator = new RandomIdentifierGenerationStrategy(idSize);

        if (null == idStore) {
            throw new ComponentInitializationException("Attribute definition '" + getId() + ": No Id store set");
        }
        log.debug("Using the store called {}", idStore.getId());
    }

    /** {@inheritDoc} */
    @Nonnull protected Optional<Attribute> doAttributeDefinitionResolve(
            @Nonnull AttributeResolutionContext resolutionContext) throws ResolutionException {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        final AttributeRecipientContext attributeRecipientContext =
                resolutionContext.getSubcontext(AttributeRecipientContext.class);

        if (null == attributeRecipientContext) {
            throw new ResolutionException("Attribute definition '" + getId()
                    + " no attribute recipient context provided ");
        }

        final String attributeIssuerID = attributeRecipientContext.getAttributeIssuerID();
        if (null == attributeIssuerID) {
            throw new ResolutionException("Attribute definition '" + getId()
                    + " provided attribute issuer ID was empty");
        }

        final String attributeRecipientID = attributeRecipientContext.getAttributeRecipientID();
        if (null == attributeRecipientID) {
            throw new ResolutionException("Attribute definition '" + getId()
                    + " provided attribute recipient ID was empty");
        }

        final String principalName = attributeRecipientContext.getPrincipal();
        if (null == principalName) {
            throw new ResolutionException("Attribute definition '" + getId()
                    + " provided prinicipal name was empty");
        }

        final Attribute result = new Attribute(getId());

        StringBuilder principalTokenIdBuilder = new StringBuilder();
        principalTokenIdBuilder.append(attributeIssuerID).append("!").append(attributeRecipientID);
        principalTokenIdBuilder.append("!").append(principalName);
        String principalTokenId = principalTokenIdBuilder.toString();

        TransientIdEntry tokenEntry = idStore.get(principalTokenId);
        if (tokenEntry == null || tokenEntry.isExpired()) {
            String token = idGenerator.generateIdentifier();
            if (tokenEntry == null) {
                log.debug("Creating new transient ID {} for request {}", token, resolutionContext.getId());
            } else {
                log.debug("Previous token expired, Creating new transient ID {} for request {}", token,
                        resolutionContext.getId());
            }
            tokenEntry = new TransientIdEntry(idLifetime, attributeRecipientID, principalName, token);
            idStore.persist(token, tokenEntry);
            idStore.persist(principalTokenId, tokenEntry);
        } else {
            log.debug("Found existing transient ID {} for request {}", tokenEntry.getId(), resolutionContext.getId());
        }
        Set<AttributeValue> vals = Collections.singleton((AttributeValue) new StringAttributeValue(tokenEntry.getId()));
        result.setValues(vals);
        return Optional.of(result);
    }
}

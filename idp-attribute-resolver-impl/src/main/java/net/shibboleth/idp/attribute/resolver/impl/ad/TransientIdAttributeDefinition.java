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

import java.io.IOException;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AbstractAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.security.RandomIdentifierGenerationStrategy;

import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An attribute definition that generates random identifiers useful for transient subject IDs. <br/>
 * Information about the created IDs are stored within a provided {@link StorageService}. The identifier itself is the
 * record key, and the value combines the principal name with the identifier of the recipient.
 */
public class TransientIdAttributeDefinition extends AbstractAttributeDefinition {

    /** Context label for storage of IDs. */
    public static final String CONTEXT = "TransientId";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(TransientIdAttributeDefinition.class);

    /** Store used to map identifiers to principals. */
    private StorageService idStore;

    /** Generator of random, hex-encoded, identifiers. */
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
    @Nullable @NonnullAfterInit public StorageService getIdStore() {
        return idStore;
    }

    /**
     * Sets the ID store we should use.
     * 
     * @param store the store to use.
     */
    public void setIdStore(@Nonnull final StorageService store) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        idStore = Constraint.isNotNull(store, "StorageService cannot be null");
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
    public void setIdSize(final int size) {
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
    public void setIdLifetime(final long lifetime) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        idLifetime = lifetime;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        idGenerator = new RandomIdentifierGenerationStrategy(idSize);

        if (null == idStore) {
            throw new ComponentInitializationException(getLogPrefix() + " no Id store set");
        }
        log.debug("{} using the store '{}'", getLogPrefix(), idStore.getId());
    }

    /**
     * Police and get the AttributeRecipientID.
     * 
     * @param attributeRecipientContext where to look
     * @return the AttributeRecipientID
     * @throws ResolutionException if it was non null
     */
    @Nonnull @NotEmpty private String getAttributeRecipientID(
            @Nonnull final AttributeRecipientContext attributeRecipientContext) throws ResolutionException {
        final String attributeRecipientID =
                StringSupport.trimOrNull(attributeRecipientContext.getAttributeRecipientID());
        if (null == attributeRecipientID) {
            throw new ResolutionException(getLogPrefix() + " provided attribute recipient ID was empty");
        }
        return attributeRecipientID;
    }

    /**
     * Police and get the Principal.
     * 
     * @param attributeRecipientContext where to look
     * @return the Principal
     * @throws ResolutionException if it was non null
     */
    @Nonnull @NotEmpty private String getPrincipal(@Nonnull final AttributeRecipientContext attributeRecipientContext)
            throws ResolutionException {
        final String principalName = StringSupport.trimOrNull(attributeRecipientContext.getPrincipal());
        if (null == principalName) {
            throw new ResolutionException(getLogPrefix() + " provided prinicipal name was empty");
        }

        return principalName;
    }

    /** {@inheritDoc} */
    @Override @Nonnull protected IdPAttribute doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        final AttributeRecipientContext attributeRecipientContext =
                resolutionContext.getSubcontext(AttributeRecipientContext.class);

        if (null == attributeRecipientContext) {
            throw new ResolutionException(getLogPrefix() + " no attribute recipient context provided ");
        }

        final String attributeRecipientID = getAttributeRecipientID(attributeRecipientContext);

        final String principalName = getPrincipal(attributeRecipientContext);

        final IdPAttribute result = new IdPAttribute(getId());

        final String principalTokenId;
        try {
            principalTokenId = new TransientIdParameters(attributeRecipientID, principalName).encode();
        } catch (final IOException except) {
            throw new ResolutionException(except);
        }

        // This code used to store the entries keyed by the ID *and* the value, which I think
        // was used to prevent generation of multiple IDs if the resolver runs multiple times.
        // This is the source of the current V2 bug that causes the same transient to be reused
        // for the same SP within the TTL window. If we need to prevent multiple resolutions, I
        // suggest we do that by storing transactional state for resolver plugins in the context
        // tree. But in practice, I'm not sure it matters much how many times this runs, that's
        // the point of a transient. So this version never reads the store, it just writes to it.

        final String id = idGenerator.generateIdentifier();

        log.debug("{} creating new transient ID '{}' for request '{}'", new Object[] {getLogPrefix(), id,
                resolutionContext.getId(),});

        final long expiration = System.currentTimeMillis() + idLifetime;

        int collisions = 0;
        while (collisions < 5) {
            try {
                if (idStore.create(CONTEXT, id, principalTokenId, expiration)) {
                    // TODO: think we want this to be a NameID-valued attribute now. Or maybe we're keeping this,
                    // but adding a parallel version. I'm thinking maybe we could handle compatibility with the old
                    // String-based encoders by special-casing them to handle NameID-valued attributes?
                    result.setValues(Collections.singleton(new StringAttributeValue(id)));
                    return result;
                } else {
                    ++collisions;
                }
            } catch (final IOException e) {
                throw new ResolutionException(getLogPrefix() + " error saving transient ID to storage service", e);
            }
        }

        throw new ResolutionException(getLogPrefix() + " exceeded allowable number of collisions");
    }

}
/*
 * Copyright 2007 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition;

import java.security.NoSuchAlgorithmException;

import org.opensaml.common.IdentifierGenerator;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.util.storage.StorageService;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.profile.provider.SAMLProfileRequestContext;

/**
 * An attribute definition that generates random identifiers useful for transient subject IDs.
 * 
 * Information about the created IDs are stored within a provided {@link StorageService} in the form of {@link TransientIdEntry}s.
 * Each entry is mapped under two keys; the generated ID and a key derived from the tuple (outbound message issuer,
 * inbound message issuer, principal name).
 */
public class TransientIdAttributeDefinition extends BaseAttributeDefinition {

    /** Store used to map tokens to principals. */
    private StorageService<String, TransientIdEntry> idStore;

    /** Storage partition in which IDs are stored. */
    private String partition;

    /** Generator of random, hex-encoded, tokens. */
    private IdentifierGenerator idGenerator;

    /** Size, in bytes, of the token. */
    private int idSize;

    /** Length, in milliseconds, tokens are valid. */
    private long idLifetime;

    /**
     * Constructor.
     * 
     * @param store store used to map tokens to principals
     * 
     * @throws NoSuchAlgorithmException thrown if the SHA1PRNG, used as the default random number generation algorithm,
     *             is not supported
     */
    public TransientIdAttributeDefinition(StorageService<String, TransientIdEntry> store) throws NoSuchAlgorithmException {
        idGenerator = new SecureRandomIdentifierGenerator();
        idStore = store;
        partition = "transientId";
        idSize = 16;
        idLifetime = 1000 * 60 * 60 * 4;

        // Prime the generator
        idGenerator.generateIdentifier(idSize);
    }

    /** {@inheritDoc} */
    protected BaseAttribute doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {

        SAMLProfileRequestContext requestContext = resolutionContext.getAttributeRequestContext();

        StringBuilder principalTokenIdBuilder = new StringBuilder();
        principalTokenIdBuilder.append(requestContext.getOutboundMessageIssuer()).append("!").append(
                requestContext.getInboundMessageIssuer()).append("!").append(requestContext.getPrincipalName());
        String principalTokenId = principalTokenIdBuilder.toString();

        TransientIdEntry tokenEntry = idStore.get(partition, principalTokenId);
        if (tokenEntry == null || tokenEntry.isExpired()) {
            String token = idGenerator.generateIdentifier(idSize);
            tokenEntry = new TransientIdEntry(idLifetime, requestContext.getInboundMessageIssuer(), requestContext
                    .getPrincipalName(), token);
            idStore.put(partition, token, tokenEntry);
            idStore.put(partition, principalTokenId, tokenEntry);
        }

        BasicAttribute<String> attribute = new BasicAttribute<String>();
        attribute.setId(getId());
        attribute.getValues().add(tokenEntry.getId());

        return attribute;
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
    public void setTokenLiftetime(long lifetime) {
        idLifetime = lifetime;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {

    }
}
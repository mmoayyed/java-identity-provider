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

package net.shibboleth.idp.saml.nameid.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.codec.Base32Support;
import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.saml.common.SAMLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The basis of a {@link PersistentIdGenerationStrategy} that generates a unique ID by computing the hash of
 * a given attribute value, the entity ID of the inbound message issuer, and a provided salt.
 * 
 * <p>The original implementation and values in common use relied on base64 encoding of the result,
 * but due to discovery of the lack of appropriate case handling of identifiers by applications, the
 * ability to use base32 has been added to eliminate the possibility of case conflicts.</p> 
 */
public class ComputedPersistentIdGenerationStrategy extends AbstractInitializableComponent
        implements PersistentIdGenerationStrategy {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ComputedPersistentIdGenerationStrategy.class);

    /** Post-digest encoding types. */
    public enum Encoding {
        /** Use Base64 encoding. */
        BASE64,
        
        /** Use Base32 encoding. */
        BASE32,
    };

    /** Salt used when computing the ID. */
    @NonnullAfterInit private byte[] salt;

    /** JCE digest algorithm name to use. */
    @Nonnull @NotEmpty private String algorithm;

    /** The encoding to apply to the digest. */
    @Nonnull private Encoding encoding;
    
    /** Constructor. */
    public ComputedPersistentIdGenerationStrategy() {
        algorithm = "SHA";
        encoding = Encoding.BASE64;
    }
    
    /**
     * Get the salt used when computing the ID.
     * 
     * @return salt used when computing the ID
     */
    @NonnullAfterInit public byte[] getSalt() {
        return salt;
    }

    /**
     * Set the salt used when computing the ID.
     * 
     * <p>An empty/null input is ignored.</p>
     * 
     * @param newValue used when computing the ID
     */
    public void setSalt(@Nullable final byte[] newValue) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        if (newValue != null && newValue.length > 0) {
            salt = newValue;
        }
    }
    
    /**
     * Set the base64-encoded salt used when computing the ID.
     * 
     * <p>An empty/null input is ignored.</p>
     * 
     * @param newValue used when computing the ID
     * 
     * @since 3.3.0
     */
    public void setEncodedSalt(@Nullable final String newValue) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        if (newValue != null && !newValue.isEmpty()) {
            salt = Base64Support.decode(newValue);
        }
    }

    /**
     * Set the JCE algorithm name of the digest algorithm to use (default is SHA).
     * 
     * @param alg JCE message digest algorithm
     */
    public void setAlgorithm(@Nonnull @NotEmpty final String alg) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        algorithm = Constraint.isNotNull(StringSupport.trimOrNull(alg), "Digest algorithm cannot be null or empty");
    }
    
    /**
     * Set the post-digest encoding to use.
     * 
     * @param enc encoding
     */
    public void setEncoding(@Nonnull final Encoding enc) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        encoding = Constraint.isNotNull(enc, "Encoding cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (null == getSalt()) {
            throw new ComponentInitializationException("Salt cannot be null");
        }

        if (getSalt().length < 16) {
            throw new ComponentInitializationException("Salt must be at least 16 bytes in size");
        }

    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty public String generate(@Nonnull @NotEmpty final String assertingPartyId,
            @Nonnull @NotEmpty final String relyingPartyId, @Nonnull @NotEmpty final String principalName,
            @Nonnull @NotEmpty final String sourceId) throws SAMLException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        try {
            final MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(relyingPartyId.getBytes());
            md.update((byte) '!');
            md.update(sourceId.getBytes());
            md.update((byte) '!');

            if (encoding == Encoding.BASE32) {
                return Base32Support.encode(md.digest(salt), Base32Support.UNCHUNKED);
            } else if (encoding == Encoding.BASE64) {
                return Base64Support.encode(md.digest(salt), Base64Support.UNCHUNKED);
            } else {
                throw new SAMLException("Desired encoding was not recognized, unable to compute ID");
            }
        } catch (final NoSuchAlgorithmException e) {
            log.error("Digest algorithm {} is not supported", algorithm);
            throw new SAMLException("Digest algorithm was not supported, unable to compute ID", e);
        }
    }
    
}
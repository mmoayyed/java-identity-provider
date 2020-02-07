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

package net.shibboleth.idp.attribute.impl;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.PairwiseId;
import net.shibboleth.idp.attribute.PairwiseIdStore;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.codec.Base32Support;
import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.codec.DecodingException;
import net.shibboleth.utilities.java.support.codec.EncodingException;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link PairwiseIdStore} that generates a pairwise ID by computing the hash of
 * a given attribute value, the entity ID of the recipient, and a provided salt.
 * 
 * <p>The original implementation and values in common use relied on base64 encoding of the result,
 * but due to discovery of the lack of appropriate case handling of identifiers by applications, the
 * ability to use base32 has been added to eliminate the possibility of case conflicts.</p>
 * 
 * @since 4.0.0
 */
public class ComputedPairwiseIdStore extends AbstractInitializableComponent implements PairwiseIdStore {

    /** An override trigger to apply to all relying parties. */
    @Nonnull @NotEmpty public static final String WILDCARD_OVERRIDE = "*";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ComputedPairwiseIdStore.class);

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
    
    /** Override map to block or re-issue identifiers. */
    @Nonnull private Map<String,Map<String,String>> exceptionMap;
    
    /** Constructor. */
    public ComputedPairwiseIdStore() {
        algorithm = "SHA";
        encoding = Encoding.BASE64;
        exceptionMap = Collections.emptyMap();
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
     * Set the salt used when computing the ID.
     * 
     * <p>An empty/null input is ignored.</p>
     * 
     * @param newValue used when computing the ID
     */
    public void setSalt(@Nullable final String newValue) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        if (newValue != null && !newValue.isEmpty()) {
            salt = newValue.getBytes();
        }
    }
    
    /**
     * Set the base64-encoded salt used when computing the ID.
     * 
     * <p>An empty/null input is ignored.</p>
     * 
     * @param newValue used when computing the ID
     */
    public void setEncodedSalt(@Nullable final String newValue) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        if (newValue != null && !newValue.isEmpty()) {
            try {
                salt = Base64Support.decode(newValue);
            } catch (final DecodingException e) {
                //convert back to an illegal argument exception as salt is invalid base64.
               throw new IllegalArgumentException("Can not decode base64 encoded salt value",e);
            }
        }
    }

    /**
     * Get the JCE algorithm name of the digest algorithm to use (default is SHA).
     * 
     * @return JCE message digest algorithm
     */
    @Nonnull @NotEmpty public String getAlgorithm() {
        return algorithm;
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
     * Get the post-digest encoding to use.
     * 
     * @return encoding
     */
    @Nonnull public Encoding getEncoding() {
        return encoding;
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
   
    /**
     * Install map of exceptions that override standard generation.
     * 
     * <p>The map is keyed by principal name (or '*' for all), and the values are a map of relying party
     * to salt overrides. A relying party of '*' applies to all parties. A null mapped value implies that
     * no value should be generated, while a string value is fed into the computation in place of the default
     * salt. Specific rules trump wildcarded rules.</p> 
     * 
     * @param map exceptions to apply
     */
    public void setExceptionMap(@Nullable @NotEmpty final Map<String,Map<String,String>> map) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        if (map == null) {
            exceptionMap = Collections.emptyMap();
        } else {
            exceptionMap = new HashMap<>(map.size());
            for (final Map.Entry<String,Map<String,String>> entry : map.entrySet()) {
                final String principal = StringSupport.trimOrNull(entry.getKey());
                if (principal != null && entry.getValue() != null) {
                    final Map<String,String> overrides = new HashMap<>(entry.getValue().size());
                    for (final Map.Entry<String,String> subentry : entry.getValue().entrySet()) {
                        final String rpname = StringSupport.trimOrNull(subentry.getKey());
                        if (rpname != null) {
                            final String override = StringSupport.trimOrNull(subentry.getValue());
                            overrides.put(rpname, override);
                        }
                    }
                    exceptionMap.put(principal, overrides);
                }
            }
        }
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
    @Nullable public PairwiseId getBySourceValue(@Nonnull final PairwiseId pid, final boolean allowCreate)
            throws IOException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(pid, "Input PairwiseId object cannot be null");
        Constraint.isNotEmpty(pid.getRecipientEntityID(), "Recipient entityID cannot be null or empty");
        Constraint.isNotEmpty(pid.getPrincipalName(), "Principal name cannot be null or empty");
        Constraint.isNotEmpty(pid.getSourceSystemId(), "Source system ID cannot be null or empty");
    
        final byte[] effectiveSalt = getEffectiveSalt(pid.getPrincipalName(), pid.getRecipientEntityID());
        if (effectiveSalt == null) {
            log.warn("Pairwise ID generation blocked for relying party ({})", pid.getRecipientEntityID());
            throw new IOException("Pairwise ID generation blocked by exception rule");
        }
        
        try {
            final MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(pid.getRecipientEntityID().getBytes());
            md.update((byte) '!');
            md.update(pid.getSourceSystemId().getBytes());
            md.update((byte) '!');

            if (encoding == Encoding.BASE32) {
                pid.setPairwiseId(Base32Support.encode(md.digest(effectiveSalt), Base32Support.UNCHUNKED));
            } else if (encoding == Encoding.BASE64) {
                pid.setPairwiseId(Base64Support.encode(md.digest(effectiveSalt), Base64Support.UNCHUNKED));
            } else {
                throw new IOException("Desired encoding was not recognized, unable to compute ID");
            }
        } catch (final NoSuchAlgorithmException e) {
            log.error("Digest algorithm {} is not supported", algorithm);
            throw new IOException("Digest algorithm was not supported, unable to compute ID", e);
        } catch (final EncodingException e) {
            log.error("Unable to {} encode digest",encoding);
            throw new IOException("Unable to either base64 or base32 encode digest, unable to compute ID", e);
        }
        
        return pid;
    }
    
    /**
     * Get the effective salt to apply for a particular principal/RP pair, or null to refuse to generate one.
     * 
     * @param principalName name of subject
     * @param relyingPartyId name of relying party scope
     * 
     * @return salt to use
     */
    @Nullable private byte[] getEffectiveSalt(@Nonnull @NotEmpty final String principalName,
            @Nonnull @NotEmpty final String relyingPartyId) {
        
        Map<String,String> override = exceptionMap.get(principalName);
        if (override == null) {
            override = exceptionMap.get(WILDCARD_OVERRIDE);
        }
        
        if (override != null) {
            if (override.containsKey(relyingPartyId)) {
                final String s = override.get(relyingPartyId);
                if (s != null) {
                    log.debug("Overriding salt for principal '{}' and relying party '{}'", principalName,
                            relyingPartyId);
                    return s.getBytes();
                }
                log.debug("Blocked generation of ID for principal '{}' for relying party '{}'",
                        principalName, relyingPartyId);
                return null;
            } else if (override.containsKey(WILDCARD_OVERRIDE)) {
                final String s = override.get(WILDCARD_OVERRIDE);
                if (s != null) {
                    log.debug("Overriding salt for principal '{}' and relying party '{}'", principalName,
                            relyingPartyId);
                    return s.getBytes();
                }
                log.debug("Blocked generation of ID for principal '{}' for relying party '{}'",
                        principalName, relyingPartyId);
                return null;
            }
        }
        
        return salt;
    }
    
}
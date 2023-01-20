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

package net.shibboleth.idp.saml.saml2.profile.config;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.saml.profile.config.AbstractSAMLProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NonNegative;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.logic.PredicateSupport;
import net.shibboleth.shared.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;

/** Base class for SAML 2 profile configurations. */
public abstract class AbstractSAML2ProfileConfiguration extends AbstractSAMLProfileConfiguration implements
        SAML2ProfileConfiguration {

    /** Whether to ignore signatures in requests. */
    @Nonnull private Predicate<ProfileRequestContext> ignoreRequestSignaturesPredicate;
    
    /** Whether encryption is optional in the face of no key, etc. */
    @Nonnull private Predicate<ProfileRequestContext> encryptionOptionalPredicate;
    
    /** Predicate used to determine if assertions should be encrypted. */
    @Nonnull private Predicate<ProfileRequestContext> encryptAssertionsPredicate;

    /** Predicate used to determine if name identifiers should be encrypted. */
    @Nonnull private Predicate<ProfileRequestContext> encryptNameIDsPredicate;

    /** Predicate used to determine if attributes should be encrypted. */
    @Nonnull private Predicate<ProfileRequestContext> encryptAttributesPredicate;

    /** Lookup function to supply proxyCount property. */
    @Nonnull private Function<ProfileRequestContext,Integer> proxyCountLookupStrategy;

    /** Lookup function to supply proxy audiences. */
    @Nonnull private Function<ProfileRequestContext,Collection<String>> proxyAudiencesLookupStrategy;

    /**
     * Constructor.
     * 
     * @param profileId ID of the communication profile, never null or empty
     */
    public AbstractSAML2ProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);

        ignoreRequestSignaturesPredicate = PredicateSupport.alwaysFalse();
        encryptionOptionalPredicate = PredicateSupport.alwaysFalse();
        encryptAssertionsPredicate = PredicateSupport.alwaysFalse();
        encryptNameIDsPredicate = PredicateSupport.alwaysFalse();
        encryptAttributesPredicate = PredicateSupport.alwaysFalse();
        proxyCountLookupStrategy = FunctionSupport.constant(null);
        proxyAudiencesLookupStrategy = FunctionSupport.constant(null);
    }

    /** {@inheritDoc} */
    @Nullable public Integer getProxyCount(@Nullable final ProfileRequestContext profileRequestContext) {
        final Integer count = proxyCountLookupStrategy.apply(profileRequestContext);
        if (count != null) {
            Constraint.isGreaterThanOrEqual(0, count, "Proxy count must be greater than or equal to 0");
        }
        return count;
    }

    /**
     * Set the maximum number of times an assertion may be proxied.
     * 
     * @param count maximum number of times an assertion may be proxied
     */
    public void setProxyCount(@Nullable @NonNegative final Integer count) {
        if (count != null) {
            Constraint.isGreaterThanOrEqual(0, count, "Proxy count must be greater than or equal to 0");
        }
        proxyCountLookupStrategy = FunctionSupport.constant(count);
    }

    /**
     * Set a lookup strategy for the maximum number of times an assertion may be proxied.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setProxyCountLookupStrategy(@Nonnull final Function<ProfileRequestContext,Integer> strategy) {
        proxyCountLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<String> getProxyAudiences(
            @Nullable final ProfileRequestContext profileRequestContext) {
        final Collection<String> audiences = proxyAudiencesLookupStrategy.apply(profileRequestContext);
        if (audiences != null) {
            return Set.copyOf(audiences);
        }
        return CollectionSupport.emptySet();
    }

    /**
     * Set the proxy audiences to be added to responses.
     * 
     * @param audiences proxy audiences to be added to responses
     */
    public void setProxyAudiences(@Nullable @NonnullElements final Collection<String> audiences) {
        if (audiences == null || audiences.isEmpty()) {
            proxyAudiencesLookupStrategy = FunctionSupport.constant(null);
        } else {
            proxyAudiencesLookupStrategy = FunctionSupport.constant(
                    List.copyOf(StringSupport.normalizeStringCollection(audiences)));
        }
    }

    /**
     * Set a lookup strategy for the proxy audiences to be added to responses.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setProxyAudiencesLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Collection<String>> strategy) {
        proxyAudiencesLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    public boolean isIgnoreRequestSignatures(@Nonnull final ProfileRequestContext profileRequestContext) {
        return ignoreRequestSignaturesPredicate.test(profileRequestContext);
    }
    
    /**
     * Sets whether to bypass verification of request signatures.
     * 
     * @param flag flag to set
     * 
     * @since 4.0.0
     */
    public void setIgnoreRequestSignatures(final boolean flag) {
        ignoreRequestSignaturesPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Sets a condition to determine whether to bypass verification of request signatures.
     * 
     * @param condition condition to set
     * 
     * @since 4.0.0
     */
    public void setIgnoreRequestSignaturesPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        ignoreRequestSignaturesPredicate = Constraint.isNotNull(condition, "Condition cannot be null");
    }

    /** {@inheritDoc} */
    public boolean isEncryptionOptional(@Nullable final ProfileRequestContext profileRequestContext) {
        return encryptionOptionalPredicate.test(profileRequestContext);
    }
    
    /**
     * Set whether encryption is optional in the face of a missing key, etc.
     * 
     * @param flag  flag to set
     */
    public void setEncryptionOptional(final boolean flag) {
        encryptionOptionalPredicate = PredicateSupport.constant(flag);
    }

    /**
     * Set a condition to determine whether encryption is optional in the face of a missing key, etc.
     *
     * @param condition condition to set
     * 
     * @since 3.3.0
     */
    public void setEncryptionOptionalPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        encryptionOptionalPredicate = Constraint.isNotNull(condition, "Encryption optional predicate cannot be null");
    }
    
    /** {@inheritDoc} */
    public boolean isEncryptAssertions(@Nullable final ProfileRequestContext profileRequestContext) {
        return encryptAssertionsPredicate.test(profileRequestContext);
    }

    /**
     * Set whether assertions should be encrypted.
     * 
     * @param flag  flag to set
     */
    public void setEncryptAssertions(final boolean flag) {
        encryptAssertionsPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set the predicate used to determine if assertions should be encrypted.
     * 
     * @param predicate predicate used to determine if assertions should be encrypted
     * 
     * @since 4.0.0
     */
    public void setEncryptAssertionsPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        encryptAssertionsPredicate = Constraint.isNotNull(predicate, "Condition cannot be null");
    }

    /** {@inheritDoc} */
    public boolean isEncryptNameIDs(@Nullable final ProfileRequestContext profileRequestContext) {
        return encryptNameIDsPredicate.test(profileRequestContext);
    }

    /**
     * Set whether name identifiers should be encrypted.
     * 
     * @param flag  flag to set
     */
    public void setEncryptNameIDs(final boolean flag) {
        encryptNameIDsPredicate = PredicateSupport.constant(flag);
    }

    /**
     * Set the predicate used to determine if name identifiers should be encrypted.
     * 
     * @param predicate predicate used to determine if name identifiers should be encrypted
     * 
     * @since 4.0.0
     */
    public void setEncryptNameIDsPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        encryptNameIDsPredicate = Constraint.isNotNull(predicate, "Condition cannot be null");
    }

    /** {@inheritDoc} */
    public boolean isEncryptAttributes(@Nullable final ProfileRequestContext profileRequestContext) {
        return encryptAttributesPredicate.test(profileRequestContext);
    }

    /**
     * Set whether attributes should be encrypted.
     * 
     * @param flag  flag to set
     */
    public void setEncryptAttributes(final boolean flag) {
        encryptAttributesPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set the predicate used to determine if attributes should be encrypted.
     * 
     * @param predicate predicate used to determine if attributes should be encrypted
     * 
     * @since 4.0.0
     */
    public void setEncryptAttributesPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        encryptAttributesPredicate = Constraint.isNotNull(predicate, "Condition cannot be null");
    }

}
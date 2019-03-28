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
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.saml.profile.config.AbstractSAMLProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.CollectionSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Predicates;

/** Base class for SAML 2 profile configurations. */
public abstract class AbstractSAML2ProfileConfiguration extends AbstractSAMLProfileConfiguration implements
        SAML2ProfileConfiguration {
    
    /** Default proxy count. */
    @Nonnull public static final Long DEFAULT_PROXY_COUNT = 0L;

    /** Whether encryption is optional in the face of no key, etc. */
    @Nonnull private Predicate<ProfileRequestContext> encryptionOptionalPredicate;
    
    /** Predicate used to determine if assertions should be encrypted. */
    @Nonnull private Predicate<ProfileRequestContext> encryptAssertionsPredicate;

    /** Predicate used to determine if name identifiers should be encrypted. */
    @Nonnull private Predicate<ProfileRequestContext> encryptNameIDsPredicate;

    /** Predicate used to determine if attributes should be encrypted. */
    @Nonnull private Predicate<ProfileRequestContext> encryptAttributesPredicate;

    /** Lookup function to supply {@link #proxyCount} property. */
    @Nonnull private Function<ProfileRequestContext,Long> proxyCountLookupStrategy;

    /** Lookup function to supply proxy audiences. */
    @Nonnull private Function<ProfileRequestContext,Collection<String>> proxyAudiencesLookupStrategy;

    /**
     * Constructor.
     * 
     * @param profileId ID of the communication profile, never null or empty
     */
    public AbstractSAML2ProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);

        encryptionOptionalPredicate = Predicates.alwaysFalse();
        encryptAssertionsPredicate = Predicates.alwaysFalse();
        encryptNameIDsPredicate = Predicates.alwaysFalse();
        encryptAttributesPredicate = Predicates.alwaysFalse();
        proxyCountLookupStrategy = FunctionSupport.constant(DEFAULT_PROXY_COUNT);
        proxyAudiencesLookupStrategy = FunctionSupport.constant(null);
    }

    /** {@inheritDoc} */
    public long getProxyCount(@Nullable final ProfileRequestContext profileRequestContext) {
        final Long count = proxyCountLookupStrategy.apply(profileRequestContext);
        Constraint.isNotNull(count, "Proxy count cannot be null");
        Constraint.isGreaterThanOrEqual(0, count, "Proxy count must be greater than or equal to 0");
        return count;
    }

    /**
     * Set the maximum number of times an assertion may be proxied.
     * 
     * @param count maximum number of times an assertion may be proxied
     */
    public void setProxyCount(@NonNegative final long count) {
        proxyCountLookupStrategy = FunctionSupport.constant(
                Constraint.isGreaterThanOrEqual(0, count, "Proxy count must be greater than or equal to 0"));
    }

    /**
     * Set a lookup strategy for the maximum number of times an assertion may be proxied.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setProxyCountLookupStrategy(@Nonnull final Function<ProfileRequestContext,Long> strategy) {
        proxyCountLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Collection<String> getProxyAudiences(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return CollectionSupport.buildImmutableList(proxyAudiencesLookupStrategy.apply(profileRequestContext));
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
            proxyAudiencesLookupStrategy = FunctionSupport.constant(StringSupport.normalizeStringCollection(audiences));
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
    public boolean isEncryptionOptional(@Nullable final ProfileRequestContext profileRequestContext) {
        return encryptionOptionalPredicate.test(profileRequestContext);
    }
    
    /**
     * Set whether encryption is optional in the face of a missing key, etc.
     * 
     * @param flag  flag to set
     */
    public void setEncryptionOptional(final boolean flag) {
        encryptionOptionalPredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
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
        encryptAssertionsPredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
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
        encryptNameIDsPredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
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
        encryptAttributesPredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
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
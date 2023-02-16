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

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.logic.NoConfidentialityMessageChannelPredicate;
import org.opensaml.profile.logic.NoIntegrityMessageChannelPredicate;

import net.shibboleth.saml.saml2.profile.config.SAML2AssertionProducingProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.PredicateSupport;

/** Configuration support for SAML 2 artifact resolution requests. */
public class ArtifactResolutionProfileConfiguration extends AbstractSAML2ProfileConfiguration
        implements SAML2AssertionProducingProfileConfiguration {

    /** ID for this profile configuration. */
    @Nonnull @NotEmpty public static final String PROFILE_ID = "http://shibboleth.net/ns/profiles/saml2/query/artifact";

    /** Predicate used to determine if assertions should be encrypted. */
    @Nonnull private Predicate<ProfileRequestContext> encryptAssertionsPredicate;
    
    /** Predicate used to determine if attributes should be encrypted. */
    @Nonnull private Predicate<ProfileRequestContext> encryptAttributesPredicate;

    /** Constructor. */
    public ArtifactResolutionProfileConfiguration() {
        this(PROFILE_ID);
        encryptAssertionsPredicate = PredicateSupport.alwaysFalse();
        encryptAttributesPredicate = PredicateSupport.alwaysFalse();
    }

    /**
     * Constructor.
     * 
     * @param profileId unique ID for this profile
     */
    protected ArtifactResolutionProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);
        setSignResponsesPredicate(new NoIntegrityMessageChannelPredicate());
        setEncryptAssertionsPredicate(new NoConfidentialityMessageChannelPredicate());
        encryptAssertionsPredicate = PredicateSupport.alwaysFalse();
        encryptAttributesPredicate = PredicateSupport.alwaysFalse();
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
     */
    public void setEncryptAssertionsPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        encryptAssertionsPredicate = Constraint.isNotNull(predicate, "Condition cannot be null");
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
     */
    public void setEncryptAttributesPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        encryptAttributesPredicate = Constraint.isNotNull(predicate, "Condition cannot be null");
    }

}
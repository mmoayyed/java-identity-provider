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

package net.shibboleth.idp.saml.saml2.profile.config.impl;

import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.logic.NoIntegrityMessageChannelPredicate;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.saml.profile.config.SAMLArtifactAwareProfileConfiguration;
import net.shibboleth.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.saml.profile.config.SAMLArtifactConsumerProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.logic.PredicateSupport;

/**
 * Configuration support for artifact-aware profiles.
 * 
 * @since 3.4.0
 */
public abstract class AbstractSAML2ArtifactAwareProfileConfiguration extends AbstractSAML2ProfileConfiguration
        implements SAMLArtifactAwareProfileConfiguration, SAMLArtifactConsumerProfileConfiguration {
    
    /** Lookup function to supply artifactConfiguration property. */
    @Nonnull private Function<ProfileRequestContext,SAMLArtifactConfiguration> artifactConfigurationLookupStrategy;
    
    /** Predicate used to determine if artifact resolution requests should be signed. */
    @Nonnull private Predicate<MessageContext> signArtifactRequestsPredicate;
    
    /** Predicate used to determine if artifact resolution requests should use client TLS. */
    @Nonnull private Predicate<MessageContext> clientTLSArtifactRequestsPredicate;

    /**
     * Constructor.
     * 
     * @param profileId unique ID for this profile
     */
    protected AbstractSAML2ArtifactAwareProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);
        artifactConfigurationLookupStrategy = FunctionSupport.constant(null);
        signArtifactRequestsPredicate = new NoIntegrityMessageChannelPredicate();
        final Predicate<MessageContext> pred = new NoIntegrityMessageChannelPredicate().negate();
        assert pred != null;
        clientTLSArtifactRequestsPredicate = pred;
    }
    
    /** {@inheritDoc} */
    @Nullable public SAMLArtifactConfiguration getArtifactConfiguration(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return artifactConfigurationLookupStrategy.apply(profileRequestContext);
    }

    /**
     * Set the SAML artifact configuration, if any.
     * 
     * @param config configuration to set
     */
    public void setArtifactConfiguration(@Nullable final SAMLArtifactConfiguration config) {
        artifactConfigurationLookupStrategy = FunctionSupport.constant(config);
    }

    /**
     * Set a lookup strategy for the SAML artifact configuration.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setArtifactConfigurationLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLArtifactConfiguration> strategy) {
        artifactConfigurationLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    public boolean isSignArtifactRequests(@Nullable final MessageContext messageContext) {
        return signArtifactRequestsPredicate.test(messageContext);
    }

    /**
     * Set whether artifact resolution requests should be signed.
     * 
     * @param flag flag to set
     */
    public void setSignArtifactRequests(final boolean flag) {
        signArtifactRequestsPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set the predicate used to determine if artifact resolution requests should be signed.
     * 
     * @param predicate the predicate
     * 
     * @since 4.0.0
     */
    public void setSignArtifactRequestsPredicate(@Nonnull final Predicate<MessageContext> predicate) {
        signArtifactRequestsPredicate = Constraint.isNotNull(predicate, 
                "Predicate used to determine artifact request signing may not be null");
    }

    /** {@inheritDoc} */
    public boolean isClientTLSArtifactRequests(@Nullable final MessageContext messageContext) {
        return clientTLSArtifactRequestsPredicate.test(messageContext);
    }

    /**
     * Set whether artifact resolution requests should use client TLS.
     * 
     * @param flag flag to set
     */
    public void setClientTLSArtifactRequests(final boolean flag) {
        clientTLSArtifactRequestsPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set the predicate used to determine if artifact resolution requests should use client TLS.
     * 
     * @param predicate the predicate
     * 
     * @since 4.0.0
     */
    public void setClientTLSArtifactRequestsPredicate(@Nonnull final Predicate<MessageContext> predicate) {
        clientTLSArtifactRequestsPredicate = Constraint.isNotNull(predicate, 
                "Predicate used to determine artifact client TLS use may not be null");
    }

}
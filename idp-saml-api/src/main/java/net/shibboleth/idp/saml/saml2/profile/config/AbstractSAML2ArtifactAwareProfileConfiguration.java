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

import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.logic.NoIntegrityMessageChannelPredicate;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.saml.profile.config.SAMLArtifactAwareProfileConfiguration;
import net.shibboleth.idp.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.idp.saml.profile.config.SAMLArtifactConsumerProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Configuration support for artifact-aware profiles.
 * 
 * @since 3.4.0
 */
public abstract class AbstractSAML2ArtifactAwareProfileConfiguration
        extends AbstractSAML2ProfileConfiguration
        implements SAMLArtifactAwareProfileConfiguration, SAMLArtifactConsumerProfileConfiguration {

    /** Explicitly set artifact configuration. */
    @Nullable private SAMLArtifactConfiguration artifactConfiguration; 
    
    /** Lookup function to supply <code>artifactConfiguration</code> property. */
    @Nullable private Function<ProfileRequestContext,SAMLArtifactConfiguration> artifactConfigurationLookupStrategy;
    
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
        signArtifactRequestsPredicate = new NoIntegrityMessageChannelPredicate();
        clientTLSArtifactRequestsPredicate = new NoIntegrityMessageChannelPredicate().negate();
    }
    
    /** {@inheritDoc} */
    @Nullable public SAMLArtifactConfiguration getArtifactConfiguration() {
        return getIndirectProperty(artifactConfigurationLookupStrategy, artifactConfiguration);
    }

    /**
     * Set the SAML artifact configuration, if any.
     * 
     * @param config configuration to set
     */
    public void setArtifactConfiguration(@Nullable final SAMLArtifactConfiguration config) {
        artifactConfiguration = config;
    }

    /**
     * Set a lookup strategy for the <code>artifactConfiguration</code> property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setArtifactConfigurationLookupStrategy(
            @Nullable final Function<ProfileRequestContext,SAMLArtifactConfiguration> strategy) {
        artifactConfigurationLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    public Predicate<MessageContext> getSignArtifactRequests() {
        return signArtifactRequestsPredicate;
    }
    
    /**
     * Set the predicate used to determine if artifact resolution requests should be signed.
     * 
     * @param predicate the predicate
     */
    public void setSignArtifactRequests(@Nonnull final Predicate<MessageContext> predicate) {
        signArtifactRequestsPredicate = Constraint.isNotNull(predicate, 
                "Predicate used to determine artifact request signing may not be null");
    }

    /** {@inheritDoc} */
    public Predicate<MessageContext> getClientTLSArtifactRequests() {
        return clientTLSArtifactRequestsPredicate;
    }

    /**
     * Set the predicate used to determine if artifact resolution requests should use client TLS.
     * 
     * @param predicate the predicate
     */
    public void setClientTLSArtifactRequests(@Nonnull final Predicate<MessageContext> predicate) {
        clientTLSArtifactRequestsPredicate = Constraint.isNotNull(predicate, 
                "Predicate used to determine artifact client TLS use may not be null");
    }

}
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.saml.profile.config.SAMLArtifactAwareProfileConfiguration;
import net.shibboleth.idp.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

/**
 * Configuration support for artifact-aware profiles.
 * 
 * @since 3.4.0
 */
public abstract class AbstractSAML2ArtifactAwareProfileConfiguration
        extends AbstractSAML2ProfileConfiguration
        implements SAMLArtifactAwareProfileConfiguration {

    /** Lookup function to supply {@link #artifactConfig} property. */
    @Nonnull private Function<ProfileRequestContext,SAMLArtifactConfiguration> artifactConfigurationLookupStrategy;

    /**
     * Constructor.
     * 
     * @param profileId unique ID for this profile
     */
    protected AbstractSAML2ArtifactAwareProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);
        artifactConfigurationLookupStrategy = FunctionSupport.constant(null);
    }
    
    /** {@inheritDoc} */
    @Override @Nullable public SAMLArtifactConfiguration getArtifactConfiguration() {
        return artifactConfigurationLookupStrategy.apply(getProfileRequestContext());
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
     * Set a lookup strategy for the {@link #artifactConfig} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setArtifactConfigurationLookupStrategy(
            @Nullable final Function<ProfileRequestContext,SAMLArtifactConfiguration> strategy) {
        artifactConfigurationLookupStrategy = strategy != null
                ? strategy : FunctionSupport.<ProfileRequestContext,SAMLArtifactConfiguration>constant(null);
    }

}
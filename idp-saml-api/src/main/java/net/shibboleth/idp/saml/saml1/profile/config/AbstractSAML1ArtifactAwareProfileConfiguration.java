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

package net.shibboleth.idp.saml.saml1.profile.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.saml.profile.config.AbstractSAMLProfileConfiguration;
import net.shibboleth.idp.saml.profile.config.SAMLArtifactAwareProfileConfiguration;
import net.shibboleth.idp.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * Configuration support for artifact-aware profiles.
 * 
 * @since 3.4.0
 */
public abstract class AbstractSAML1ArtifactAwareProfileConfiguration
        extends AbstractSAMLProfileConfiguration
        implements SAML1ProfileConfiguration, SAMLArtifactAwareProfileConfiguration {

    /** Explicitly set artifact configuration. */
    @Nullable private SAMLArtifactConfiguration artifactConfiguration; 
    
    /** Lookup function to supply <code>artifactConfiguration</code> property. */
    @Nullable private Function<ProfileRequestContext,SAMLArtifactConfiguration> artifactConfigurationLookupStrategy;

    /**
     * Constructor.
     * 
     * @param profileId unique ID for this profile
     */
    protected AbstractSAML1ArtifactAwareProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);
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

}
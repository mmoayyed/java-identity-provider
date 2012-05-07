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

package net.shibboleth.idp.relyingparty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.BaseContext;

/**
 * {@link org.opensaml.messaging.context.Subcontext} containing relying party specific information. This is usually a
 * subcontext of a {@link net.shibboleth.idp.profile.ProfileRequestContext}.
 */
public final class RelyingPartyContext extends BaseContext {

    /** The identifier for the relying party. */
    private final String relyingPartyId;

    /** The relying party configuration. */
    private RelyingPartyConfiguration relyingPartyConfiguration;

    /** Profile configuration that is in use. */
    private ProfileConfiguration profileConfiguration;

    /**
     * Constructor.
     * 
     * @param rpId the relying party identifier, can not be null or empty
     */
    public RelyingPartyContext(@Nonnull @NotEmpty final String rpId) {
        relyingPartyId =
                Constraint.isNotNull(StringSupport.trimOrNull(rpId), "Relying party ID can not be null or empty");
    }

    /**
     * Gets the unique identifier of the relying party.
     * 
     * @return unique identifier of the relying party, never null or empty
     */
    @Nonnull @NotEmpty public String getRelyingPartyId() {
        return relyingPartyId;
    }

    /**
     * Gets the relying party configuration.
     * 
     * @return the relying party configuration, may be null
     */
    @Nullable public RelyingPartyConfiguration getConfiguration() {
        return relyingPartyConfiguration;
    }

    /**
     * Sets the configuration to use when processing requests for this relying party.
     * 
     * @param config configuration to use when processing requests for this relying party, may be null
     */
    public void setRelyingPartyConfiguration(@Nullable final RelyingPartyConfiguration config) {
        relyingPartyConfiguration = config;
    }

    /**
     * Gets the configuration for the request profile currently being processed.
     * 
     * @return profile configuration for the request profile currently being processed, may be null
     */
    @Nullable public ProfileConfiguration getProfileConfig() {
        return profileConfiguration;
    }

    /**
     * Sets the configuration for the request profile currently being processed.
     * 
     * @param config configuration for the request profile currently being processed, may be null
     */
    public void setProfileConfiguration(@Nullable final ProfileConfiguration config) {
        this.profileConfiguration = config;
    }
}
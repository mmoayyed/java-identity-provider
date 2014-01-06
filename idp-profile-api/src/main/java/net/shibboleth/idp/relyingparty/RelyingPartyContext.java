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

import javax.annotation.Nullable;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.BaseContext;

/**
 * {@link BaseContext} containing relying party specific information. This is usually a
 * subcontext of an {@link org.opensaml.profile.context.ProfileRequestContext}.
 */
public final class RelyingPartyContext extends BaseContext {

    /** The identifier for the relying party. */
    @Nullable private String relyingPartyId;

    /** The relying party configuration. */
    @Nullable private RelyingPartyConfiguration relyingPartyConfiguration;

    /** Profile configuration that is in use. */
    @Nullable private ProfileConfiguration profileConfiguration;

    /**
     * Get the unique identifier of the relying party.
     * 
     * @return unique identifier of the relying party
     */
    @Nullable public String getRelyingPartyId() {
        return relyingPartyId;
    }

    /**
     * Set the unique identifier of the relying party.
     * 
     * @param rpId the relying party identifier, or null
     */
    public void setRelyingPartyId(@Nullable final String rpId) {
        relyingPartyId = StringSupport.trimOrNull(rpId);
    }

    /**
     * Get the relying party configuration.
     * 
     * @return the relying party configuration, or null
     */
    @Nullable public RelyingPartyConfiguration getConfiguration() {
        return relyingPartyConfiguration;
    }

    /**
     * Set the configuration to use when processing requests for this relying party.
     * 
     * @param config configuration to use when processing requests for this relying party, or null
     */
    public void setConfiguration(@Nullable final RelyingPartyConfiguration config) {
        relyingPartyConfiguration = config;
    }

    /**
     * Get the configuration for the request profile currently being processed.
     * 
     * @return profile configuration for the request profile currently being processed, or null
     */
    @Nullable public ProfileConfiguration getProfileConfig() {
        return profileConfiguration;
    }

    /**
     * Set the configuration for the request profile currently being processed.
     * 
     * @param config configuration for the request profile currently being processed, or null
     */
    public void setProfileConfig(@Nullable final ProfileConfiguration config) {
        profileConfiguration = config;
    }
}
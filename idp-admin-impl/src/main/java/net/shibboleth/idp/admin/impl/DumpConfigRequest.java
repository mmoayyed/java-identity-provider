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

package net.shibboleth.idp.admin.impl;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

import com.google.common.base.MoreObjects;

/**
 * Object representing a request to mock a profile request to obtain the effective configuration.
 * 
 * @since 5.0.0
 */
@ThreadSafe
public class DumpConfigRequest {

    /** Profile identifier to simulate a response for. */
    @Nonnull @NotEmpty private final String profileId;

    /** Protocol identifier for metadata access. */
    @Nonnull @NotEmpty private final String protocolId;

    /** The ID of the requester. */
    @Nonnull @NotEmpty private final String requesterId;

    /**
     * Constructor.
     * 
     * @param profile profile ID
     * @param protocol protocol ID for metadata access
     * @param requester ID of requester
     */
    public DumpConfigRequest(@Nonnull final String profile, @Nonnull final String protocol,
            @Nonnull final String requester) {
        
        profileId = Constraint.isNotNull(StringSupport.trimOrNull(profile), "Profile ID cannot be null or empty");
        protocolId = Constraint.isNotNull(StringSupport.trimOrNull(protocol), "Protocol cannot be null or empty");
        requesterId = Constraint.isNotNull(StringSupport.trimOrNull(requester),
                "Requester name cannot be null or empty");
    }

    /**
     * Get the profile to simulate.
     * 
     * @return profile ID to simulate
     */
    @Nonnull @NotEmpty public String getProfileId() {
        return profileId;
    }

    /**
     * Get the protocol for metadata access.
     * 
     * @return protocol for metadata access
     */
    @Nonnull @NotEmpty public String getProtocolId() {
        return protocolId;
    }

    /**
     * Get the ID of the requesting relying party.
     * 
     * @return ID of the requesting relying party
     */
    @Nonnull @NotEmpty public String getRequesterId() {
        return requesterId;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("profileId", profileId)
                .add("protocolId", protocolId)
                .add("requesterId", requesterId)
                .toString();
    }
    
}
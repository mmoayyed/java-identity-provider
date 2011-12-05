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

import net.shibboleth.idp.profile.config.ProfileConfiguration;

import org.opensaml.messaging.context.AbstractSubcontextContainer;
import org.opensaml.messaging.context.Subcontext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;

/**
 * {@link org.opensaml.messaging.context.Subcontext} containing relying party specific information. This is usually a
 * subcontext of a {@link net.shibboleth.idp.profile.ProfileRequestContext}.
 */
public final class RelyingPartySubcontext extends AbstractSubcontextContainer implements Subcontext {

    /** The parent of this context. */
    private SubcontextContainer parent;

    /** The identifier for the relying party. */
    private final String relyingPartyId;

    /** The relying party configuration. */
    private RelyingPartyConfiguration relyingPartyConfiguration;

    /** Profile configuration that is in use. */
    private ProfileConfiguration profileConfiguration;

    /**
     * Constructor.
     * 
     * @param owner owner of this context, may be null
     * @param rpId the relying party identifier, can not be null or empty
     */
    public RelyingPartySubcontext(SubcontextContainer owner, String rpId) {
        super();

        relyingPartyId = Assert.isNotNull(StringSupport.trimOrNull(rpId), "Relying party ID can not be null or empty");

        if (owner != null) {
            parent = owner;
            owner.addSubcontext(this);
        }
    }

    /** {@inheritDoc} */
    public SubcontextContainer getOwner() {
        return parent;
    }

    /**
     * Gets the unique identifier of the relying party.
     * 
     * @return unique identifier of the relying party, never null or empty
     */
    public String getRelyingPartyId() {
        return relyingPartyId;
    }

    /**
     * Gets the relying party configuration.
     * 
     * @return the relying party configuration, may be null
     */
    public RelyingPartyConfiguration getConfiguration() {
        return relyingPartyConfiguration;
    }

    /**
     * Sets the configuration to use when processing requests for this relying party.
     * 
     * @param config configuration to use when processing requests for this relying party, may be null
     */
    public void setRelyingPartyConfiguration(RelyingPartyConfiguration config) {
        relyingPartyConfiguration = config;
    }

    /**
     * Gets the configuration for the request profile currently being processed.
     * 
     * @return profile configuration for the request profile currently being processed, may be null
     */
    public ProfileConfiguration getProfileConfig() {
        return profileConfiguration;
    }

    /**
     * Sets the configuration for the request profile currently being processed.
     * 
     * @param config configuration for the request profile currently being processed, may be null
     */
    public void setProfileConfiguration(ProfileConfiguration config) {
        this.profileConfiguration = config;
    }
}
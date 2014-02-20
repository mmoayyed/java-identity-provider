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

package net.shibboleth.idp.profile.context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;

/**
 * {@link BaseContext} containing relying party specific information, usually a
 * subcontext of {@link org.opensaml.profile.context.ProfileRequestContext}.
 */
public final class RelyingPartyContext extends BaseContext {

    /** The identifier for the relying party. */
    @Nullable private String relyingPartyId;

    /** A pointer to a context tree containing identifying material for the relying party. */
    @Nullable private BaseContext relyingPartyIdContextTree;
    
    /** A lookup stratgy for deriving a relying party ID based on contained information. */
    @Nullable private ContextDataLookupFunction<RelyingPartyContext,String> relyingPartyIdLookupStrategy;
    
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
        
        if (relyingPartyId != null) {
            return relyingPartyId;
        } else {
            return relyingPartyIdLookupStrategy.apply(this);
        }
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
     * Get the context tree containing identifying information for this relying party.
     * 
     * <p>The subtree root may, but need not, be an actual subcontext of this context.</p>
     * 
     * @return context tree
     */
    @Nullable public BaseContext getRelyingPartyIdContextTree() {
        return relyingPartyIdContextTree;
    }
    
    /**
     * Set the context tree containing identifying information for this relying party.
     * 
     * <p>The subtree root may, but need not, be an actual subcontext of this context.</p>
     * 
     * @param root  root of context tree
     */
    public void setRelyingPartyIdContextTree(@Nullable final BaseContext root) {
        relyingPartyIdContextTree = root;
    }
    
    /**
     * Get the lookup strategy for a non-explicit relying party ID.
     * 
     * @return lookup strategy
     */
    @Nullable ContextDataLookupFunction<RelyingPartyContext,String> getRelyingPartyIdLookupStrategy() {
        return relyingPartyIdLookupStrategy;
    }
    
    /**
     * Set the lookup strategy for a non-explicit relying party ID.
     * 
     * @param strategy  lookup strategy
     */
    public void setRelyingPartyIdLookupStrategy(
            @Nonnull final ContextDataLookupFunction<RelyingPartyContext,String> strategy) {
        relyingPartyIdLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
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
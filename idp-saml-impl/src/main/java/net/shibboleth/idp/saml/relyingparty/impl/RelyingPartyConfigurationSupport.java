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

package net.shibboleth.idp.saml.relyingparty.impl;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.shibboleth.idp.profile.logic.RelyingPartyIdPredicate;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.saml.profile.logic.EntitiesDescriptorPredicate;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * Support functions for building {@link RelyingPartyConfiguration} objects with SAML functionality.
 */
public final class RelyingPartyConfigurationSupport {
    
    /** Constructor. */
    private RelyingPartyConfigurationSupport() {
        
    }
    
    /**
     * A shorthand method for constructing a {@link RelyingPartyConfiguration} with an activation condition based on
     * one or more relying party IDs.
     * 
     * <p>If a single ID is supplied, then the ID is also set as the identifier for the configuration.</p>
     * 
     * @param relyingPartyIds the relying parties for which the configuration should be active
     * @return  a default-constructed configuration with the appropriate condition set
     */
    @Nonnull public static RelyingPartyConfiguration byName(
            @Nonnull @NonnullElements final Collection<String> relyingPartyIds) {
        final RelyingPartyConfiguration config = new RelyingPartyConfiguration();
        config.setActivationCondition(new RelyingPartyIdPredicate(relyingPartyIds));
        if (relyingPartyIds.size() == 1) {
            config.setId(relyingPartyIds.iterator().next());
        }
        return config;
    }

    /**
     * A shorthand method for constructing a {@link RelyingPartyConfiguration} with an activation condition based on
     * an {@link org.opensaml.saml.saml2.metadata.EntitiesDescriptor} group.
     * 
     * @param name the group name
     * @return  a default-constructed configuration with the appropriate condition set
     */
    @Nonnull public static RelyingPartyConfiguration byGroup(@Nonnull @NotEmpty final String name) {
        final RelyingPartyConfiguration config = new RelyingPartyConfiguration();
        config.setId(name);
        config.setActivationCondition(new EntitiesDescriptorPredicate());
        return config;
    }

}
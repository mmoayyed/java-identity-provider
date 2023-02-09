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

import org.opensaml.security.config.SecurityConfiguration;

import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.IdentifiedComponent;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.Resolver;

/** Resolves a {@link RelyingPartyConfiguration} for a given {@link CriteriaSet}. */
public interface CriteriaRelyingPartyConfigurationResolver extends Resolver<RelyingPartyConfiguration,CriteriaSet>,
        IdentifiedComponent {

    /**
     * Return the default security configuration for the profile.
     * 
     * @param profileId the profile ID (available via {@link ProfileConfiguration#getId()}
     * 
     * @return the configured default configuration
     */
    @Nullable SecurityConfiguration getDefaultSecurityConfiguration(@Nonnull @NotEmpty final String profileId);
    
}
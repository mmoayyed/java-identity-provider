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

package net.shibboleth.idp.saml.saml2.profile.config.impl;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.primitive.StringSupport;

/** Configuration support for IdP SAML 2.0 ECP profile. */
public class ECPProfileConfiguration extends BrowserSSOProfileConfiguration 
        implements net.shibboleth.saml.saml2.profile.config.ECPProfileConfiguration {

    /** Name of profile counter. */
    @Nonnull @NotEmpty public static final String PROFILE_COUNTER = "net.shibboleth.idp.profiles.saml2.sso.ecp";

    /** Lookup function to supply Local error events to handle without a SOAP fault. */
    @Nonnull private Function<ProfileRequestContext,Set<String>> localEventsLookupStrategy;
        
    /** Constructor. */
    public ECPProfileConfiguration() {
        this(net.shibboleth.saml.saml2.profile.config.ECPProfileConfiguration.PROFILE_ID);
        
        localEventsLookupStrategy = FunctionSupport.constant(null);
    }

    /**
     * Constructor.
     * 
     * @param profileId unique ID for this profile
     */
    protected ECPProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);
        
        localEventsLookupStrategy = FunctionSupport.constant(null);
    }

    /**
     * Get the set of local events to handle without a SOAP fault.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return  truly local events
     * 
     * @since 3.3.0
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<String> getLocalEvents(
            @Nullable final ProfileRequestContext profileRequestContext) {
        
        final Set<String> events = localEventsLookupStrategy.apply(profileRequestContext);
        if (events != null) {
            return CollectionSupport.copyToSet(events);
        }
        return CollectionSupport.emptySet();
    }

    /**
     * Set the local events to handle without a SOAP fault.
     * 
     * @param events truly local events
     * 
     * @since 3.3.0
     */
    public void setLocalEvents(@Nullable @NonnullElements final Collection<String> events) {

        if (events != null && !events.isEmpty()) {
            localEventsLookupStrategy = FunctionSupport.constant(
                    Set.copyOf(StringSupport.normalizeStringCollection(events)));
        } else {
            localEventsLookupStrategy = FunctionSupport.constant(null);
        }
    }

    /**
     * Set a lookup strategy for the local events to handle without a SOAP fault.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setLocalEventsLookupStrategy(@Nonnull final Function<ProfileRequestContext,Set<String>> strategy) {
        localEventsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
}
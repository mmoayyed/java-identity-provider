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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** Configuration support for SAML 2 ECP. */
public class ECPProfileConfiguration extends BrowserSSOProfileConfiguration {

    /** ID for this profile configuration. */
    public static final String PROFILE_ID = "http://shibboleth.net/ns/profiles/saml2/sso/ecp";

    /** Lookup function to supply {@link #localEvents} property. */
    @Nullable private Function<ProfileRequestContext,Set<String>> localEventsLookupStrategy;

    /** Local error events to handle without a SOAP fault. */
    @Nonnull @NonnullElements private Set<String> localEvents;
        
    /** Constructor. */
    public ECPProfileConfiguration() {
        this(PROFILE_ID);
        
        localEvents = Collections.emptySet();
    }

    /**
     * Constructor.
     * 
     * @param profileId unique ID for this profile
     */
    protected ECPProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);
    }

    /**
     * Get the set of local events to handle without a SOAP fault.
     * 
     * @return  truly local events
     * 
     * @since 3.3.0
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<String> getLocalEvents() {
        return ImmutableSet.copyOf(getIndirectProperty(localEventsLookupStrategy, localEvents));
    }

    /**
     * Set the local events to handle without a SOAP fault.
     * 
     * @param events truly local events
     * 
     * @since 3.3.0
     */
    public void setLocalEvents(@Nullable @NonnullElements final Collection<String> events) {

        if (events != null) {
            localEvents = new HashSet<>(StringSupport.normalizeStringCollection(events));
        } else {
            localEvents = Collections.emptySet();
        }
    }

    /**
     * Set a lookup strategy for the {@link #localEvents} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setAuthenticationFlowsLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Set<String>> strategy) {
        localEventsLookupStrategy = strategy;
    }
    
}
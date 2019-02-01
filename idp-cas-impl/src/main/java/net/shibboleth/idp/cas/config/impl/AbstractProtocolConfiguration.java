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

package net.shibboleth.idp.cas.config.impl;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.cas.ticket.impl.TicketIdentifierGenerationStrategy;
import net.shibboleth.idp.profile.config.AbstractConditionalProfileConfiguration;
import net.shibboleth.idp.profile.config.SecurityConfiguration;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.InitializableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Predicates;

/**
 * Base class for CAS protocol configuration.
 * 
 * @author Marvin S. Addison
 */
public abstract class AbstractProtocolConfiguration extends AbstractConditionalProfileConfiguration implements
        InitializableComponent {

    /** CAS base protocol URI. */
    public static final String PROTOCOL_URI = "https://www.apereo.org/cas/protocol";

    /** Lookup function to supply {@link #ticketValidityPeriod} property. */
    @Nullable private Function<ProfileRequestContext, Long> ticketValidityPeriodLookupStrategy;

    /** Validity time period of tickets. */
    @Duration @Positive private long ticketValidityPeriod;

    /** Whether attributes should be resolved in the course of the profile. */
    @Nonnull private Predicate<ProfileRequestContext> resolveAttributesPredicate;

    /**
     * Creates a new configuration instance.
     * 
     * @param profileId Unique profile identifier.
     */
    public AbstractProtocolConfiguration(@Nonnull @NotEmpty final String profileId) {
        this(profileId, 15000L);
    }

    /**
     * Creates a new configuration instance.
     *
     * @param profileId Unique profile identifier.
     * @param ticketTTL Ticket validity period in milliseconds.
     */
    public AbstractProtocolConfiguration(@Nonnull @NotEmpty final String profileId, @Positive final long ticketTTL) {
        super(profileId);
        resolveAttributesPredicate = Predicates.alwaysTrue();
        ticketValidityPeriod = ticketTTL;
        setSecurityConfiguration(new SecurityConfiguration(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES),
                new TicketIdentifierGenerationStrategy(getDefaultTicketPrefix(), getDefaultTicketLength())));
    }

    /** {@inheritDoc} */
    @Override public void doInitialize() throws ComponentInitializationException {
        Constraint.isNotNull(getSecurityConfiguration(), "Security configuration cannot be null.");
        Constraint.isNotNull(getSecurityConfiguration().getIdGenerator(),
                "Security configuration ID generator cannot be null.");
    }

    /**
     * Get ticket validity period.
     * 
     * @return Ticket validity period in milliseconds.
     */
    @Positive @Duration public long getTicketValidityPeriod() {
        return Constraint.isGreaterThan(0,
                getIndirectProperty(ticketValidityPeriodLookupStrategy, ticketValidityPeriod),
                "Ticket validity period must be positive.");
    }

    /**
     * Sets the ticket validity period.
     * 
     * @param millis Ticket validity period in milliseconds.
     */
    @Duration public void setTicketValidityPeriod(@Duration @Positive final long millis) {
        ticketValidityPeriod = Constraint.isGreaterThan(0, millis, "Ticket validity period must be positive.");
    }

    /**
     * Set a lookup strategy for the {@link #ticketValidityPeriod} property.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.3.0
     */
    public void setTicketValidityPeriodLookupStrategy(@Nullable final Function<ProfileRequestContext, Long> strategy) {
        ticketValidityPeriodLookupStrategy = strategy;
    }

    /**
     * Get whether attributes should be resolved during the profile.
     * 
     * <p>
     * Default is true
     * </p>
     * 
     * @return true iff attributes should be resolved
     * 
     * @deprecated Use {@link #getResolveAttributesPredicate()} instead.
     */
    @Deprecated
    public boolean isResolveAttributes() {
        return resolveAttributesPredicate.test(getProfileRequestContext());
    }

    /**
     * Set whether attributes should be resolved during the profile.
     * 
     * @param flag flag to set
     */
    public void setResolveAttributes(final boolean flag) {
        resolveAttributesPredicate =
                flag ? Predicates.<ProfileRequestContext> alwaysTrue() : Predicates
                        .<ProfileRequestContext> alwaysFalse();
    }

    /**
     * Get a condition to determine whether attributes should be resolved during the profile.
     * 
     * @return condition
     * 
     * @since 3.3.0
     */
    @Nonnull public Predicate<ProfileRequestContext> getResolveAttributesPredicate() {
        return resolveAttributesPredicate;
    }

    /**
     * Set a condition to determine whether attributes should be resolved during the profile.
     * 
     * @param condition condition to set
     * 
     * @since 3.3.0
     */
    public void setResolveAttributesPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        resolveAttributesPredicate = Constraint.isNotNull(condition, "Resolve attributes predicate cannot be null");
    }

    /**
     * Get default ticket prefix.
     * 
     * @return prefix
     */
    @Nonnull @NotEmpty protected abstract String getDefaultTicketPrefix();

    /**
     * Get default ticket length.
     * 
     * @return length
     */
    protected abstract int getDefaultTicketLength();

}
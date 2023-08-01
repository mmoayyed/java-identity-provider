/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.cas.config;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.security.config.BasicSecurityConfiguration;
import org.opensaml.security.config.SecurityConfiguration;

import net.shibboleth.idp.cas.ticket.TicketIdentifierGenerationStrategy;
import net.shibboleth.idp.profile.config.AbstractInterceptorAwareProfileConfiguration;
import net.shibboleth.profile.config.AttributeResolvingProfileConfiguration;
import net.shibboleth.shared.annotation.ConfigurationSetting;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.InitializableComponent;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.logic.PredicateSupport;

/**
 * Base class for CAS protocol configuration.
 * 
 * @author Marvin S. Addison
 */
public abstract class AbstractProtocolConfiguration extends AbstractInterceptorAwareProfileConfiguration implements
        AttributeResolvingProfileConfiguration, InitializableComponent {

    /** CAS base protocol URI. */
    @Nonnull @NotEmpty public static final String PROTOCOL_URI = "https://www.apereo.org/cas/protocol";

    /** CAS base profile counter prefix. */
    @Nonnull @NotEmpty public static final String PROTOCOL_COUNTER = "net.shibboleth.idp.profiles.cas";
    
    /** Default ticket validity. */
    @SuppressWarnings("null")
    @Nonnull public static final Duration DEFAULT_TICKET_VALIDITY_PERIOD = Duration.ofSeconds(15);

    /** Lookup function to supply ticketValidityPeriod property. */
    @Nonnull private Function<ProfileRequestContext,Duration> ticketValidityPeriodLookupStrategy;

    /** Whether attributes should be resolved in the course of the profile. */
    @Nonnull private Predicate<ProfileRequestContext> resolveAttributesPredicate;
    
    /** Holds default security config object to use. */
    @Nonnull private final SecurityConfiguration defaultSecurityConfiguration;

    /**
     * Creates a new configuration instance.
     *
     * @param profileId Unique profile identifier
     */
    public AbstractProtocolConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);
        
        resolveAttributesPredicate = PredicateSupport.alwaysTrue();
        ticketValidityPeriodLookupStrategy = FunctionSupport.constant(DEFAULT_TICKET_VALIDITY_PERIOD);
        final Duration fiveMins = Duration.ofMinutes(5);
        assert fiveMins!=null;
        defaultSecurityConfiguration = new BasicSecurityConfiguration(fiveMins,
                new TicketIdentifierGenerationStrategy(getDefaultTicketPrefix(), getDefaultTicketLength()));
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public SecurityConfiguration getSecurityConfiguration(
            @Nullable final ProfileRequestContext profileRequestContext) {
        final SecurityConfiguration sc = super.getSecurityConfiguration(profileRequestContext);
        return sc != null ? sc : defaultSecurityConfiguration;
    }
    
    /**
     * Get ticket validity period.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return ticket validity period
     */
    @ConfigurationSetting(name="ticketValidityPeriod")
    @Nonnull public Duration getTicketValidityPeriod(@Nullable final ProfileRequestContext profileRequestContext) {
        
        final Duration ticketTTL = ticketValidityPeriodLookupStrategy.apply(profileRequestContext);
        Constraint.isNotNull(ticketTTL, "Ticket lifetime cannot be null");
        Constraint.isFalse(ticketTTL.isNegative() || ticketTTL.isZero(), "Ticket lifetime must be greater than 0");
        return ticketTTL;
    }

    /**
     * Sets the ticket validity period.
     * 
     * @param ticketTTL ticket validity period
     */
    public void setTicketValidityPeriod(@Nonnull final Duration ticketTTL) {
        Constraint.isNotNull(ticketTTL, "Ticket lifetime cannot be null");
        Constraint.isFalse(ticketTTL.isNegative() || ticketTTL.isZero(), "Ticket lifetime must be greater than 0");

        ticketValidityPeriodLookupStrategy = FunctionSupport.constant(ticketTTL);
    }

    /**
     * Set a lookup strategy for the ticket validity period.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.3.0
     */
    public void setTicketValidityPeriodLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Duration> strategy) {
        ticketValidityPeriodLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    public boolean isResolveAttributes(@Nullable final ProfileRequestContext profileRequestContext) {
        return resolveAttributesPredicate.test(profileRequestContext);
    }

    /**
     * Set whether attributes should be resolved during the profile.
     * 
     * @param flag flag to set
     */
    public void setResolveAttributes(final boolean flag) {
        resolveAttributesPredicate = flag ? PredicateSupport.alwaysTrue() : PredicateSupport.alwaysFalse();
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
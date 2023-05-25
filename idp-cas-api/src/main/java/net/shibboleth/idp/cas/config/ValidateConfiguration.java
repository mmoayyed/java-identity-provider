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

package net.shibboleth.idp.cas.config;

import java.time.Duration;
import java.util.Comparator;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.cas.service.DefaultServiceComparator;
import net.shibboleth.idp.cas.ticket.TicketIdentifierGenerationStrategy;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;

/**
 * CAS protocol configuration. Applies to the following ticket validation URIs:
 *
 * <ul>
 *     <li><code>/proxyValidate</code></li>
 *     <li><code>/serviceValidate</code></li>
 * </ul>
 *
 * @author Marvin S. Addison
 */
public class ValidateConfiguration extends AbstractProtocolConfiguration {

    /** Ticket validation profile ID. */
    @Nonnull @NotEmpty public static final String PROFILE_ID = PROTOCOL_URI + "/serviceValidate";

    /** Ticket validation profile counter name. */
    @Nonnull @NotEmpty public static final String PROFILE_COUNTER = PROTOCOL_COUNTER + ".serviceValidate";

    /** Default ticket validity. */
    @SuppressWarnings("null")
    @Nonnull public static final Duration DEFAULT_TICKET_VALIDITY_PERIOD = Duration.ofHours(12);
    
    /** Default ticket prefix. */
    @Nonnull @NotEmpty public static final String DEFAULT_TICKET_PREFIX = "PGT";

    /** Default ticket length (random part). */
    public static final int DEFAULT_TICKET_LENGTH = 50;

    /** Lookup strategy for PGTIOU ticket ID generator. */
    @Nonnull private Function<ProfileRequestContext,IdentifierGenerationStrategy> pgtIOUGeneratorLookupStrategy;

    /** Default PGTIOU ticket ID generator. */
    @Nonnull private final IdentifierGenerationStrategy defaultPGTIOUGenerator;

    /** Lookup strategy for enforcing ticket requester matches ticket validator. */
    @Nonnull private Function<ProfileRequestContext,Comparator<String>> serviceComparatorLookupStrategy;

    /** Lookup strategy for Name of IdP attribute to use for user returned in CAS ticket validation response. */
    @Nonnull private Function<ProfileRequestContext,String> userAttributeLookupStrategy;

    /** Creates a new instance. */
    public ValidateConfiguration() {
        super(PROFILE_ID);
        
        // Ticket validity period for this configuration container applies to proxy-granting tickets
        // Default to 12H
        setTicketValidityPeriod(DEFAULT_TICKET_VALIDITY_PERIOD);
        
        userAttributeLookupStrategy = FunctionSupport.constant(null);
        serviceComparatorLookupStrategy = FunctionSupport.constant(new DefaultServiceComparator());
        
        defaultPGTIOUGenerator = new TicketIdentifierGenerationStrategy("PGTIOU", 50);
        pgtIOUGeneratorLookupStrategy = FunctionSupport.constant(defaultPGTIOUGenerator);
    }

    /**
     * Get the PGTIOU ticket ID generator.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return PGTIOU ticket ID generator
     */
    @Nonnull public IdentifierGenerationStrategy getPGTIOUGenerator(
            @Nullable final ProfileRequestContext profileRequestContext) {
        
        final IdentifierGenerationStrategy strategy = pgtIOUGeneratorLookupStrategy.apply(profileRequestContext);
        return strategy != null ? strategy : defaultPGTIOUGenerator;
    }

    /**
     * Set the PGTIOU ticket ID generator.
     *
     * @param generator ID generator
     */
    public void setPGTIOUGenerator(@Nonnull final IdentifierGenerationStrategy generator) {
        pgtIOUGeneratorLookupStrategy = FunctionSupport.constant(
                Constraint.isNotNull(generator, "Generator cannot be null"));
    }
    
    /**
     * Set the lookup strategy to use for the PGTIOU ticket ID generator.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.3.0
     */
    public void setPGTIOUGeneratorLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,IdentifierGenerationStrategy> strategy) {
        pgtIOUGeneratorLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /**
     * Get component responsible for enforcing ticket requester matches ticket validator.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return ticket requester/validator comparator
     */
    @Nonnull public Comparator<String> getServiceComparator(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return Constraint.isNotNull(
                serviceComparatorLookupStrategy.apply(profileRequestContext), "Service comparator cannot be null");
    }

    /**
     * Set component responsible for enforcing ticket requester matches ticket validator.
     * 
     * @param comparator ticket requester/validator comparator
     */
    public void setServiceComparator(@Nonnull final Comparator<String> comparator) {
        serviceComparatorLookupStrategy = FunctionSupport.constant(
                Constraint.isNotNull(comparator, "ServiceComparator cannot be null"));
    }

    /**
     * Set the lookup strategy to use for the component responsible for enforcing that the
     * ticket requester matches the ticket validator.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.3.0
     */
    public void setServiceComparatorLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Comparator<String>> strategy) {
        serviceComparatorLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /**
     * Get name of IdP attribute to use for username returned in CAS ticket validation response.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return attribute name
     */
    @Nullable public String getUserAttribute(@Nullable final ProfileRequestContext profileRequestContext) {
        return userAttributeLookupStrategy.apply(profileRequestContext);
    }

    /**
     * Set the name of IdP attribute to use for username returned in CAS ticket validation response.
     *
     * @param attribute attribute name to use
     */
    public void setUserAttribute(@Nullable final String attribute) {
        userAttributeLookupStrategy = FunctionSupport.constant(attribute);
    }
    
    /**
     * Set the lookup strategy to use for the name of the IdP attribute to use for username returned
     * in CAS ticket validation response.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.3.0
     */
    public void setUserAttributeLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        userAttributeLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty protected String getDefaultTicketPrefix() {
        return DEFAULT_TICKET_PREFIX;
    }

    /** {@inheritDoc} */
    @Override
    protected int getDefaultTicketLength() {
        return DEFAULT_TICKET_LENGTH;
    }

}
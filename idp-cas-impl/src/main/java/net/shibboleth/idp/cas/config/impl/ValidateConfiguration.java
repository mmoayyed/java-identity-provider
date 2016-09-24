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

import java.util.Comparator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;

import net.shibboleth.idp.cas.service.impl.DefaultServiceComparator;
import net.shibboleth.idp.cas.ticket.impl.TicketIdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;

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
    public static final String PROFILE_ID = PROTOCOL_URI + "/serviceValidate";

    /** Default ticket prefix. */
    public static final String DEFAULT_TICKET_PREFIX = "PGT";

    /** Default ticket length (random part). */
    public static final int DEFAULT_TICKET_LENGTH = 50;

    /** Lookup strategy for {@link #pgtIOUGenerator} property. */
    @Nullable private Function<ProfileRequestContext,IdentifierGenerationStrategy> pgtIOUGeneratorLookupStrategy;

    /** PGTIOU ticket ID generator. */
    @Nullable private IdentifierGenerationStrategy pgtIOUGenerator;

    /** Lookup strategy for {@link #serviceComparator} property. */
    @Nullable private Function<ProfileRequestContext,Comparator<String>> serviceComparatorLookupStrategy;

    /** Component responsible for enforcing ticket requester matches ticket validator. */
    @Nonnull private Comparator<String> serviceComparator;

    /** Lookup strategy for {@link #userAttribute} property. */
    @Nullable private Function<ProfileRequestContext,String> userAttributeLookupStrategy;
    
    /** Name of IdP attribute to use for user returned in CAS ticket validation response. */
    @Nullable private String userAttribute;

    /** Creates a new instance. */
    public ValidateConfiguration() {
        super(PROFILE_ID);
        
        pgtIOUGenerator = new TicketIdentifierGenerationStrategy("PGTIOU", 50);
        serviceComparator = new DefaultServiceComparator();
    }

    /**
     * Get the PGTIOU ticket ID generator.
     * 
     * @return PGTIOU ticket ID generator
     */
    @Nonnull public IdentifierGenerationStrategy getPGTIOUGenerator() {
        return Constraint.isNotNull(getIndirectProperty(pgtIOUGeneratorLookupStrategy, pgtIOUGenerator),
                "PGTIOU generator cannot be null");
    }

    /**
     * Set the PGTIOU ticket ID generator.
     *
     * @param generator ID generator
     */
    public void setPGTIOUGenerator(@Nullable final IdentifierGenerationStrategy generator) {
        pgtIOUGenerator = generator;
    }
    
    /**
     * Set the lookup strategy to use for the name of the IdP attribute to use for username returned
     * in CAS ticket validation response.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.3.0
     */
    public void setPGTIOUGeneratorLookupStrategy(
            @Nullable final Function<ProfileRequestContext,IdentifierGenerationStrategy> strategy) {
        pgtIOUGeneratorLookupStrategy = strategy;
    }

    /**
     * Get component responsible for enforcing ticket requester matches ticket validator.
     * 
     * @return ticket requester/validator comparator
     */
    @Nonnull public Comparator<String> getServiceComparator() {
        return Constraint.isNotNull(getIndirectProperty(serviceComparatorLookupStrategy, serviceComparator),
                "Service comparator cannot be null");
    }

    /**
     * Set component responsible for enforcing ticket requester matches ticket validator.
     * 
     * @param comparator ticket requester/validator comparator
     */
    public void setServiceComparator(@Nullable final Comparator<String> comparator) {
        serviceComparator = comparator;
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
            @Nullable final Function<ProfileRequestContext,Comparator<String>> strategy) {
        serviceComparatorLookupStrategy = strategy;
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

    /**
     * Get name of IdP attribute to use for username returned in CAS ticket validation response.
     * 
     * @return attribute name
     */
    @Nullable public String getUserAttribute() {
        return getIndirectProperty(userAttributeLookupStrategy, userAttribute);
    }

    /**
     * Set the name of IdP attribute to use for username returned in CAS ticket validation response.
     *
     * @param attribute attribute name to use
     */
    public void setUserAttribute(@Nullable final String attribute) {
        userAttribute = attribute;
    }
    
    /**
     * Set the lookup strategy to use for the name of the IdP attribute to use for username returned
     * in CAS ticket validation response.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.3.0
     */
    public void setUserAttributeLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        userAttributeLookupStrategy = strategy;
    }
    
}
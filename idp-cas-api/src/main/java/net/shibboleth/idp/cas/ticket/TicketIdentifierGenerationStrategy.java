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

package net.shibboleth.idp.cas.ticket;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.Positive;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.cryptacular.generator.IdGenerator;
import org.cryptacular.generator.RandomIdGenerator;

/**
 * Generates CAS protocol ticket identifiers of the form:
 *
 * <pre>
 * [PREFIX]-[SEQUENCE_PART]-[RANDOM_PART]-[SUFFIX],
 * </pre>
 *
 * where suffix is optional. By default tickets have at least 128 bits of entropy in the random part of the identifier.
 *
 * @author Marvin S. Addison
 */
public class TicketIdentifierGenerationStrategy implements IdentifierGenerationStrategy {

    /** Generator for random part of the ticket. */
    private final IdGenerator idGenerator;

    /** Ticket prefix. */
    @Nonnull
    @NotEmpty
    private String ticketPrefix;

    /** Ticket suffix. */
    @Nullable
    private String ticketSuffix;

    /** Number of characters in random part of generated ticket. */
    @Positive
    private int ticketLength;


    /**
     * Creates a new ticket ID generator.
     *
     * @param prefix Ticket ID prefix (e.g. ST, PT, PGT). MUST be a URL safe string.
     * @param randomLength Length in characters of random part of the ticket.
     */
    public TicketIdentifierGenerationStrategy(
            @Nonnull @NotEmpty @ParameterName(name="prefix") final String prefix,
            @Positive @ParameterName(name="randomLength") final int randomLength) {
        ticketLength = Constraint.isGreaterThan(0, randomLength, "Random length must be positive");
        ticketPrefix = Constraint.isNotNull(StringSupport.trimOrNull(prefix), "Prefix cannot be null or empty");
        Constraint.isTrue(isUrlSafe(ticketPrefix), "Unsupported prefix " + ticketPrefix);
        idGenerator = new RandomIdGenerator(ticketLength);
    }

    /**
     * Sets the ticket ID suffix.
     *
     * @param suffix Ticket suffix.
     */
    public void setSuffix(@Nullable final String suffix) {
        final String s = StringSupport.trimOrNull(suffix);
        if (s != null) {
            Constraint.isTrue(isUrlSafe(s), "Unsupported suffix " + s);
            ticketSuffix = s;
        }
    }

    @Override
    @Nonnull
    public String generateIdentifier() {
        final StringBuilder builder = new StringBuilder(ticketLength * 2);
        builder.append(ticketPrefix).append('-');
        builder.append(System.currentTimeMillis()).append('-');
        builder.append(idGenerator.generate());
        if (ticketSuffix != null) {
            builder.append('-').append(ticketSuffix);
        }
        return builder.toString();
    }

    @Override
    @Nonnull
    public String generateIdentifier(final boolean xmlSafe) {
        return generateIdentifier();
    }

    /**
     * Whether the URL is safe.
     * 
     * @param s URL
     * @return whether the URL is safe
     */
    private static boolean isUrlSafe(final String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.US_ASCII.name()).equals(s);
        } catch (final Exception e) {
            return false;
        }
    }
}

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

import net.shibboleth.idp.cas.ticket.TicketIdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;

import javax.annotation.Nonnull;

/**
 * CAS proxy-granting ticket configuration modeled as an IdP profile.
 *
 * @author Marvin S. Addison
 */
public class ProxyGrantingTicketConfiguration extends AbstractTicketConfiguration {

    /** Proxy ticket profile URI. */
    public static final String PROFILE_ID = PROTOCOL_URI + "/pgt";

    /** Default ticket prefix. */
    public static final String DEFAULT_TICKET_PREFIX = "PGT";

    /** Default ticket length (random part). */
    public static final int DEFAULT_TICKET_LENGTH = 50;


    /** PGTIOU ticket ID generator. */
    @Nonnull
    private IdentifierGenerationStrategy pgtIOUGenerator = new TicketIdentifierGenerationStrategy("PGTIOU", 50);


    /** Creates a new instance. */
    public ProxyGrantingTicketConfiguration() {
        super(PROFILE_ID);
    }

    /**
     * @return PGTIOU ticket ID generator.
     */
    @Nonnull
    public IdentifierGenerationStrategy getPGTIOUGenerator() {
        return pgtIOUGenerator;
    }

    /**
     * Sets the PGTIOU ticket ID generator.
     *
     * @param generator ID generator.
     */
    public void setPGTIOUGenerator(@Nonnull IdentifierGenerationStrategy generator) {
        this.pgtIOUGenerator = Constraint.isNotNull(generator, "PGTIOU generator cannot be null");
    }

    @Override
    @Nonnull
    protected String getDefaultTicketPrefix() {
        return DEFAULT_TICKET_PREFIX;
    }

    @Override
    @Nonnull
    protected int getDefaultTicketLength() {
        return DEFAULT_TICKET_LENGTH;
    }
}

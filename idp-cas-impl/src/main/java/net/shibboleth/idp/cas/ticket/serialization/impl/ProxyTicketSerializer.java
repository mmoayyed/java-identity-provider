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

package net.shibboleth.idp.cas.ticket.serialization.impl;

import javax.annotation.Nonnull;
import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;

import net.shibboleth.idp.cas.ticket.ProxyTicket;
import org.joda.time.Instant;

/**
 * Proxy ticket storage serializer.
 *
 * @author Marvin S. Addison
 */
public class ProxyTicketSerializer extends AbstractTicketSerializer<ProxyTicket> {

    /** PGT ID field name. */
    private static final String PGTID_FIELD = "pgt";


    @Override
    protected void serializeInternal(@Nonnull final JsonGenerator generator, @Nonnull final ProxyTicket ticket) {
        generator.write(PGTID_FIELD, ticket.getPgtId());
    }

    @Override
    protected ProxyTicket createTicket(
            @Nonnull final JsonObject o,
            @Nonnull final String id,
            @Nonnull final String service,
            @Nonnull final Instant expiry) {
        return new ProxyTicket(id, service, expiry, o.getString(PGTID_FIELD));
    }
}

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

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;

import net.shibboleth.idp.cas.ticket.ServiceTicket;

/**
 * Serializes service tickets in simple field-delimited form.
 *
 * @author Marvin S. Addison
 */
public class ServiceTicketSerializer extends AbstractTicketSerializer<ServiceTicket> {

    /** Renew field name. */
    private static final String RENEW_FIELD = "r";


    @Override
    protected void serializeInternal(@Nonnull final JsonGenerator generator, @Nonnull final ServiceTicket ticket) {
        generator.write(RENEW_FIELD, ticket.isRenew());
    }

    @Override
    protected ServiceTicket createTicket(
            @Nonnull final JsonObject o,
            @Nonnull final String id,
            @Nonnull final String service,
            @Nonnull final Instant expiry) {
        return new ServiceTicket(id, service, expiry, o.getBoolean(RENEW_FIELD));
    }
}

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

package net.shibboleth.idp.cas.session.impl;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;

import net.shibboleth.idp.session.AbstractSPSessionSerializer;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;

/**
 * JSON serializer for {@link CASSPSession} class.
 *
 * @author Marvin S. Addison
 */
public class CASSPSessionSerializer extends AbstractSPSessionSerializer {

    /** Field name of CAS ticket. */
    @Nonnull @NotEmpty private static final String TICKET_FIELD = "st";

    /** Field name of CAS service URL. */
    @Nonnull @NotEmpty private static final String SERVICE_URL_FIELD = "surl";

    /**
     * Constructor.
     *
     * @param offset time to subtract from record expiration to establish session expiration value
     */
    public CASSPSessionSerializer(@Nonnull @ParameterName(name="offset") final Duration offset) {
        super(offset);
    }

    /** {@inheritDoc} */
    @Override
    protected void doSerializeAdditional(@Nonnull final SPSession instance, @Nonnull final JsonGenerator generator) {
        if (!(instance instanceof CASSPSession)) {
            throw new IllegalArgumentException("Expected instance of CASSPSession but got " + instance);
        }
        
        final CASSPSession casSession = (CASSPSession) instance;
        generator.write(TICKET_FIELD, casSession.getTicketId());
        if (!casSession.getServiceURL().equals(casSession.getId())) {
            generator.write(SERVICE_URL_FIELD, casSession.getServiceURL());
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected SPSession doDeserialize(@Nonnull final JsonObject obj, @Nonnull @NotEmpty final String id,
            @Nonnull final Instant creation, @Nonnull final Instant expiration) throws IOException {
        
        final String ticketField = Constraint.isNotNull(obj.getString(TICKET_FIELD), "No ticket field");
        
        // Default the service URL to the session ID if absent.
        final String serviceURL = obj.getString(SERVICE_URL_FIELD, id);
        assert serviceURL != null;
        
        return new CASSPSession(id, creation, expiration, ticketField, serviceURL);
    }
    
}
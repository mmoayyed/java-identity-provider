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

package net.shibboleth.idp.cas.audit.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.cas.protocol.ProtocolContext;
import net.shibboleth.idp.cas.protocol.ProxyTicketRequest;
import net.shibboleth.idp.cas.protocol.ServiceTicketRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.shared.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

/**
 * Looks up the service URL from the CAS protocol request.
 *
 * @author Marvin S. Addison
 */
@SuppressWarnings("rawtypes")
public class ServiceLookupFunction implements Function<ProfileRequestContext, String> {
    
    /** Lookup strategy for protocol context. */
    @Nonnull private final Function<ProfileRequestContext,ProtocolContext> protocolContextFunction;

    /** Constructor. */
    public ServiceLookupFunction() {
        this(new ChildContextLookup<>(ProtocolContext.class));
    }

    /**
     * Constructor.
     *
     * @param protocolLookup lookup strategy for protocol context
     */
    public ServiceLookupFunction(@Nonnull final Function<ProfileRequestContext,ProtocolContext> protocolLookup) {
        protocolContextFunction = Constraint.isNotNull(protocolLookup, "ProtocolContext lookup cannot be null");
    }

    /**
     * Get service URL.
     * 
     * {@inheritDoc}
     */
    @Nullable public String apply(@Nullable final ProfileRequestContext input) {
        final ProtocolContext<?,?> protocolContext = protocolContextFunction.apply(input);
        if (protocolContext == null || protocolContext.getRequest() ==  null) {
            return null;
        }
        final Object request = protocolContext.getRequest();
        final String service;
        if (request instanceof ServiceTicketRequest req) {
            service = req.getService();
        } else if (request instanceof ProxyTicketRequest req) {
            service = req.getTargetService();
        } else if (request instanceof TicketValidationRequest req) {
            service = req.getService();
        } else {
            service = null;
        }
        return service;
    }

}
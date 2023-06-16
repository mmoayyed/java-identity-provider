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

package net.shibboleth.idp.cas.protocol;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.shared.logic.Constraint;

/**
 * Container for proxy ticket response parameters returned from <code>/proxy</code> URI.
 *
 * @author Marvin S. Addison
 */
public class ProxyTicketResponse extends AbstractProtocolResponse {
    /** Proxy ticket. */
    @Nullable private String proxyTicket;

    /** Default no-arg constructor. */
    public ProxyTicketResponse() {
    }

    /**
     * Creates a new instance with given parameters.
     *
     * @param pt Proxy ticket ID.
     */
    public ProxyTicketResponse(@Nonnull final String pt) {
        proxyTicket = Constraint.isNotNull(pt, "PT cannot be null");
    }

    /**
     * Get the proxy ticket ID.
     * 
     * @return proxy ticket ID
     */
    @Nullable public String getPt() {
        return proxyTicket;
    }

}
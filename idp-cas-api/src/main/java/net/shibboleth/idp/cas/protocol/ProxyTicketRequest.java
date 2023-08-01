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

package net.shibboleth.idp.cas.protocol;

import javax.annotation.Nonnull;

import net.shibboleth.shared.logic.Constraint;

/**
 * Container for proxy ticket request parameters provided to <code>/proxy</code> URI.
 *
 * @author Marvin S. Addison
 */
public class ProxyTicketRequest {
    /** Proxy-granting ticket ID. */
    @Nonnull private final String proxyGrantingTicketId;

    /** Target service to which proxy ticket will be delivered. */
    @Nonnull private final String proxyTargetService;


    /**
     * Creates a new proxy ticket request with given parameters.
     *
     * @param pgt Non-null proxy-granting ticket ID.
     * @param targetService Non-null
     */
    public ProxyTicketRequest(@Nonnull final String pgt, @Nonnull final String targetService) {
        proxyGrantingTicketId = Constraint.isNotNull(pgt, "PGT cannot be null");
        proxyTargetService = Constraint.isNotNull(targetService, "TargetService cannot be null");
    }

    /**
     * Get the proxy-granting ticket ID.
     * 
     * @return proxy-granting ticket ID
     */
    @Nonnull public String getPgt() {
        return proxyGrantingTicketId;
    }

    /**
     * Get the target service to which proxy ticket will be delivered.
     * 
     * @return target service to which proxy ticket will be delivered
     */
    @Nonnull public String getTargetService() {
        return proxyTargetService;
    }

}
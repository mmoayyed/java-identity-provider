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

import net.shibboleth.shared.primitive.StringSupport;

/**
 * Ticket validation request message.
 *
 * @author Marvin S. Addison
 */
public class TicketValidationRequest extends ServiceTicketResponse {

    /** CAS protocol renew flag. */
    private boolean renew;

    /** Proxy-granting ticket validation URL. */
    @Nullable private String pgtUrl;

    /**
     * Creates a CAS ticket validation request message.
     *
     * @param service Service to which ticket was issued.
     * @param ticket Ticket to validate.
     */
    public TicketValidationRequest(@Nonnull final String service, @Nonnull final String ticket) {
        super(service, ticket);
    }

    /**
     * Get whether to require fresh subject interaction to succeed.
     * 
     * @return whether subject interaction must occur
     */
    public boolean isRenew() {
        return renew;
    }

    /**
     * Set whether to require fresh subject interaction to succeed.
     * 
     * @param force whether subject interaction must occur
     */
    public void setRenew(final boolean force) {
        renew = force;
    }

    /**
     * Get the proxy-granting ticket validation URL.
     * 
     * @return proxy-granting ticket validation URL
     */
    @Nullable public String getPgtUrl() {
        return pgtUrl;
    }

    /**
     * Set the proxy-granting ticket validation URL.
     * 
     * @param url proxy-granting ticket validation URL
     */
    public void setPgtUrl(@Nullable final String url) {
        pgtUrl = StringSupport.trimOrNull(url);
    }
}
